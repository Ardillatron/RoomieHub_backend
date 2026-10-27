package roomiehub.vivienda.application.usecase.cuenta;

import java.nio.charset.StandardCharsets;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.port.HasherDeContrasenasPort;
import roomiehub.vivienda.domain.exception.CorreoYaRegistradoException;
import roomiehub.vivienda.domain.exception.DatoInvalidoException;
import roomiehub.vivienda.domain.model.cuenta.Usuario;
import roomiehub.vivienda.domain.repository.UsuarioRepository;

/** CU-01: registro con correo y contraseña. */
public class RegistrarUsuario {

    private static final int MINIMO_CARACTERES = 8;
    private static final int MAXIMO_BYTES = 72; // BCrypt ignora lo que pase de 72 bytes

    private final UsuarioRepository usuarios;
    private final HasherDeContrasenasPort hasher;
    private final EmisorDeSesiones sesiones;

    public RegistrarUsuario(UsuarioRepository usuarios, HasherDeContrasenasPort hasher, EmisorDeSesiones sesiones) {
        this.usuarios = usuarios;
        this.hasher = hasher;
        this.sesiones = sesiones;
    }

    @Transactional
    public Sesion ejecutar(String email, String contrasena, String nombre, String dispositivo) {
        if (contrasena == null || contrasena.length() < MINIMO_CARACTERES
                || contrasena.getBytes(StandardCharsets.UTF_8).length > MAXIMO_BYTES) {
            throw new DatoInvalidoException("La contraseña debe tener entre 8 caracteres y 72 bytes");
        }
        String correo = Usuario.normalizarCorreo(email);
        if (usuarios.buscarPorEmail(correo).isPresent()) {
            throw new CorreoYaRegistradoException();
        }
        Usuario usuario = usuarios.guardar(Usuario.conCorreo(correo, hasher.hash(contrasena), nombre));
        return sesiones.emitir(usuario, dispositivo);
    }
}
