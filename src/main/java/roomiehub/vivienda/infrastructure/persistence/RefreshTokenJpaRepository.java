package roomiehub.vivienda.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, Long> {

    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshTokenJpaEntity t SET t.revocadoEn = :ahora WHERE t.id = :id AND t.revocadoEn IS NULL")
    int revocarSiVigente(long id, Instant ahora);

    @Modifying
    @Query("UPDATE RefreshTokenJpaEntity t SET t.revocadoEn = :ahora WHERE t.usuarioId = :usuarioId AND t.revocadoEn IS NULL")
    int revocarTodos(long usuarioId, Instant ahora);
}
