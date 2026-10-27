package roomiehub.vivienda.domain.model.finanzas;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import roomiehub.vivienda.domain.exception.ConflictoDeEstadoException;
import roomiehub.vivienda.domain.exception.DatoInvalidoException;
import roomiehub.vivienda.domain.exception.RecursoNoEncontradoException;
import roomiehub.vivienda.domain.model.Periodo;
import roomiehub.vivienda.domain.service.CalendarioDePeriodos;

/** RN-21 a RN-25. Los participantes viven aparte (servicio_participantes), con su orden fijo. */
public final class GastoCompartido {

    private final Long id;
    private final long salaId;
    private String concepto;
    private Dinero monto;
    private FrecuenciaGasto frecuencia;
    private LocalDate fechaInicio;
    /** Primer día cuyo periodo genera deudas: salta lo que pasó con la sala desactivada (RN-41). */
    private LocalDate generarDesde;
    private boolean eliminado;
    private Long eliminadoPor;
    private Instant eliminadoEn;

    public GastoCompartido(Long id, long salaId, String concepto, Dinero monto, FrecuenciaGasto frecuencia,
                           LocalDate fechaInicio, LocalDate generarDesde, boolean eliminado, Long eliminadoPor,
                           Instant eliminadoEn) {
        this.id = id;
        this.salaId = salaId;
        this.concepto = validarConcepto(concepto);
        this.monto = validarMonto(monto);
        this.frecuencia = Objects.requireNonNull(frecuencia, "frecuencia");
        this.fechaInicio = Objects.requireNonNull(fechaInicio, "fechaInicio");
        this.generarDesde = Objects.requireNonNull(generarDesde, "generarDesde");
        this.eliminado = eliminado;
        this.eliminadoPor = eliminadoPor;
        this.eliminadoEn = eliminadoEn;
    }

    /** CU-20. Con una fecha de inicio pasada, se generan las deudas de esos periodos (recibos reales). */
    public static GastoCompartido nuevo(long salaId, String concepto, Dinero monto, FrecuenciaGasto frecuencia,
                                        LocalDate fechaInicio) {
        return new GastoCompartido(null, salaId, concepto, monto, frecuencia, fechaInicio, fechaInicio, false, null, null);
    }

    /**
     * RN-24: solo afecta a los periodos futuros. Las deudas ya generadas guardan su monto, y una frecuencia
     * nueva empieza cuando cierra el periodo en curso, como en las tareas (RN-12).
     */
    public void editar(String concepto, Dinero monto, FrecuenciaGasto nuevaFrecuencia, Instant ahora, ZoneId zona) {
        if (monto.moneda() != this.monto.moneda()) {
            throw new DatoInvalidoException("La moneda de un gasto no se cambia: las deudas ya generadas la usan");
        }
        this.concepto = validarConcepto(concepto);
        this.monto = validarMonto(monto);
        if (Objects.requireNonNull(nuevaFrecuencia, "frecuencia") != frecuencia) {
            if (nuevaFrecuencia == FrecuenciaGasto.UNICA || frecuencia == FrecuenciaGasto.UNICA) {
                throw new ConflictoDeEstadoException("Un gasto único no cambia de frecuencia: crea otro gasto");
            }
            LocalDate inicioNuevo = periodoActual(ahora, zona)
                    .map(p -> LocalDate.ofInstant(p.fin(), zona))
                    .orElse(fechaInicio);
            fechaInicio = inicioNuevo;
            generarDesde = generarDesde.isAfter(inicioNuevo) ? generarDesde : inicioNuevo;
            frecuencia = nuevaFrecuencia;
        }
    }

    /** RN-25: borrado lógico; deja de generar periodos y su historial se conserva. */
    public void eliminar(long usuarioId, Instant ahora) {
        eliminado = true;
        eliminadoPor = usuarioId;
        eliminadoEn = ahora;
    }

