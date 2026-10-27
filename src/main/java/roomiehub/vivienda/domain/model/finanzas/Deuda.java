package roomiehub.vivienda.domain.model.finanzas;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import roomiehub.vivienda.domain.exception.DeudaCerradaException;
import roomiehub.vivienda.domain.exception.OperacionNoPermitidaException;

/**
 * RN-26: la deuda se aprueba cuando todos los demás participantes activos del gasto confirman,
 * en cualquier orden. Si no queda ningún otro participante, se aprueba al avisar.
 * Aprobada y Repartida (RN-08) son finales.
 */
public final class Deuda {

    /** Sección 10: al deudor a los 3 días si no ha avisado, luego cada 7, máximo 3 por periodo. */
    static final Duration PRIMER_RECORDATORIO = Duration.ofDays(3);
    static final Duration ENTRE_RECORDATORIOS = Duration.ofDays(7);
    static final int MAXIMO_RECORDATORIOS = 3;
    /** Sección 10: a quien falte por confirmar, a las 24 horas del aviso. */
    static final Duration RECORDATORIO_CONFIRMACION = Duration.ofHours(24);

    private final Long id;
    private final long servicioId;
    private final long salaId;
    private final LocalDate periodo;
    private final long deudorId;
    private final Dinero monto;
    private final Long deudaOrigenId;
    private final Set<Long> confirmadores;
    private final Instant creadoEn;
    private EstadoDeuda estado;
    private Instant avisoPagoEn;
    private int recordatoriosEnviados;
    private Instant ultimoRecordatorioEn;
    private Instant recordatorioConfirmacionEn;

    public Deuda(Long id, long servicioId, long salaId, LocalDate periodo, long deudorId, Dinero monto,
                 Long deudaOrigenId, EstadoDeuda estado, Instant avisoPagoEn, Set<Long> confirmadores,
                 Instant creadoEn, int recordatoriosEnviados, Instant ultimoRecordatorioEn,
                 Instant recordatorioConfirmacionEn) {
        this.id = id;
        this.servicioId = servicioId;
        this.salaId = salaId;
        this.periodo = Objects.requireNonNull(periodo, "periodo");
        this.deudorId = deudorId;
        this.monto = Objects.requireNonNull(monto, "monto");
        this.deudaOrigenId = deudaOrigenId;
        this.estado = Objects.requireNonNull(estado, "estado");
        this.avisoPagoEn = avisoPagoEn;
        this.confirmadores = new HashSet<>(confirmadores);
        this.creadoEn = creadoEn;
        this.recordatoriosEnviados = recordatoriosEnviados;
        this.ultimoRecordatorioEn = ultimoRecordatorioEn;
        this.recordatorioConfirmacionEn = recordatorioConfirmacionEn;
    }

    public static Deuda nueva(long servicioId, long salaId, LocalDate periodo, long deudorId, Dinero monto,
                              Long deudaOrigenId) {
        return new Deuda(null, servicioId, salaId, periodo, deudorId, monto, deudaOrigenId,
                EstadoDeuda.PENDIENTE, null, Set.of(), null, 0, null, null);
    }

    /** El deudor avisa que pagó. Un segundo aviso conserva la hora del primero. */
    public boolean avisarPago(Instant ahora, Set<Long> participantesActivos) {
        exigirPendiente();
        if (avisoPagoEn == null) {
            avisoPagoEn = Objects.requireNonNull(ahora, "ahora");
        }
        return reevaluar(participantesActivos);
    }

    /** @return true si con esta confirmación la deuda quedó aprobada. */
    public boolean confirmar(long confirmadorId, Set<Long> participantesActivos) {
        exigirPendiente();
        if (confirmadorId == deudorId) {
            throw new OperacionNoPermitidaException("El deudor no puede confirmar su propio pago");
        }
        if (!participantesActivos.contains(confirmadorId)) {
            throw new OperacionNoPermitidaException("Solo un participante activo del gasto puede confirmar");
        }
        confirmadores.add(confirmadorId);
        return reevaluar(participantesActivos);
    }

    /**
     * Recalcula con los participantes activos actuales; se llama también cuando alguien sale del
     * gasto, por si la deuda ya reúne las confirmaciones que ahora se requieren.
     */
    public boolean reevaluar(Set<Long> participantesActivos) {
        if (estado != EstadoDeuda.PENDIENTE) {
            return false;
        }
        Set<Long> requeridos = new HashSet<>(participantesActivos);
        requeridos.remove(deudorId);
        boolean aprobada = requeridos.isEmpty() ? avisoPagoEn != null : confirmadores.containsAll(requeridos);
        if (aprobada) {
            estado = EstadoDeuda.APROBADA;
        }
        return aprobada;
    }

    /** RN-08: el deudor salió; su monto se reparte en deudas nuevas que apuntan a esta. */
    public void marcarRepartida() {
        exigirPendiente();
        estado = EstadoDeuda.REPARTIDA;
    }

    /** Sección 10. @return true si toca recordarle al deudor ahora; deja registrado el envío. */
    public boolean tocaRecordarAlDeudor(Instant ahora) {
        if (estado != EstadoDeuda.PENDIENTE || avisoPagoEn != null || creadoEn == null
                || recordatoriosEnviados >= MAXIMO_RECORDATORIOS) {
            return false;
        }
        Instant siguiente = ultimoRecordatorioEn == null
                ? creadoEn.plus(PRIMER_RECORDATORIO)
                : ultimoRecordatorioEn.plus(ENTRE_RECORDATORIOS);
        if (ahora.isBefore(siguiente)) {
            return false;
        }
        recordatoriosEnviados++;
        ultimoRecordatorioEn = ahora;
        return true;
    }

    /** Sección 10, una sola vez por deuda. @return a quiénes recordarles confirmar (vacío si no toca). */
    public Set<Long> recordarConfirmaciones(Instant ahora, Set<Long> participantesActivos) {
        if (estado != EstadoDeuda.PENDIENTE || avisoPagoEn == null || recordatorioConfirmacionEn != null
                || ahora.isBefore(avisoPagoEn.plus(RECORDATORIO_CONFIRMACION))) {
            return Set.of();
        }
        recordatorioConfirmacionEn = ahora;
        return faltanPorConfirmar(participantesActivos);
    }

    public Set<Long> faltanPorConfirmar(Set<Long> participantesActivos) {
        Set<Long> faltan = new HashSet<>(participantesActivos);
        faltan.remove(deudorId);
        faltan.removeAll(confirmadores);
        return faltan;
    }

    private void exigirPendiente() {
        if (estado != EstadoDeuda.PENDIENTE) {
            throw new DeudaCerradaException();
        }
    }

    public Long id() { return id; }
    public long servicioId() { return servicioId; }
    public long salaId() { return salaId; }
    public LocalDate periodo() { return periodo; }
    public long deudorId() { return deudorId; }
    public Dinero monto() { return monto; }
    public Long deudaOrigenId() { return deudaOrigenId; }
    public EstadoDeuda estado() { return estado; }
    public Instant avisoPagoEn() { return avisoPagoEn; }
    public Set<Long> confirmadores() { return Collections.unmodifiableSet(confirmadores); }
    public Instant creadoEn() { return creadoEn; }
    public int recordatoriosEnviados() { return recordatoriosEnviados; }
    public Instant ultimoRecordatorioEn() { return ultimoRecordatorioEn; }
    public Instant recordatorioConfirmacionEn() { return recordatorioConfirmacionEn; }
}
