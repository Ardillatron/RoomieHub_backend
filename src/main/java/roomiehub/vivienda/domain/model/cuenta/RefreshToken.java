package roomiehub.vivienda.domain.model.cuenta;

import java.time.Instant;

/** Sesión de un dispositivo. Solo se guarda el hash del token, nunca el token. */
public record RefreshToken(Long id, long usuarioId, String tokenHash, String dispositivo,
                           Instant expiraEn, Instant revocadoEn) {

    public boolean revocado() {
        return revocadoEn != null;
    }

    public boolean expirado(Instant ahora) {
        return !ahora.isBefore(expiraEn);
    }
}
