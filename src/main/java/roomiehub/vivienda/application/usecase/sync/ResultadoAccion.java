package roomiehub.vivienda.application.usecase.sync;

import java.util.UUID;

/** Sección 8: lo que el servidor responde por cada acción de la cola. */
public record ResultadoAccion(UUID uuid, Estado estado, String motivo, Long recursoId) {

    public enum Estado {
        /** Se aplicó. {@code recursoId}: la nota o el mensaje creado. La app la quita de la cola. */
        PROCESADA,
        /** El UUID ya se había procesado; cuenta como éxito y la app la quita de la cola. */
        DUPLICADA,
        /** El recurso se eliminó (por ejemplo, el gasto). La app muestra el error y la quita. */
        RECHAZADA_RECURSO_ELIMINADO,
        /** El usuario ya no es miembro: se rechaza toda la cola de esa sala y la app borra su caché (RN-36). */
        RECHAZADA_EXPULSION,
        /** Violó otra regla (por ejemplo, marcar una tarea fuera de plazo). La app muestra el error y la quita. */
        RECHAZADA,
        /** Falla inesperada del servidor: la app la conserva y la reintenta después, para no perderla. */
        ERROR
    }

    static ResultadoAccion de(AccionOffline accion, Estado estado, String motivo, Long recursoId) {
        return new ResultadoAccion(accion.uuid(), estado, motivo, recursoId);
    }
}
