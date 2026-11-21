package roomiehub.vivienda.adapters.persistence;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import roomiehub.vivienda.domain.model.convivencia.TipoReaccion;
import roomiehub.vivienda.domain.repository.ReaccionRepository;
import roomiehub.vivienda.infrastructure.persistence.ReaccionJpaEntity;
import roomiehub.vivienda.infrastructure.persistence.ReaccionJpaRepository;

@Component
public class JpaReaccionRepositoryAdapter implements ReaccionRepository {

    private final ReaccionJpaRepository jpa;

    public JpaReaccionRepositoryAdapter(ReaccionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void reaccionar(long notaId, long salaId, long usuarioId, TipoReaccion tipo) {
        ReaccionJpaEntity e = jpa.findByNotaIdAndUsuarioId(notaId, usuarioId).orElseGet(ReaccionJpaEntity::new);
        e.setNotaId(notaId);
        e.setSalaId(salaId);
        e.setUsuarioId(usuarioId);
        e.setTipo(tipo);
        jpa.saveAndFlush(e);
    }

    @Override
    public void quitar(long notaId, long usuarioId) {
        jpa.quitar(notaId, usuarioId);
    }

    @Override
    public Map<Long, Conteo> conteos(Collection<Long> notaIds) {
        if (notaIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, long[]> totales = new HashMap<>(); // likes y dislikes
        for (ReaccionJpaRepository.ConteoVista v : jpa.conteos(notaIds)) {
            totales.computeIfAbsent(v.getNotaId(), id -> new long[2])[v.getTipo() == TipoReaccion.LIKE ? 0 : 1] =
                    v.getTotal();
        }
        return totales.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, t -> new Conteo(t.getValue()[0], t.getValue()[1])));
    }

    @Override
    public Map<Long, TipoReaccion> deUsuario(Collection<Long> notaIds, long usuarioId) {
        if (notaIds.isEmpty()) {
            return Map.of();
        }
        return jpa.findByNotaIdInAndUsuarioId(notaIds, usuarioId).stream()
                .collect(Collectors.toMap(ReaccionJpaEntity::getNotaId, ReaccionJpaEntity::getTipo));
    }
}
