package roomiehub.vivienda.domain.exception;

// Un value object rechazo su valor al crearse
public class DatoInvalidoException extends DomainException {

    public DatoInvalidoException(String message) {
        super(message);
    }
}
