package roomiehub.vivienda.application.usecase.cuenta;

import java.time.Clock;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.domain.repository.DispositivoRepository;
import roomiehub.vivienda.domain.repository.RefreshTokenRepository;

/* Revoca el refresh token del telefono y deja de mandarle push
   un token desconocido no es error porque la sesion ya no existe */
public class CerrarSesion {

    private final RefreshTokenRepository refreshTokens;
    private final DispositivoRepository dispositivos;
    private final Clock reloj;

    public CerrarSesion(RefreshTokenRepository refreshTokens, DispositivoRepository dispositivos, Clock reloj) {
        this.refreshTokens = refreshTokens;
        this.dispositivos = dispositivos;
        this.reloj = reloj;
    }

    // fcmToken es el token de push del telefono o null si no tiene
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
