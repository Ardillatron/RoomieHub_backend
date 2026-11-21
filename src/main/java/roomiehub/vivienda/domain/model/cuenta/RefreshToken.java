package roomiehub.vivienda.domain.model.cuenta;

import java.time.Instant;

// Sesion de un telefono y solo se guarda el hash del token
public record RefreshToken(Long id, long usuarioId, String tokenHash, String dispositivo,
                           Instant expiraEn, Instant revocadoEn) {

    public boolean revocado() {
        return revocadoEn != null;
    }

    public boolean expirado(Instant ahora) {
        return !ahora.isBefore(expiraEn);
    }
}
