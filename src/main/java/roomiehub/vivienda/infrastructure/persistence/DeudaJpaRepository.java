package roomiehub.vivienda.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import roomiehub.vivienda.domain.model.finanzas.EstadoDeuda;

public interface DeudaJpaRepository extends JpaRepository<DeudaJpaEntity, Long> {

    List<DeudaJpaEntity> findByServicioIdAndEstado(long servicioId, EstadoDeuda estado);

    List<DeudaJpaEntity> findBySalaIdAndEstadoOrderByPeriodoDescIdAsc(long salaId, EstadoDeuda estado);

    boolean existsByServicioIdAndPeriodoAndDeudaOrigenIdIsNull(long servicioId, LocalDate periodo);
}
