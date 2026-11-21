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

// Gasto compartido y sus participantes van aparte con su orden fijo
public final class GastoCompartido {

    private final Long id;
    private final long salaId;
    private String concepto;
    private Dinero monto;
    private FrecuenciaGasto frecuencia;
    private LocalDate fechaInicio;
    // Primer dia cuyo periodo genera deudas y salta lo que paso con la sala desactivada
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

    // CU-20 con una fecha de inicio pasada se generan las deudas de esos periodos
    public static GastoCompartido nuevo(long salaId, String concepto, Dinero monto, FrecuenciaGasto frecuencia,
                                        LocalDate fechaInicio) {
        return new GastoCompartido(null, salaId, concepto, monto, frecuencia, fechaInicio, fechaInicio, false, null, null);
    }

    /* Solo cambia los periodos futuros y las deudas ya creadas guardan su monto
       una frecuencia nueva empieza cuando cierra el periodo actual como en las tareas */
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

    // Borrado logico y deja de generar periodos pero su historial se queda
    public void eliminar(long usuarioId, Instant ahora) {
        eliminado = true;
        eliminadoPor = usuarioId;
        eliminadoEn = ahora;
    }

    // Actuar sobre un gasto borrado se rechaza como recurso eliminado
    public void exigirVigente() {
        if (eliminado) {
            throw new RecursoNoEncontradoException("El gasto «" + concepto + "» fue eliminado");
        }
    }

    /* Ningun periodo que empiece antes del dia dado genera deudas
       se usa al reactivar la sala y despues de generar para no revisar el pasado */
    public void avanzarGeneracionHasta(LocalDate dia) {
        if (dia.isAfter(generarDesde)) {
            generarDesde = dia;
        }
    }

    /* CU-25 todos los periodos ya iniciados desde generarDesde incluidos los atrasados
       quien llama descarta los que ya tienen deudas */
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
            if (!p.inicio().isBefore(desde)) { // uno que empezo antes de generarDesde no cuenta
                periodos.add(p);
            }
        }
        return periodos;
    }

    // Numero del periodo que empieza en inicioPeriodo para rotar los centavos
    public long numeroDePeriodo(LocalDate inicioPeriodo, ZoneId zona) {
        return frecuencia.paso()
                .map(paso -> new CalendarioDePeriodos(fechaInicio, paso, zona)
                        .periodoQueContiene(inicioPeriodo.atStartOfDay(zona).toInstant())
                        .orElseThrow(() -> new IllegalArgumentException("Periodo anterior al inicio del gasto"))
                        .numero())
                .orElse(0L); // UNICA tiene un solo periodo
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
