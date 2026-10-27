package roomiehub.vivienda.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotaJpaRepository extends JpaRepository<NotaJpaEntity, Long> {

    Optional<NotaJpaEntity> findByClientUuid(UUID clientUuid);

    // Sin las fijadas vigentes: esas ya van arriba del tablón.
    @Query("""
            SELECT n FROM NotaJpaEntity n
            WHERE n.salaId = :salaId AND n.eliminada = false AND (n.fijadaHasta IS NULL OR n.fijadaHasta <= :ahora)
            ORDER BY n.id DESC""")
    List<NotaJpaEntity> noFijadas(long salaId, Instant ahora, Limit limite);

    @Query("""
            SELECT n FROM NotaJpaEntity n
            WHERE n.salaId = :salaId AND n.eliminada = false AND (n.fijadaHasta IS NULL OR n.fijadaHasta <= :ahora)
              AND n.id < :antesDe
            ORDER BY n.id DESC""")
    List<NotaJpaEntity> noFijadasAntesDe(long salaId, Instant ahora, long antesDe, Limit limite);

    List<NotaJpaEntity> findBySalaIdAndEliminadaFalseAndFijadaHastaAfterOrderByFijadaHastaDesc(long salaId,
                                                                                              Instant ahora);

    @Modifying
    @Query("UPDATE NotaJpaEntity n SET n.fijadaHasta = null WHERE n.salaId = :salaId AND n.fijadaHasta <= :ahora")
    int desfijarVencidas(long salaId, Instant ahora);
}
