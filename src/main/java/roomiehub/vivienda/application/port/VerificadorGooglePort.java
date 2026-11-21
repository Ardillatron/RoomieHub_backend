package roomiehub.vivienda.application.port;

public interface VerificadorGooglePort {

    record IdentidadGoogle(String sub, String email, boolean emailVerificado, String nombre, String fotoUrl) {}

    // Valida firma emisor audiencia y vigencia del token de Google y si falla lanza CredencialesInvalidasException
    IdentidadGoogle verificar(String idToken);
}
