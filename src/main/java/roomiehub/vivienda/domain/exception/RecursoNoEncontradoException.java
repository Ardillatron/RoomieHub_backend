package roomiehub.vivienda.domain.exception;

// No existe o RLS no deja verlo y los dos casos responden igual
public class RecursoNoEncontradoException extends DomainException {

    public RecursoNoEncontradoException(String message) {
        super(message);
    }
}
