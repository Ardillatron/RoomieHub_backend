package roomiehub.vivienda.infrastructure.security;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/* Pone app usuario_id en cada transaccion del proyecto para que RLS filtre
   corre despues del interceptor de transacciones
   solo aplica a roomiehub vivienda para no repetirlo en cada repositorio de Spring Data */
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
            return; // sin JWT no se pone nada y RLS no regresa filas de salas
        }
        jdbc.queryForObject("SELECT set_config('app.usuario_id', ?, true)", String.class, usuarioId.toString());
    }
}
