package roomiehub.vivienda.application.port;

/** Un servicio externo no está configurado o no responde; el adaptador web responde 503. */
public class ServicioNoDisponibleException extends RuntimeException {

    public ServicioNoDisponibleException(String message) {
        super(message);
    }
}
