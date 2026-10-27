package roomiehub.vivienda.application.usecase.tareas;

import java.time.Clock;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.port.NotificadorPort;
import roomiehub.vivienda.application.usecase.sala.AccesoASala;
import roomiehub.vivienda.domain.exception.RecursoNoEncontradoException;
import roomiehub.vivienda.domain.model.sala.Membresia;
import roomiehub.vivienda.domain.model.tareas.AsignacionTarea;
import roomiehub.vivienda.domain.repository.AsignacionRepository;
import roomiehub.vivienda.domain.repository.MembresiaRepository;
import roomiehub.vivienda.domain.repository.PuntosRepository;
import roomiehub.vivienda.domain.repository.TareaRepository;
import roomiehub.vivienda.domain.repository.UsuarioRepository;

/** CU-17: RN-15 y RN-16. La hora que cuenta es la del servidor al recibir la marca. */
public class CompletarTarea {

    private final AccesoASala acceso;
    private final AsignacionRepository asignaciones;
    private final PuntosRepository puntos;
    private final TareaRepository tareas;
    private final UsuarioRepository usuarios;
    private final MembresiaRepository membresias;
    private final NotificadorPort notificador;
    private final Clock reloj;

    public CompletarTarea(AccesoASala acceso, AsignacionRepository asignaciones, PuntosRepository puntos,
                          TareaRepository tareas, UsuarioRepository usuarios, MembresiaRepository membresias,
                          NotificadorPort notificador, Clock reloj) {
        this.acceso = acceso;
        this.asignaciones = asignaciones;
        this.puntos = puntos;
        this.tareas = tareas;
        this.usuarios = usuarios;
        this.membresias = membresias;
        this.notificador = notificador;
        this.reloj = reloj;
    }

    @Transactional
    public AsignacionTarea ejecutar(long usuarioId, long asignacionId) {
        // RLS: una asignación de otra sala no se ve, así que también responde 404.
        AsignacionTarea asignacion = asignaciones.buscarPorId(asignacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Asignación no encontrada"));
        acceso.exigirMiembroDeSalaActiva(asignacion.salaId(), usuarioId);

        puntos.registrar(asignacion.completar(usuarioId, reloj.instant()));
        asignaciones.guardar(asignacion);

        String quien = usuarios.buscarPorId(usuarioId).orElseThrow().nombre();
        String tarea = tareas.buscarPorId(asignacion.tareaId()).orElseThrow().nombre();
        notificador.notificar(membresias.activasDeSala(asignacion.salaId()).stream()
                        .map(Membresia::usuarioId).filter(id -> id != usuarioId).toList(),
                "Tarea completada", quien + " completó: " + tarea);
        return asignacion;
    }
}
