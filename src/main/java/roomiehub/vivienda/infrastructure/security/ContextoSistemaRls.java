package roomiehub.vivienda.infrastructure.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Sección 9: los procesos programados no tienen JWT. Cada sala se procesa en su propia transacción con
 * el rol app_scheduler, cuyas políticas solo dejan ver las filas de app.sala_id. Al terminar la
 * transacción, el rol y la variable vuelven solos a su estado original.
 */
@Component
public class ContextoSistemaRls {

    private final JdbcTemplate jdbc;

    public ContextoSistemaRls(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Primera instrucción de cada transacción del scheduler. */
    public void entrarComoSistema(long salaId) {
        jdbc.execute("SET LOCAL ROLE app_scheduler");
        jdbc.queryForObject("SELECT set_config('app.sala_id', ?, true)", String.class, String.valueOf(salaId));
    }
}
