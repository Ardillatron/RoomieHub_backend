package roomiehub.vivienda.domain.exception;

/** Base de toda regla de negocio violada. El adaptador web la traduce a un código HTTP. */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
