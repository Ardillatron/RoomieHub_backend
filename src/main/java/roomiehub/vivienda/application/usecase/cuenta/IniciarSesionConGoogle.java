package roomiehub.vivienda.application.usecase.cuenta;

import java.time.Clock;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.port.VerificadorGooglePort;
import roomiehub.vivienda.application.port.VerificadorGooglePort.IdentidadGoogle;
import roomiehub.vivienda.domain.exception.CredencialesInvalidasException;
import roomiehub.vivienda.domain.model.cuenta.Usuario;
import roomiehub.vivienda.domain.repository.RefreshTokenRepository;
import roomiehub.vivienda.domain.repository.UsuarioRepository;

/* CU-01 con Google busca la cuenta por el id de Google
   si no existe vincula la cuenta con el mismo correo verificado o crea una nueva */
public class IniciarSesionConGoogle {

    private final VerificadorGooglePort google;
    private final UsuarioRepository usuarios;
    private final EmisorDeSesiones sesiones;
    private final RefreshTokenRepository refreshTokens;
    private final Clock reloj;

    public IniciarSesionConGoogle(VerificadorGooglePort google, UsuarioRepository usuarios, EmisorDeSesiones sesiones,
                                  RefreshTokenRepository refreshTokens, Clock reloj) {
        this.google = google;
        this.usuarios = usuarios;
        this.sesiones = sesiones;
        this.refreshTokens = refreshTokens;
        this.reloj = reloj;
    }

    @Transactional
    public Sesion ejecutar(String idToken, String dispositivo) {
        IdentidadGoogle identidad = google.verificar(idToken);
        Usuario usuario = usuarios.buscarPorGoogleSub(identidad.sub())
                .orElseGet(() -> vincularOCrear(identidad));
        return sesiones.emitir(usuario, dispositivo);
    }

    private Usuario vincularOCrear(IdentidadGoogle identidad) {
        // Sin correo verificado cualquiera podria quedarse con una cuenta ajena
        if (!identidad.emailVerificado()) {
            throw new CredencialesInvalidasException();
        }
        String correo = Usuario.normalizarCorreo(identidad.email());
        return usuarios.buscarPorEmail(correo)
                .map(existente -> {
                    if (existente.googleSub() != null) {
                        throw new CredencialesInvalidasException(); // ya esta vinculada a otra cuenta de Google
                    }
                    existente.vincularGoogle(identidad.sub());
                    // Las sesiones abiertas con esa contraseña pueden ser de otra persona y se cierran
                    refreshTokens.revocarTodos(existente.id(), reloj.instant());
                    return usuarios.guardar(existente);
                })
                .orElseGet(() -> usuarios.guardar(Usuario.conGoogle(identidad.sub(), correo,
                        identidad.nombre() != null ? identidad.nombre() : correo.split("@")[0],
                        identidad.fotoUrl())));
    }
}
