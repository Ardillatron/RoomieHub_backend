package roomiehub.vivienda.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MembresiaJpaRepository extends JpaRepository<MembresiaJpaEntity, Long> {

    List<MembresiaJpaEntity> findBySalaIdAndActivoTrue(long salaId);

    List<MembresiaJpaEntity> findByUsuarioIdAndActivoTrue(long usuarioId);

    Optional<MembresiaJpaEntity> findBySalaIdAndUsuarioId(long salaId, long usuarioId);
}
