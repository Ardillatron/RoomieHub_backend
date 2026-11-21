package roomiehub.vivienda.domain.model.tareas;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import roomiehub.vivienda.domain.exception.ConflictoDeEstadoException;
import roomiehub.vivienda.domain.exception.OperacionNoPermitidaException;
import roomiehub.vivienda.domain.model.Periodo;

// Una tarea en un periodo con quien la tiene cuanto valia y si se cumplio
public final class AsignacionTarea {

    // Cuenta toda marca que llegue hasta 3 horas despues del cierre del periodo
    public static final Duration GRACIA = Duration.ofHours(3);

    private final Long id;
    private final long tareaId;
    private final long salaId;
    private final Instant inicio;
    private final Instant fin;
    private final ValorTarea valorAlAsignar;
    private Long usuarioId;
    private EstadoAsignacion estado;
    private Instant completadaEn;

    public AsignacionTarea(Long id, long tareaId, long salaId, Long usuarioId, Instant inicio, Instant fin,
                           ValorTarea valorAlAsignar, EstadoAsignacion estado, Instant completadaEn) {
        this.id = id;
        this.tareaId = tareaId;
        this.salaId = salaId;
        this.usuarioId = usuarioId;
        this.inicio = Objects.requireNonNull(inicio, "inicio");
        this.fin = Objects.requireNonNull(fin, "fin");
        this.valorAlAsignar = Objects.requireNonNull(valorAlAsignar, "valorAlAsignar");
        this.estado = Objects.requireNonNull(estado, "estado");
        this.completadaEn = completadaEn;
    }

    // usuarioId es null si no hay a quien asignarla y queda sin asignar
    public static AsignacionTarea nueva(Tarea tarea, Periodo periodo, Long usuarioId) {
        return new AsignacionTarea(null, tarea.id(), tarea.salaId(), usuarioId, periodo.inicio(), periodo.fin(),
                tarea.valor(), EstadoAsignacion.ASIGNADA, null);
    }

    // Marcar basta y se suman los puntos que valia al asignarse
    public MovimientoPuntos completar(long quien, Instant ahora) {
        if (usuarioId == null || usuarioId != quien) {
            throw new OperacionNoPermitidaException("Solo el responsable marca su tarea");
        }
        if (estado != EstadoAsignacion.ASIGNADA) {
            throw new ConflictoDeEstadoException("La tarea ya está " + estado.name().toLowerCase());
        }
        if (graciaVencida(ahora)) {
            throw new ConflictoDeEstadoException("El plazo para marcar esta tarea ya venció");
        }
        estado = EstadoAsignacion.COMPLETADA;
        completadaEn = ahora;
        return new MovimientoPuntos(quien, salaId, id, valorAlAsignar.valor(), TipoMovimiento.COMPLETADA);
    }

    /* Pasado el margen la tarea vence y se resta la mitad del valor al responsable
       regresa el movimiento de castigo o vacio si no habia responsable */
    public Optional<MovimientoPuntos> vencer(Instant ahora) {
        if (estado != EstadoAsignacion.ASIGNADA || !graciaVencida(ahora)) {
            throw new ConflictoDeEstadoException("La tarea todavía no vence");
        }
        estado = EstadoAsignacion.VENCIDA;
        return Optional.ofNullable(usuarioId).map(u ->
                new MovimientoPuntos(u, salaId, id, -valorAlAsignar.penalizacion(), TipoMovimiento.PENALIZACION));
    }

    // El admin reparte las que quedaron sin asignar
    public void asignarA(long nuevoResponsable) {
        if (usuarioId != null || estado != EstadoAsignacion.ASIGNADA) {
            throw new ConflictoDeEstadoException("Esta tarea ya tiene responsable");
        }
        usuarioId = nuevoResponsable;
    }

    public boolean graciaVencida(Instant ahora) {
        return !ahora.isBefore(fin.plus(GRACIA));
    }

    public Long id() { return id; }
    public long tareaId() { return tareaId; }
    public long salaId() { return salaId; }
    public Long usuarioId() { return usuarioId; }
    public Instant inicio() { return inicio; }
    public Instant fin() { return fin; }
    public ValorTarea valorAlAsignar() { return valorAlAsignar; }
    public EstadoAsignacion estado() { return estado; }
    public Instant completadaEn() { return completadaEn; }
}
