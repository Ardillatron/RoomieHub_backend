package roomiehub.vivienda.adapters.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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

/** CU-20 a CU-22 y CU-25 por HTTP contra PostgreSQL real: {@code ./mvnw test -Ddb=true} */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named = "db", matches = "true")
class FinanzasIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate tx;

    record Cuenta(long id, String access) {}

    final String ip = "10.66.%d.%d".formatted((int) (Math.random() * 250), (int) (Math.random() * 250));
    final LocalDate hoy = LocalDate.now(ZoneId.of("America/Mexico_City"));

    @Test
    void gastosYDeudasDeUnaSala() throws Exception {
        Cuenta ana = registrar("Ana"), beto = registrar("Beto"), carla = registrar("Carla");
        long sala = id(como(ana, post("/salas"), "{\"nombre\":\"Depa\",\"zonaHoraria\":\"America/Mexico_City\"}"));
        String codigo = JsonPath.read(como(ana, get("/salas/{id}", sala), null).andReturn().getResponse()
                .getContentAsString(), "$.codigoInvitacion");
        for (Cuenta c : List.of(beto, carla)) {
            como(c, post("/salas/unirse"), "{\"codigo\":\"%s\"}".formatted(codigo)).andExpect(status().isOk());
        }

        // ─── CU-20: solo la Administradora; participantes válidos ───
        crearGasto(beto, sala, "Internet", "100", "MENSUAL", hoy, List.of(ana, beto)).andExpect(status().isForbidden());
        crearGasto(ana, sala, "Internet", "100", "MENSUAL", hoy, List.of()).andExpect(status().isBadRequest());
        crearGasto(ana, sala, "Internet", "100", "MENSUAL", hoy, List.of(ana, new Cuenta(999_999, "")))
                .andExpect(status().isBadRequest());
        crearGasto(ana, sala, "Internet", "100.999", "MENSUAL", hoy, List.of(ana)).andExpect(status().isBadRequest());

        // Empezó hace un mes: se generan de inmediato ese periodo y el actual (RN-41), con el centavo rotando
        long internet = id(crearGasto(ana, sala, "Internet", "100", "MENSUAL", hoy.minusMonths(1),
                List.of(ana, beto, carla)).andExpect(status().isCreated()));
        List<Map<String, Object>> pendientes = deudas(ana, sala, "PENDIENTE");
        assertEquals(6, pendientes.size());
        Map<String, List<String>> porPeriodo = montosPorPeriodo(pendientes, List.of(ana, beto, carla));
        List<String> periodos = porPeriodo.keySet().stream().sorted().toList();
        assertEquals(List.of("33.34", "33.33", "33.33"), porPeriodo.get(periodos.get(0)), "periodo 0");
        assertEquals(List.of("33.33", "33.34", "33.33"), porPeriodo.get(periodos.get(1)), "periodo 1: rota el centavo");

        // CU-25: el proceso programado no duplica lo ya generado
        jobs().andExpect(status().isNoContent());
        assertEquals(6, deudas(ana, sala, "PENDIENTE").size());

        // ─── CU-21 y CU-22: avisar y confirmar (RN-26) ───
        long deBeto = deudaDe(pendientes, beto, periodos.get(1));
        como(carla, post("/deudas/{id}/aviso", deBeto), null).andExpect(status().isForbidden());
        como(beto, post("/deudas/{id}/aviso", deBeto), null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("PENDIENTE"));
        como(beto, post("/deudas/{id}/confirmaciones", deBeto), null).andExpect(status().isForbidden());
        como(ana, post("/deudas/{id}/confirmaciones", deBeto), null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("PENDIENTE"));
        como(carla, post("/deudas/{id}/confirmaciones", deBeto), null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("APROBADA"));
        como(carla, post("/deudas/{id}/confirmaciones", deBeto), null).andExpect(status().isConflict());
        assertEquals(2L, sql(ana, () -> jdbc.queryForObject(
                "SELECT count(*) FROM confirmaciones_pago WHERE deuda_id = ?", Long.class, deBeto)));
        assertEquals(1, deudas(ana, sala, "APROBADA").size());

        // ─── Gasto único entre dos: basta la confirmación del otro, pero solo después del aviso ───
        long pizza = id(crearGasto(ana, sala, "Pizza", "300", "UNICA", hoy, List.of(ana, beto)));
        List<Map<String, Object>> dePizza = deudas(ana, sala, "PENDIENTE").stream()
                .filter(d -> "Pizza".equals(d.get("concepto"))).toList();
        assertEquals(List.of("150.00", "150.00"), dePizza.stream().map(d -> (String) d.get("monto")).toList());
        long pizzaDeAna = deudaDe(dePizza, ana, null), pizzaDeBeto = deudaDe(dePizza, beto, null);
        como(beto, post("/deudas/{id}/confirmaciones", pizzaDeAna), null).andExpect(status().isConflict());
        como(ana, post("/deudas/{id}/aviso", pizzaDeAna), null).andExpect(status().isOk());
        como(beto, post("/deudas/{id}/confirmaciones", pizzaDeAna), null)
                .andExpect(jsonPath("$.estado").value("APROBADA"));

        // ─── RN-25: eliminar; sus deudas ya no admiten avisos ───
        como(ana, delete("/salas/{s}/gastos/{g}", sala, pizza), null).andExpect(status().isNoContent());
        como(beto, post("/deudas/{id}/aviso", pizzaDeBeto), null).andExpect(status().isNotFound());

        // ─── RN-24: editar solo afecta lo que sigue; quitar a Carla cambia quién debe confirmar ───
        long anaDelMesActual = deudaDe(pendientes, ana, periodos.get(1));
        como(ana, put("/salas/{s}/gastos/{g}", sala, internet),
                "{\"concepto\":\"Internet fibra\",\"monto\":\"120\",\"frecuencia\":\"MENSUAL\",\"participantes\":[%d,%d]}"
                        .formatted(ana.id(), beto.id()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.monto").value("120.00"));
        assertEquals("33.33", monto(ana, anaDelMesActual), "las deudas ya generadas conservan su monto");
        como(ana, post("/deudas/{id}/aviso", anaDelMesActual), null).andExpect(status().isOk());
        como(beto, post("/deudas/{id}/confirmaciones", anaDelMesActual), null)
                .andExpect(jsonPath("$.estado").value("APROBADA"));
        como(carla, get("/salas/{id}/gastos", sala), null)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].participantes.length()").value(2));

        // ─── Sección 10: recordatorio al deudor a los 3 días sin aviso ───
        long carlaMesPasado = deudaDe(pendientes, carla, periodos.get(0));
        sql(ana, () -> jdbc.update("UPDATE deudas_usuarios SET creado_en = now() - interval '4 days' WHERE id = ?",
                carlaMesPasado));
        jobs().andExpect(status().isNoContent());
        assertEquals(1, sql(ana, () -> jdbc.queryForObject(
                "SELECT recordatorios_enviados FROM deudas_usuarios WHERE id = ?", Integer.class, carlaMesPasado)));
    }

    // ─── Ayudantes ───

    ResultActions crearGasto(Cuenta c, long sala, String concepto, String monto, String frecuencia, LocalDate inicio,
                             List<Cuenta> participantes) throws Exception {
        String ids = participantes.stream().map(p -> Long.toString(p.id())).collect(Collectors.joining(","));
        return como(c, post("/salas/{id}/gastos", sala), """
                {"concepto":"%s","monto":"%s","moneda":"MXN","frecuencia":"%s","fechaInicio":"%s","participantes":[%s]}"""
                .formatted(concepto, monto, frecuencia, inicio, ids));
    }

    List<Map<String, Object>> deudas(Cuenta c, long sala, String estado) throws Exception {
        return JsonPath.read(como(c, get("/salas/{id}/deudas?estado={e}", sala, estado), null)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$");
    }

    /** periodo → montos en el orden dado de deudores */
    static Map<String, List<String>> montosPorPeriodo(List<Map<String, Object>> deudas, List<Cuenta> orden) {
        return deudas.stream().collect(Collectors.groupingBy(d -> (String) d.get("periodo"),
                Collectors.collectingAndThen(Collectors.toList(), lista -> lista.stream()
                        .sorted(Comparator.comparingInt(d -> posicion(orden, ((Number) d.get("deudorId")).longValue())))
                        .map(d -> (String) d.get("monto"))
                        .toList())));
    }

    static int posicion(List<Cuenta> orden, long id) {
        for (int i = 0; i < orden.size(); i++) {
            if (orden.get(i).id() == id) return i;
        }
        throw new IllegalArgumentException();
    }

    static long deudaDe(List<Map<String, Object>> deudas, Cuenta deudor, String periodo) {
        return deudas.stream()
                .filter(d -> ((Number) d.get("deudorId")).longValue() == deudor.id()
                        && (periodo == null || periodo.equals(d.get("periodo"))))
                .map(d -> ((Number) d.get("id")).longValue())
                .findFirst().orElseThrow();
    }

    String monto(Cuenta c, long deuda) {
        return sql(c, () -> jdbc.queryForObject("SELECT monto::text FROM deudas_usuarios WHERE id = ?", String.class, deuda));
    }

    ResultActions jobs() throws Exception {
        return mvc.perform(post("/internal/jobs/ejecutar").header("X-Jobs-Secret", "secreto-de-prueba"));
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
