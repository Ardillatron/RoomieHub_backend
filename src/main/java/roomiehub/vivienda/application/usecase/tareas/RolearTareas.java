package roomiehub.vivienda.application.usecase.tareas;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.port.NotificadorPort;
import roomiehub.vivienda.domain.model.Periodo;
import roomiehub.vivienda.domain.model.sala.EstadoSala;
import roomiehub.vivienda.domain.model.sala.Membresia;
import roomiehub.vivienda.domain.model.sala.Sala;
import roomiehub.vivienda.domain.model.tareas.AsignacionTarea;
import roomiehub.vivienda.domain.model.tareas.Frecuencia;
import roomiehub.vivienda.domain.model.tareas.Tarea;
import roomiehub.vivienda.domain.repository.AsignacionRepository;
import roomiehub.vivienda.domain.repository.MembresiaRepository;
import roomiehub.vivienda.domain.repository.PuntosRepository;
import roomiehub.vivienda.domain.repository.SalaRepository;
import roomiehub.vivienda.domain.repository.TareaRepository;
import roomiehub.vivienda.domain.service.AsignadorDeTareas;
import roomiehub.vivienda.domain.service.AsignadorDeTareas.TareaARolear;

/* CU-23 asigna solo el periodo actual de cada tarea
   si el proceso se atraso los periodos que nadie vio no se crean y una sala desactivada no rolea */
public class RolearTareas {

    private record PorRolear(Tarea tarea, Periodo periodo) {}

    private final SalaRepository salas;
    private final TareaRepository tareas;
    private final AsignacionRepository asignaciones;
    private final MembresiaRepository membresias;
    private final PuntosRepository puntos;
    private final AsignadorDeTareas asignador;
    private final NotificadorPort notificador;

    public RolearTareas(SalaRepository salas, TareaRepository tareas, AsignacionRepository asignaciones,
                        MembresiaRepository membresias, PuntosRepository puntos, AsignadorDeTareas asignador,
                        NotificadorPort notificador) {
        this.salas = salas;
        this.tareas = tareas;
        this.asignaciones = asignaciones;
        this.membresias = membresias;
        this.puntos = puntos;
        this.asignador = asignador;
        this.notificador = notificador;
    }

    @Transactional
    public void ejecutar(long salaId, Instant ahora) {
        Sala sala = salas.buscarPorId(salaId).orElseThrow();
        if (sala.estado() == EstadoSala.DESACTIVADA) {
            return;
        }
        // Cada frecuencia tiene su roleo y todos leen el mismo historico
        Map<Frecuencia, List<PorRolear>> porFrecuencia = new LinkedHashMap<>();
        for (Tarea tarea : tareas.activasDeSala(salaId)) {
            tarea.periodoEn(ahora, sala.zonaHoraria())
                    .filter(p -> !asignaciones.existe(tarea.id(), p.inicio()))
                    .ifPresent(p -> porFrecuencia.computeIfAbsent(tarea.frecuencia(), f -> new ArrayList<>())
                            .add(new PorRolear(tarea, p)));
        }
        if (porFrecuencia.isEmpty()) {
            return;
        }
        Map<Long, Integer> historicos = puntos.historicos(salaId);
        Map<Long, Integer> puntajes = membresias.activasDeSala(salaId).stream()
                .collect(Collectors.toMap(Membresia::usuarioId, m -> historicos.getOrDefault(m.usuarioId(), 0)));

        for (List<PorRolear> grupo : porFrecuencia.values()) {
            Map<Long, Long> responsables = asignador.asignar(
                    grupo.stream().map(r -> new TareaARolear(r.tarea().id(), r.tarea().valor())).toList(), puntajes);
            for (PorRolear r : grupo) {
                Long responsable = responsables.get(r.tarea().id()); // null si la sala no tiene miembros
                asignaciones.guardar(AsignacionTarea.nueva(r.tarea(), r.periodo(), responsable));
                if (responsable != null) {
                    notificador.notificar(List.of(responsable), "Nueva tarea", r.tarea().nombre());
                }
            }
        }
    }
}
