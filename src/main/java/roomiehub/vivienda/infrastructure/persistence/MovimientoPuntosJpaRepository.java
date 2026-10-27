package roomiehub.vivienda.infrastructure.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import roomiehub.vivienda.domain.model.tareas.TipoMovimiento;

public interface MovimientoPuntosJpaRepository extends JpaRepository<MovimientoPuntosJpaEntity, Long> {

    interface TotalVista {
        Long getUsuarioId();
        Long getTotal();
    }

    @Query("""
            SELECT m.usuarioId AS usuarioId, SUM(m.puntos) AS total FROM MovimientoPuntosJpaEntity m
            WHERE m.salaId = :salaId GROUP BY m.usuarioId""")
    List<TotalVista> historicos(long salaId);

    @Query("""
            SELECT m.usuarioId AS usuarioId, SUM(m.puntos) AS total FROM MovimientoPuntosJpaEntity m
            WHERE m.salaId = :salaId AND m.creadoEn >= :desde AND m.tipo IN :tipos GROUP BY m.usuarioId""")
    List<TotalVista> totales(long salaId, Instant desde, Collection<TipoMovimiento> tipos);
}
