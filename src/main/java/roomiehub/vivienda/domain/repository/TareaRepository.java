package roomiehub.vivienda.domain.repository;

import java.util.List;
import java.util.Optional;

import roomiehub.vivienda.domain.model.tareas.Tarea;

public interface TareaRepository {

    Tarea guardar(Tarea tarea);

    Optional<Tarea> buscarPorId(long id);

    List<Tarea> activasDeSala(long salaId);

    /** RN-08: las asignaciones pendientes de quien sale quedan sin asignar (RN-19 las reasigna). */
    void desasignarPendientes(long salaId, long usuarioId);

    /** RN-18: valor de la tarea activa más alta de la sala, o 0 si no hay tareas. */
    int valorMasAlto(long salaId);
}
