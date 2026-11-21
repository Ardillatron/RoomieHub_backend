package roomiehub.vivienda.domain.repository;

import java.util.Collection;
import java.util.Map;

import roomiehub.vivienda.domain.model.convivencia.TipoReaccion;

public interface ReaccionRepository {

    record Conteo(long likes, long dislikes) {}

    // Una por persona y nota y reaccionar otra vez cambia el tipo
    void reaccionar(long notaId, long salaId, long usuarioId, TipoReaccion tipo);

    void quitar(long notaId, long usuarioId);

    Map<Long, Conteo> conteos(Collection<Long> notaIds);

    // Por cada nota la reaccion de ese usuario solo donde reacciono
    Map<Long, TipoReaccion> deUsuario(Collection<Long> notaIds, long usuarioId);
}
