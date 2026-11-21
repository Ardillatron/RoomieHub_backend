package roomiehub.vivienda.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import roomiehub.vivienda.domain.model.convivencia.Nota;

public interface NotaRepository {

    // Inserta o actualiza y regresa la nota con su id
    Nota guardar(Nota nota);

    Optional<Nota> buscarPorId(long id);

    // La nota que ya creo esta misma accion offline si existe
    Optional<Nota> buscarPorClientUuid(UUID clientUuid);

    /* Las visibles no fijadas de la mas nueva a la mas vieja
       antesDe pagina por id y null empieza desde la ultima */
    List<Nota> noFijadasDeSala(long salaId, Instant ahora, Long antesDe, int limite);

    // Las fijadas vigentes y la que vence primero va al final
    List<Nota> fijadasDeSala(long salaId, Instant ahora);

    // CU-30 quita la fijacion a las que ya vencieron
    void desfijarVencidas(long salaId, Instant ahora);
}
