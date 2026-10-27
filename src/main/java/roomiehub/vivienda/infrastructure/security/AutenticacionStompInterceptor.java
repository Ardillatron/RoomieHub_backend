package roomiehub.vivienda.infrastructure.security;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Sección 9: el JWT se valida en el CONNECT de STOMP (el handshake HTTP no lleva encabezados desde el
 * teléfono). Cada quien solo puede suscribirse a su propia cola; nadie publica: los mensajes entran por REST.
 */
@Component
public class AutenticacionStompInterceptor implements ChannelInterceptor {

    /** La única suscripción permitida; Spring la traduce a la cola privada del usuario conectado. */
    public static final String COLA_DEL_CHAT = "/user/queue/chat";

    private final JwtDecoder jwtDecoder;

    public AutenticacionStompInterceptor(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor stomp = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (stomp == null || stomp.getCommand() == null) {
            return message;
        }
        StompCommand comando = stomp.getCommand();
        if (comando == StompCommand.CONNECT) {
            stomp.setUser(autenticar(stomp.getFirstNativeHeader("Authorization")));
        } else if (comando == StompCommand.SUBSCRIBE) {
            if (stomp.getUser() == null || !COLA_DEL_CHAT.equals(stomp.getDestination())) {
                throw new MessageDeliveryException("Solo se permite suscribirse a " + COLA_DEL_CHAT);
            }
        } else if (comando == StompCommand.SEND) {
            throw new MessageDeliveryException("Los mensajes se envían por POST /salas/{id}/mensajes");
        }
        return message;
    }

    private JwtAuthenticationToken autenticar(String encabezado) {
        if (encabezado == null || !encabezado.startsWith("Bearer ")) {
            throw new MessageDeliveryException("Falta el JWT en el encabezado Authorization del CONNECT");
        }
        try {
            // shortcut: el JWT se revisa al conectar y la conexión sigue aunque expire después; lo que se entrega
            // ya se limita a los miembros activos de cada sala.
            return new JwtAuthenticationToken(jwtDecoder.decode(encabezado.substring("Bearer ".length())));
        } catch (JwtException e) {
            throw new MessageDeliveryException("JWT inválido");
        }
    }
}
