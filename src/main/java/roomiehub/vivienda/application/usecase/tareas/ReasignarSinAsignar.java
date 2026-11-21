package roomiehub.vivienda.application.usecase.tareas;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.port.NotificadorPort;
import roomiehub.vivienda.application.usecase.sala.AccesoASala;
import roomiehub.vivienda.domain.model.sala.Membresia;
import roomiehub.vivienda.domain.model.tareas.AsignacionTarea;
import roomiehub.vivienda.domain.repository.AsignacionRepository;
import roomiehub.vivienda.domain.repository.MembresiaRepository;
import roomiehub.vivienda.domain.repository.PuntosRepository;
import roomiehub.vivienda.domain.service.AsignadorDeTareas;
import roomiehub.vivienda.domain.service.AsignadorDeTareas.TareaARolear;

// CU-19 el admin reparte con el mismo algoritmo las que quedaron sin responsable
public class ReasignarSinAsignar {

    private final AccesoASala acceso;
    private final AsignacionRepository asignaciones;
    private final MembresiaRepository membresias;
    private final PuntosRepository puntos;
    private final AsignadorDeTareas asignador;
    private final NotificadorPort notificador;
    private final Clock reloj;

    public ReasignarSinAsignar(AccesoASala acceso, AsignacionRepository asignaciones, MembresiaRepository membresias,
                               PuntosRepository puntos, AsignadorDeTareas asignador, NotificadorPort notificador,
                               Clock reloj) {
        this.acceso = acceso;
        this.asignaciones = asignaciones;
        this.membresias = membresias;
        this.puntos = puntos;
        this.asignador = asignador;
        this.notificador = notificador;
        this.reloj = reloj;
    }

    // Regresa cuantas asignaciones recibieron responsable
    @Transactional
    public int ejecutar(long adminId, long salaId) {
        acceso.exigirAdministradorDeSalaActiva(salaId, adminId);
        Instant ahora = reloj.instant();
        // Solo las de periodos abiertos porque las cerradas ya no se pueden cumplir
        Map<Long, AsignacionTarea> sinAsignar = asignaciones.pendientesDeSala(salaId).stream()
                .filter(a -> a.usuarioId() == null && a.fin().isAfter(ahora))
                .collect(Collectors.toMap(AsignacionTarea::id, Function.identity()));
        if (sinAsignar.isEmpty()) {
            return 0;
        }
        Map<Long, Integer> historicos = puntos.historicos(salaId);
        Map<Long, Integer> puntajes = membresias.activasDeSala(salaId).stream()
                .collect(Collectors.toMap(Membresia::usuarioId, m -> historicos.getOrDefault(m.usuarioId(), 0)));

        // El asignador recibe ids de asignacion y no de tarea porque cada una es un periodo distinto
        List<TareaARolear> pendientes = sinAsignar.values().stream()
                .map(a -> new TareaARolear(a.id(), a.valorAlAsignar()))
                .toList();
        asignador.asignar(pendientes, puntajes).forEach((asignacionId, usuarioId) -> {
            AsignacionTarea asignacion = sinAsignar.get(asignacionId);
            asignacion.asignarA(usuarioId);
            asignaciones.guardar(asignacion);
            notificador.notificar(List.of(usuarioId), "Nueva tarea", "Se te asignó una tarea pendiente");
        });
        return sinAsignar.size();
    }
}
