package roomiehub.vivienda.infrastructure.persistence;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfirmacionPagoJpaRepository extends JpaRepository<ConfirmacionPagoJpaEntity, Long> {

    List<ConfirmacionPagoJpaEntity> findByDeudaIdIn(Collection<Long> deudaIds);
}
