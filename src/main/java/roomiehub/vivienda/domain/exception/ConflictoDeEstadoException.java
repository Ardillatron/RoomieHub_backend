package roomiehub.vivienda.domain.exception;

/** La acción no aplica al estado actual (por ejemplo, desactivar una sala ya desactivada). */
public class ConflictoDeEstadoException extends DomainException {

    public ConflictoDeEstadoException(String message) {
        super(message);
    }
}
