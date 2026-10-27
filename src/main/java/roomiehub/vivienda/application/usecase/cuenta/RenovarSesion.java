package roomiehub.vivienda.application.usecase.cuenta;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.domain.exception.CredencialesInvalidasException;
import roomiehub.vivienda.domain.model.cuenta.RefreshToken;
import roomiehub.vivienda.domain.model.cuenta.Usuario;
import roomiehub.vivienda.domain.repository.RefreshTokenRepository;
import roomiehub.vivienda.domain.repository.UsuarioRepository;

/** Sección 9: el refresh token se rota en cada uso. */
public class RenovarSesion {

    static final Duration GRACIA_REINTENTO = Duration.ofSeconds(10);

    private final RefreshTokenRepository refreshTokens;
    private final UsuarioRepository usuarios;
    private final EmisorDeSesiones sesiones;
    private final Clock reloj;

    public RenovarSesion(RefreshTokenRepository refreshTokens, UsuarioRepository usuarios,
                         EmisorDeSesiones sesiones, Clock reloj) {
        this.refreshTokens = refreshTokens;
        this.usuarios = usuarios;
        this.sesiones = sesiones;
        this.reloj = reloj;
    }

    // noRollbackFor: si se detecta reúso, la revocación de todas las sesiones debe quedar guardada.
    @Transactional(noRollbackFor = CredencialesInvalidasException.class)
    public Sesion ejecutar(String refreshToken) {
        Instant ahora = reloj.instant();
        RefreshToken actual = refreshTokens.buscarPorHash(EmisorDeSesiones.hash(refreshToken))
                .orElseThrow(CredencialesInvalidasException::new);
        if (actual.expirado(ahora)) {
            throw new CredencialesInvalidasException();
        }
        if (!refreshTokens.revocarSiVigente(actual.id(), ahora)) {
            // Revocado hace un instante (o en paralelo, por eso aún se leyó vigente): es el mismo teléfono que
            // renovó dos veces a la vez o reintentó tras perder la respuesta. Solo se rechaza.
            boolean reintento = actual.revocadoEn() == null || actual.revocadoEn().isAfter(ahora.minus(GRACIA_REINTENTO));
            if (!reintento) {
                // Reusar un token ya rotado indica que alguien más lo tiene: se cierran todas las sesiones.
                refreshTokens.revocarTodos(actual.usuarioId(), ahora);
            }
            throw new CredencialesInvalidasException();
        }
        Usuario usuario = usuarios.buscarPorId(actual.usuarioId())
                .filter(u -> u.eliminadoEn() == null)
                .orElseThrow(CredencialesInvalidasException::new);
        return sesiones.emitir(usuario, actual.dispositivo());
    }
}
