package roomiehub.vivienda.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Verifica las políticas de la sección 9 contra PostgreSQL real, conectado como app_user.
 * Necesita la base de db/roles.sql y local.properties: {@code ./mvnw test -Ddb=true}
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "db", matches = "true")
class RlsIntegrationTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate tx;

    long ana, beto, intruso, sala;
    String codigo;

    @BeforeEach
    void salaDeAnaConBeto() {
        ana = crearUsuario("Ana");
        beto = crearUsuario("Beto");
        intruso = crearUsuario("Intruso");
        codigo = UUID.randomUUID().toString().substring(0, 8);
        sala = como(ana, () -> {
            long id = jdbc.queryForObject("""
                    INSERT INTO salas (nombre, creador_id, zona_horaria, codigo_invitacion)
                    VALUES ('Depa', ?, 'America/Mexico_City', ?) RETURNING id""", Long.class, ana, codigo);
            jdbc.update("INSERT INTO usuario_sala (usuario_id, sala_id, rol) VALUES (?, ?, 'ADMIN')", ana, id);
            return id;
        });
        como(beto, () -> {
            long encontrada = jdbc.queryForObject("SELECT sala_id FROM buscar_sala_por_codigo(?)", Long.class, codigo);
            return jdbc.update("INSERT INTO usuario_sala (usuario_id, sala_id, rol) VALUES (?, ?, 'HABITANTE')",
                    beto, encontrada);
        });
        como(ana, () -> jdbc.update("INSERT INTO notas_tablon (sala_id, autor_id, texto) VALUES (?, ?, 'Hola')",
                sala, ana));
    }

    @Test
    void losMiembrosVenLaSalaYSusNotas() {
        assertEquals(1, contar(beto, "SELECT count(*) FROM salas WHERE id = " + sala));
        assertEquals(1, contar(beto, "SELECT count(*) FROM notas_tablon WHERE sala_id = " + sala));
        assertEquals(2, contar(beto, "SELECT count(*) FROM usuario_sala WHERE sala_id = " + sala));
    }

    @Test
    void unAjenoNoVeNadaDeLaSala() {
        assertEquals(0, contar(intruso, "SELECT count(*) FROM salas WHERE id = " + sala));
        assertEquals(0, contar(intruso, "SELECT count(*) FROM notas_tablon WHERE sala_id = " + sala));
        assertEquals(0, contar(intruso, "SELECT count(*) FROM usuario_sala WHERE sala_id = " + sala));
        assertThrows(DataAccessException.class, () -> como(intruso, () -> jdbc.update(
                "INSERT INTO notas_tablon (sala_id, autor_id, texto) VALUES (?, ?, 'spam')", sala, intruso)));
    }

    @Test
    void nadieSeHaceAdminDeUnaSalaAjena() {
        assertThrows(DataAccessException.class, () -> como(intruso, () -> jdbc.update(
                "INSERT INTO usuario_sala (usuario_id, sala_id, rol) VALUES (?, ?, 'ADMIN')", intruso, sala)));
    }

    @Test
    void elExpulsadoDejaDeVerLaSalaPeroVeSuMembresia() {
        como(ana, () -> jdbc.update("UPDATE usuario_sala SET activo = false WHERE usuario_id = ? AND sala_id = ?",
                beto, sala));
        assertEquals(0, contar(beto, "SELECT count(*) FROM notas_tablon WHERE sala_id = " + sala));
        assertEquals(1, contar(beto, "SELECT count(*) FROM usuario_sala WHERE sala_id = " + sala));
    }

    @Test
    void sinUsuarioFijadoNoHayFilasNiErrores() {
        contar(ana, "SELECT 1"); // deja una conexión del pool que ya tuvo app.usuario_id
        for (int i = 0; i < 3; i++) {
            Long vistas = tx.execute(s -> jdbc.queryForObject(
                    "SELECT count(*) FROM notas_tablon WHERE sala_id = " + sala, Long.class));
            assertEquals(0L, vistas);
        }
    }

    @Test
    void elSchedulerSoloVeLaSalaQueProcesa() {
        long otraSala = como(intruso, () -> {
            long id = jdbc.queryForObject("""
                    INSERT INTO salas (nombre, creador_id, zona_horaria, codigo_invitacion)
                    VALUES ('Otra', ?, 'America/Mexico_City', ?) RETURNING id""",
                    Long.class, intruso, UUID.randomUUID().toString().substring(0, 8));
            jdbc.update("INSERT INTO usuario_sala (usuario_id, sala_id, rol) VALUES (?, ?, 'ADMIN')", intruso, id);
            return id;
        });
        long[] vistas = tx.execute(s -> {
            jdbc.execute("SET LOCAL ROLE app_scheduler");
            jdbc.queryForObject("SELECT set_config('app.sala_id', ?, true)", String.class, String.valueOf(sala));
            return new long[] {
                    jdbc.queryForObject("SELECT count(*) FROM salas WHERE id IN (?, ?)", Long.class, sala, otraSala),
                    jdbc.queryForObject("SELECT count(*) FROM usuario_sala WHERE sala_id = ?", Long.class, otraSala)};
        });
        assertEquals(1, vistas[0]);
        assertEquals(0, vistas[1]);
    }

    @Test
    void appUserNoHeredaLosPermisosDelScheduler() {
        // Sin SET ROLE, app_user no puede saltarse su propia política usando la del scheduler.
        long vistas = tx.execute(s -> {
            jdbc.queryForObject("SELECT set_config('app.sala_id', ?, true)", String.class, String.valueOf(sala));
            return jdbc.queryForObject("SELECT count(*) FROM notas_tablon WHERE sala_id = ?", Long.class, sala);
        });
        assertEquals(0, vistas);
    }

    long crearUsuario(String nombre) {
        return jdbc.queryForObject("""
                INSERT INTO usuarios (email, password_hash, proveedor_auth, nombre)
                VALUES (?, 'x', 'EMAIL', ?) RETURNING id""",
                Long.class, UUID.randomUUID() + "@test.local", nombre);
    }

    long contar(long usuario, String sql) {
        return como(usuario, () -> jdbc.queryForObject(sql, Long.class));
    }

    <T> T como(long usuario, Supplier<T> accion) {
        return tx.execute(s -> {
            jdbc.queryForObject("SELECT set_config('app.usuario_id', ?, true)", String.class, String.valueOf(usuario));
            return accion.get();
        });
    }
}
