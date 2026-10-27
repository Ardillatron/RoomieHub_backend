package roomiehub.vivienda.application.usecase.cuenta;

import java.time.Clock;
import java.time.Instant;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.usecase.sala.SalidaDeMiembro;
import roomiehub.vivienda.domain.exception.CredencialesInvalidasException;
import roomiehub.vivienda.domain.model.cuenta.Usuario;
import roomiehub.vivienda.domain.model.sala.Membresia;
import roomiehub.vivienda.domain.repository.DispositivoRepository;
import roomiehub.vivienda.domain.repository.MembresiaRepository;
import roomiehub.vivienda.domain.repository.RefreshTokenRepository;
import roomiehub.vivienda.domain.repository.UsuarioRepository;

/**
 * CU-31 y RN-37: sale de cada sala (RN-06, RN-08) y después borra sus datos personales.
 * Deudas, confirmaciones, puntos y mensajes se conservan y aparecen como "Usuario eliminado".
 */
public class EliminarCuenta {

    private final UsuarioRepository usuarios;
    private final MembresiaRepository membresias;
    private final SalidaDeMiembro salida;
    private final RefreshTokenRepository refreshTokens;
    private final DispositivoRepository dispositivos;
    private final Clock reloj;

    public EliminarCuenta(UsuarioRepository usuarios, MembresiaRepository membresias, SalidaDeMiembro salida,
                          RefreshTokenRepository refreshTokens, DispositivoRepository dispositivos, Clock reloj) {
        this.usuarios = usuarios;
        this.membresias = membresias;
        this.salida = salida;
        this.refreshTokens = refreshTokens;
        this.dispositivos = dispositivos;
        this.reloj = reloj;
    }

    @Transactional
    public void ejecutar(long usuarioId) {
        Instant ahora = reloj.instant();
        Usuario usuario = usuarios.buscarPorId(usuarioId)
                .filter(u -> u.eliminadoEn() == null)
                .orElseThrow(CredencialesInvalidasException::new);

        for (Membresia membresia : membresias.activasDeUsuario(usuarioId)) {
            salida.ejecutar(membresia.salaId(), usuarioId, ahora);
        }
        usuario.anonimizar(ahora);
        usuarios.guardar(usuario);
        refreshTokens.revocarTodos(usuarioId, ahora);
        dispositivos.borrarDeUsuario(usuarioId);
    }
}
