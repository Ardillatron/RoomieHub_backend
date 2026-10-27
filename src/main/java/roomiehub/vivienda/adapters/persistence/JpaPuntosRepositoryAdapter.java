package roomiehub.vivienda.adapters.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import roomiehub.vivienda.domain.model.tareas.MovimientoPuntos;
import roomiehub.vivienda.domain.model.tareas.TipoMovimiento;
import roomiehub.vivienda.domain.repository.PuntosRepository;
import roomiehub.vivienda.infrastructure.persistence.MovimientoPuntosJpaEntity;
import roomiehub.vivienda.infrastructure.persistence.MovimientoPuntosJpaRepository;
import roomiehub.vivienda.infrastructure.persistence.MovimientoPuntosJpaRepository.TotalVista;

@Component
public class JpaPuntosRepositoryAdapter implements PuntosRepository {

    private final MovimientoPuntosJpaRepository jpa;

    public JpaPuntosRepositoryAdapter(MovimientoPuntosJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Map<Long, Integer> historicos(long salaId) {
        return aMapa(jpa.historicos(salaId));
    }

    @Override
    public Map<Long, Integer> visibles(long salaId, Instant desde) {
        return aMapa(jpa.totales(salaId, desde, List.of(TipoMovimiento.COMPLETADA, TipoMovimiento.PENALIZACION)));
    }

    @Override
    public void registrar(MovimientoPuntos m) {
        MovimientoPuntosJpaEntity e = new MovimientoPuntosJpaEntity();
        e.setUsuarioId(m.usuarioId());
        e.setSalaId(m.salaId());
        e.setAsignacionId(m.asignacionId());
        e.setPuntos(m.puntos());
        e.setTipo(m.tipo());
        jpa.saveAndFlush(e);
    }

    private static Map<Long, Integer> aMapa(List<TotalVista> totales) {
        return totales.stream().collect(Collectors.toMap(TotalVista::getUsuarioId, v -> Math.toIntExact(v.getTotal())));
    }
}
