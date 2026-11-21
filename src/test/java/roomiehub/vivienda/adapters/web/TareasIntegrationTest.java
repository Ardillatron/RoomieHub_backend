package roomiehub.vivienda.adapters.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import com.jayway.jsonpath.JsonPath;

/**
 * CU-16 a CU-19 por HTTP y CU-23/CU-24 por el endpoint del disparador externo, que procesa cada sala con
 * el rol app_scheduler: {@code ./mvnw test -Ddb=true}
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named = "db", matches = "true")
class TareasIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate tx;

    record Cuenta(long id, String access) {}

    final String ip = "10.99.%d.%d".formatted((int) (Math.random() * 250), (int) (Math.random() * 250));
    final String hoy = LocalDate.now(ZoneId.of("America/Mexico_City")).toString();

    @Test
    void tareasDeUnaSala() throws Exception {
        Cuenta ana = registrar("Ana"), beto = registrar("Beto"), carla = registrar("Carla");
        long sala = id(como(ana, post("/salas"), "{\"nombre\":\"Depa\",\"zonaHoraria\":\"America/Mexico_City\"}"));
        String codigo = JsonPath.read(como(ana, get("/salas/{id}", sala), null).andReturn().getResponse()
                .getContentAsString(), "$.codigoInvitacion");
        for (Cuenta c : List.of(beto, carla)) {
            como(c, post("/salas/unirse"), "{\"codigo\":\"%s\"}".formatted(codigo)).andExpect(status().isOk());
        }

        // ─── CU-16: solo la Administradora crea tareas ───
        crearTarea(beto, sala, "Barrer", 5, "P7D").andExpect(status().isForbidden());
        long banos = id(crearTarea(ana, sala, "Lavar baños", 8, "P7D").andExpect(status().isCreated()));
        long barrer = id(crearTarea(ana, sala, "Barrer", 5, "P7D"));
        long basura = id(crearTarea(ana, sala, "Sacar basura", 3, "P1D"));
        crearTarea(ana, sala, "Imposible", 11, "P7D").andExpect(status().isBadRequest());
        crearTarea(ana, sala, "Cada 400 días", 5, "P400D").andExpect(status().isBadRequest());
        como(carla, get("/salas/{id}/tareas", sala), null).andExpect(jsonPath("$.length()").value(3));

        // ─── CU-23: el disparador externo rolea; repetirlo no duplica nada ───
        jobs("secreto-de-prueba").andExpect(status().isNoContent());
        jobs("secreto-de-prueba").andExpect(status().isNoContent());
        List<Map<String, Object>> asignadas = asignaciones(ana, sala);
        assertEquals(3, asignadas.size());
        assertTrue(asignadas.stream().allMatch(a -> a.get("usuarioId") != null && "ASIGNADA".equals(a.get("estado"))));
        assertNotEquals(de(asignadas, banos).get("usuarioId"), de(asignadas, barrer).get("usuarioId"),
                "con todos empatados, las dos semanales van a personas distintas");

        // ─── CU-17: solo el responsable marca, una vez ───
        Map<String, Object> deBanos = de(asignadas, banos);
        long responsable = ((Number) deBanos.get("usuarioId")).longValue();
        Cuenta quien = List.of(ana, beto, carla).stream().filter(c -> c.id() == responsable).findFirst().orElseThrow();
        Cuenta otro = List.of(ana, beto, carla).stream().filter(c -> c.id() != responsable).findFirst().orElseThrow();
        long asignacionBanos = ((Number) deBanos.get("id")).longValue();
        como(otro, post("/asignaciones/{id}/completar", asignacionBanos), null).andExpect(status().isForbidden());
        como(quien, post("/asignaciones/{id}/completar", asignacionBanos), null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("COMPLETADA"));
        como(quien, post("/asignaciones/{id}/completar", asignacionBanos), null).andExpect(status().isConflict());

        // ─── CU-18: balance visible e histórico ───
        como(otro, get("/salas/{id}/balance", sala), null)
                .andExpect(jsonPath("$[?(@.usuarioId == %d)].visible".formatted(quien.id())).value(8))
                .andExpect(jsonPath("$[?(@.usuarioId == %d)].historico".formatted(quien.id())).value(8));

        // ─── RN-12: editar el valor no cambia la asignación ya creada ───
        como(ana, put("/salas/{s}/tareas/{t}", sala, banos),
                "{\"nombre\":\"Lavar baños\",\"valor\":9,\"frecuencia\":\"P7D\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.valor").value(9));
        assertEquals(8, de(asignaciones(ana, sala), banos).get("valor"));

        // ─── Cambiar la frecuencia: la nueva empieza cuando cierra la semana en curso, sin empalmarse ───
        String finDeSemana = LocalDate.parse(hoy).plusDays(7).toString();
        como(ana, put("/salas/{s}/tareas/{t}", sala, barrer),
                "{\"nombre\":\"Barrer\",\"valor\":5,\"frecuencia\":\"P1D\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frecuencia").value("P1D"))
                .andExpect(jsonPath("$.fechaInicio").value(finDeSemana));
        jobs("secreto-de-prueba").andExpect(status().isNoContent());
        assertEquals(1, asignaciones(ana, sala).stream()
                .filter(a -> ((Number) a.get("tareaId")).longValue() == barrer).count(),
                "sigue solo la asignación semanal");
        como(ana, put("/salas/{s}/tareas/{t}", sala, barrer), "{\"nombre\":\"Barrer\",\"valor\":5}")
                .andExpect(status().isBadRequest());

        // ─── CU-24 y RN-16: 3 horas de gracia tras el cierre; después vence y resta ⌈5 / 2⌉ = 3 ───
        long vencidaDeCarla = insertarAsignacion(ana, barrer, sala, carla.id(), "8 days", "4 hours");
        long enGraciaDeBeto = insertarAsignacion(ana, barrer, sala, beto.id(), "9 days", "2 hours");
        como(beto, post("/asignaciones/{id}/completar", enGraciaDeBeto), null).andExpect(status().isOk());
        como(carla, post("/asignaciones/{id}/completar", vencidaDeCarla), null).andExpect(status().isConflict());
        jobs("secreto-de-prueba").andExpect(status().isNoContent());
        assertEquals("VENCIDA", sql(ana, () -> jdbc.queryForObject(
                "SELECT estado FROM tarea_asignaciones WHERE id = ?", String.class, vencidaDeCarla)));
        assertEquals(-3L, sql(ana, () -> jdbc.queryForObject(
                "SELECT sum(puntos) FROM movimientos_puntos WHERE usuario_id = ? AND tipo = 'PENALIZACION'",
                Long.class, carla.id())));

        // ─── CU-16: eliminar quita sus asignaciones pendientes ───
        como(ana, delete("/salas/{s}/tareas/{t}", sala, basura), null).andExpect(status().isNoContent());
        como(ana, get("/salas/{id}/tareas", sala), null).andExpect(jsonPath("$.length()").value(2));
        assertTrue(asignaciones(ana, sala).stream().noneMatch(a -> ((Number) a.get("tareaId")).longValue() == basura
                && "ASIGNADA".equals(a.get("estado"))));

        // ─── CU-19: lo de Beto queda sin asignar al salir y la Administradora lo reparte ───
        long abiertaDeBeto = insertarAsignacion(ana, barrer, sala, beto.id(), "1 hour", "-5 hours");
        como(beto, delete("/salas/{id}/membresia", sala), null).andExpect(status().isNoContent());
        como(carla, post("/salas/{id}/asignaciones/reasignar", sala), null).andExpect(status().isForbidden());
        como(ana, post("/salas/{id}/asignaciones/reasignar", sala), null).andExpect(status().isOk());
        Object nuevoResponsable = asignaciones(ana, sala).stream()
                .filter(a -> ((Number) a.get("id")).longValue() == abiertaDeBeto)
                .findFirst().orElseThrow().get("usuarioId");
        assertTrue(List.of(ana.id(), carla.id()).contains(((Number) nuevoResponsable).longValue()));
    }

    @Test
    void elDisparadorExigeElSecreto() throws Exception {
        mvc.perform(post("/internal/jobs/ejecutar")).andExpect(status().isForbidden());
        jobs("otro").andExpect(status().isForbidden());
    }

    // ─── Ayudantes ───

    ResultActions crearTarea(Cuenta c, long sala, String nombre, int valor, String frecuencia) throws Exception {
        return como(c, post("/salas/{id}/tareas", sala),
                "{\"nombre\":\"%s\",\"valor\":%d,\"frecuencia\":\"%s\",\"fechaInicio\":\"%s\"}"
                        .formatted(nombre, valor, frecuencia, hoy));
    }

    /** Una asignación que empezó hace {@code inicio} y terminó hace {@code fin} (negativo = termina en el futuro). */
    long insertarAsignacion(Cuenta c, long tarea, long sala, long usuario, String inicio, String fin) {
        return sql(c, () -> jdbc.queryForObject("""
                INSERT INTO tarea_asignaciones (tarea_id, sala_id, usuario_id, periodo_inicio, periodo_fin, valor_al_asignar)
                VALUES (?, ?, ?, now() - ?::interval, now() - ?::interval, 5) RETURNING id""",
                Long.class, tarea, sala, usuario, inicio, fin));
    }

    List<Map<String, Object>> asignaciones(Cuenta c, long sala) throws Exception {
        return JsonPath.read(como(c, get("/salas/{id}/asignaciones", sala), null)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$");
    }

    /** La asignación del periodo actual de esa tarea (las vencidas se ignoran). */
    static Map<String, Object> de(List<Map<String, Object>> asignaciones, long tareaId) {
        return asignaciones.stream()
                .filter(a -> ((Number) a.get("tareaId")).longValue() == tareaId && !"VENCIDA".equals(a.get("estado")))
                .findFirst().orElseThrow();
    }

    ResultActions jobs(String secreto) throws Exception {
        return mvc.perform(post("/internal/jobs/ejecutar").header("X-Jobs-Secret", secreto));
    }

    ResultActions como(Cuenta c, MockHttpServletRequestBuilder req, String cuerpo) throws Exception {
        req.header("Authorization", "Bearer " + c.access());
        if (cuerpo != null) {
            req.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        }
        return mvc.perform(req);
    }

    static long id(ResultActions r) throws Exception {
        return ((Number) JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    Cuenta registrar(String nombre) throws Exception {
        String cuerpo = mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s@test.local\",\"contrasena\":\"secreta123\",\"nombre\":\"%s\"}"
                                .formatted(UUID.randomUUID(), nombre))
                        .with(r -> { r.setRemoteAddr(ip); return r; }))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return new Cuenta(((Number) JsonPath.read(cuerpo, "$.usuario.id")).longValue(), JsonPath.read(cuerpo, "$.accessToken"));
    }

    <T> T sql(Cuenta c, Supplier<T> accion) {
        return tx.execute(s -> {
            jdbc.queryForObject("SELECT set_config('app.usuario_id', ?, true)", String.class, String.valueOf(c.id()));
            return accion.get();
        });
    }
}
