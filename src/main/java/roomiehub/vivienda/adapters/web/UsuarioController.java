package roomiehub.vivienda.adapters.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import roomiehub.vivienda.application.usecase.cuenta.EditarPerfil;
import roomiehub.vivienda.application.usecase.cuenta.EliminarCuenta;
import roomiehub.vivienda.application.usecase.cuenta.ObtenerPerfil;
import roomiehub.vivienda.application.usecase.cuenta.RegistrarDispositivo;
import roomiehub.vivienda.domain.model.cuenta.Usuario;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    public record UsuarioResponse(long id, String nombre, String email, String fotoUrl) {
        static UsuarioResponse de(Usuario u) {
            return new UsuarioResponse(u.id(), u.nombre(), u.email(), u.fotoUrl());
        }
    }

    public record DispositivoRequest(String fcmToken, String plataforma) {}

    public record PerfilRequest(@NotBlank @Size(max = 60) String nombre) {}

    private final ObtenerPerfil obtenerPerfil;
    private final EditarPerfil editarPerfil;
    private final EliminarCuenta eliminarCuenta;
    private final RegistrarDispositivo registrarDispositivo;

    public UsuarioController(ObtenerPerfil obtenerPerfil, EditarPerfil editarPerfil, EliminarCuenta eliminarCuenta,
                             RegistrarDispositivo registrarDispositivo) {
        this.obtenerPerfil = obtenerPerfil;
        this.editarPerfil = editarPerfil;
        this.eliminarCuenta = eliminarCuenta;
        this.registrarDispositivo = registrarDispositivo;
    }

    @GetMapping("/yo")
    public UsuarioResponse yo(@AuthenticationPrincipal Jwt jwt) {
        return UsuarioResponse.de(obtenerPerfil.ejecutar(Long.parseLong(jwt.getSubject())));
    }

    @PutMapping("/yo")
    public UsuarioResponse editar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PerfilRequest body) {
        return UsuarioResponse.de(editarPerfil.ejecutar(Long.parseLong(jwt.getSubject()), body.nombre()));
    }

    /** CU-31: lo exigen ambas tiendas (sección 12). */
    @DeleteMapping("/yo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@AuthenticationPrincipal Jwt jwt) {
        eliminarCuenta.ejecutar(Long.parseLong(jwt.getSubject()));
    }

    /** Sección 10: idempotente, la app lo llama al iniciar sesión y cuando FCM renueva el token. */
    @PutMapping("/yo/dispositivos")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registrarDispositivo(@AuthenticationPrincipal Jwt jwt, @RequestBody DispositivoRequest body) {
        registrarDispositivo.ejecutar(Long.parseLong(jwt.getSubject()), body.fcmToken(), body.plataforma());
    }
}
