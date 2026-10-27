package roomiehub.vivienda.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import roomiehub.vivienda.domain.model.cuenta.Usuario;

public interface UsuarioRepository {

    Usuario guardar(Usuario usuario);

    Optional<Usuario> buscarPorId(long id);

    /** El correo ya viene normalizado (Usuario.normalizarCorreo). */
    Optional<Usuario> buscarPorEmail(String email);

    Optional<Usuario> buscarPorGoogleSub(String googleSub);

    List<Usuario> buscarPorIds(Collection<Long> ids);
}
