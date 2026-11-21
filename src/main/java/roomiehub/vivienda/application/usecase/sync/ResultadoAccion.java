package roomiehub.vivienda.application.usecase.sync;

import java.util.UUID;

// Lo que responde el servidor por cada accion de la cola
public record ResultadoAccion(UUID uuid, Estado estado, String motivo, Long recursoId) {

    public enum Estado {
        // Se aplico y recursoId es la nota o mensaje creado y la app la quita de la cola
        PROCESADA,
        // El UUID ya se habia procesado y cuenta como exito y la app la quita
        DUPLICADA,
        // El recurso se borro y la app muestra el error y la quita
        RECHAZADA_RECURSO_ELIMINADO,
        // El usuario ya no es miembro y se rechaza toda la cola de esa sala y la app borra su cache
        RECHAZADA_EXPULSION,
        // Rompio otra regla y la app muestra el error y la quita
        RECHAZADA,
        // Error del servidor y la app la guarda para reintentar despues
        ERROR
    }

    static ResultadoAccion de(AccionOffline accion, Estado estado, String motivo, Long recursoId) {
        return new ResultadoAccion(accion.uuid(), estado, motivo, recursoId);
    }
}
