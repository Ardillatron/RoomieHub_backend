package roomiehub.vivienda.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReporteJpaRepository extends JpaRepository<ReporteJpaEntity, Long> {

    List<ReporteJpaEntity> findBySalaIdAndResueltoFalseOrderByIdAsc(long salaId);
}
