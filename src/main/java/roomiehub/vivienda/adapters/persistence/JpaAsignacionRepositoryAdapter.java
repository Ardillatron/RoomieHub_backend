package roomiehub.vivienda.adapters.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import roomiehub.vivienda.domain.model.tareas.AsignacionTarea;
import roomiehub.vivienda.domain.model.tareas.EstadoAsignacion;
import roomiehub.vivienda.domain.model.tareas.ValorTarea;
import roomiehub.vivienda.domain.repository.AsignacionRepository;
import roomiehub.vivienda.infrastructure.persistence.TareaAsignacionJpaEntity;
import roomiehub.vivienda.infrastructure.persistence.TareaAsignacionJpaRepository;

@Component
public class JpaAsignacionRepositoryAdapter implements AsignacionRepository {

    private final TareaAsignacionJpaRepository jpa;

    public JpaAsignacionRepositoryAdapter(TareaAsignacionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<AsignacionTarea> buscarPorId(long id) {
        return jpa.findById(id).map(JpaAsignacionRepositoryAdapter::aDominio);
    }

    @Override
    public boolean existe(long tareaId, Instant periodoInicio) {
        return jpa.existsByTareaIdAndPeriodoInicio(tareaId, periodoInicio);
    }

    @Override
    public List<AsignacionTarea> pendientesDeSala(long salaId) {
        return jpa.findBySalaIdAndEstado(salaId, EstadoAsignacion.ASIGNADA).stream()
                .map(JpaAsignacionRepositoryAdapter::aDominio).toList();
    }

    @Override
    public List<AsignacionTarea> recientesDeSala(long salaId, Instant desde) {
        return jpa.findBySalaIdAndPeriodoFinAfterOrderByPeriodoInicioAscIdAsc(salaId, desde).stream()
                .map(JpaAsignacionRepositoryAdapter::aDominio).toList();
    }

    @Override
    public void guardar(AsignacionTarea a) {
        TareaAsignacionJpaEntity e = new TareaAsignacionJpaEntity();
        e.setId(a.id());
        e.setTareaId(a.tareaId());
        e.setSalaId(a.salaId());
        e.setUsuarioId(a.usuarioId());
        e.setPeriodoInicio(a.inicio());
        e.setPeriodoFin(a.fin());
        e.setValorAlAsignar((short) a.valorAlAsignar().valor());
        e.setEstado(a.estado());
        e.setCompletadaEn(a.completadaEn());
        jpa.saveAndFlush(e);
    }

    @Override
    public void borrarPendientesDeTarea(long tareaId) {
        jpa.borrarDeTarea(tareaId, EstadoAsignacion.ASIGNADA);
    }

    @Override
    public void vencerSinPenalizar(long salaId, Instant finAntesDe) {
        jpa.vencerSinPenalizar(salaId, finAntesDe, EstadoAsignacion.ASIGNADA, EstadoAsignacion.VENCIDA);
    }

    private static AsignacionTarea aDominio(TareaAsignacionJpaEntity e) {
        return new AsignacionTarea(e.getId(), e.getTareaId(), e.getSalaId(), e.getUsuarioId(), e.getPeriodoInicio(),
                e.getPeriodoFin(), new ValorTarea(e.getValorAlAsignar()), e.getEstado(), e.getCompletadaEn());
    }
}
