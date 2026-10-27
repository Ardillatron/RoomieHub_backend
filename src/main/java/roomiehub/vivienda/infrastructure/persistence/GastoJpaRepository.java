package roomiehub.vivienda.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GastoJpaRepository extends JpaRepository<GastoJpaEntity, Long> {

    List<GastoJpaEntity> findBySalaIdAndEliminadoFalseOrderById(long salaId);

    @Query("""
            SELECT g FROM GastoJpaEntity g
            WHERE g.salaId = :salaId AND g.eliminado = false
              AND EXISTS (SELECT 1 FROM ParticipanteGastoJpaEntity p
                          WHERE p.servicioId = g.id AND p.usuarioId = :usuarioId AND p.activo = true)""")
    List<GastoJpaEntity> conParticipanteActivo(long salaId, long usuarioId);
}
