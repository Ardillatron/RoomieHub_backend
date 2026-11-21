package roomiehub.vivienda.domain.model.convivencia;

import java.time.Instant;
import java.util.Objects;

import roomiehub.vivienda.domain.exception.ConflictoDeEstadoException;
import roomiehub.vivienda.domain.exception.DatoInvalidoException;

// El reporte de una nota o mensaje y lo resuelve el admin
public final class Reporte {

    private final Long id;
    private final long salaId;
    private final Long notaId;
    private final Long mensajeId;
    private final long usuarioId;
    private final String motivo;
    private final Instant creadoEn;
    private boolean resuelto;

    public Reporte(Long id, long salaId, Long notaId, Long mensajeId, long usuarioId, String motivo, Instant creadoEn,
                   boolean resuelto) {
        if ((notaId == null) == (mensajeId == null)) {
            throw new IllegalArgumentException("Un reporte es de una nota o de un mensaje, no de ambos");
        }
        if (motivo == null || motivo.isBlank() || motivo.trim().length() > 500) {
            throw new DatoInvalidoException("El motivo tiene de 1 a 500 caracteres");
        }
        this.id = id;
        this.salaId = salaId;
        this.notaId = notaId;
        this.mensajeId = mensajeId;
        this.usuarioId = usuarioId;
        this.motivo = motivo.trim();
        this.creadoEn = Objects.requireNonNull(creadoEn, "creadoEn");
        this.resuelto = resuelto;
    }

    public static Reporte deNota(long salaId, long notaId, long usuarioId, String motivo, Instant ahora) {
        return new Reporte(null, salaId, notaId, null, usuarioId, motivo, ahora, false);
    }

    // CU-32
    public static Reporte deMensaje(long salaId, long mensajeId, long usuarioId, String motivo, Instant ahora) {
        return new Reporte(null, salaId, null, mensajeId, usuarioId, motivo, ahora, false);
    }

    public void resolver() {
        if (resuelto) {
            throw new ConflictoDeEstadoException("El reporte ya está resuelto");
        }
        resuelto = true;
    }

    public Long id() { return id; }
    public long salaId() { return salaId; }
    public Long notaId() { return notaId; }
    public Long mensajeId() { return mensajeId; }
    public long usuarioId() { return usuarioId; }
    public String motivo() { return motivo; }
    public Instant creadoEn() { return creadoEn; }
    public boolean resuelto() { return resuelto; }
}
