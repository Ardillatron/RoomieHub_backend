package roomiehub.vivienda.application.usecase.sala;

import java.time.Clock;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.port.NotificadorPort;
import roomiehub.vivienda.domain.model.sala.Membresia;
import roomiehub.vivienda.domain.model.sala.Sala;
import roomiehub.vivienda.domain.repository.MembresiaRepository;
import roomiehub.vivienda.domain.repository.SalaRepository;
import roomiehub.vivienda.domain.service.ReglasDeMembresia;

// CU-08 solo lectura por 15 dias y despues la borra CU-29
public class DesactivarSala {

    private final SalaRepository salas;
    private final MembresiaRepository membresias;
    private final NotificadorPort notificador;
    private final Clock reloj;

    public DesactivarSala(SalaRepository salas, MembresiaRepository membresias, NotificadorPort notificador,
                          Clock reloj) {
        this.salas = salas;
        this.membresias = membresias;
        this.notificador = notificador;
        this.reloj = reloj;
    }

    @Transactional
    public void ejecutar(long adminId, long salaId) {
        List<Membresia> activas = membresias.activasDeSala(salaId);
        ReglasDeMembresia.exigirAdministrador(activas, adminId);
        Sala sala = salas.buscarPorId(salaId).orElseThrow();
        sala.desactivar(reloj.instant());
        salas.guardar(sala);
        notificador.notificar(activas.stream().map(Membresia::usuarioId).filter(id -> id != adminId).toList(),
                sala.nombre(), "La sala se desactivó: queda en solo lectura 15 días");
    }
}
