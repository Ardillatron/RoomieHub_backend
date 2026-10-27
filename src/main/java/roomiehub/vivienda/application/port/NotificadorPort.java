package roomiehub.vivienda.application.port;

import java.util.Collection;

/** Notificaciones push (sección 10). */
public interface NotificadorPort {

    void notificar(Collection<Long> usuarios, String titulo, String cuerpo);
}
