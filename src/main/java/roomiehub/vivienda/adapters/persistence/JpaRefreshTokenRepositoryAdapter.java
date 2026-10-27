package roomiehub.vivienda.adapters.persistence;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Component;

import roomiehub.vivienda.domain.model.cuenta.RefreshToken;
import roomiehub.vivienda.domain.repository.RefreshTokenRepository;
import roomiehub.vivienda.infrastructure.persistence.RefreshTokenJpaEntity;
import roomiehub.vivienda.infrastructure.persistence.RefreshTokenJpaRepository;

@Component
public class JpaRefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpa;

    public JpaRefreshTokenRepositoryAdapter(RefreshTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void guardar(RefreshToken token) {
        RefreshTokenJpaEntity e = new RefreshTokenJpaEntity();
        e.setId(token.id());
        e.setUsuarioId(token.usuarioId());
        e.setTokenHash(token.tokenHash());
        e.setDispositivo(token.dispositivo());
        e.setExpiraEn(token.expiraEn());
        e.setRevocadoEn(token.revocadoEn());
        jpa.save(e);
    }

    @Override
    public Optional<RefreshToken> buscarPorHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(e -> new RefreshToken(e.getId(), e.getUsuarioId(),
                e.getTokenHash(), e.getDispositivo(), e.getExpiraEn(), e.getRevocadoEn()));
    }

    @Override
    public boolean revocarSiVigente(long id, Instant ahora) {
        return jpa.revocarSiVigente(id, ahora) == 1;
    }

    @Override
    public void revocarTodos(long usuarioId, Instant ahora) {
        jpa.revocarTodos(usuarioId, ahora);
    }
}
