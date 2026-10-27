package roomiehub.vivienda.application.usecase.sync;

import java.util.UUID;

/**
 * RN-34 y RN-35: una acción que el teléfono hizo sin conexión, con el UUID que generó al hacerla. Cada tipo
 * usa solo sus campos: CREAR_NOTA (texto, imagenId), ENVIAR_MENSAJE (texto, imagenId, notaRespondidaId),
 * AVISAR_PAGO y CONFIRMAR_PAGO (deudaId), COMPLETAR_TAREA (asignacionId).
 */
public record AccionOffline(UUID uuid, Tipo tipo, long salaId, String texto, String imagenId, Long notaRespondidaId,
                            Long deudaId, Long asignacionId) {

    public enum Tipo {
        CREAR_NOTA,
        ENVIAR_MENSAJE,
        AVISAR_PAGO,
        CONFIRMAR_PAGO,
        COMPLETAR_TAREA
    }
}
