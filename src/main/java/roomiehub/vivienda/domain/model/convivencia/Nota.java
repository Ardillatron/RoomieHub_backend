package roomiehub.vivienda.domain.model.convivencia;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import roomiehub.vivienda.domain.exception.DatoInvalidoException;
import roomiehub.vivienda.domain.exception.RecursoNoEncontradoException;

// Una nota del tablon
public final class Nota {

    // Maximo 30 dias fijada
    public static final Duration FIJADA_MAXIMO = Duration.ofDays(30);
    // Maximo 3 notas fijadas a la vez por sala
    public static final int MAXIMO_FIJADAS = 3;

    private final Long id;
    private final long salaId;
    private final long autorId;
    private final String texto;
    private final String imagenId;
    private final Instant creadoEn;
    private final UUID clientUuid;
    private Instant fijadaHasta;
    private boolean eliminada;

    public Nota(Long id, long salaId, long autorId, String texto, String imagenId, Instant creadoEn, UUID clientUuid,
                Instant fijadaHasta, boolean eliminada) {
        this.id = id;
        this.salaId = salaId;
        this.autorId = autorId;
        this.texto = texto == null ? "" : texto.trim();
        this.imagenId = imagenId;
        this.creadoEn = Objects.requireNonNull(creadoEn, "creadoEn");
        this.clientUuid = clientUuid;
        this.fijadaHasta = fijadaHasta;
        this.eliminada = eliminada;
        if (this.texto.isEmpty() && imagenId == null) {
            throw new DatoInvalidoException("Una nota necesita texto o imagen");
        }
        if (this.texto.length() > 2000) {
            throw new DatoInvalidoException("Una nota tiene a lo más 2000 caracteres");
        }
    }

    // CU-09 clientUuid es el de la app y reintentar no duplica la nota
    public static Nota nueva(long salaId, long autorId, String texto, String imagenId, UUID clientUuid, Instant ahora) {
        return new Nota(null, salaId, autorId, texto, imagenId, ahora, clientUuid, null, false);
    }

    /* Fijada hasta la fecha dada que no puede pasar de 30 dias
       el tope de 3 lo revisa el caso de uso */
    public void fijar(Instant hasta, Instant ahora) {
        exigirVisible();
        if (!hasta.isAfter(ahora) || hasta.isAfter(ahora.plus(FIJADA_MAXIMO))) {
            throw new DatoInvalidoException("Una nota se fija por un plazo de hasta 30 días");
        }
        fijadaHasta = hasta;
    }

    public void desfijar() {
        fijadaHasta = null;
    }

    // CU-13 borrado logico y deja de mostrarse
    public void eliminar() {
        exigirVisible();
        eliminada = true;
        fijadaHasta = null;
    }

    // Una nota borrada responde como si no existiera
    public void exigirVisible() {
        if (eliminada) {
            throw new RecursoNoEncontradoException("Nota no encontrada");
        }
    }

    // CU-30 el plazo vencido deja de contar aunque el proceso no la haya desfijado
    public boolean estaFijada(Instant ahora) {
        return fijadaHasta != null && fijadaHasta.isAfter(ahora);
    }

    public Long id() { return id; }
    public long salaId() { return salaId; }
    public long autorId() { return autorId; }
    public String texto() { return texto; }
    public String imagenId() { return imagenId; }
    public Instant creadoEn() { return creadoEn; }
    public UUID clientUuid() { return clientUuid; }
    public Instant fijadaHasta() { return fijadaHasta; }
    public boolean eliminada() { return eliminada; }
}
