package roomiehub.vivienda.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;

// El aspecto pone app usuario_id en la conexion de JPA y JdbcTemplate y se corre con mvnw test y db en true
@SpringBootTest
@EnabledIfSystemProperty(named = "db", matches = "true")
class RlsContextAspectIntegrationTest {

    // Simula un caso de uso con un bean del proyecto y un metodo @Transactional
    static class Sonda {
        private final JdbcTemplate jdbc;
        private final EntityManager em;

        Sonda(JdbcTemplate jdbc, EntityManager em) {
            this.jdbc = jdbc;
            this.em = em;
        }

        @Transactional(readOnly = true)
        public List<String> usuarioVisto() {
            String sql = "SELECT coalesce(current_setting('app.usuario_id', true), '')";
            return List.of(jdbc.queryForObject(sql, String.class),
                    (String) em.createNativeQuery(sql).getSingleResult());
        }
    }

    @TestConfiguration
    static class Config {
        @Bean
        Sonda sonda(JdbcTemplate jdbc, EntityManager em) {
            return new Sonda(jdbc, em);
        }
    }

    @Autowired Sonda sonda;

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void conJwtLaTransaccionVeAlUsuarioEnJdbcYEnJpa() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "HS256").subject("42").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        assertEquals(List.of("42", "42"), sonda.usuarioVisto());
    }

    @Test
    void sinJwtNoSeFijaNadaNiQuedaDeUnaTransaccionAnterior() {
        conJwtLaTransaccionVeAlUsuarioEnJdbcYEnJpa();
        SecurityContextHolder.clearContext();
        for (int i = 0; i < 5; i++) {
            assertEquals(List.of("", ""), sonda.usuarioVisto());
        }
    }
}
