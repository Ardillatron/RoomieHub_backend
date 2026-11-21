package roomiehub.vivienda.infrastructure.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/* Los procesos programados no tienen JWT
   cada sala se procesa en su transaccion con el rol app_scheduler que solo ve las filas de esa sala
   al terminar la transaccion el rol y la variable regresan solos */
@Component
public class ContextoSistemaRls {

    private final JdbcTemplate jdbc;

    public ContextoSistemaRls(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Primera instruccion de cada transaccion del scheduler
    public void entrarComoSistema(long salaId) {
        jdbc.execute("SET LOCAL ROLE app_scheduler");
        jdbc.queryForObject("SELECT set_config('app.sala_id', ?, true)", String.class, String.valueOf(salaId));
    }
}
