package roomiehub.vivienda.domain.model.sala;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

import roomiehub.vivienda.domain.exception.ConflictoDeEstadoException;
import roomiehub.vivienda.domain.exception.DatoInvalidoException;
import roomiehub.vivienda.domain.exception.OperacionNoPermitidaException;

public final class Sala {

    /** RN-09: plazo para reactivar una sala desactivada antes de que se borre. */
    public static final Duration PLAZO_REACTIVACION = Duration.ofDays(15);

    private final Long id;
    private final String nombre;
    private final long creadorId;
    private final ZoneId zonaHoraria;
    private final String codigoInvitacion;
    private EstadoSala estado;
    private Instant desactivadaEn;

    public Sala(Long id, String nombre, long creadorId, ZoneId zonaHoraria, String codigoInvitacion,
                EstadoSala estado, Instant desactivadaEn) {
        this.id = id;
        this.nombre = validarNombre(nombre);
        this.creadorId = creadorId;
        this.zonaHoraria = Objects.requireNonNull(zonaHoraria, "zonaHoraria");
        this.codigoInvitacion = Objects.requireNonNull(codigoInvitacion, "codigoInvitacion");
        this.estado = Objects.requireNonNull(estado, "estado");
        this.desactivadaEn = desactivadaEn;
    }

    /** CU-02. La zona horaria viene del teléfono del creador (RN-38). */
    public static Sala nueva(String nombre, long creadorId, String zonaHoraria, CodigoInvitacion codigo) {
        return new Sala(null, nombre, creadorId, zona(zonaHoraria), codigo.valor(), EstadoSala.ACTIVA, null);
    }

    /** RN-09: solo lectura hasta que se reactive o pasen 15 días. */
    public void desactivar(Instant ahora) {
        if (estado == EstadoSala.DESACTIVADA) {
            throw new ConflictoDeEstadoException("La sala ya está desactivada");
        }
        estado = EstadoSala.DESACTIVADA;
        desactivadaEn = ahora;
    }

    public void reactivar(Instant ahora) {
        if (estado == EstadoSala.ACTIVA) {
            throw new ConflictoDeEstadoException("La sala ya está activa");
        }
        if (!ahora.isBefore(desactivadaEn.plus(PLAZO_REACTIVACION))) {
            throw new ConflictoDeEstadoException("El plazo de 15 días para reactivar la sala ya venció");
        }
        estado = EstadoSala.ACTIVA;
        desactivadaEn = null;
    }

    /** Las acciones que cambian la sala (salvo reactivarla o salir de ella) llaman a esto primero. */
    public void exigirActiva() {
        if (estado == EstadoSala.DESACTIVADA) {
            throw new OperacionNoPermitidaException("La sala está desactivada: solo lectura");
        }
    }

    private static ZoneId zona(String zonaHoraria) {
        try {
            return ZoneId.of(Objects.requireNonNull(zonaHoraria, "zonaHoraria"));
        } catch (DateTimeException e) {
            throw new DatoInvalidoException("Zona horaria inválida: " + zonaHoraria);
        }
    }

    private static String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank() || nombre.trim().length() > 60) {
            throw new DatoInvalidoException("El nombre de la sala tiene de 1 a 60 caracteres");
        }
        return nombre.trim();
    }

    public Long id() { return id; }
    public String nombre() { return nombre; }
    public long creadorId() { return creadorId; }
    public ZoneId zonaHoraria() { return zonaHoraria; }
    public String codigoInvitacion() { return codigoInvitacion; }
    public EstadoSala estado() { return estado; }
    public Instant desactivadaEn() { return desactivadaEn; }
}
