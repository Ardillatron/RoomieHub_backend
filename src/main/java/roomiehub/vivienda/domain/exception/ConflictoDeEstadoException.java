package roomiehub.vivienda.domain.exception;

// La accion no aplica al estado actual como desactivar una sala ya desactivada
public class ConflictoDeEstadoException extends DomainException {

    public ConflictoDeEstadoException(String message) {
        super(message);
    }
}
