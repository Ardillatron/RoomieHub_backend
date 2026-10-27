package roomiehub.vivienda.domain.exception;

/** El usuario no puede hacer esta acción (por ejemplo, confirmar su propia deuda). */
public class OperacionNoPermitidaException extends DomainException {

    public OperacionNoPermitidaException(String message) {
        super(message);
    }
}
