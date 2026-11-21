package roomiehub.vivienda.application.usecase.sync;

import java.util.UUID;

/* Una accion que el telefono hizo sin conexion con el UUID que genero
   cada tipo usa solo sus campos */
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
