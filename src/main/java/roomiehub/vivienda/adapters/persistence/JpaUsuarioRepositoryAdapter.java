package roomiehub.vivienda.adapters.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import roomiehub.vivienda.domain.model.cuenta.Usuario;
import roomiehub.vivienda.domain.repository.UsuarioRepository;
import roomiehub.vivienda.infrastructure.persistence.UsuarioJpaEntity;
import roomiehub.vivienda.infrastructure.persistence.UsuarioJpaRepository;

@Component
public class JpaUsuarioRepositoryAdapter implements UsuarioRepository {

    private final UsuarioJpaRepository jpa;

    public JpaUsuarioRepositoryAdapter(UsuarioJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        return aDominio(jpa.save(aEntidad(usuario)));
    }

    @Override
    public Optional<Usuario> buscarPorId(long id) {
        return jpa.findById(id).map(JpaUsuarioRepositoryAdapter::aDominio);
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return jpa.findByEmail(email).map(JpaUsuarioRepositoryAdapter::aDominio);
    }

    @Override
    public Optional<Usuario> buscarPorGoogleSub(String googleSub) {
        return jpa.findByGoogleSub(googleSub).map(JpaUsuarioRepositoryAdapter::aDominio);
    }

    @Override
    public List<Usuario> buscarPorIds(Collection<Long> ids) {
        return jpa.findAllById(ids).stream().map(JpaUsuarioRepositoryAdapter::aDominio).toList();
    }

    private static Usuario aDominio(UsuarioJpaEntity e) {
        return new Usuario(e.getId(), e.getProveedorAuth(), e.getEmail(), e.getPasswordHash(), e.getGoogleSub(),
                e.getNombre(), e.getFotoUrl(), e.getEliminadoEn());
    }

    private static UsuarioJpaEntity aEntidad(Usuario u) {
        UsuarioJpaEntity e = new UsuarioJpaEntity();
        e.setId(u.id());
        e.setProveedorAuth(u.proveedor());
        e.setEmail(u.email());
        e.setPasswordHash(u.passwordHash());
        e.setGoogleSub(u.googleSub());
        e.setNombre(u.nombre());
        e.setFotoUrl(u.fotoUrl());
        e.setEliminadoEn(u.eliminadoEn());
        return e;
    }
}
