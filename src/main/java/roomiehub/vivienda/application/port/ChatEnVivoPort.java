package roomiehub.vivienda.application.port;

import java.util.Collection;

import roomiehub.vivienda.application.usecase.convivencia.MensajeVista;

/**
 * Entrega en tiempo real a quien tiene la app abierta (WebSocket + STOMP). Se envía a cada destinatario por
 * su cola privada, así que quien sale o es expulsado deja de recibir en ese mismo instante.
 */
public interface ChatEnVivoPort {

    void publicarMensaje(Collection<Long> destinatarios, MensajeVista mensaje);

    void publicarEliminacion(Collection<Long> destinatarios, long salaId, long mensajeId);

    /** Sección 10: con la app abierta, el chat llega por WebSocket y no genera push. */
    boolean estaConectado(long usuarioId);
}
