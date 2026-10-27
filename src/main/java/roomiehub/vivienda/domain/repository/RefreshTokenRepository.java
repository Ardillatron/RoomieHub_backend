package roomiehub.vivienda.domain.repository;

import java.time.Instant;
import java.util.Optional;

import roomiehub.vivienda.domain.model.cuenta.RefreshToken;

public interface RefreshTokenRepository {

    void guardar(RefreshToken token);

    Optional<RefreshToken> buscarPorHash(String tokenHash);

    /**
     * Revoca el token solo si nadie lo revocó antes, en una sola operación atómica.
     * @return false si ya estaba revocado (dos renovaciones simultáneas o un token robado)
     */
    boolean revocarSiVigente(long id, Instant ahora);

    void revocarTodos(long usuarioId, Instant ahora);
}
