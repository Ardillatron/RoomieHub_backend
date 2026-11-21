package roomiehub.vivienda.adapters.web;

// Responde 429 cuando la cuenta tiene demasiados intentos fallidos de login
public class DemasiadosIntentosException extends RuntimeException {

    private final long segundos;

    public DemasiadosIntentosException(long segundos) {
        super("Demasiados intentos fallidos; intenta de nuevo más tarde");
        this.segundos = segundos;
    }

    public long segundos() {
        return segundos;
    }
}
