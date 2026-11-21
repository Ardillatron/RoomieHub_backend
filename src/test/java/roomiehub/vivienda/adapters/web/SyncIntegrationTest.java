package roomiehub.vivienda.adapters.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

/** CU-27 por HTTP contra PostgreSQL real, con las cinco acciones de RN-34: {@code ./mvnw test -Ddb=true} */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named = "db", matches = "true")
class SyncIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate tx;

    record Cuenta(long id, String access) {}

    final String ip = "10.44.%d.%d".formatted((int) (Math.random() * 250), (int) (Math.random() * 250));
    final LocalDate hoy = LocalDate.now(ZoneId.of("America/Mexico_City"));

    @Test
    void colaOfflineDeUnHabitante() throws Exception {
        Cuenta ana = registrar("Ana"), beto = registrar("Beto");
        long sala = id(como(ana, post("/salas"), "{\"nombre\":\"Depa\",\"zonaHoraria\":\"America/Mexico_City\"}"));
        String codigo = JsonPath.read(cuerpo(como(ana, get("/salas/{id}", sala), null)), "$.codigoInvitacion");
        como(beto, post("/salas/unirse"), "{\"codigo\":\"%s\"}".formatted(codigo)).andExpect(status().isOk());
        long salaDeBeto = id(como(beto, post("/salas"), "{\"nombre\":\"Otra\",\"zonaHoraria\":\"America/Mexico_City\"}"));

        gasto(ana, sala, "Internet", "MENSUAL", ana, beto);
        List<Map<String, Object>> deudas = JsonPath.read(cuerpo(como(ana, get("/salas/{id}/deudas", sala), null)), "$");
        long deBeto = deudaDe(deudas, beto, "Internet"), deAna = deudaDe(deudas, ana, "Internet");
        // Beto solo confirma lo que Ana ya avisó que pagó.
        como(ana, post("/deudas/{id}/aviso", deAna), null).andExpect(status().isOk());

        long tarea = id(como(ana, post("/salas/{id}/tareas", sala), """
                {"nombre":"Barrer","valor":5,"frecuencia":"P7D","fechaInicio":"%s"}""".formatted(hoy.plusDays(1))));
        long abierta = asignacion(ana, tarea, sala, beto, "1 hour", "-5 hours");
        long vencida = asignacion(ana, tarea, sala, beto, "8 days", "4 hours");

        // ─── Sin conexión, Beto hizo esto en orden ───
        String[] u = IntStream.range(0, 8).mapToObj(i -> UUID.randomUUID().toString()).toArray(String[]::new);
        String cola = acciones(
                accion(u[0], "CREAR_NOTA", sala, "\"texto\":\"Desde el metro\""),
                accion(u[1], "ENVIAR_MENSAJE", sala, "\"texto\":\"Llego tarde\""),
                accion(u[2], "AVISAR_PAGO", sala, "\"deudaId\":" + deBeto),
                accion(u[3], "CONFIRMAR_PAGO", sala, "\"deudaId\":" + deAna),
                accion(u[4], "COMPLETAR_TAREA", sala, "\"asignacionId\":" + abierta),
                accion(u[5], "COMPLETAR_TAREA", sala, "\"asignacionId\":" + vencida),
                accion(u[6], "COMPLETAR_TAREA", sala, null),
                accion(u[7], "AVISAR_PAGO", salaDeBeto, "\"deudaId\":" + deBeto));

        String primera = cuerpo(sync(beto, cola).andExpect(status().isOk()));
        assertEquals(List.of("PROCESADA", "PROCESADA", "PROCESADA", "PROCESADA", "PROCESADA", "RECHAZADA", "RECHAZADA",
                "RECHAZADA"), JsonPath.read(primera, "$.resultados[*].estado"));
        assertNotNull(JsonPath.read(primera, "$.resultados[0].recursoId"), "la nota creada");
        assertEquals("El plazo para marcar esta tarea ya venció", JsonPath.read(primera, "$.resultados[5].motivo"));
        assertEquals("APROBADA", estadoDeuda(ana, deAna), "con dos participantes basta la confirmación de Beto");
        assertEquals("COMPLETADA", sql(ana, () -> jdbc.queryForObject(
                "SELECT estado FROM tarea_asignaciones WHERE id = ?", String.class, abierta)));

        // ─── RN-35: reenviar la misma cola no duplica nada ───
        String segunda = cuerpo(sync(beto, cola));
        assertEquals(List.of("DUPLICADA", "DUPLICADA", "DUPLICADA", "DUPLICADA", "DUPLICADA", "RECHAZADA", "RECHAZADA",
                "RECHAZADA"), JsonPath.read(segunda, "$.resultados[*].estado"));
        assertEquals(1, ((List<?>) JsonPath.read(cuerpo(como(ana, get("/salas/{id}/mensajes", sala), null)), "$")).size());
        como(ana, get("/salas/{id}/notas", sala), null)
                .andExpect(jsonPath("$.recientes.length()").value(1))
                .andExpect(jsonPath("$.recientes[0].clientUuid").value(u[0]));

        // ─── Recurso eliminado mientras Beto no tenía conexión ───
        long pizza = gasto(ana, sala, "Pizza", "UNICA", ana, beto);
        long pizzaDeAna = deudaDe(JsonPath.read(cuerpo(como(ana, get("/salas/{id}/deudas", sala), null)), "$"),
                ana, "Pizza");
        como(ana, delete("/salas/{s}/gastos/{g}", sala, pizza), null).andExpect(status().isNoContent());
        sync(beto, acciones(accion(UUID.randomUUID().toString(), "CONFIRMAR_PAGO", sala, "\"deudaId\":" + pizzaDeAna)))
                .andExpect(jsonPath("$.resultados[0].estado").value("RECHAZADA_RECURSO_ELIMINADO"));

        // ─── RN-36: expulsado, se rechaza toda su cola de esa sala; la de su otra sala sigue ───
        como(ana, delete("/salas/{s}/miembros/{u}", sala, beto.id()), null).andExpect(status().isNoContent());
        String tercera = cuerpo(sync(beto, acciones(
                accion(UUID.randomUUID().toString(), "CREAR_NOTA", sala, "\"texto\":\"Ya no llega\""),
                accion(UUID.randomUUID().toString(), "ENVIAR_MENSAJE", sala, "\"texto\":\"Tampoco\""),
                accion(UUID.randomUUID().toString(), "CREAR_NOTA", salaDeBeto, "\"texto\":\"En mi sala\""))));
        assertEquals(List.of("RECHAZADA_EXPULSION", "RECHAZADA_EXPULSION", "PROCESADA"),
                JsonPath.read(tercera, "$.resultados[*].estado"));
        assertEquals(List.of(((Number) sala).intValue()), JsonPath.read(tercera, "$.salasExpulsadas"));
        como(ana, get("/salas/{id}/notas", sala), null).andExpect(jsonPath("$.recientes.length()").value(1));
    }

    @Test
    void lotesInvalidos() throws Exception {
        Cuenta ana = registrar("Ana");
        String repetido = UUID.randomUUID().toString();
        sync(ana, acciones(accion(repetido, "CREAR_NOTA", 1, null), accion(repetido, "CREAR_NOTA", 1, null)))
                .andExpect(status().isBadRequest());
        sync(ana, acciones(IntStream.range(0, 101)
                .mapToObj(i -> accion(UUID.randomUUID().toString(), "CREAR_NOTA", 1, null)).toArray(String[]::new)))
                .andExpect(status().isBadRequest());
        sync(ana, "{\"acciones\":[{\"tipo\":\"CREAR_NOTA\",\"salaId\":1}]}").andExpect(status().isBadRequest());
        sync(ana, acciones(accion(UUID.randomUUID().toString(), "BORRAR_TODO", 1, null)))
                .andExpect(status().isBadRequest());
    }

    // ─── Ayudantes ───

    static String accion(String uuid, String tipo, long sala, String extra) {
        return "{\"uuid\":\"%s\",\"tipo\":\"%s\",\"salaId\":%d%s}".formatted(uuid, tipo, sala,
                extra == null ? "" : "," + extra);
    }

    static String acciones(String... acciones) {
        return "{\"acciones\":[" + String.join(",", acciones) + "]}";
    }

    ResultActions sync(Cuenta c, String cuerpo) throws Exception {
        return como(c, post("/sync"), cuerpo);
    }

    long gasto(Cuenta admin, long sala, String concepto, String frecuencia, Cuenta... participantes) throws Exception {
        String ids = List.of(participantes).stream().map(p -> Long.toString(p.id())).collect(Collectors.joining(","));
        return id(como(admin, post("/salas/{id}/gastos", sala), """
                {"concepto":"%s","monto":"100","moneda":"MXN","frecuencia":"%s","fechaInicio":"%s","participantes":[%s]}"""
                .formatted(concepto, frecuencia, hoy, ids)).andExpect(status().isCreated()));
    }

    static long deudaDe(List<Map<String, Object>> deudas, Cuenta deudor, String concepto) {
        return deudas.stream()
                .filter(d -> ((Number) d.get("deudorId")).longValue() == deudor.id() && concepto.equals(d.get("concepto")))
                .map(d -> ((Number) d.get("id")).longValue())
                .findFirst().orElseThrow();
    }

    String estadoDeuda(Cuenta c, long deuda) {
        return sql(c, () -> jdbc.queryForObject("SELECT estado FROM deudas_usuarios WHERE id = ?", String.class, deuda));
    }

    long asignacion(Cuenta c, long tarea, long sala, Cuenta responsable, String inicio, String fin) {
        return sql(c, () -> jdbc.queryForObject("""
                INSERT INTO tarea_asignaciones (tarea_id, sala_id, usuario_id, periodo_inicio, periodo_fin, valor_al_asignar)
                VALUES (?, ?, ?, now() - ?::interval, now() - ?::interval, 5) RETURNING id""",
                Long.class, tarea, sala, responsable.id(), inicio, fin));
    }

    ResultActions como(Cuenta c, MockHttpServletRequestBuilder req, String cuerpo) throws Exception {
        req.header("Authorization", "Bearer " + c.access());
        if (cuerpo != null) {
            req.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        }
        return mvc.perform(req);
    }

    static String cuerpo(ResultActions r) throws Exception {
        return r.andReturn().getResponse().getContentAsString();
    }

    static long id(ResultActions r) throws Exception {
        return ((Number) JsonPath.read(cuerpo(r), "$.id")).longValue();
    }

    Cuenta registrar(String nombre) throws Exception {
        String cuerpo = cuerpo(mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s@test.local\",\"contrasena\":\"secreta123\",\"nombre\":\"%s\"}"
                                .formatted(UUID.randomUUID(), nombre))
                        .with(r -> { r.setRemoteAddr(ip); return r; }))
                .andExpect(status().isCreated()));
        return new Cuenta(((Number) JsonPath.read(cuerpo, "$.usuario.id")).longValue(), JsonPath.read(cuerpo, "$.accessToken"));
    }

    <T> T sql(Cuenta c, Supplier<T> accion) {
        return tx.execute(s -> {
            jdbc.queryForObject("SELECT set_config('app.usuario_id', ?, true)", String.class, String.valueOf(c.id()));
            return accion.get();
        });
    }
}
