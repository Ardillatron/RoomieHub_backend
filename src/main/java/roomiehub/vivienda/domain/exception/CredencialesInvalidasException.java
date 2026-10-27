package roomiehub.vivienda.domain.exception;

/** Correo, contraseña o token incorrectos. El mensaje no dice cuál, para no revelar cuentas. */
public class CredencialesInvalidasException extends DomainException {

    public CredencialesInvalidasException() {
        super("Credenciales inválidas");
    }
}