    /** Sección 8: actuar sobre un gasto eliminado se rechaza como recurso eliminado. */
    public void exigirVigente() {
        if (eliminado) {
            throw new RecursoNoEncontradoException("El gasto «" + concepto + "» fue eliminado");
        }
    }

    /**
     * Ningún periodo que empiece antes de {@code dia} genera deudas. Se usa al reactivar la sala (RN-41: lo
     * que empezó mientras estuvo desactivada no se cobra) y tras generar, para no revisar el pasado otra vez.
     */
    public void avanzarGeneracionHasta(LocalDate dia) {
        if (dia.isAfter(generarDesde)) {
            generarDesde = dia;
        }
    }

    /**
     * CU-25 y RN-41: todos los periodos ya iniciados desde {@code generarDesde}, incluidos los que el
     * proceso no alcanzó a generar mientras estuvo dormido. Quien llama descarta los que ya tienen deudas.
     */
    public List<Periodo> periodosIniciados(Instant ahora, ZoneId zona) {
        Instant desde = generarDesde.atStartOfDay(zona).toInstant();
        Optional<Period> paso = frecuencia.paso();
        if (paso.isEmpty()) {
            Instant inicio = fechaInicio.atStartOfDay(zona).toInstant();
            return ahora.isBefore(inicio) || inicio.isBefore(desde) ? List.of()
                    : List.of(new Periodo(0, inicio, fechaInicio.plusDays(1).atStartOfDay(zona).toInstant()));
        }
        CalendarioDePeriodos calendario = new CalendarioDePeriodos(fechaInicio, paso.get(), zona);
        Optional<Periodo> actual = calendario.periodoQueContiene(ahora);
        if (actual.isEmpty()) {
            return List.of();
        }
        long primero = calendario.periodoQueContiene(desde).map(Periodo::numero).orElse(0L);
        List<Periodo> periodos = new ArrayList<>();
        for (long k = primero; k <= actual.get().numero(); k++) {
            Periodo p = calendario.periodo(k);
            if (!p.inicio().isBefore(desde)) { // uno que empezó antes de generarDesde no cuenta
                periodos.add(p);
            }
        }
        return periodos;
    }

    /** Número del periodo que empieza en {@code inicioPeriodo}, para rotar los centavos (RN-22). */
    public long numeroDePeriodo(LocalDate inicioPeriodo, ZoneId zona) {
        return frecuencia.paso()
                .map(paso -> new CalendarioDePeriodos(fechaInicio, paso, zona)
                        .periodoQueContiene(inicioPeriodo.atStartOfDay(zona).toInstant())
                        .orElseThrow(() -> new IllegalArgumentException("Periodo anterior al inicio del gasto"))
                        .numero())
                .orElse(0L); // UNICA: un solo periodo
    }

    private Optional<Periodo> periodoActual(Instant ahora, ZoneId zona) {
        return frecuencia.paso().flatMap(paso ->
                new CalendarioDePeriodos(fechaInicio, paso, zona).periodoQueContiene(ahora));
    }

    private static String validarConcepto(String concepto) {
        if (concepto == null || concepto.isBlank() || concepto.trim().length() > 80) {
            throw new DatoInvalidoException("El concepto tiene de 1 a 80 caracteres");
        }
        return concepto.trim();
    }

    private static Dinero validarMonto(Dinero monto) {
        if (Objects.requireNonNull(monto, "monto").monto().signum() <= 0) {
            throw new DatoInvalidoException("El monto de un gasto debe ser mayor que cero");
        }
        return monto;
    }

    public Long id() { return id; }
    public long salaId() { return salaId; }
    public String concepto() { return concepto; }
    public Dinero monto() { return monto; }
    public FrecuenciaGasto frecuencia() { return frecuencia; }
    public LocalDate fechaInicio() { return fechaInicio; }
    public LocalDate generarDesde() { return generarDesde; }
    public boolean eliminado() { return eliminado; }
    public Long eliminadoPor() { return eliminadoPor; }
    public Instant eliminadoEn() { return eliminadoEn; }
}
