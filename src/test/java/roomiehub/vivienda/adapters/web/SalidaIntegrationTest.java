package roomiehub.vivienda.adapters.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

import com.jayway.jsonpath.JsonPath;

import roomiehub.vivienda.application.port.AlmacenamientoImagenesPort;

/**
 * CU-05 y CU-31 contra PostgreSQL real, con RLS activo: {@code ./mvnw test -Ddb=true}
 *
 * Ana (Administradora), Beto y Carla comparten Internet de $100 MXN; Beto ya confirmó el pago de Carla.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named = "db", matches = "true")
class SalidaIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate tx;
    @MockitoSpyBean AlmacenamientoImagenesPort almacenamiento;

    record Cuenta(long id, String access, String refresh, String correo) {}

    @Test
    void anaSaleCarlaBorraSuCuentaYBetoCierraLaSala() throws Exception {
        Cuenta ana = registrar("Ana"), beto = registrar("Beto"), carla = registrar("Carla");
        String codigo = UUID.randomUUID().toString().substring(0, 8);

        long sala = como(ana.id(), () -> {
            long id = jdbc.queryForObject("""
                    INSERT INTO salas (nombre, creador_id, zona_horaria, codigo_invitacion)
                    VALUES ('Depa', ?, 'America/Mexico_City', ?) RETURNING id""", Long.class, ana.id(), codigo);
            jdbc.update("INSERT INTO usuario_sala (usuario_id, sala_id, rol, unido_en) VALUES (?, ?, 'ADMIN', '2026-01-01')",
                    ana.id(), id);
            return id;
        });
        como(beto.id(), () -> jdbc.update("INSERT INTO usuario_sala (usuario_id, sala_id, rol, unido_en) "
                + "VALUES (?, ?, 'HABITANTE', '2026-02-01')", beto.id(), sala));
        como(carla.id(), () -> jdbc.update("INSERT INTO usuario_sala (usuario_id, sala_id, rol, unido_en) "
                + "VALUES (?, ?, 'HABITANTE', '2026-03-01')", carla.id(), sala));

        long[] deudas = como(ana.id(), () -> {
            long gasto = jdbc.queryForObject("""
                    INSERT INTO servicios_pago (sala_id, concepto, monto, moneda, frecuencia, fecha_inicio, generar_desde)
                    VALUES (?, 'Internet', 100.00, 'MXN', 'MENSUAL', '2026-11-01', '2026-11-01') RETURNING id""",
                    Long.class, sala);
            long[] ids = new long[3];
            long[] personas = {ana.id(), beto.id(), carla.id()};
            String[] montos = {"33.34", "33.33", "33.33"};
            for (int i = 0; i < 3; i++) {
                jdbc.update("INSERT INTO servicio_participantes (servicio_id, sala_id, usuario_id, orden) VALUES (?, ?, ?, ?)",
                        gasto, sala, personas[i], i);
                ids[i] = jdbc.queryForObject("""
                        INSERT INTO deudas_usuarios (servicio_id, sala_id, periodo, deudor_id, monto)
                        VALUES (?, ?, '2026-11-01', ?, ?::numeric) RETURNING id""", Long.class,
                        gasto, sala, personas[i], montos[i]);
            }
            jdbc.update("INSERT INTO confirmaciones_pago (deuda_id, sala_id, confirmador_id) VALUES (?, ?, ?)",
                    ids[2], sala, beto.id());
            long tarea = jdbc.queryForObject("""
                    INSERT INTO tareas (sala_id, nombre, valor, frecuencia, fecha_inicio)
                    VALUES (?, 'Lavar baños', 8, 'SEMANAL', '2026-11-02') RETURNING id""", Long.class, sala);
            jdbc.update("""
                    INSERT INTO tarea_asignaciones (tarea_id, sala_id, usuario_id, periodo_inicio, periodo_fin, valor_al_asignar)
                    VALUES (?, ?, ?, now(), now() + interval '7 days', 8)""", tarea, sala, ana.id());
            return ids;
        });

        // ─── Ana, la Administradora, abandona la sala ───
        abandonar(ana, sala).andExpect(status().isNoContent());

        assertEquals(Map.of("ADMIN", 1L, "HABITANTE", 1L), como(beto.id(), () -> Map.of(
                "ADMIN", contar("SELECT count(*) FROM usuario_sala WHERE sala_id = ? AND activo AND rol = 'ADMIN' AND usuario_id = ?", sala, beto.id()),
                "HABITANTE", contar("SELECT count(*) FROM usuario_sala WHERE sala_id = ? AND activo AND rol = 'HABITANTE'", sala))),
                "RN-06: Beto, el más antiguo, es el nuevo Administrador");
        assertEquals(1L, como(beto.id(), () -> contar(
                "SELECT count(*) FROM tarea_asignaciones WHERE sala_id = ? AND usuario_id IS NULL", sala)),
                "RN-08: la tarea de Ana quedó sin asignar");
        assertEquals("REPARTIDA", estado(beto, deudas[0]));
        assertEquals(List.of("16.67", "16.67"), como(beto.id(), () -> jdbc.queryForList(
                "SELECT monto::text FROM deudas_usuarios WHERE deuda_origen_id = ? ORDER BY deudor_id", String.class, deudas[0])));
        assertEquals("APROBADA", estado(beto, deudas[2]), "solo faltaba Beto, y ya había confirmado a Carla");
        assertEquals("PENDIENTE", estado(beto, deudas[1]), "a Beto le falta la confirmación de Carla");
        assertEquals(0L, como(ana.id(), () -> contar("SELECT count(*) FROM salas WHERE id = ?", sala)),
                "Ana ya no ve la sala");
        abandonar(ana, sala).andExpect(status().isForbidden());

        // ─── Carla elimina su cuenta (CU-31) ───
        mvc.perform(delete("/usuarios/yo").header("Authorization", "Bearer " + carla.access()))
                .andExpect(status().isNoContent());

        assertEquals("Usuario eliminado", jdbc.queryForObject("SELECT nombre FROM usuarios WHERE id = ?", String.class, carla.id()));
        assertEquals(0L, contar("SELECT count(*) FROM usuarios WHERE id = ? AND email IS NOT NULL", carla.id()));
        assertEquals("APROBADA", estado(beto, deudas[2]), "RN-37: su historial se conserva");
        assertEquals(List.of("16.67"), como(beto.id(), () -> jdbc.queryForList("""
                SELECT d.monto::text FROM deudas_usuarios d JOIN deudas_usuarios o ON d.deuda_origen_id = o.id
                WHERE o.deudor_id = ? AND o.deuda_origen_id IS NOT NULL""", String.class, carla.id())),
                "la parte que Carla había recibido de Ana pasa toda a Beto");
        json(post("/auth/login"), "{\"email\":\"%s\",\"contrasena\":\"secreta123\"}".formatted(carla.correo()))
                .andExpect(status().isUnauthorized());
        json(post("/auth/refresh"), "{\"refreshToken\":\"%s\"}".formatted(carla.refresh()))
                .andExpect(status().isUnauthorized());

        // ─── Beto, el último, sale: la sala se borra (RN-10) ───
        abandonar(beto, sala).andExpect(status().isNoContent());
        assertEquals(0L, contar("SELECT count(*) FROM buscar_sala_por_codigo(?)", codigo));
        verify(almacenamiento).borrarCarpeta("roomiehub/salas/" + sala); // y sus imágenes con ella
    }

    ResultActions abandonar(Cuenta c, long sala) throws Exception {
        return mvc.perform(delete("/salas/{id}/membresia", sala).header("Authorization", "Bearer " + c.access()));
    }

    Cuenta registrar(String nombre) throws Exception {
        String correo = UUID.randomUUID() + "@test.local";
        String cuerpo = json(post("/auth/registro"),
                "{\"email\":\"%s\",\"contrasena\":\"secreta123\",\"nombre\":\"%s\"}".formatted(correo, nombre))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return new Cuenta(((Number) JsonPath.read(cuerpo, "$.usuario.id")).longValue(),
                JsonPath.read(cuerpo, "$.accessToken"), JsonPath.read(cuerpo, "$.refreshToken"), correo);
    }

    final String ip = "10.77.%d.%d".formatted((int) (Math.random() * 250), (int) (Math.random() * 250));

    ResultActions json(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req, String cuerpo)
            throws Exception {
        return mvc.perform(req.contentType(MediaType.APPLICATION_JSON).content(cuerpo)
                .with(r -> { r.setRemoteAddr(ip); return r; }));
    }

    String estado(Cuenta quien, long deuda) {
        return como(quien.id(), () -> jdbc.queryForObject("SELECT estado FROM deudas_usuarios WHERE id = ?", String.class, deuda));
    }

    long contar(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    <T> T como(long usuario, Supplier<T> accion) {
        return tx.execute(s -> {
            jdbc.queryForObject("SELECT set_config('app.usuario_id', ?, true)", String.class, String.valueOf(usuario));
            return accion.get();
        });
    }
}
