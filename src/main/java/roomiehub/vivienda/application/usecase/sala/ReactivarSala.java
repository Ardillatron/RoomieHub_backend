package roomiehub.vivienda.application.usecase.sala;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.port.NotificadorPort;
import roomiehub.vivienda.domain.model.finanzas.GastoCompartido;
import roomiehub.vivienda.domain.model.sala.Membresia;
import roomiehub.vivienda.domain.model.sala.Sala;
import roomiehub.vivienda.domain.model.tareas.AsignacionTarea;
import roomiehub.vivienda.domain.repository.AsignacionRepository;
import roomiehub.vivienda.domain.repository.GastoRepository;
import roomiehub.vivienda.domain.repository.MembresiaRepository;
import roomiehub.vivienda.domain.repository.SalaRepository;
import roomiehub.vivienda.domain.service.ReglasDeMembresia;

// CU-08 reactiva dentro de los 15 dias y los periodos desactivados no se recuperan
public class ReactivarSala {

    private final SalaRepository salas;
    private final MembresiaRepository membresias;
    private final AsignacionRepository asignaciones;
    private final GastoRepository gastos;
    private final NotificadorPort notificador;
    private final Clock reloj;

    public ReactivarSala(SalaRepository salas, MembresiaRepository membresias, AsignacionRepository asignaciones,
                         GastoRepository gastos, NotificadorPort notificador, Clock reloj) {
        this.salas = salas;
        this.membresias = membresias;
        this.asignaciones = asignaciones;
        this.gastos = gastos;
        this.notificador = notificador;
        this.reloj = reloj;
    }

    @Transactional
    public void ejecutar(long adminId, long salaId) {
        Instant ahora = reloj.instant();
        List<Membresia> activas = membresias.activasDeSala(salaId);
        ReglasDeMembresia.exigirAdministrador(activas, adminId);
        Sala sala = salas.buscarPorId(salaId).orElseThrow();
        sala.reactivar(ahora);
        salas.guardar(sala);
        // En solo lectura nadie pudo marcar tareas y lo que vencio no se penaliza
        asignaciones.vencerSinPenalizar(salaId, ahora.minus(AsignacionTarea.GRACIA));
        // Los periodos de gastos que empezaron desactivada no generan deudas
        LocalDate hoy = LocalDate.ofInstant(ahora, sala.zonaHoraria());
        for (GastoCompartido gasto : gastos.vigentesDeSala(salaId)) {
            gasto.avanzarGeneracionHasta(hoy);
            gastos.guardar(gasto);
        }
        notificador.notificar(activas.stream().map(Membresia::usuarioId).filter(id -> id != adminId).toList(),
                sala.nombre(), "La sala se reactivó");
    }
}
