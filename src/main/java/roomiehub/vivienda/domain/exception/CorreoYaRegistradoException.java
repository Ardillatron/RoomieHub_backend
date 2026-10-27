package roomiehub.vivienda.domain.exception;

public class CorreoYaRegistradoException extends DomainException {

    public CorreoYaRegistradoException() {
        super("Ese correo ya tiene una cuenta");
    }
}
