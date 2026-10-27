package roomiehub.vivienda.adapters.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import roomiehub.vivienda.application.usecase.convivencia.ConsultarChat;
import roomiehub.vivienda.application.usecase.convivencia.EliminarMensaje;
import roomiehub.vivienda.application.usecase.convivencia.EnviarMensaje;
import roomiehub.vivienda.application.usecase.convivencia.MensajeVista;
import roomiehub.vivienda.application.usecase.convivencia.ReportarMensaje;
import roomiehub.vivienda.domain.model.convivencia.MensajeChat;

/**
 * CU-11, CU-14, CU-15 y CU-32. Los mensajes se envían aquí y llegan en vivo por STOMP: conectarse a /ws
 * con "Authorization: Bearer {jwt}" en el CONNECT y suscribirse a /user/queue/chat.
 */
@RestController
public class ChatController {

    /**
     * Texto, imagen o ambos. {@code notaRespondidaId}: para responder una nota del tablón (RN-30).
     * {@code clientUuid}: lo genera la app (RN-35); reenviar con el mismo no duplica el mensaje.
     */
    public record EnviarMensajeRequest(@Size(max = 4000) String texto, @Size(max = 200) String imagenId,
                                       Long notaRespondidaId, UUID clientUuid) {}

    public record ReporteRequest(@NotBlank @Size(max = 500) String motivo) {}

    public record NotaCitadaResponse(long id, String texto, String imagenMiniaturaUrl) {}

    public record MensajeResponse(long id, long salaId, long autorId, String autor, String texto, String imagenUrl,
                                  String imagenMiniaturaUrl, NotaCitadaResponse notaCitada, Instant creadoEn,
                                  UUID clientUuid) {
        public static MensajeResponse de(MensajeVista v) {
            MensajeChat m = v.mensaje();
            NotaCitadaResponse citada = v.notaCitada() == null ? null : new NotaCitadaResponse(v.notaCitada().id(),
                    v.notaCitada().texto(), v.notaCitada().imagenMiniaturaUrl());
            return new MensajeResponse(m.id(), m.salaId(), m.autorId(), v.autor(), m.texto(), v.imagenUrl(),
                    v.imagenMiniaturaUrl(), citada, m.creadoEn(), m.clientUuid());
        }
    }

    private final EnviarMensaje enviarMensaje;
    private final ConsultarChat consultarChat;
    private final EliminarMensaje eliminarMensaje;
    private final ReportarMensaje reportarMensaje;

    public ChatController(EnviarMensaje enviarMensaje, ConsultarChat consultarChat, EliminarMensaje eliminarMensaje,
                          ReportarMensaje reportarMensaje) {
        this.enviarMensaje = enviarMensaje;
        this.consultarChat = consultarChat;
        this.eliminarMensaje = eliminarMensaje;
        this.reportarMensaje = reportarMensaje;
    }

    /** Del más nuevo al más viejo; para los anteriores, {@code antesDe} = el id más viejo que ya se tiene. */
    @GetMapping("/salas/{salaId}/mensajes")
    public List<MensajeResponse> historial(@AuthenticationPrincipal Jwt jwt, @PathVariable long salaId,
                                           @RequestParam(required = false) Long antesDe,
                                           @RequestParam(defaultValue = "50") int limite) {
        return consultarChat.ejecutar(usuario(jwt), salaId, antesDe, limite).stream().map(MensajeResponse::de).toList();
    }

    @PostMapping("/salas/{salaId}/mensajes")
    @ResponseStatus(HttpStatus.CREATED)
    public MensajeResponse enviar(@AuthenticationPrincipal Jwt jwt, @PathVariable long salaId,
                                  @Valid @RequestBody EnviarMensajeRequest r) {
        return MensajeResponse.de(enviarMensaje.ejecutar(usuario(jwt), salaId, r.texto(), r.imagenId(),
                r.notaRespondidaId(), r.clientUuid()));
    }

    @DeleteMapping("/mensajes/{mensajeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@AuthenticationPrincipal Jwt jwt, @PathVariable long mensajeId) {
        eliminarMensaje.ejecutar(usuario(jwt), mensajeId);
    }

    @PostMapping("/mensajes/{mensajeId}/reportes")
    @ResponseStatus(HttpStatus.CREATED)
    public void reportar(@AuthenticationPrincipal Jwt jwt, @PathVariable long mensajeId,
                         @Valid @RequestBody ReporteRequest r) {
        reportarMensaje.ejecutar(usuario(jwt), mensajeId, r.motivo());
    }

    private static long usuario(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
