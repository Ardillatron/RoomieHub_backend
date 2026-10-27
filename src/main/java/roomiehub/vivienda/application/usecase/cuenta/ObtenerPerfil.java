package roomiehub.vivienda.application.usecase.cuenta;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.domain.exception.CredencialesInvalidasException;
import roomiehub.vivienda.domain.model.cuenta.Usuario;
import roomiehub.vivienda.domain.repository.UsuarioRepository;

public class ObtenerPerfil {

    private final UsuarioRepository usuarios;

    public ObtenerPerfil(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    /** Un JWT vigente de una cuenta ya eliminada se trata como inválido. */
    @Transactional(readOnly = true)
    public Usuario ejecutar(long usuarioId) {
        return usuarios.buscarPorId(usuarioId)
                .filter(u -> u.eliminadoEn() == null)
                .orElseThrow(CredencialesInvalidasException::new);
    }
}
