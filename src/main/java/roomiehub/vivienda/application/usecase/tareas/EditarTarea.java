package roomiehub.vivienda.application.usecase.tareas;

import java.time.Clock;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.usecase.sala.AccesoASala;
import roomiehub.vivienda.domain.exception.RecursoNoEncontradoException;
import roomiehub.vivienda.domain.model.sala.Sala;
import roomiehub.vivienda.domain.model.tareas.Frecuencia;
import roomiehub.vivienda.domain.model.tareas.Tarea;
import roomiehub.vivienda.domain.model.tareas.ValorTarea;
import roomiehub.vivienda.domain.repository.TareaRepository;

// CU-16 edita una tarea y los cambios solo cuentan para lo que sigue
public class EditarTarea {

    private final AccesoASala acceso;
    private final TareaRepository tareas;
    private final Clock reloj;

    public EditarTarea(AccesoASala acceso, TareaRepository tareas, Clock reloj) {
        this.acceso = acceso;
        this.tareas = tareas;
        this.reloj = reloj;
    }

    @Transactional
    public Tarea ejecutar(long adminId, long salaId, long tareaId, String nombre, int valor, Frecuencia frecuencia) {
        Sala sala = acceso.exigirAdministradorDeSalaActiva(salaId, adminId);
        Tarea tarea = tareaActiva(tareas, salaId, tareaId);
        tarea.editar(nombre, new ValorTarea(valor), frecuencia, reloj.instant(), sala.zonaHoraria());
        return tareas.guardar(tarea);
    }

    static Tarea tareaActiva(TareaRepository tareas, long salaId, long tareaId) {
        return tareas.buscarPorId(tareaId)
                .filter(t -> t.salaId() == salaId && t.activa())
                .orElseThrow(() -> new RecursoNoEncontradoException("Tarea no encontrada"));
    }
}
