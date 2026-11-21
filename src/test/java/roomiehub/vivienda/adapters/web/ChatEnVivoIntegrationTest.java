package roomiehub.vivienda.adapters.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.client.RestClient;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import com.jayway.jsonpath.JsonPath;

/* CU-11 CU-14 CU-15 y CU-32 con el servidor en un puerto real
   los mensajes se envian por REST y llegan por STOMP y se corre con mvnw test y db en true */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfSystemProperty(named = "db", matches = "true")
class ChatEnVivoIntegrationTest {

    @LocalServerPort int puerto;

    record Cuenta(long id, String access) {}

    final WebSocketStompClient stomp = new WebSocketStompClient(new StandardWebSocketClient());

    {
        stomp.setMessageConverter(new JacksonJsonMessageConverter());
    }

    @AfterEach
    void cerrar() {
        stomp.stop();
    }

    @Test
    void elChatLlegaEnVivoSoloALosMiembrosActivos() throws Exception {
        Cuenta ana = registrar("Ana"), beto = registrar("Beto"), carla = registrar("Carla"), intruso = registrar("Ivan");
        long sala = id(post(ana, "/salas", Map.of("nombre", "Depa", "zonaHoraria", "America/Mexico_City")));
        String codigo = JsonPath.read(get(ana, "/salas/" + sala), "$.codigoInvitacion");
        post(beto, "/salas/unirse", Map.of("codigo", codigo));
        post(carla, "/salas/unirse", Map.of("codigo", codigo));

        BlockingQueue<Map<String, Object>> deBeto = conectar(beto), deCarla = conectar(carla), deIntruso = conectar(intruso);

        // CU-14 llega a los miembros y no a quien esta fuera de la sala
        long hola = id(post(ana, "/salas/" + sala + "/mensajes", Map.of("texto", "Hola a todos")));
        assertEquals("Hola a todos", textoDe(deBeto.poll(5, TimeUnit.SECONDS)));
        assertEquals("Hola a todos", textoDe(deCarla.poll(5, TimeUnit.SECONDS)));
        assertNull(deIntruso.poll(1, TimeUnit.SECONDS));

        // Quien es expulsado deja de recibir al instante
        status(ana, HttpMethod.DELETE, "/salas/" + sala + "/miembros/" + carla.id(), null);
        post(ana, "/salas/" + sala + "/mensajes", Map.of("texto", "Solo para Beto"));
        assertEquals("Solo para Beto", textoDe(deBeto.poll(5, TimeUnit.SECONDS)));
        assertNull(deCarla.poll(1, TimeUnit.SECONDS));

        // CU-15 borrar quita el mensaje de los chats abiertos
        assertEquals(403, status(beto, HttpMethod.DELETE, "/mensajes/" + hola, null));
        assertEquals(204, status(ana, HttpMethod.DELETE, "/mensajes/" + hola, null));
        Map<String, Object> eliminado = deBeto.poll(5, TimeUnit.SECONDS);
        assertNotNull(eliminado);
        assertEquals("MENSAJE_ELIMINADO", eliminado.get("tipo"));
        assertEquals(hola, ((Number) eliminado.get("mensajeId")).longValue());
        assertEquals(List.of("Solo para Beto"), JsonPath.read(get(beto, "/salas/" + sala + "/mensajes"), "$[*].texto"));
    }

    @Test
    void historialRespuestasAUnaNotaReintentosYReportes() throws Exception {
        Cuenta ana = registrar("Ana"), beto = registrar("Beto");
        long sala = id(post(ana, "/salas", Map.of("nombre", "Depa", "zonaHoraria", "America/Mexico_City")));
        post(beto, "/salas/unirse", Map.of("codigo", JsonPath.read(get(ana, "/salas/" + sala), "$.codigoInvitacion")));
        long otraSala = id(post(beto, "/salas", Map.of("nombre", "Otra", "zonaHoraria", "America/Mexico_City")));

        // CU-11 responder una nota del tablon la muestra citada
        long nota = id(post(ana, "/salas/" + sala + "/notas", Map.of("texto", "Junta el domingo")));
        String respuesta = post(beto, "/salas/" + sala + "/mensajes", Map.of("texto", "Ahí estaré", "notaRespondidaId", nota));
        assertEquals("Junta el domingo", JsonPath.read(respuesta, "$.notaCitada.texto"));
        long notaDeOtraSala = id(post(beto, "/salas/" + otraSala + "/notas", Map.of("texto", "Ajena")));
        assertEquals(404, status(beto, HttpMethod.POST, "/salas/" + sala + "/mensajes",
                Map.of("texto", "x", "notaRespondidaId", notaDeOtraSala)));

        // Reenviar con el mismo UUID no duplica
        String uuid = UUID.randomUUID().toString();
        long primero = id(post(ana, "/salas/" + sala + "/mensajes", Map.of("texto", "Sin señal", "clientUuid", uuid)));
        assertEquals(primero, id(post(ana, "/salas/" + sala + "/mensajes", Map.of("texto", "Sin señal", "clientUuid", uuid))));
        assertEquals(400, status(ana, HttpMethod.POST, "/salas/" + sala + "/mensajes", Map.of("texto", " ")));

        // Historial del mas nuevo al mas viejo por paginas
        String historial = get(beto, "/salas/" + sala + "/mensajes?limite=1");
        assertEquals(List.of("Sin señal"), JsonPath.read(historial, "$[*].texto"));
        assertEquals(List.of("Ahí estaré"), JsonPath.read(
                get(beto, "/salas/" + sala + "/mensajes?limite=1&antesDe=" + primero), "$[*].texto"));

        // CU-32 el reporte llega al admin con el texto del mensaje
        assertEquals(201, status(beto, HttpMethod.POST, "/mensajes/" + primero + "/reportes", Map.of("motivo", "Spam")));
        assertEquals("Sin señal", JsonPath.read(get(ana, "/salas/" + sala + "/reportes"), "$[0].contenido"));
    }

