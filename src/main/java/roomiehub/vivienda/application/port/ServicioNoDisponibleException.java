package roomiehub.vivienda.application.port;

// Un servicio externo no esta configurado o no responde y se responde 503
public class ServicioNoDisponibleException extends RuntimeException {

    public ServicioNoDisponibleException(String message) {
        super(message);
    }
}
