package roomiehub.vivienda.domain.exception;

// Correo contraseña o token incorrectos sin decir cual para no revelar cuentas
public class CredencialesInvalidasException extends DomainException {

    public CredencialesInvalidasException() {
        super("Credenciales inválidas");
    }
}
