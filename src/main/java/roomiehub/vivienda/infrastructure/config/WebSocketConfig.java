package roomiehub.vivienda.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import roomiehub.vivienda.infrastructure.security.AutenticacionStompInterceptor;

// Chat en vivo por WebSocket en /ws con STOMP y un broker en memoria con colas por usuario
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final AutenticacionStompInterceptor autenticacion;

    public WebSocketConfig(AutenticacionStompInterceptor autenticacion) {
        this.autenticacion = autenticacion;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        ThreadPoolTaskScheduler latidos = new ThreadPoolTaskScheduler();
        latidos.setThreadNamePrefix("stomp-latidos-");
        latidos.initialize();
        /* shortcut broker en memoria de una sola instancia y con varias hay que usar uno externo
           los latidos cada 10 segundos mantienen viva la conexion detras del proxy de Render */
        registry.enableSimpleBroker("/queue").setHeartbeatValue(new long[] {10_000, 10_000}).setTaskScheduler(latidos);
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(autenticacion);
    }
}
