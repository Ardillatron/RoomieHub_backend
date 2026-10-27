package roomiehub.vivienda.domain.model.convivencia;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import roomiehub.vivienda.domain.exception.DatoInvalidoException;
import roomiehub.vivienda.domain.exception.RecursoNoEncontradoException;

/** RN-30 y RN-32: un mensaje del chat general de la sala, con texto, imagen o ambos. */
public final class MensajeChat {

    private final Long id;
    private final long salaId;
    private final long autorId;
    private final String texto;
    private final String imagenId;
    private final Long notaRespondidaId;
    private final Instant creadoEn;
    private final UUID clientUuid;
    private boolean eliminado;

    public MensajeChat(Long id, long salaId, long autorId, String texto, String imagenId, Long notaRespondidaId,
                       Instant creadoEn, UUID clientUuid, boolean eliminado) {
        String limpio = texto == null || texto.isBlank() ? null : texto.trim();
        if (limpio == null && imagenId == null) {
            throw new DatoInvalidoException("Un mensaje necesita texto o imagen");
        }
        if (limpio != null && limpio.length() > 4000) {
            throw new DatoInvalidoException("Un mensaje tiene a lo más 4000 caracteres");
        }
        this.id = id;
        this.salaId = salaId;
        this.autorId = autorId;
        this.texto = limpio;
        this.imagenId = imagenId;
        this.notaRespondidaId = notaRespondidaId;
        this.creadoEn = Objects.requireNonNull(creadoEn, "creadoEn");
        this.clientUuid = clientUuid;
        this.eliminado = eliminado;
    }

    /**
     * CU-14 y CU-11. @param notaRespondidaId la nota que se responde (RN-30), o nulo
     * @param clientUuid el de la app (RN-35): reenviar el mismo mensaje no lo duplica
     */
    public static MensajeChat nuevo(long salaId, long autorId, String texto, String imagenId, Long notaRespondidaId,
                                    UUID clientUuid, Instant ahora) {
        return new MensajeChat(null, salaId, autorId, texto, imagenId, notaRespondidaId, ahora, clientUuid, false);
    }

    /** CU-15: borrado lógico. */
    public void eliminar() {
        exigirVisible();
        eliminado = true;
    }

    public void exigirVisible() {
        if (eliminado) {
            throw new RecursoNoEncontradoException("Mensaje no encontrado");
        }
    }

    public Long id() { return id; }
    public long salaId() { return salaId; }
    public long autorId() { return autorId; }
    public String texto() { return texto; }
    public String imagenId() { return imagenId; }
    public Long notaRespondidaId() { return notaRespondidaId; }
    public Instant creadoEn() { return creadoEn; }
    public UUID clientUuid() { return clientUuid; }
    public boolean eliminado() { return eliminado; }
}
