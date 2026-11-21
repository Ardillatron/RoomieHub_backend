package roomiehub.vivienda.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import roomiehub.vivienda.domain.model.tareas.AsignacionTarea;

public interface AsignacionRepository {

    Optional<AsignacionTarea> buscarPorId(long id);

    boolean existe(long tareaId, Instant periodoInicio);

    // Las que siguen en ASIGNADA de cualquier periodo
    List<AsignacionTarea> pendientesDeSala(long salaId);

    // Las de periodos que terminaron despues de desde
    List<AsignacionTarea> recientesDeSala(long salaId, Instant desde);

    // Se escribe de inmediato
    void guardar(AsignacionTarea asignacion);

    // Al borrar una tarea sus asignaciones sin completar desaparecen y no se castigan
    void borrarPendientesDeTarea(long tareaId);

    /* Al reactivar una sala lo que vencio en solo lectura se cierra sin castigo
       cierra las pendientes cuyo periodo termino antes de finAntesDe */
    void vencerSinPenalizar(long salaId, Instant finAntesDe);
}
