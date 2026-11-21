package roomiehub.vivienda.application.usecase.cuenta;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.domain.model.cuenta.Usuario;
import roomiehub.vivienda.domain.repository.UsuarioRepository;

public class EditarPerfil {

    private final ObtenerPerfil perfil;
    private final UsuarioRepository usuarios;

    public EditarPerfil(ObtenerPerfil perfil, UsuarioRepository usuarios) {
        this.perfil = perfil;
        this.usuarios = usuarios;
    }

    @Transactional
    public Usuario ejecutar(long usuarioId, String nombre) {
        Usuario usuario = perfil.ejecutar(usuarioId);
        usuario.renombrar(nombre);
        return usuarios.guardar(usuario);
    }
}
