package roomiehub.vivienda.application.usecase.tareas;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.usecase.sala.AccesoASala;
import roomiehub.vivienda.domain.model.tareas.Tarea;
import roomiehub.vivienda.domain.repository.AsignacionRepository;
import roomiehub.vivienda.domain.repository.TareaRepository;

/** CU-16: borrado lógico. Sus asignaciones sin completar desaparecen, para no penalizar una tarea que ya no existe. */
public class EliminarTarea {

    private final AccesoASala acceso;
    private final TareaRepository tareas;
    private final AsignacionRepository asignaciones;

    public EliminarTarea(AccesoASala acceso, TareaRepository tareas, AsignacionRepository asignaciones) {
        this.acceso = acceso;
        this.tareas = tareas;
        this.asignaciones = asignaciones;
    }

    @Transactional
    public void ejecutar(long adminId, long salaId, long tareaId) {
        acceso.exigirAdministradorDeSalaActiva(salaId, adminId);
        Tarea tarea = EditarTarea.tareaActiva(tareas, salaId, tareaId);
        tarea.eliminar();
        tareas.guardar(tarea);
        asignaciones.borrarPendientesDeTarea(tareaId);
    }
}
