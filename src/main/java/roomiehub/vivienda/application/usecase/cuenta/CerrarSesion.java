package roomiehub.vivienda.application.usecase.cuenta;

import java.time.Clock;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.domain.repository.DispositivoRepository;
import roomiehub.vivienda.domain.repository.RefreshTokenRepository;

/**
 * Revoca el refresh token del dispositivo y deja de mandarle push, para que en un teléfono compartido quien
 * cerró sesión no siga recibiendo sus notificaciones. Un token desconocido no es error: la sesión ya no existe.
 */
public class CerrarSesion {

    private final RefreshTokenRepository refreshTokens;
    private final DispositivoRepository dispositivos;
    private final Clock reloj;

    public CerrarSesion(RefreshTokenRepository refreshTokens, DispositivoRepository dispositivos, Clock reloj) {
        this.refreshTokens = refreshTokens;
        this.dispositivos = dispositivos;
        this.reloj = reloj;
    }

    /** @param fcmToken el token de push de ese teléfono, o nulo si no tiene */
    @Transactional
    public void ejecutar(String refreshToken, String fcmToken) {
        refreshTokens.buscarPorHash(EmisorDeSesiones.hash(refreshToken)).ifPresent(token -> {
            refreshTokens.revocarSiVigente(token.id(), reloj.instant());
            if (fcmToken != null) {
                dispositivos.borrarToken(fcmToken);
            }
        });
    }
}
