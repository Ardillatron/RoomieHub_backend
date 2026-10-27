package roomiehub.vivienda.adapters.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
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

/** CU-09, CU-10, CU-12, CU-13 y CU-30 por HTTP contra PostgreSQL real: {@code ./mvnw test -Ddb=true} */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named = "db", matches = "true")
class TablonIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate tx;

    record Cuenta(long id, String access) {}

    final String ip = "10.55.%d.%d".formatted((int) (Math.random() * 250), (int) (Math.random() * 250));

    @Test
    void tablonDeUnaSala() throws Exception {
        Cuenta ana = registrar("Ana"), beto = registrar("Beto"), carla = registrar("Carla"), intruso = registrar("Ivan");
        long sala = id(como(ana, post("/salas"), "{\"nombre\":\"Depa\",\"zonaHoraria\":\"America/Mexico_City\"}"));
        String codigo = JsonPath.read(como(ana, get("/salas/{id}", sala), null).andReturn().getResponse()
                .getContentAsString(), "$.codigoInvitacion");
        for (Cuenta c : List.of(beto, carla)) {
            como(c, post("/salas/unirse"), "{\"codigo\":\"%s\"}".formatted(codigo)).andExpect(status().isOk());
        }

        // ─── CU-09: con el mismo UUID de la app, reintentar no duplica (RN-35) ───
        String uuid = UUID.randomUUID().toString();
        long primera = id(nota(beto, sala, "Hay pastel", uuid).andExpect(status().isCreated()));
        assertEquals(primera, id(nota(beto, sala, "Hay pastel", uuid).andExpect(status().isCreated())));
        long segunda = id(nota(carla, sala, "Se acabó el gas", null));
        long tercera = id(nota(ana, sala, "Junta el domingo", null));
        long cuarta = id(nota(ana, sala, "Reciclaje los martes", null));
        nota(intruso, sala, "spam", null).andExpect(status().isNotFound());
        como(intruso, get("/salas/{id}/notas", sala), null).andExpect(status().isNotFound());

        // ─── CU-10: like o dislike, se cambia y se quita ───
        reaccion(beto, tercera, "LIKE");
        reaccion(carla, tercera, "DISLIKE");
        reaccion(beto, tercera, "DISLIKE");
        como(carla, get("/salas/{id}/notas", sala), null)
                .andExpect(jsonPath("$.recientes[?(@.id == %d)].likes".formatted(tercera)).value(0))
                .andExpect(jsonPath("$.recientes[?(@.id == %d)].dislikes".formatted(tercera)).value(2))
                .andExpect(jsonPath("$.recientes[?(@.id == %d)].miReaccion".formatted(tercera)).value("DISLIKE"));
        como(beto, delete("/notas/{id}/reaccion", tercera), null).andExpect(status().isNoContent());
        como(beto, get("/salas/{id}/notas", sala), null)
                .andExpect(jsonPath("$.recientes[?(@.id == %d)].dislikes".formatted(tercera)).value(1));

        // ─── CU-12: solo la Administradora, hasta 30 días, máximo 3 a la vez ───
        String enUnaSemana = Instant.now().plus(Duration.ofDays(7)).toString();
        fijar(beto, primera, enUnaSemana).andExpect(status().isForbidden());
        fijar(ana, primera, Instant.now().plus(Duration.ofDays(31)).toString()).andExpect(status().isBadRequest());
        for (long n : List.of(primera, segunda, tercera)) {
            fijar(ana, n, enUnaSemana).andExpect(status().isNoContent());
        }
        fijar(ana, cuarta, enUnaSemana).andExpect(status().isConflict());
        fijar(ana, primera, Instant.now().plus(Duration.ofDays(3)).toString()).andExpect(status().isNoContent());
        como(beto, get("/salas/{id}/notas", sala), null)
                .andExpect(jsonPath("$.fijadas.length()").value(3))
                .andExpect(jsonPath("$.recientes.length()").value(1))
                .andExpect(jsonPath("$.recientes[0].id").value(cuarta));

        // Paginación de las no fijadas
        como(ana, delete("/notas/{id}/fijada", segunda), null).andExpect(status().isNoContent());
        como(ana, get("/salas/{id}/notas?limite=1", sala), null)
                .andExpect(jsonPath("$.recientes[0].id").value(cuarta));
        como(ana, get("/salas/{id}/notas?limite=1&antesDe={a}", sala, cuarta), null)
                .andExpect(jsonPath("$.fijadas.length()").value(0))
                .andExpect(jsonPath("$.recientes[0].id").value(segunda));

        // ─── RN-29: reportes al Administrador ───
        como(carla, post("/notas/{id}/reportes", tercera), "{\"motivo\":\"Es ofensiva\"}")
                .andExpect(status().isCreated());
        como(beto, get("/salas/{id}/reportes", sala), null).andExpect(status().isForbidden());
        String reportes = como(ana, get("/salas/{id}/reportes", sala), null)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].contenido").value("Junta el domingo"))
                .andExpect(jsonPath("$[0].reportadoPor").value("Carla"))
                .andReturn().getResponse().getContentAsString();
        long reporte = ((Number) JsonPath.read(reportes, "$[0].id")).longValue();
        como(ana, post("/reportes/{id}/resolver", reporte), null).andExpect(status().isNoContent());
        como(ana, post("/reportes/{id}/resolver", reporte), null).andExpect(status().isConflict());
        como(ana, get("/salas/{id}/reportes", sala), null).andExpect(jsonPath("$.length()").value(0));

        // ─── CU-13: solo la Administradora; una nota eliminada ya no admite nada ───
        como(beto, delete("/notas/{id}", tercera), null).andExpect(status().isForbidden());
        como(ana, delete("/notas/{id}", tercera), null).andExpect(status().isNoContent());
        reaccionSinEsperar(beto, tercera, "LIKE").andExpect(status().isNotFound());
        como(beto, get("/salas/{id}/notas", sala), null).andExpect(jsonPath("$.fijadas.length()").value(1));

        // ─── Nota con imagen: firma de subida por sala y URL firmada al leer ───
        String firma = como(beto, post("/salas/{id}/imagenes/firma", sala), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parametros.type").value("authenticated"))
                .andExpect(jsonPath("$.parametros.signature").exists())
                .andReturn().getResponse().getContentAsString();
        String imagenId = JsonPath.read(firma, "$.imagenId");
        como(intruso, post("/salas/{id}/imagenes/firma", sala), null).andExpect(status().isNotFound());
        String conImagen = como(beto, post("/salas/{id}/notas", sala), "{\"imagenId\":\"%s\"}".formatted(imagenId))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String url = JsonPath.read(conImagen, "$.imagenUrl");
        assertEquals(true, url.contains("/image/authenticated/s--") && url.endsWith(imagenId), url);
        String miniatura = JsonPath.read(conImagen, "$.imagenMiniaturaUrl");
        assertEquals(true, miniatura.contains("/c_limit,h_600,w_600/") && miniatura.endsWith(imagenId), miniatura);
        como(beto, post("/salas/{id}/notas", sala), "{\"imagenId\":\"roomiehub/salas/1/%s\"}".formatted(UUID.randomUUID()))
                .andExpect(status().isBadRequest());
        como(beto, post("/salas/{id}/notas", sala), "{\"texto\":\"\"}").andExpect(status().isBadRequest());

        // ─── CU-30: el proceso programado desfija lo que venció ───
        sql(ana, () -> jdbc.update("UPDATE notas_tablon SET fijada_hasta = now() - interval '1 minute' WHERE id = ?",
                primera));
        mvc.perform(post("/internal/jobs/ejecutar").header("X-Jobs-Secret", "secreto-de-prueba"))
                .andExpect(status().isNoContent());
        assertNull(sql(ana, () -> jdbc.queryForObject(
                "SELECT fijada_hasta FROM notas_tablon WHERE id = ?", Instant.class, primera)));
    }

    // ─── Ayudantes ───

    ResultActions nota(Cuenta c, long sala, String texto, String uuid) throws Exception {
        String cuerpo = uuid == null ? "{\"texto\":\"%s\"}".formatted(texto)
                : "{\"texto\":\"%s\",\"clientUuid\":\"%s\"}".formatted(texto, uuid);
        return como(c, post("/salas/{id}/notas", sala), cuerpo);
    }

    void reaccion(Cuenta c, long nota, String tipo) throws Exception {
        reaccionSinEsperar(c, nota, tipo).andExpect(status().isNoContent());
    }

    ResultActions reaccionSinEsperar(Cuenta c, long nota, String tipo) throws Exception {
        return como(c, put("/notas/{id}/reaccion", nota), "{\"tipo\":\"%s\"}".formatted(tipo));
    }

    ResultActions fijar(Cuenta c, long nota, String hasta) throws Exception {
        return como(c, put("/notas/{id}/fijada", nota), "{\"hasta\":\"%s\"}".formatted(hasta));
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
