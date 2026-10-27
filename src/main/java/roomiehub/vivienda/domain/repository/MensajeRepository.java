package roomiehub.vivienda.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import roomiehub.vivienda.domain.model.convivencia.MensajeChat;

public interface MensajeRepository {

    /** Inserta o actualiza; devuelve el mensaje con su id. */
    MensajeChat guardar(MensajeChat mensaje);

    Optional<MensajeChat> buscarPorId(long id);

    /** RN-35: el mensaje que ya creó esta misma acción offline, si existe. */
    Optional<MensajeChat> buscarPorClientUuid(UUID clientUuid);

    /** Los visibles, del más nuevo al más viejo; {@code antesDe} pagina por id (nulo = desde el último). */
    List<MensajeChat> visiblesDeSala(long salaId, Long antesDe, int limite);
}
