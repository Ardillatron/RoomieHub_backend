package roomiehub.vivienda.application.usecase.convivencia;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.usecase.sala.AccesoASala;
import roomiehub.vivienda.domain.exception.RecursoNoEncontradoException;
import roomiehub.vivienda.domain.model.convivencia.MensajeChat;
import roomiehub.vivienda.domain.model.convivencia.Nota;
import roomiehub.vivienda.domain.model.convivencia.Reporte;
import roomiehub.vivienda.domain.model.cuenta.Usuario;
import roomiehub.vivienda.domain.repository.MensajeRepository;
import roomiehub.vivienda.domain.repository.NotaRepository;
import roomiehub.vivienda.domain.repository.ReporteRepository;
import roomiehub.vivienda.domain.repository.UsuarioRepository;
import roomiehub.vivienda.domain.service.ReglasDeMembresia;

/* El admin recibe y resuelve los reportes
   resolver solo los cierra y borrar la nota o mensaje se hace aparte */
public class ModerarReportes {

    // contenido es el texto de la nota o mensaje reportado
    public record ReporteVista(Reporte reporte, String reportadoPor, String contenido) {}

    private final AccesoASala acceso;
    private final ReporteRepository reportes;
    private final NotaRepository notas;
    private final MensajeRepository mensajes;
    private final UsuarioRepository usuarios;

    public ModerarReportes(AccesoASala acceso, ReporteRepository reportes, NotaRepository notas,
                           MensajeRepository mensajes, UsuarioRepository usuarios) {
        this.acceso = acceso;
        this.reportes = reportes;
        this.notas = notas;
        this.mensajes = mensajes;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public List<ReporteVista> pendientes(long adminId, long salaId) {
        ReglasDeMembresia.exigirAdministrador(acceso.exigirMiembro(salaId, adminId), adminId);
        List<Reporte> lista = reportes.pendientesDeSala(salaId);
        Map<Long, String> nombres = usuarios.buscarPorIds(lista.stream().map(Reporte::usuarioId).distinct().toList())
                .stream().collect(Collectors.toMap(Usuario::id, Usuario::nombre));
        return lista.stream()
                .map(r -> new ReporteVista(r, nombres.get(r.usuarioId()), r.notaId() != null
                        ? notas.buscarPorId(r.notaId()).map(Nota::texto).orElse(null)
                        : mensajes.buscarPorId(r.mensajeId()).map(MensajeChat::texto).orElse(null)))
                .toList();
    }

    @Transactional
    public void resolver(long adminId, long reporteId) {
        Reporte reporte = reportes.buscarPorId(reporteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reporte no encontrado"));
        ReglasDeMembresia.exigirAdministrador(acceso.exigirMiembro(reporte.salaId(), adminId), adminId);
        reporte.resolver();
        reportes.guardar(reporte);
    }
}
