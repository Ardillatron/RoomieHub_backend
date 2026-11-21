package roomiehub.vivienda.adapters.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import roomiehub.vivienda.domain.model.tareas.EstadoAsignacion;
import roomiehub.vivienda.domain.model.tareas.Frecuencia;
import roomiehub.vivienda.domain.model.tareas.Tarea;
import roomiehub.vivienda.domain.model.tareas.ValorTarea;
import roomiehub.vivienda.domain.repository.TareaRepository;
import roomiehub.vivienda.infrastructure.persistence.TareaAsignacionJpaRepository;
import roomiehub.vivienda.infrastructure.persistence.TareaJpaEntity;
import roomiehub.vivienda.infrastructure.persistence.TareaJpaRepository;

@Component
public class JpaTareaRepositoryAdapter implements TareaRepository {

    private final TareaJpaRepository tareas;
    private final TareaAsignacionJpaRepository asignaciones;

    public JpaTareaRepositoryAdapter(TareaJpaRepository tareas, TareaAsignacionJpaRepository asignaciones) {
        this.tareas = tareas;
        this.asignaciones = asignaciones;
    }

    @Override
    public Tarea guardar(Tarea t) {
        TareaJpaEntity e = new TareaJpaEntity();
        e.setId(t.id());
        e.setSalaId(t.salaId());
        e.setNombre(t.nombre());
        e.setDescripcion(t.descripcion());
        e.setValor((short) t.valor().valor());
        e.setFrecuencia(t.frecuencia().toString());
        e.setFechaInicio(t.fechaInicio());
        e.setActiva(t.activa());
        return aDominio(tareas.saveAndFlush(e));
    }

    @Override
    public Optional<Tarea> buscarPorId(long id) {
        return tareas.findById(id).map(JpaTareaRepositoryAdapter::aDominio);
    }

    @Override
    public List<Tarea> activasDeSala(long salaId) {
        return tareas.findBySalaIdAndActivaTrueOrderById(salaId).stream().map(JpaTareaRepositoryAdapter::aDominio).toList();
    }

    @Override
    public void desasignarPendientes(long salaId, long usuarioId) {
        asignaciones.desasignar(salaId, usuarioId, EstadoAsignacion.ASIGNADA);
    }

    @Override
    public int valorMasAlto(long salaId) {
        Short maximo = tareas.valorMasAlto(salaId);
        return maximo == null ? 0 : maximo;
    }

    private static Tarea aDominio(TareaJpaEntity e) {
        return new Tarea(e.getId(), e.getSalaId(), e.getNombre(), e.getDescripcion(), new ValorTarea(e.getValor()),
                Frecuencia.de(e.getFrecuencia()),
                e.getFechaInicio(), e.isActiva());
    }
}
