package roomiehub.vivienda.adapters.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import com.jayway.jsonpath.JsonPath;

/** CU-02 a CU-08 por HTTP contra PostgreSQL real, con RLS activo: {@code ./mvnw test -Ddb=true} */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named = "db", matches = "true")
class SalasIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate tx;

    record Cuenta(long id, String access) {}

    final String ip = "10.88.%d.%d".formatted((int) (Math.random() * 250), (int) (Math.random() * 250));

    @Test
    void cicloDeVidaDeUnaSala() throws Exception {
        Cuenta ana = registrar("Ana"), beto = registrar("Beto"), carla = registrar("Carla");

        // ─── CU-02: Ana crea la sala y queda como Administradora ───
        String creada = como(ana, post("/salas"), "{\"nombre\":\"Depa\",\"zonaHoraria\":\"America/Mexico_City\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.miRol").value("ADMIN"))
                .andReturn().getResponse().getContentAsString();
        long sala = ((Number) JsonPath.read(creada, "$.id")).longValue();
        String codigo = JsonPath.read(creada, "$.codigoInvitacion");
        assertEquals(8, codigo.length());
        como(ana, get("/salas"), null).andExpect(jsonPath("$[0].id").value(sala));

        // RN-18: Ana ya tiene 12 puntos y hay una tarea de valor 8
        sql(ana, () -> {
            jdbc.update("INSERT INTO tareas (sala_id, nombre, valor, frecuencia, fecha_inicio) "
                    + "VALUES (?, 'Lavar baños', 8, 'SEMANAL', '2026-11-02')", sala);
            return jdbc.update("INSERT INTO movimientos_puntos (usuario_id, sala_id, puntos, tipo) "
                    + "VALUES (?, ?, 12, 'INICIAL')", ana.id(), sala);
        });

        // ─── CU-03: Beto y Carla se unen con el código (en minúsculas también sirve) ───
        unirse(beto, codigo.toLowerCase()).andExpect(status().isOk()).andExpect(jsonPath("$.miRol").value("HABITANTE"));
        assertEquals(20, historico(beto, sala), "promedio 12 + tarea 8 = 20");
        unirse(carla, codigo).andExpect(status().isOk());
        assertEquals(24, historico(carla, sala), "promedio (12 + 20) / 2 + 8 = 24");

        unirse(beto, codigo).andExpect(status().isConflict());
        unirse(beto, "ZZZZ2222").andExpect(status().isNotFound());
        // Errores del cliente que detecta Spring: 4xx, no 500
        como(beto, get("/salas/abc"), null).andExpect(status().isBadRequest());
        como(beto, get("/no-existe"), null).andExpect(status().isNotFound());
        como(beto, get("/salas/{id}", sala), null)
                .andExpect(jsonPath("$.codigoInvitacion").value(codigo))
                .andExpect(jsonPath("$.miembros.length()").value(3));

        // ─── CU-06: solo la Administradora expulsa; el expulsado vuelve con ajuste de puntos ───
        como(beto, delete("/salas/{s}/miembros/{u}", sala, carla.id()), null).andExpect(status().isForbidden());
        como(ana, delete("/salas/{s}/miembros/{u}", sala, beto.id()), null).andExpect(status().isNoContent());
        como(beto, get("/salas/{id}", sala), null).andExpect(status().isNotFound());
        unirse(beto, codigo).andExpect(status().isOk());
        assertEquals(26, historico(beto, sala), "ajuste a promedio (12 + 24) / 2 + 8 = 26");

        // ─── RN-01: como máximo 6 personas ───
        for (String nombre : new String[] {"Diego", "Elena", "Fer"}) {
            unirse(registrar(nombre), codigo).andExpect(status().isOk());
        }
        Cuenta gabi = registrar("Gabi");
        unirse(gabi, codigo).andExpect(status().isConflict());

        // ─── RN-02: como máximo 2 membresías ───
        for (int i = 0; i < 2; i++) {
            como(gabi, post("/salas"), "{\"nombre\":\"Sala %d\",\"zonaHoraria\":\"America/Mexico_City\"}".formatted(i))
                    .andExpect(status().isCreated());
        }
        como(gabi, post("/salas"), "{\"nombre\":\"Tercera\",\"zonaHoraria\":\"America/Mexico_City\"}")
                .andExpect(status().isConflict());

        // ─── CU-07: Ana transfiere el rol a Carla ───
        como(ana, post("/salas/{id}/administrador", sala), "{\"usuarioId\":%d}".formatted(carla.id()))
                .andExpect(status().isNoContent());
        como(ana, delete("/salas/{s}/miembros/{u}", sala, beto.id()), null).andExpect(status().isForbidden());
        como(carla, get("/salas/{id}", sala), null).andExpect(jsonPath("$.miRol").value("ADMIN"));

        // ─── CU-08: desactivada queda en solo lectura, pero se puede salir; luego se reactiva ───
        como(beto, post("/salas/{id}/desactivar", sala), null).andExpect(status().isForbidden());
        como(carla, post("/salas/{id}/desactivar", sala), null).andExpect(status().isNoContent());
        como(carla, get("/salas/{id}", sala), null).andExpect(jsonPath("$.estado").value("DESACTIVADA"));
        como(carla, delete("/salas/{s}/miembros/{u}", sala, beto.id()), null).andExpect(status().isForbidden());
        como(beto, delete("/salas/{id}/membresia", sala), null).andExpect(status().isNoContent());
        como(carla, post("/salas/{id}/reactivar", sala), null).andExpect(status().isNoContent());
        como(carla, get("/salas/{id}", sala), null)
                .andExpect(jsonPath("$.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.miembros.length()").value(5));
    }

    @Test
    void datosInvalidos() throws Exception {
        Cuenta ana = registrar("Ana");
        como(ana, post("/salas"), "{\"nombre\":\"Depa\",\"zonaHoraria\":\"Marte/Olympus\"}")
                .andExpect(status().isBadRequest());
        como(ana, post("/salas"), "{\"nombre\":\"\",\"zonaHoraria\":\"America/Mexico_City\"}")
                .andExpect(status().isBadRequest());
        mvc.perform(get("/salas")).andExpect(status().isUnauthorized());
    }

    ResultActions unirse(Cuenta c, String codigo) throws Exception {
        return como(c, post("/salas/unirse"), "{\"codigo\":\"%s\"}".formatted(codigo));
    }

    ResultActions como(Cuenta c, MockHttpServletRequestBuilder req, String cuerpo) throws Exception {
        req.header("Authorization", "Bearer " + c.access());
        if (cuerpo != null) {
            req.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        }
        return mvc.perform(req);
    }

    Cuenta registrar(String nombre) throws Exception {
        String cuerpo = mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s@test.local\",\"contrasena\":\"secreta123\",\"nombre\":\"%s\"}"
                                .formatted(UUID.randomUUID(), nombre))
                        .with(r -> { r.setRemoteAddr(ip); return r; }))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return new Cuenta(((Number) JsonPath.read(cuerpo, "$.usuario.id")).longValue(), JsonPath.read(cuerpo, "$.accessToken"));
    }

    long historico(Cuenta c, long sala) {
        return sql(c, () -> jdbc.queryForObject(
                "SELECT coalesce(sum(puntos), 0) FROM movimientos_puntos WHERE usuario_id = ? AND sala_id = ?",
                Long.class, c.id(), sala));
    }

    <T> T sql(Cuenta c, Supplier<T> accion) {
        return tx.execute(s -> {
            jdbc.queryForObject("SELECT set_config('app.usuario_id', ?, true)", String.class, String.valueOf(c.id()));
            return accion.get();
        });
    }
}
