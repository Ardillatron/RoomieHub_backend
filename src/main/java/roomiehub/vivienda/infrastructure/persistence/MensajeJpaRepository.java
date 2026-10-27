package roomiehub.vivienda.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MensajeJpaRepository extends JpaRepository<MensajeJpaEntity, Long> {

    Optional<MensajeJpaEntity> findByClientUuid(UUID clientUuid);

    List<MensajeJpaEntity> findBySalaIdAndEliminadoFalseOrderByIdDesc(long salaId, Limit limite);

    List<MensajeJpaEntity> findBySalaIdAndEliminadoFalseAndIdLessThanOrderByIdDesc(long salaId, long antesDe,
                                                                                  Limit limite);
}
