package roomiehub.vivienda.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import roomiehub.vivienda.domain.model.convivencia.MensajeChat;

public interface MensajeRepository {

    // Inserta o actualiza y regresa el mensaje con su id
    MensajeChat guardar(MensajeChat mensaje);

    Optional<MensajeChat> buscarPorId(long id);

    // El mensaje que ya creo esta misma accion offline si existe
    Optional<MensajeChat> buscarPorClientUuid(UUID clientUuid);

    // Los visibles del mas nuevo al mas viejo y antesDe pagina por id
    List<MensajeChat> visiblesDeSala(long salaId, Long antesDe, int limite);
}
