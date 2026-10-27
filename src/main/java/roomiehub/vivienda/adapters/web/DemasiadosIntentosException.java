package roomiehub.vivienda.adapters.web;

/** Se respondió 429: la cuenta acumuló demasiados intentos fallidos de inicio de sesión. */
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
