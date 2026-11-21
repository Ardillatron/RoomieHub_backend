package roomiehub.vivienda.adapters.web;

import java.util.Collection;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;

import roomiehub.vivienda.adapters.web.ChatController.MensajeResponse;
import roomiehub.vivienda.application.port.ChatEnVivoPort;
import roomiehub.vivienda.application.usecase.convivencia.MensajeVista;
import roomiehub.vivienda.infrastructure.config.TrasElCommit;

/* Publica en la cola privada de cada destinatario despues del commit
   para que nadie vea un mensaje que luego se deshizo */
@Component
public class StompChatEnVivo implements ChatEnVivoPort {

    // La app recibe mensaje en MENSAJE y mensajeId en MENSAJE_ELIMINADO
    public record EventoChat(String tipo, long salaId, MensajeResponse mensaje, Long mensajeId) {}

    private static final String COLA = "/queue/chat";

    private final SimpMessagingTemplate stomp;
    private final SimpUserRegistry conectados;

    public StompChatEnVivo(SimpMessagingTemplate stomp, SimpUserRegistry conectados) {
        this.stomp = stomp;
        this.conectados = conectados;
    }

    @Override
    public void publicarMensaje(Collection<Long> destinatarios, MensajeVista mensaje) {
        EventoChat evento = new EventoChat("MENSAJE", mensaje.mensaje().salaId(), MensajeResponse.de(mensaje), null);
        TrasElCommit.ejecutar(() -> enviar(destinatarios, evento));
    }

    @Override
    public void publicarEliminacion(Collection<Long> destinatarios, long salaId, long mensajeId) {
        EventoChat evento = new EventoChat("MENSAJE_ELIMINADO", salaId, null, mensajeId);
        TrasElCommit.ejecutar(() -> enviar(destinatarios, evento));
    }

    @Override
    public boolean estaConectado(long usuarioId) {
        return conectados.getUser(Long.toString(usuarioId)) != null;
    }

    private void enviar(Collection<Long> destinatarios, EventoChat evento) {
        // El nombre del usuario conectado es el subject del JWT que es su id
        destinatarios.forEach(id -> stomp.convertAndSendToUser(Long.toString(id), COLA, evento));
    }
}
