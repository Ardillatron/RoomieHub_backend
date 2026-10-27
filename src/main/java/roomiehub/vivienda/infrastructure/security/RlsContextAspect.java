package roomiehub.vivienda.infrastructure.security;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Sección 9: fija app.usuario_id dentro de cada transacción del proyecto, para que RLS filtre.
 * Corre después del interceptor transaccional (TransaccionesConfig lo pone en orden 0).
 * Se limita a roomiehub.vivienda para no repetir el set_config en cada llamada a un repositorio de Spring Data.
 */
@Aspect
@Component
public class RlsContextAspect {

    private final JdbcTemplate jdbc;

    public RlsContextAspect(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Before("(@within(org.springframework.transaction.annotation.Transactional)"
            + " || @annotation(org.springframework.transaction.annotation.Transactional))"
            + " && within(roomiehub.vivienda..*)")
    public void fijarUsuario() {
        Long usuarioId = SecurityUtils.usuarioActualIdONulo();
        if (usuarioId == null) {
            return; // sin JWT (login, scheduler): no se fija nada y RLS no devuelve filas de salas
        }
        jdbc.queryForObject("SELECT set_config('app.usuario_id', ?, true)", String.class, usuarioId.toString());
    }
}
