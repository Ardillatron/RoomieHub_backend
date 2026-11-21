package roomiehub.vivienda.application.port;

import java.util.Collection;

// Notificaciones push
public interface NotificadorPort {

    void notificar(Collection<Long> usuarios, String titulo, String cuerpo);
}
