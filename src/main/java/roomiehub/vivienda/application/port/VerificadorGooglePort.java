package roomiehub.vivienda.application.port;

public interface VerificadorGooglePort {

    record IdentidadGoogle(String sub, String email, boolean emailVerificado, String nombre, String fotoUrl) {}

    /** Valida firma, emisor, audiencia y vigencia del ID token. Lanza CredencialesInvalidasException si falla. */
    IdentidadGoogle verificar(String idToken);
}
