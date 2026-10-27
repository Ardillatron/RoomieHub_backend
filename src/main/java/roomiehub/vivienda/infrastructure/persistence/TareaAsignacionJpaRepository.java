package roomiehub.vivienda.infrastructure.persistence;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import roomiehub.vivienda.domain.model.tareas.EstadoAsignacion;

public interface TareaAsignacionJpaRepository extends JpaRepository<TareaAsignacionJpaEntity, Long> {

    boolean existsByTareaIdAndPeriodoInicio(long tareaId, Instant periodoInicio);

    List<TareaAsignacionJpaEntity> findBySalaIdAndEstado(long salaId, EstadoAsignacion estado);

    List<TareaAsignacionJpaEntity> findBySalaIdAndPeriodoFinAfterOrderByPeriodoInicioAscIdAsc(long salaId, Instant desde);

    @Modifying
    @Query("""
            UPDATE TareaAsignacionJpaEntity a SET a.usuarioId = null
            WHERE a.salaId = :salaId AND a.usuarioId = :usuarioId AND a.estado = :estado""")
    int desasignar(long salaId, long usuarioId, EstadoAsignacion estado);

    @Modifying
    @Query("DELETE FROM TareaAsignacionJpaEntity a WHERE a.tareaId = :tareaId AND a.estado = :estado")
    int borrarDeTarea(long tareaId, EstadoAsignacion estado);

    @Modifying
    @Query("""
            UPDATE TareaAsignacionJpaEntity a SET a.estado = :vencida
            WHERE a.salaId = :salaId AND a.estado = :asignada AND a.periodoFin < :finAntesDe""")
    int vencerSinPenalizar(long salaId, Instant finAntesDe, EstadoAsignacion asignada, EstadoAsignacion vencida);
}
