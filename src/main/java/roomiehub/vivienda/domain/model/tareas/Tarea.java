package roomiehub.vivienda.domain.model.tareas;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;

import roomiehub.vivienda.domain.exception.DatoInvalidoException;
import roomiehub.vivienda.domain.model.Periodo;
import roomiehub.vivienda.domain.service.CalendarioDePeriodos;

// Se crea una vez con una frecuencia y se asigna al empezar cada periodo
public final class Tarea {

    private final Long id;
    private final long salaId;
    private Frecuencia frecuencia;
    private LocalDate fechaInicio;
    private String nombre;
    // Opcional y null cuando no tiene
    private final String descripcion;
    private ValorTarea valor;
    private boolean activa;

    public Tarea(Long id, long salaId, String nombre, String descripcion, ValorTarea valor, Frecuencia frecuencia,
                 LocalDate fechaInicio, boolean activa) {
        this.id = id;
        this.salaId = salaId;
        this.nombre = validarNombre(nombre);
        this.descripcion = validarDescripcion(descripcion);
        this.valor = Objects.requireNonNull(valor, "valor");
        this.frecuencia = Objects.requireNonNull(frecuencia, "frecuencia");
        this.fechaInicio = Objects.requireNonNull(fechaInicio, "fechaInicio");
        this.activa = activa;
    }

    public static Tarea nueva(long salaId, String nombre, String descripcion, ValorTarea valor, Frecuencia frecuencia,
                              LocalDate fechaInicio) {
        return new Tarea(null, salaId, nombre, descripcion, valor, frecuencia, fechaInicio, true);
    }

    /* Los cambios solo cuentan para las asignaciones futuras
       cada asignacion guarda el valor que tenia al crearse
       la frecuencia nueva empieza cuando cierra el periodo actual para no empalmar asignaciones */
    public void editar(String nombre, ValorTarea valor, Frecuencia nuevaFrecuencia, Instant ahora, ZoneId zona) {
        this.nombre = validarNombre(nombre);
        this.valor = Objects.requireNonNull(valor, "valor");
        if (!Objects.requireNonNull(nuevaFrecuencia, "frecuencia").equals(frecuencia)) {
            // Se calcula con la frecuencia anterior y si la tarea no empieza conserva su fecha de inicio
            fechaInicio = periodoEn(ahora, zona).map(p -> LocalDate.ofInstant(p.fin(), zona)).orElse(fechaInicio);
            frecuencia = nuevaFrecuencia;
        }
    }

    // Borrado logico y deja de rolearse pero su historial se queda
    public void eliminar() {
        activa = false;
    }

    // El periodo de esta tarea que contiene el instante o vacio si aun no empieza
    public Optional<Periodo> periodoEn(Instant instante, ZoneId zona) {
        return new CalendarioDePeriodos(fechaInicio, frecuencia.paso(), zona).periodoQueContiene(instante);
    }

    private static String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank() || nombre.trim().length() > 80) {
            throw new DatoInvalidoException("El nombre de la tarea tiene de 1 a 80 caracteres");
        }
        return nombre.trim();
    }

    // En blanco cuenta como sin descripcion
    private static String validarDescripcion(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            return null;
        }
        if (descripcion.trim().length() > 500) {
            throw new DatoInvalidoException("La descripción de la tarea tiene como mucho 500 caracteres");
        }
        return descripcion.trim();
    }

    public Long id() { return id; }
    public long salaId() { return salaId; }
    public String nombre() { return nombre; }
    public String descripcion() { return descripcion; }
    public ValorTarea valor() { return valor; }
    public Frecuencia frecuencia() { return frecuencia; }
    public LocalDate fechaInicio() { return fechaInicio; }
    public boolean activa() { return activa; }
}
