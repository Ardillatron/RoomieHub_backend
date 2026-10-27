package roomiehub.vivienda.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ParticipanteGastoJpaRepository extends JpaRepository<ParticipanteGastoJpaEntity, Long> {

    List<ParticipanteGastoJpaEntity> findByServicioIdAndActivoTrueOrderByOrden(long servicioId);

    List<ParticipanteGastoJpaEntity> findByServicioIdOrderByOrden(long servicioId);

    @Modifying
    @Query("UPDATE ParticipanteGastoJpaEntity p SET p.activo = false WHERE p.servicioId = :servicioId AND p.usuarioId = :usuarioId")
    int desactivar(long servicioId, long usuarioId);
}
