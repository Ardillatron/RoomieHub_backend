package roomiehub.vivienda.application.usecase.cuenta;

import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.port.HasherDeContrasenasPort;
import roomiehub.vivienda.domain.exception.CredencialesInvalidasException;
import roomiehub.vivienda.domain.model.cuenta.Usuario;
import roomiehub.vivienda.domain.repository.UsuarioRepository;

// CU-01 login con correo y contraseña
public class IniciarSesion {

    private final UsuarioRepository usuarios;
    private final HasherDeContrasenasPort hasher;
    private final EmisorDeSesiones sesiones;
    private final String hashSenuelo;

    public IniciarSesion(UsuarioRepository usuarios, HasherDeContrasenasPort hasher, EmisorDeSesiones sesiones) {
        this.usuarios = usuarios;
        this.hasher = hasher;
        this.sesiones = sesiones;
        this.hashSenuelo = hasher.hash("contrasena-senuelo");
    }

    @Transactional
    public Sesion ejecutar(String email, String contrasena, String dispositivo) {
        Optional<Usuario> usuario = usuarios.buscarPorEmail(Usuario.normalizarCorreo(email))
                .filter(Usuario::tieneContrasena);
        // Compara aunque la cuenta no exista para no revelar que correos existen
        boolean coincide = hasher.coincide(contrasena, usuario.map(Usuario::passwordHash).orElse(hashSenuelo));
        if (usuario.isEmpty() || !coincide) {
            throw new CredencialesInvalidasException();
        }
        return sesiones.emitir(usuario.get(), dispositivo);
    }
}
