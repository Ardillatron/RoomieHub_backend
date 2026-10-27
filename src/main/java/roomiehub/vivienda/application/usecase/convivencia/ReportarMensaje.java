package roomiehub.vivienda.application.usecase.convivencia;

import java.time.Clock;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.port.NotificadorPort;
import roomiehub.vivienda.application.usecase.sala.AccesoASala;
import roomiehub.vivienda.domain.exception.RecursoNoEncontradoException;
import roomiehub.vivienda.domain.model.convivencia.MensajeChat;
import roomiehub.vivienda.domain.model.convivencia.Reporte;
import roomiehub.vivienda.domain.model.sala.Membresia;
import roomiehub.vivienda.domain.model.sala.Rol;
import roomiehub.vivienda.domain.repository.MensajeRepository;
import roomiehub.vivienda.domain.repository.ReporteRepository;

/** CU-32: RN-29, el reporte llega al Administrador. */
public class ReportarMensaje {

    private final AccesoASala acceso;
    private final MensajeRepository mensajes;
    private final ReporteRepository reportes;
    private final NotificadorPort notificador;
    private final Clock reloj;

    public ReportarMensaje(AccesoASala acceso, MensajeRepository mensajes, ReporteRepository reportes,
                           NotificadorPort notificador, Clock reloj) {
        this.acceso = acceso;
        this.mensajes = mensajes;
        this.reportes = reportes;
        this.notificador = notificador;
        this.reloj = reloj;
    }

    @Transactional
    public Reporte ejecutar(long usuarioId, long mensajeId, String motivo) {
        MensajeChat mensaje = mensajeVisible(mensajes, mensajeId);
        List<Membresia> miembros = acceso.exigirMiembro(mensaje.salaId(), usuarioId);
        acceso.exigirMiembroDeSalaActiva(mensaje.salaId(), usuarioId);

        Reporte reporte = reportes.guardar(Reporte.deMensaje(mensaje.salaId(), mensajeId, usuarioId, motivo,
                reloj.instant()));
        notificador.notificar(miembros.stream().filter(m -> m.rol() == Rol.ADMIN).map(Membresia::usuarioId).toList(),
                "Mensaje reportado", reporte.motivo());
        return reporte;
    }

    /** RLS: un mensaje de otra sala no se ve, así que responde igual que uno eliminado o inexistente. */
    static MensajeChat mensajeVisible(MensajeRepository mensajes, long mensajeId) {
        MensajeChat mensaje = mensajes.buscarPorId(mensajeId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mensaje no encontrado"));
        mensaje.exigirVisible();
        return mensaje;
    }
}
