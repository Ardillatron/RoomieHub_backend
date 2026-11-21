package roomiehub.vivienda.domain.model.sala;

import java.time.Instant;
import java.util.Objects;

public final class Membresia {

    private final Long id;
    private final long usuarioId;
    private final long salaId;
    private Instant unidoEn;
    private Rol rol;
    private boolean activo;

    public Membresia(Long id, long usuarioId, long salaId, Rol rol, boolean activo, Instant unidoEn) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.salaId = salaId;
        this.rol = Objects.requireNonNull(rol, "rol");
        this.activo = activo;
        this.unidoEn = Objects.requireNonNull(unidoEn, "unidoEn");
    }

    public static Membresia nueva(long usuarioId, long salaId, Rol rol, Instant ahora) {
        return new Membresia(null, usuarioId, salaId, rol, true, ahora);
    }

    public void promover() { rol = Rol.ADMIN; }

    public void degradar() { rol = Rol.HABITANTE; }

    // Borrado logico y la fila y el historial se quedan
    public void desactivar() { activo = false; }

    // Quien vuelve entra como Habitante y su antiguedad cuenta desde hoy
    public void reactivar(Instant ahora) {
        activo = true;
        rol = Rol.HABITANTE;
        unidoEn = Objects.requireNonNull(ahora, "ahora");
    }

    public Long id() { return id; }
    public long usuarioId() { return usuarioId; }
    public long salaId() { return salaId; }
    public Rol rol() { return rol; }
    public boolean activo() { return activo; }
    public Instant unidoEn() { return unidoEn; }
}
