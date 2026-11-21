package roomiehub.vivienda.domain.exception;

// Base de toda regla de negocio rota y el adaptador web la pasa a HTTP
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
