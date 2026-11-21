package roomiehub.vivienda.adapters.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

// Flujo HTTP completo contra PostgreSQL real con mvnw test y db en true
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named = "db", matches = "true")
class AuthIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    final String correo = UUID.randomUUID() + "@test.local";

    @Test
    void registroLoginYPerfil() throws Exception {
        String access = leer(registrar(correo).andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario.email").value(correo)), "$.accessToken");

        mvc.perform(get("/usuarios/yo").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana"));
        mvc.perform(get("/usuarios/yo")).andExpect(status().isUnauthorized());
        mvc.perform(get("/usuarios/yo").header("Authorization", "Bearer " + access + "x"))
                .andExpect(status().isUnauthorized());

        json(post("/auth/login"), "{\"email\":\"%s\",\"contrasena\":\"secreta123\"}".formatted(correo.toUpperCase()))
                .andExpect(status().isOk());
        json(post("/auth/login"), "{\"email\":\"%s\",\"contrasena\":\"mala12345\"}".formatted(correo))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Credenciales inválidas"));
    }

    @Test
    void erroresDeRegistro() throws Exception {
        registrar(correo).andExpect(status().isCreated());
        registrar(correo).andExpect(status().isConflict());
        registrar("no-es-correo").andExpect(status().isBadRequest());
        json(post("/auth/registro"), "{rota").andExpect(status().isBadRequest());
    }

    @Test
    void elReusoDeUnRefreshQuedaRevocadoAunqueLaPeticionFalle() throws Exception {
        String original = leer(registrar(correo), "$.refreshToken");
        String rotado = leer(refresh(original).andExpect(status().isOk()), "$.refreshToken");
        // Fuera del margen de reintento porque el cambio fue hace un minuto
        jdbc.update("""
                UPDATE refresh_tokens SET revocado_en = now() - interval '1 minute'
                WHERE revocado_en IS NOT NULL AND usuario_id = (SELECT id FROM usuarios WHERE email = ?)""", correo);

        refresh(original).andExpect(status().isUnauthorized());
        // Si la transaccion hubiera hecho rollback el token viejo seguiria sirviendo
        refresh(rotado).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevocaElRefresh() throws Exception {
        String refresh = leer(registrar(correo), "$.refreshToken");
        json(post("/auth/logout"), "{\"refreshToken\":\"%s\"}".formatted(refresh)).andExpect(status().isNoContent());
        refresh(refresh).andExpect(status().isUnauthorized());
    }

    ResultActions registrar(String email) throws Exception {
        return json(post("/auth/registro"),
                "{\"email\":\"%s\",\"contrasena\":\"secreta123\",\"nombre\":\"Ana\"}".formatted(email));
    }

    ResultActions refresh(String token) throws Exception {
        return json(post("/auth/refresh"), "{\"refreshToken\":\"%s\"}".formatted(token));
    }

    @Test
    void cincoContrasenasMalasBloqueanLaCuenta() throws Exception {
        registrar(correo).andExpect(status().isCreated());
        String mala = "{\"email\":\"%s\",\"contrasena\":\"mala12345\"}".formatted(correo);
        for (int i = 0; i < 5; i++) {
            json(post("/auth/login"), mala).andExpect(status().isUnauthorized());
        }
        // Ni con la contraseña correcta entra mientras dura el bloqueo
        json(post("/auth/login"), "{\"email\":\"%s\",\"contrasena\":\"secreta123\"}".formatted(correo))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
        // Desde otra IP la dueña si entra y otro no puede dejarla fuera de su cuenta
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"contrasena\":\"secreta123\"}".formatted(correo))
                        .with(r -> { r.setRemoteAddr("192.168.77.1"); return r; }))
                .andExpect(status().isOk());
    }

    @Test
    void unaMismaIpTieneLimiteEnAuth() throws Exception {
        for (int i = 0; i < 30; i++) {
            refresh("inventado").andExpect(status().isUnauthorized());
        }
        refresh("inventado").andExpect(status().isTooManyRequests());
        mvc.perform(get("/usuarios/yo").with(r -> { r.setRemoteAddr(ip); return r; }))
                .andExpect(status().isUnauthorized()); // fuera de /auth no aplica
    }

    // Una IP distinta por test para que el limite de un test no afecte a los demas
    final String ip = "10.%d.%d.%d".formatted((int) (Math.random() * 250), (int) (Math.random() * 250),
            (int) (Math.random() * 250));

    ResultActions json(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req, String cuerpo)
            throws Exception {
        return mvc.perform(req.contentType(MediaType.APPLICATION_JSON).content(cuerpo)
                .with(r -> { r.setRemoteAddr(ip); return r; }));
    }

    static String leer(ResultActions r, String ruta) throws Exception {
        return JsonPath.read(r.andReturn().getResponse().getContentAsString(), ruta);
    }
}