    @Test
    void sinJwtNoSeConectaYNoSePuedeEscucharOtraCola() throws Exception {
        assertThrows(Exception.class, () -> stomp.connectAsync("ws://localhost:" + puerto + "/ws",
                new WebSocketHttpHeaders(), new StompHeaders(), new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS));

        Cuenta ana = registrar("Ana");
        StompSession sesion = sesion(ana);
        sesion.subscribe("/topic/salas/1", new ColaDeEventos(new LinkedBlockingQueue<>()));
        for (int i = 0; i < 50 && sesion.isConnected(); i++) {
            Thread.sleep(100);
        }
        assertFalse(sesion.isConnected(), "el servidor corta la conexión al intentar otra cola");
    }

    // Ayudantes

    BlockingQueue<Map<String, Object>> conectar(Cuenta c) throws Exception {
        BlockingQueue<Map<String, Object>> eventos = new LinkedBlockingQueue<>();
        sesion(c).subscribe("/user/queue/chat", new ColaDeEventos(eventos));
        Thread.sleep(300); // que la suscripcion quede registrada antes de publicar
        return eventos;
    }

    StompSession sesion(Cuenta c) throws Exception {
        StompHeaders connect = new StompHeaders();
        connect.add("Authorization", "Bearer " + c.access());
        return stomp.connectAsync("ws://localhost:" + puerto + "/ws", new WebSocketHttpHeaders(), connect,
                new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);
    }

    record ColaDeEventos(BlockingQueue<Map<String, Object>> eventos) implements StompFrameHandler {
        @Override public Type getPayloadType(StompHeaders headers) { return Map.class; }
        @SuppressWarnings("unchecked")
        @Override public void handleFrame(StompHeaders headers, Object payload) { eventos.add((Map<String, Object>) payload); }
    }

    @SuppressWarnings("unchecked")
    static String textoDe(Map<String, Object> evento) {
        assertNotNull(evento, "no llegó el evento");
        assertEquals("MENSAJE", evento.get("tipo"));
        return (String) ((Map<String, Object>) evento.get("mensaje")).get("texto");
    }

    RestClient api() {
        return RestClient.builder().baseUrl("http://localhost:" + puerto).build();
    }

    String post(Cuenta c, String ruta, Object cuerpo) {
        return api().post().uri(ruta).header("Authorization", "Bearer " + c.access())
                .contentType(MediaType.APPLICATION_JSON).body(cuerpo).retrieve().body(String.class);
    }

    String get(Cuenta c, String ruta) {
        return api().get().uri(ruta).header("Authorization", "Bearer " + c.access()).retrieve().body(String.class);
    }

    int status(Cuenta c, HttpMethod metodo, String ruta, Object cuerpo) {
        var peticion = api().method(metodo).uri(ruta).header("Authorization", "Bearer " + c.access());
        if (cuerpo != null) {
            peticion.contentType(MediaType.APPLICATION_JSON).body(cuerpo);
        }
        return peticion.exchange((req, resp) -> resp.getStatusCode().value());
    }

    static long id(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    Cuenta registrar(String nombre) {
        String cuerpo = api().post().uri("/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", UUID.randomUUID() + "@test.local", "contrasena", "secreta123", "nombre", nombre))
                .retrieve().body(String.class);
        return new Cuenta(((Number) JsonPath.read(cuerpo, "$.usuario.id")).longValue(), JsonPath.read(cuerpo, "$.accessToken"));
    }
}
