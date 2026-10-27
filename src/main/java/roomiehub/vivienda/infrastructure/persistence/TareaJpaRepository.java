package roomiehub.vivienda.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TareaJpaRepository extends JpaRepository<TareaJpaEntity, Long> {

    List<TareaJpaEntity> findBySalaIdAndActivaTrueOrderById(long salaId);

    @Query("SELECT MAX(t.valor) FROM TareaJpaEntity t WHERE t.salaId = :salaId AND t.activa = true")
    Short valorMasAlto(long salaId);
}
