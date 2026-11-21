package roomiehub.vivienda.domain.repository;

import java.time.Instant;
import java.util.Optional;

import roomiehub.vivienda.domain.model.cuenta.RefreshToken;

public interface RefreshTokenRepository {

    void guardar(RefreshToken token);

    Optional<RefreshToken> buscarPorHash(String tokenHash);

    /* Revoca el token solo si nadie lo revoco antes en una sola operacion
       regresa false si ya estaba revocado */
    boolean revocarSiVigente(long id, Instant ahora);

    void revocarTodos(long usuarioId, Instant ahora);
}
