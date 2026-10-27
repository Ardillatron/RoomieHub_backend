package roomiehub.vivienda.adapters.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import roomiehub.vivienda.domain.model.sala.Membresia;
import roomiehub.vivienda.domain.repository.MembresiaRepository;
import roomiehub.vivienda.infrastructure.persistence.MembresiaJpaEntity;
import roomiehub.vivienda.infrastructure.persistence.MembresiaJpaRepository;

@Component
public class JpaMembresiaRepositoryAdapter implements MembresiaRepository {

    private final MembresiaJpaRepository jpa;

    public JpaMembresiaRepositoryAdapter(MembresiaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Membresia> activasDeSala(long salaId) {
        return jpa.findBySalaIdAndActivoTrue(salaId).stream().map(JpaMembresiaRepositoryAdapter::aDominio).toList();
    }

    @Override
    public List<Membresia> activasDeUsuario(long usuarioId) {
        return jpa.findByUsuarioIdAndActivoTrue(usuarioId).stream().map(JpaMembresiaRepositoryAdapter::aDominio).toList();
    }

    @Override
    public Optional<Membresia> buscar(long salaId, long usuarioId) {
        return jpa.findBySalaIdAndUsuarioId(salaId, usuarioId).map(JpaMembresiaRepositoryAdapter::aDominio);
    }

    @Override
    public void guardar(Membresia m) {
        MembresiaJpaEntity e = new MembresiaJpaEntity();
        e.setId(m.id());
        e.setUsuarioId(m.usuarioId());
        e.setSalaId(m.salaId());
        e.setRol(m.rol());
        e.setActivo(m.activo());
        e.setUnidoEn(m.unidoEn());
        // Inmediato: Hibernate reordena los UPDATE al hacer flush, y aquí el orden importa (RLS, un solo Administrador).
        jpa.saveAndFlush(e);
    }

    private static Membresia aDominio(MembresiaJpaEntity e) {
        return new Membresia(e.getId(), e.getUsuarioId(), e.getSalaId(), e.getRol(), e.isActivo(), e.getUnidoEn());
    }
}
