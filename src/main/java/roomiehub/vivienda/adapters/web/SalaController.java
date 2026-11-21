package roomiehub.vivienda.adapters.web;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import roomiehub.vivienda.application.usecase.sala.AbandonarSala;
import roomiehub.vivienda.application.usecase.sala.ConsultarSalas;
import roomiehub.vivienda.application.usecase.sala.CrearSala;
import roomiehub.vivienda.application.usecase.sala.DesactivarSala;
import roomiehub.vivienda.application.usecase.sala.ExpulsarMiembro;
import roomiehub.vivienda.application.usecase.sala.ReactivarSala;
import roomiehub.vivienda.application.usecase.sala.SalaDeUsuario;
import roomiehub.vivienda.application.usecase.sala.TransferirAdministrador;
import roomiehub.vivienda.application.usecase.sala.UnirseASala;
import roomiehub.vivienda.domain.model.sala.EstadoSala;
import roomiehub.vivienda.domain.model.sala.Rol;

// CU-02 a CU-08 salas y el QR lo dibuja la app con codigoInvitacion
@RestController
@RequestMapping("/salas")
public class SalaController {

    public record CrearSalaRequest(@NotBlank @Size(max = 60) String nombre, @NotBlank String zonaHoraria) {}

    public record UnirseRequest(@NotBlank String codigo) {}

    public record TransferirRequest(@NotNull Long usuarioId) {}

    public record MiembroResponse(long usuarioId, String nombre, String fotoUrl, Rol rol, Instant unidoEn) {}

    public record SalaResponse(long id, String nombre, String codigoInvitacion, String zonaHoraria, EstadoSala estado,
                               Instant desactivadaEn, Rol miRol, List<MiembroResponse> miembros) {
        static SalaResponse de(SalaDeUsuario s) {
            return new SalaResponse(s.sala().id(), s.sala().nombre(), s.sala().codigoInvitacion(),
                    s.sala().zonaHoraria().getId(), s.sala().estado(), s.sala().desactivadaEn(), s.miRol(),
                    s.miembros().stream()
                            .map(m -> new MiembroResponse(m.usuarioId(), m.nombre(), m.fotoUrl(), m.rol(), m.unidoEn()))
                            .toList());
        }
    }

    private final CrearSala crearSala;
    private final UnirseASala unirseASala;
    private final ConsultarSalas consultarSalas;
    private final AbandonarSala abandonarSala;
    private final ExpulsarMiembro expulsarMiembro;
    private final TransferirAdministrador transferirAdministrador;
    private final DesactivarSala desactivarSala;
    private final ReactivarSala reactivarSala;

    public SalaController(CrearSala crearSala, UnirseASala unirseASala, ConsultarSalas consultarSalas,
                          AbandonarSala abandonarSala, ExpulsarMiembro expulsarMiembro,
                          TransferirAdministrador transferirAdministrador, DesactivarSala desactivarSala,
                          ReactivarSala reactivarSala) {
        this.crearSala = crearSala;
        this.unirseASala = unirseASala;
        this.consultarSalas = consultarSalas;
        this.abandonarSala = abandonarSala;
        this.expulsarMiembro = expulsarMiembro;
        this.transferirAdministrador = transferirAdministrador;
        this.desactivarSala = desactivarSala;
        this.reactivarSala = reactivarSala;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SalaResponse crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CrearSalaRequest r) {
        return SalaResponse.de(crearSala.ejecutar(usuario(jwt), r.nombre(), r.zonaHoraria()));
    }

    @PostMapping("/unirse")
    public SalaResponse unirse(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UnirseRequest r) {
        return SalaResponse.de(unirseASala.ejecutar(usuario(jwt), r.codigo()));
    }

    @GetMapping
    public List<SalaResponse> misSalas(@AuthenticationPrincipal Jwt jwt) {
        return consultarSalas.misSalas(usuario(jwt)).stream().map(SalaResponse::de).toList();
    }

    @GetMapping("/{salaId}")
    public SalaResponse detalle(@AuthenticationPrincipal Jwt jwt, @PathVariable long salaId) {
        return SalaResponse.de(consultarSalas.detalle(usuario(jwt), salaId));
    }

    // CU-05 el usuario deja la sala
    @DeleteMapping("/{salaId}/membresia")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void abandonar(@AuthenticationPrincipal Jwt jwt, @PathVariable long salaId) {
        abandonarSala.ejecutar(usuario(jwt), salaId);
    }

    @DeleteMapping("/{salaId}/miembros/{usuarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void expulsar(@AuthenticationPrincipal Jwt jwt, @PathVariable long salaId, @PathVariable long usuarioId) {
        expulsarMiembro.ejecutar(usuario(jwt), salaId, usuarioId);
    }

    @PostMapping("/{salaId}/administrador")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void transferir(@AuthenticationPrincipal Jwt jwt, @PathVariable long salaId,
                           @Valid @RequestBody TransferirRequest r) {
        transferirAdministrador.ejecutar(usuario(jwt), salaId, r.usuarioId());
    }

    @PostMapping("/{salaId}/desactivar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@AuthenticationPrincipal Jwt jwt, @PathVariable long salaId) {
        desactivarSala.ejecutar(usuario(jwt), salaId);
    }

    @PostMapping("/{salaId}/reactivar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reactivar(@AuthenticationPrincipal Jwt jwt, @PathVariable long salaId) {
        reactivarSala.ejecutar(usuario(jwt), salaId);
    }

    private static long usuario(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
