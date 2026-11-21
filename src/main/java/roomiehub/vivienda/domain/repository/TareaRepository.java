package roomiehub.vivienda.domain.repository;

import java.util.List;
import java.util.Optional;

import roomiehub.vivienda.domain.model.tareas.Tarea;

public interface TareaRepository {

    Tarea guardar(Tarea tarea);

    Optional<Tarea> buscarPorId(long id);

    List<Tarea> activasDeSala(long salaId);

    // Las asignaciones pendientes de quien sale quedan sin asignar
    void desasignarPendientes(long salaId, long usuarioId);

    // Valor de la tarea activa mas alta de la sala o 0 si no hay tareas
    int valorMasAlto(long salaId);
}
