package roomiehub.vivienda.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import roomiehub.vivienda.domain.model.tareas.AsignacionTarea;

public interface AsignacionRepository {

    Optional<AsignacionTarea> buscarPorId(long id);

    boolean existe(long tareaId, Instant periodoInicio);

    /** Las que siguen en ASIGNADA, de cualquier periodo. */
    List<AsignacionTarea> pendientesDeSala(long salaId);

    /** Las de periodos que terminaron después de {@code desde} (las actuales y las recién cerradas). */
    List<AsignacionTarea> recientesDeSala(long salaId, Instant desde);

    /** Se escribe de inmediato. */
    void guardar(AsignacionTarea asignacion);

    /** Al eliminar una tarea, sus asignaciones sin completar desaparecen y ya no se penalizan. */
    void borrarPendientesDeTarea(long tareaId);

    /**
     * Al reactivar una sala (RN-41): lo que venció mientras estaba en solo lectura nadie pudo marcarlo,
     * así que se cierra sin penalizar. Cierra las pendientes cuyo periodo terminó antes de {@code finAntesDe}.
     */
    void vencerSinPenalizar(long salaId, Instant finAntesDe);
}
