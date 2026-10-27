package roomiehub.vivienda.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import roomiehub.vivienda.domain.model.convivencia.TipoReaccion;

public interface ReaccionJpaRepository extends JpaRepository<ReaccionJpaEntity, Long> {

    interface ConteoVista {
        Long getNotaId();
        TipoReaccion getTipo();
        Long getTotal();
    }

    Optional<ReaccionJpaEntity> findByNotaIdAndUsuarioId(long notaId, long usuarioId);

    List<ReaccionJpaEntity> findByNotaIdInAndUsuarioId(Collection<Long> notaIds, long usuarioId);

    @Modifying
    @Query("DELETE FROM ReaccionJpaEntity r WHERE r.notaId = :notaId AND r.usuarioId = :usuarioId")
    int quitar(long notaId, long usuarioId);

    @Query("""
            SELECT r.notaId AS notaId, r.tipo AS tipo, COUNT(r) AS total FROM ReaccionJpaEntity r
            WHERE r.notaId IN :notaIds GROUP BY r.notaId, r.tipo""")
    List<ConteoVista> conteos(Collection<Long> notaIds);
}
