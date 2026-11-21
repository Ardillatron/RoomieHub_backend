package roomiehub.vivienda.adapters.web;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import roomiehub.vivienda.application.usecase.tareas.CompletarTarea;
import roomiehub.vivienda.application.usecase.tareas.ConsultarTareas;
import roomiehub.vivienda.application.usecase.tareas.ConsultarTareas.AsignacionConNombres;
import roomiehub.vivienda.application.usecase.tareas.ConsultarTareas.Balance;
import roomiehub.vivienda.application.usecase.tareas.CrearTarea;
import roomiehub.vivienda.application.usecase.tareas.EditarTarea;
import roomiehub.vivienda.application.usecase.tareas.EliminarTarea;
import roomiehub.vivienda.application.usecase.tareas.ReasignarSinAsignar;
import roomiehub.vivienda.domain.model.tareas.AsignacionTarea;
import roomiehub.vivienda.domain.model.tareas.EstadoAsignacion;
import roomiehub.vivienda.domain.model.tareas.Frecuencia;
import roomiehub.vivienda.domain.model.tareas.Tarea;

/** CU-16 a CU-19. */
@RestController
public class TareaController {

    /** {@code frecuencia}: periodo ISO-8601, cada N días (P1D a P365D) o cada mes (P1M). */
    public record CrearTareaRequest(@NotBlank @Size(max = 80) String nombre, @Size(max = 500) String descripcion,
                                    @NotNull @Min(1) @Max(10) Integer valor, @NotBlank String frecuencia,
                                    @NotNull LocalDate fechaInicio) {}

    public record EditarTareaRequest(@NotBlank @Size(max = 80) String nombre, @NotNull @Min(1) @Max(10) Integer valor,
                                     @NotBlank String frecuencia) {}

    public record TareaResponse(long id, String nombre, String descripcion, int valor, String frecuencia,
                                LocalDate fechaInicio) {
        static TareaResponse de(Tarea t) {
            return new TareaResponse(t.id(), t.nombre(), t.descripcion(), t.valor().valor(), t.frecuencia().toString(),
                    t.fechaInicio());
        }
    }

    public record AsignacionResponse(long id, long tareaId, String tarea, Long usuarioId, String responsable,
                                     Instant periodoInicio, Instant periodoFin, int valor, EstadoAsignacion estado,
                                     Instant completadaEn) {
        static AsignacionResponse de(AsignacionTarea a, String tarea, String responsable) {
            return new AsignacionResponse(a.id(), a.tareaId(), tarea, a.usuarioId(), responsable, a.inicio(), a.fin(),
                    a.valorAlAsignar().valor(), a.estado(), a.completadaEn());
        }
    }

    public record ReasignadasResponse(int reasignadas) {}

    private final CrearTarea crearTarea;
    private final EditarTarea editarTarea;
    private final EliminarTarea eliminarTarea;
    private final CompletarTarea completarTarea;
    private final ConsultarTareas consultarTareas;
    private final ReasignarSinAsignar reasignarSinAsignar;

    public TareaController(CrearTarea crearTarea, EditarTarea editarTarea, EliminarTarea eliminarTarea,
                           CompletarTarea completarTarea, ConsultarTareas consultarTareas,
                           ReasignarSinAsignar reasignarSinAsignar) {
        this.crearTarea = crearTarea;
        this.editarTarea = editarTarea;
        this.eliminarTarea = eliminarTarea;
        this.completarTarea = completarTarea;
        this.consultarTareas = consultarTareas;
        this.reasignarSinAsignar = reasignarSinAsignar;
    }

    @GetMapping("/salas/{salaId}/tareas")
    public List<TareaResponse> catalogo(@AuthenticationPrincipal Jwt jwt, @PathVariable long salaId) {
        return consultarTareas.catalogo(usuario(jwt), salaId).stream().map(TareaResponse::de).toList();
    }

    @PostMapping("/salas/{salaId}/tareas")
    @ResponseStatus(HttpStatus.CREATED)
    public TareaResponse crear(@AuthenticationPrincipal Jwt jwt, @PathVariable long salaId,
                               @Valid @RequestBody CrearTareaRequest r) {
        return TareaResponse.de(crearTarea.ejecutar(usuario(jwt), salaId, r.nombre(), r.descripcion(), r.valor(), Frecuencia.de(r.frecuencia()),
                r.fechaInicio()));
    }

    @PutMapping("/salas/{salaId}/tareas/{tareaId}")
    public TareaResponse editar(@AuthenticationPrincipal Jwt jwt, @PathVariable long salaId, @PathVariable long tareaId,
                                @Valid @RequestBody EditarTareaRequest r) {
        return TareaResponse.de(editarTarea.ejecutar(usuario(jwt), salaId, tareaId, r.nombre(), r.valor(),
                Frecuencia.de(r.frecuencia())));
    }

    @DeleteMapping("/salas/{salaId}/tareas/{tareaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@AuthenticationPrincipal Jwt jwt, @PathVariable long salaId, @PathVariable long tareaId) {
        eliminarTarea.ejecutar(usuario(jwt), salaId, tareaId);
    }

    @GetMapping("/salas/{salaId}/asignaciones")
    public List<AsignacionResponse> asignaciones(@AuthenticationPrincipal Jwt jwt, @PathVariable long salaId) {
        return consultarTareas.asignaciones(usuario(jwt), salaId).stream()
                .map((AsignacionConNombres a) -> AsignacionResponse.de(a.asignacion(), a.tarea(), a.responsable()))
                .toList();
    }

    @PostMapping("/salas/{salaId}/asignaciones/reasignar")
    public ReasignadasResponse reasignar(@AuthenticationPrincipal Jwt jwt, @PathVariable long salaId) {
        return new ReasignadasResponse(reasignarSinAsignar.ejecutar(usuario(jwt), salaId));
    }

    @PostMapping("/asignaciones/{asignacionId}/completar")
    public AsignacionResponse completar(@AuthenticationPrincipal Jwt jwt, @PathVariable long asignacionId) {
        return AsignacionResponse.de(completarTarea.ejecutar(usuario(jwt), asignacionId), null, null);
    }

    @GetMapping("/salas/{salaId}/balance")
    public List<Balance> balance(@AuthenticationPrincipal Jwt jwt, @PathVariable long salaId) {
        return consultarTareas.balance(usuario(jwt), salaId);
    }

    private static long usuario(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
