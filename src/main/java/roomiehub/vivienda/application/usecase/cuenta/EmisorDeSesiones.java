package roomiehub.vivienda.application.usecase.cuenta;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import roomiehub.vivienda.application.port.EmisorDeTokensPort;
import roomiehub.vivienda.domain.model.cuenta.RefreshToken;
import roomiehub.vivienda.domain.model.cuenta.Usuario;
import roomiehub.vivienda.domain.repository.RefreshTokenRepository;

/** Sección 9: JWT de acceso de 15 minutos y refresh token de 30 días, guardado como hash. */
public class EmisorDeSesiones {

    static final Duration DURACION_ACCESO = Duration.ofMinutes(15);
    static final Duration DURACION_REFRESH = Duration.ofDays(30);

    private final EmisorDeTokensPort tokens;
    private final RefreshTokenRepository refreshTokens;
    private final Clock reloj;
    private final SecureRandom azar = new SecureRandom();

    public EmisorDeSesiones(EmisorDeTokensPort tokens, RefreshTokenRepository refreshTokens, Clock reloj) {
        this.tokens = tokens;
        this.refreshTokens = refreshTokens;
        this.reloj = reloj;
    }

    Sesion emitir(Usuario usuario, String dispositivo) {
        Instant ahora = reloj.instant();
        byte[] bytes = new byte[32];
        azar.nextBytes(bytes);
        String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refreshTokens.guardar(new RefreshToken(null, usuario.id(), hash(refresh), recortar(dispositivo),
                ahora.plus(DURACION_REFRESH), null));

        Instant expiraAcceso = ahora.plus(DURACION_ACCESO);
        return new Sesion(tokens.emitirAcceso(usuario.id(), ahora, expiraAcceso), expiraAcceso, refresh, usuario);
    }

    /** SHA-256 basta: el token ya tiene 256 bits aleatorios, no es una contraseña adivinable. */
    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e); // SHA-256 existe en toda JVM
        }
    }

    private static String recortar(String dispositivo) {
        return dispositivo == null || dispositivo.length() <= 100 ? dispositivo : dispositivo.substring(0, 100);
    }
}
