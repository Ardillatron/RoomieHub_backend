package roomiehub.vivienda.domain.model.tareas;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;

import roomiehub.vivienda.domain.exception.DatoInvalidoException;
import roomiehub.vivienda.domain.model.Periodo;
import roomiehub.vivienda.domain.service.CalendarioDePeriodos;

/** RN-13: se crea una vez con una frecuencia y se asigna al iniciar cada periodo. */
public final class Tarea {

    private final Long id;
    private final long salaId;
    private Frecuencia frecuencia;
    private LocalDate fechaInicio;
    private String nombre;
    private ValorTarea valor;
    private boolean activa;

    public Tarea(Long id, long salaId, String nombre, ValorTarea valor, Frecuencia frecuencia, LocalDate fechaInicio,
                 boolean activa) {
        this.id = id;
        this.salaId = salaId;
        this.nombre = validarNombre(nombre);
        this.valor = Objects.requireNonNull(valor, "valor");
        this.frecuencia = Objects.requireNonNull(frecuencia, "frecuencia");
        this.fechaInicio = Objects.requireNonNull(fechaInicio, "fechaInicio");
        this.activa = activa;
    }

    public static Tarea nueva(long salaId, String nombre, ValorTarea valor, Frecuencia frecuencia, LocalDate fechaInicio) {
        return new Tarea(null, salaId, nombre, valor, frecuencia, fechaInicio, true);
    }

    /**
     * RN-11 y RN-12: los cambios solo cuentan para las asignaciones futuras. El valor, porque cada
     * asignación guarda el que tenía al crearse. La frecuencia, porque el periodo en curso termina con la
     * anterior y la nueva empieza cuando ese periodo cierra; así no se empalman dos asignaciones.
     */
    public void editar(String nombre, ValorTarea valor, Frecuencia nuevaFrecuencia, Instant ahora, ZoneId zona) {
        this.nombre = validarNombre(nombre);
        this.valor = Objects.requireNonNull(valor, "valor");
        if (Objects.requireNonNull(nuevaFrecuencia, "frecuencia") != frecuencia) {
            // Se calcula con la frecuencia anterior; si la tarea aún no empieza, conserva su fecha de inicio.
            fechaInicio = periodoEn(ahora, zona).map(p -> LocalDate.ofInstant(p.fin(), zona)).orElse(fechaInicio);
            frecuencia = nuevaFrecuencia;
        }
    }

    /** Borrado lógico: deja de rolearse y su historial se conserva. */
    public void eliminar() {
        activa = false;
    }

    /** El periodo de esta tarea que contiene {@code instante}, o vacío si aún no empieza. */
    public Optional<Periodo> periodoEn(Instant instante, ZoneId zona) {
        return new CalendarioDePeriodos(fechaInicio, frecuencia.paso(), zona).periodoQueContiene(instante);
    }

    private static String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank() || nombre.trim().length() > 80) {
            throw new DatoInvalidoException("El nombre de la tarea tiene de 1 a 80 caracteres");
        }
        return nombre.trim();
    }

    public Long id() { return id; }
    public long salaId() { return salaId; }
    public String nombre() { return nombre; }
    public ValorTarea valor() { return valor; }
    public Frecuencia frecuencia() { return frecuencia; }
    public LocalDate fechaInicio() { return fechaInicio; }
    public boolean activa() { return activa; }
}
