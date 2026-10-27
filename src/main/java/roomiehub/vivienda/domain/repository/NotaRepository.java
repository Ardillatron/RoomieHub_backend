package roomiehub.vivienda.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import roomiehub.vivienda.domain.model.convivencia.Nota;

public interface NotaRepository {

    /** Inserta o actualiza; devuelve la nota con su id. */
    Nota guardar(Nota nota);

    Optional<Nota> buscarPorId(long id);

    /** RN-35: la nota que ya creó esta misma acción offline, si existe. */
    Optional<Nota> buscarPorClientUuid(UUID clientUuid);

    /**
     * Las visibles que no están fijadas, de la más nueva a la más vieja; {@code antesDe} pagina por id
     * (nulo = desde la última).
     */
    List<Nota> noFijadasDeSala(long salaId, Instant ahora, Long antesDe, int limite);

    /** RN-31: las fijadas vigentes, la que vence primero al final. */
    List<Nota> fijadasDeSala(long salaId, Instant ahora);

    /** CU-30: quita el fijado a las que ya vencieron. */
    void desfijarVencidas(long salaId, Instant ahora);
}
