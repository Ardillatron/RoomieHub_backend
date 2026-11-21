package roomiehub.vivienda.application.port;

import java.util.Collection;

import roomiehub.vivienda.application.usecase.convivencia.MensajeVista;

/* Entrega en vivo a quien tiene la app abierta por WebSocket y STOMP
   cada destinatario tiene su cola y quien sale deja de recibir al instante */
public interface ChatEnVivoPort {

    void publicarMensaje(Collection<Long> destinatarios, MensajeVista mensaje);

    void publicarEliminacion(Collection<Long> destinatarios, long salaId, long mensajeId);

    // Con la app abierta el chat llega por WebSocket y no manda push
    boolean estaConectado(long usuarioId);
}
