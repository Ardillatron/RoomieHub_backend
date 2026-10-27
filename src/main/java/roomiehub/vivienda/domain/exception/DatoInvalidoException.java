package roomiehub.vivienda.domain.exception;

/** Un value object rechazó su valor al construirse (por ejemplo, ValorTarea = 11). */
public class DatoInvalidoException extends DomainException {

    public DatoInvalidoException(String message) {
        super(message);
    }
}
