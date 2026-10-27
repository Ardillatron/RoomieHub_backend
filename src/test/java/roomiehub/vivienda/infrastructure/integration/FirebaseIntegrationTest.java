package roomiehub.vivienda.infrastructure.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.FileInputStream;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import roomiehub.vivienda.domain.repository.DispositivoRepository;

/**
 * Contra el proyecto real de Firebase: {@code ./mvnw test -Dfirebase=true -Dtest=FirebaseIntegrationTest}.
 * Lee FIREBASE_CREDENTIALS de local.properties.
 */
@EnabledIfSystemProperty(named = "firebase", matches = "true")
class FirebaseIntegrationTest {

    final List<String> borrados = new ArrayList<>();
    final DispositivoRepository dispositivos = new DispositivoRepository() {
        @Override public void registrar(long usuarioId, String fcmToken, String plataforma, java.time.Instant ahora) {}
        @Override public void borrarDeUsuario(long usuarioId) {}
        @Override public List<String> tokensDe(Collection<Long> usuarios) { return List.of(); }
        @Override public void borrarToken(String fcmToken) { borrados.add(fcmToken); }
    };

    @Test
    void obtieneAccesoYRespondeAUnTokenQueNoExiste() throws Exception {
        Properties local = new Properties();
        try (FileInputStream in = new FileInputStream("local.properties")) {
            local.load(in);
        }
        FirebaseNotificador notificador = new FirebaseNotificador(local.getProperty("FIREBASE_CREDENTIALS"),
                dispositivos, Clock.systemUTC());

        String acceso = notificador.accesoFcm();
        assertNotNull(acceso, "la cuenta de servicio se cambia por un token de acceso");
        assertEquals(acceso, notificador.accesoFcm(), "y se reusa mientras no vence");

        // Un token con forma válida que nunca existió: FCM responde con error y nada truena.
        notificador.enviarA("token-inventado", "Prueba", "Hola", acceso);
    }
}
