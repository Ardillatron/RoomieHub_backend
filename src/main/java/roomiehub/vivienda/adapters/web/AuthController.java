package roomiehub.vivienda.adapters.web;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import roomiehub.vivienda.adapters.web.UsuarioController.UsuarioResponse;
import roomiehub.vivienda.application.usecase.cuenta.CerrarSesion;
import roomiehub.vivienda.application.usecase.cuenta.IniciarSesion;
import roomiehub.vivienda.application.usecase.cuenta.IniciarSesionConGoogle;
import roomiehub.vivienda.application.usecase.cuenta.RegistrarUsuario;
import roomiehub.vivienda.application.usecase.cuenta.RenovarSesion;
import roomiehub.vivienda.application.usecase.cuenta.Sesion;
import roomiehub.vivienda.domain.exception.CredencialesInvalidasException;
import roomiehub.vivienda.domain.model.cuenta.Usuario;

// CU-01 registro login con correo o Google renovar y cerrar sesion
@RestController
@RequestMapping("/auth")
public class AuthController {

    public record RegistroRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank String contrasena,
            @NotBlank @Size(max = 60) String nombre,
            @Size(max = 100) String dispositivo) {}

    public record LoginRequest(@NotBlank String email, @NotBlank String contrasena, String dispositivo) {}

    public record GoogleRequest(@NotBlank String idToken, String dispositivo) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    // fcmToken es el del telefono para que deje de recibir push de esta cuenta
    public record LogoutRequest(@NotBlank String refreshToken, @Size(max = 4096) String fcmToken) {}

    public record SesionResponse(String accessToken, Instant accesoExpiraEn, String refreshToken,
                                 UsuarioResponse usuario) {
        static SesionResponse de(Sesion s) {
            return new SesionResponse(s.accessToken(), s.accesoExpiraEn(), s.refreshToken(),
                    UsuarioResponse.de(s.usuario()));
        }
    }

    private final RegistrarUsuario registrarUsuario;
    private final IniciarSesion iniciarSesion;
    private final IniciarSesionConGoogle iniciarSesionConGoogle;
    private final RenovarSesion renovarSesion;
    private final CerrarSesion cerrarSesion;
    // 5 contraseñas malas en 15 minutos bloquean esa cuenta desde esa IP hasta que pase el tiempo
    private final LimitadorDeIntentos fallidosPorCorreo;

    public AuthController(RegistrarUsuario registrarUsuario, IniciarSesion iniciarSesion,
                          IniciarSesionConGoogle iniciarSesionConGoogle, RenovarSesion renovarSesion,
                          CerrarSesion cerrarSesion, Clock reloj) {
        this.registrarUsuario = registrarUsuario;
        this.iniciarSesion = iniciarSesion;
        this.iniciarSesionConGoogle = iniciarSesionConGoogle;
        this.renovarSesion = renovarSesion;
        this.cerrarSesion = cerrarSesion;
        this.fallidosPorCorreo = new LimitadorDeIntentos(5, Duration.ofMinutes(15), reloj);
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public SesionResponse registrar(@Valid @RequestBody RegistroRequest r) {
        return SesionResponse.de(registrarUsuario.ejecutar(r.email(), r.contrasena(), r.nombre(), r.dispositivo()));
    }

    @PostMapping("/login")
    public SesionResponse login(@Valid @RequestBody LoginRequest r, HttpServletRequest request) {
        // Con la IP en la llave quien falla a proposito con un correo ajeno solo se bloquea a si mismo
        String clave = Usuario.normalizarCorreo(r.email()) + "|" + request.getRemoteAddr();
        long espera = fallidosPorCorreo.bloqueadoPor(clave);
        if (espera > 0) {
            throw new DemasiadosIntentosException(espera);
        }
        try {
            Sesion sesion = iniciarSesion.ejecutar(r.email(), r.contrasena(), r.dispositivo());
            fallidosPorCorreo.olvidar(clave);
            return SesionResponse.de(sesion);
        } catch (CredencialesInvalidasException e) {
            fallidosPorCorreo.registrar(clave);
            throw e;
        }
    }

    @PostMapping("/google")
    public SesionResponse google(@Valid @RequestBody GoogleRequest r) {
        return SesionResponse.de(iniciarSesionConGoogle.ejecutar(r.idToken(), r.dispositivo()));
    }

    @PostMapping("/refresh")
    public SesionResponse refresh(@Valid @RequestBody RefreshRequest r) {
        return SesionResponse.de(renovarSesion.ejecutar(r.refreshToken()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest r) {
        cerrarSesion.ejecutar(r.refreshToken(), r.fcmToken());
    }
}
