package roomiehub.vivienda.application.usecase.tareas;

import java.time.LocalDate;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.usecase.sala.AccesoASala;
import roomiehub.vivienda.domain.model.tareas.Frecuencia;
import roomiehub.vivienda.domain.model.tareas.Tarea;
import roomiehub.vivienda.domain.model.tareas.ValorTarea;
import roomiehub.vivienda.domain.repository.TareaRepository;

/** CU-16: RN-11 y RN-13. Se asigna en el siguiente roleo (como mucho 15 minutos después). */
public class CrearTarea {

    private final AccesoASala acceso;
    private final TareaRepository tareas;

    public CrearTarea(AccesoASala acceso, TareaRepository tareas) {
        this.acceso = acceso;
        this.tareas = tareas;
    }

    @Transactional
    public Tarea ejecutar(long adminId, long salaId, String nombre, String descripcion, int valor, Frecuencia frecuencia,
                          LocalDate fechaInicio) {
        acceso.exigirAdministradorDeSalaActiva(salaId, adminId);
        return tareas.guardar(Tarea.nueva(salaId, nombre, descripcion, new ValorTarea(valor), frecuencia, fechaInicio));
    }
}
