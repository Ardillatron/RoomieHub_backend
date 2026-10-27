package roomiehub.vivienda.domain.repository;

import java.util.Collection;
import java.util.Map;

import roomiehub.vivienda.domain.model.convivencia.TipoReaccion;

public interface ReaccionRepository {

    record Conteo(long likes, long dislikes) {}

    /** RN-29: una por persona y nota; reaccionar otra vez cambia el tipo. */
    void reaccionar(long notaId, long salaId, long usuarioId, TipoReaccion tipo);

    void quitar(long notaId, long usuarioId);

    Map<Long, Conteo> conteos(Collection<Long> notaIds);

    /** notaId → la reacción de ese usuario, solo para las notas donde reaccionó. */
    Map<Long, TipoReaccion> deUsuario(Collection<Long> notaIds, long usuarioId);
}
