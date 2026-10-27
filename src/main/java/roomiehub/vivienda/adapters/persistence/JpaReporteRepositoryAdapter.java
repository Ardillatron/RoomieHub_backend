package roomiehub.vivienda.adapters.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import roomiehub.vivienda.domain.model.convivencia.Reporte;
import roomiehub.vivienda.domain.repository.ReporteRepository;
import roomiehub.vivienda.infrastructure.persistence.ReporteJpaEntity;
import roomiehub.vivienda.infrastructure.persistence.ReporteJpaRepository;

@Component
public class JpaReporteRepositoryAdapter implements ReporteRepository {

    private final ReporteJpaRepository jpa;

    public JpaReporteRepositoryAdapter(ReporteJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Reporte guardar(Reporte r) {
        ReporteJpaEntity e = new ReporteJpaEntity();
        e.setId(r.id());
        e.setSalaId(r.salaId());
        e.setNotaId(r.notaId());
        e.setMensajeId(r.mensajeId());
        e.setUsuarioId(r.usuarioId());
        e.setMotivo(r.motivo());
        e.setCreadoEn(r.creadoEn());
        e.setResuelto(r.resuelto());
        return aDominio(jpa.saveAndFlush(e));
    }

    @Override
    public Optional<Reporte> buscarPorId(long id) {
        return jpa.findById(id).map(JpaReporteRepositoryAdapter::aDominio);
    }

    @Override
    public List<Reporte> pendientesDeSala(long salaId) {
        return jpa.findBySalaIdAndResueltoFalseOrderByIdAsc(salaId).stream()
                .map(JpaReporteRepositoryAdapter::aDominio).toList();
    }

    private static Reporte aDominio(ReporteJpaEntity e) {
        return new Reporte(e.getId(), e.getSalaId(), e.getNotaId(), e.getMensajeId(), e.getUsuarioId(), e.getMotivo(),
                e.getCreadoEn(), e.isResuelto());
    }
}
