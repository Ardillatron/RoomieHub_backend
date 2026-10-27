package roomiehub.vivienda.application.usecase.tareas;

import java.time.Instant;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.domain.model.sala.EstadoSala;
import roomiehub.vivienda.domain.model.tareas.AsignacionTarea;
import roomiehub.vivienda.domain.repository.AsignacionRepository;
import roomiehub.vivienda.domain.repository.PuntosRepository;
import roomiehub.vivienda.domain.repository.SalaRepository;

/**
 * CU-24 (Sistema): RN-16. Vence lo no marcado 3 horas después del cierre de su periodo y resta ⌈valor / 2⌉.
 * Atiende todos los periodos vencidos, no solo el último, por si el proceso se atrasó. Pausado en una
 * sala desactivada (RN-09).
 */
public class PenalizarVencidas {

    private final SalaRepository salas;
    private final AsignacionRepository asignaciones;
    private final PuntosRepository puntos;

    public PenalizarVencidas(SalaRepository salas, AsignacionRepository asignaciones, PuntosRepository puntos) {
        this.salas = salas;
        this.asignaciones = asignaciones;
        this.puntos = puntos;
    }

    @Transactional
    public void ejecutar(long salaId, Instant ahora) {
        if (salas.buscarPorId(salaId).orElseThrow().estado() == EstadoSala.DESACTIVADA) {
            return;
        }
        for (AsignacionTarea asignacion : asignaciones.pendientesDeSala(salaId)) {
            if (asignacion.graciaVencida(ahora)) {
                asignacion.vencer(ahora).ifPresent(puntos::registrar);
                asignaciones.guardar(asignacion);
            }
        }
    }
}
