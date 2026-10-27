package roomiehub.vivienda.application.usecase.convivencia;

import java.time.Clock;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.port.NotificadorPort;
import roomiehub.vivienda.application.usecase.sala.AccesoASala;
import roomiehub.vivienda.domain.model.convivencia.Nota;
import roomiehub.vivienda.domain.model.convivencia.Reporte;
import roomiehub.vivienda.domain.model.sala.Membresia;
import roomiehub.vivienda.domain.model.sala.Rol;
import roomiehub.vivienda.domain.repository.NotaRepository;
import roomiehub.vivienda.domain.repository.ReporteRepository;

/** CU-10: RN-29, el reporte llega al Administrador, quien decide qué hacer. */
public class ReportarNota {

    private final AccesoASala acceso;
    private final NotaRepository notas;
    private final ReporteRepository reportes;
    private final NotificadorPort notificador;
    private final Clock reloj;

    public ReportarNota(AccesoASala acceso, NotaRepository notas, ReporteRepository reportes,
                        NotificadorPort notificador, Clock reloj) {
        this.acceso = acceso;
        this.notas = notas;
        this.reportes = reportes;
        this.notificador = notificador;
        this.reloj = reloj;
    }

    @Transactional
    public Reporte ejecutar(long usuarioId, long notaId, String motivo) {
        Nota nota = ReaccionarNota.notaVisible(notas, notaId);
        List<Membresia> miembros = acceso.exigirMiembro(nota.salaId(), usuarioId);
        acceso.exigirMiembroDeSalaActiva(nota.salaId(), usuarioId);

        Reporte reporte = reportes.guardar(Reporte.deNota(nota.salaId(), notaId, usuarioId, motivo, reloj.instant()));
        notificador.notificar(miembros.stream().filter(m -> m.rol() == Rol.ADMIN).map(Membresia::usuarioId).toList(),
                "Nota reportada", reporte.motivo());
        return reporte;
    }
}
