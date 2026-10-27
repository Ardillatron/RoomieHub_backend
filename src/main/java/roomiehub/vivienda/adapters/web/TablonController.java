package roomiehub.vivienda.adapters.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import roomiehub.vivienda.application.usecase.convivencia.ConsultarTablon;
import roomiehub.vivienda.application.usecase.convivencia.ConsultarTablon.NotaVista;
import roomiehub.vivienda.application.usecase.convivencia.CrearNota;
import roomiehub.vivienda.application.usecase.convivencia.EliminarNota;
import roomiehub.vivienda.application.usecase.convivencia.FijarNota;
import roomiehub.vivienda.application.usecase.convivencia.ModerarReportes;
import roomiehub.vivienda.application.usecase.convivencia.ReaccionarNota;
import roomiehub.vivienda.application.usecase.convivencia.ReportarNota;
import roomiehub.vivienda.domain.model.convivencia.Nota;
import roomiehub.vivienda.domain.model.convivencia.TipoReaccion;

/** CU-09, CU-10, CU-12, CU-13 y la moderación de reportes. */
@RestController
public class TablonController {

    /**
     * Texto, imagen o ambos. {@code imagenId}: el que devolvió POST /salas/{id}/imagenes/firma, ya subida.
     * {@code clientUuid}: lo genera la app (RN-35); reintentar con el mismo no duplica la nota.
     */
    public record CrearNotaRequest(@Size(max = 2000) String texto, @Size(max = 200) String imagenId,
                                   UUID clientUuid) {}

    public record ReaccionRequest(@NotNull TipoReaccion tipo) {}

    public record ReporteRequest(@NotBlank @Size(max = 500) String motivo) {}

    public record FijarRequest(@NotNull Instant hasta) {}

    /** Con foto: {@code imagenMiniaturaUrl} en el tablón, {@code imagenUrl} al ampliar y {@code texto} al reverso. */
    /** {@code clientUuid}: con él la app empareja la nota que creó sin conexión con la del servidor. */
    public record NotaResponse(long id, long autorId, String autor, String texto, String imagenUrl,
                               String imagenMiniaturaUrl, Instant creadoEn, Instant fijadaHasta, long likes,
                               long dislikes, TipoReaccion miReaccion, UUID clientUuid) {
        static NotaResponse de(NotaVista v) {
            Nota n = v.nota();
            return new NotaResponse(n.id(), n.autorId(), v.autor(), n.texto(), v.imagenUrl(), v.imagenMiniaturaUrl(),
                    n.creadoEn(), v.fijada() ? n.fijadaHasta() : null, v.likes(), v.dislikes(), v.miReaccion(),
                    n.clientUuid());
        }
    }

    public record TablonResponse(List<NotaResponse> fijadas, List<NotaResponse> recientes) {}

    public record ReporteResponse(long id, Long notaId, Long mensajeId, long usuarioId, String reportadoPor,
                                  String motivo, String contenido, Instant creadoEn) {}

    private final CrearNota crearNota;
    private final ReaccionarNota reaccionarNota;
    private final ReportarNota reportarNota;
    private final FijarNota fijarNota;
    private final EliminarNota eliminarNota;
    private final ConsultarTablon consultarTablon;
    private final ModerarReportes moderarReportes;

    public TablonController(CrearNota crearNota, ReaccionarNota reaccionarNota, ReportarNota reportarNota,
                            FijarNota fijarNota, EliminarNota eliminarNota, ConsultarTablon consultarTablon,
                            ModerarReportes moderarReportes) {
        this.crearNota = crearNota;
        this.reaccionarNota = reaccionarNota;
        this.reportarNota = reportarNota;
        this.fijarNota = fijarNota;
        this.eliminarNota = eliminarNota;
        this.consultarTablon = consultarTablon;
        this.moderarReportes = moderarReportes;
    }

    /** Las fijadas arriba (solo en la primera página) y las demás de la más nueva a la más vieja. */
    @GetMapping("/salas/{salaId}/notas")
    public TablonResponse tablon(@AuthenticationPrincipal Jwt jwt, @PathVariable long salaId,
                                 @RequestParam(required = false) Long antesDe,
                                 @RequestParam(defaultValue = "50") int limite) {
        ConsultarTablon.Tablon t = consultarTablon.ejecutar(usuario(jwt), salaId, antesDe, limite);
        return new TablonResponse(t.fijadas().stream().map(NotaResponse::de).toList(),
                t.recientes().stream().map(NotaResponse::de).toList());
    }

    @PostMapping("/salas/{salaId}/notas")
    @ResponseStatus(HttpStatus.CREATED)
    public NotaResponse crear(@AuthenticationPrincipal Jwt jwt, @PathVariable long salaId,
                              @Valid @RequestBody CrearNotaRequest r) {
        return NotaResponse.de(crearNota.ejecutar(usuario(jwt), salaId, r.texto(), r.imagenId(), r.clientUuid()));
    }

    @PutMapping("/notas/{notaId}/reaccion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reaccionar(@AuthenticationPrincipal Jwt jwt, @PathVariable long notaId,
                           @Valid @RequestBody ReaccionRequest r) {
        reaccionarNota.reaccionar(usuario(jwt), notaId, r.tipo());
    }

    @DeleteMapping("/notas/{notaId}/reaccion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitarReaccion(@AuthenticationPrincipal Jwt jwt, @PathVariable long notaId) {
        reaccionarNota.quitar(usuario(jwt), notaId);
    }

    @PostMapping("/notas/{notaId}/reportes")
    @ResponseStatus(HttpStatus.CREATED)
    public void reportar(@AuthenticationPrincipal Jwt jwt, @PathVariable long notaId,
                         @Valid @RequestBody ReporteRequest r) {
        reportarNota.ejecutar(usuario(jwt), notaId, r.motivo());
    }

    @PutMapping("/notas/{notaId}/fijada")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void fijar(@AuthenticationPrincipal Jwt jwt, @PathVariable long notaId,
                      @Valid @RequestBody FijarRequest r) {
        fijarNota.fijar(usuario(jwt), notaId, r.hasta());
    }

    @DeleteMapping("/notas/{notaId}/fijada")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desfijar(@AuthenticationPrincipal Jwt jwt, @PathVariable long notaId) {
        fijarNota.desfijar(usuario(jwt), notaId);
    }

    @DeleteMapping("/notas/{notaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@AuthenticationPrincipal Jwt jwt, @PathVariable long notaId) {
        eliminarNota.ejecutar(usuario(jwt), notaId);
    }

    @GetMapping("/salas/{salaId}/reportes")
    public List<ReporteResponse> reportes(@AuthenticationPrincipal Jwt jwt, @PathVariable long salaId) {
        return moderarReportes.pendientes(usuario(jwt), salaId).stream()
                .map(v -> new ReporteResponse(v.reporte().id(), v.reporte().notaId(), v.reporte().mensajeId(),
                        v.reporte().usuarioId(), v.reportadoPor(), v.reporte().motivo(), v.contenido(),
                        v.reporte().creadoEn()))
                .toList();
    }

    @PostMapping("/reportes/{reporteId}/resolver")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resolver(@AuthenticationPrincipal Jwt jwt, @PathVariable long reporteId) {
        moderarReportes.resolver(usuario(jwt), reporteId);
    }

    private static long usuario(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
