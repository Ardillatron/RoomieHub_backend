package roomiehub.vivienda.domain.repository;

import java.util.List;
import java.util.Optional;

import roomiehub.vivienda.domain.model.finanzas.GastoCompartido;

public interface GastoRepository {

    /** También devuelve gastos eliminados: quien llama decide (sección 8, recurso eliminado). */
    Optional<GastoCompartido> buscarPorId(long id);

    List<GastoCompartido> vigentesDeSala(long salaId);

    /** Gastos vigentes de la sala en los que el usuario es participante activo. */
    List<GastoCompartido> conParticipanteActivo(long salaId, long usuarioId);

    /** Ids de los participantes activos, en el orden fijo del gasto (RN-22). */
    List<Long> participantesActivos(long gastoId);

    /**
     * Deja activos exactamente estos usuarios. Quien ya estuvo conserva su lugar en el orden fijo;
     * quien entra por primera vez va al final (RN-22).
     */
    void definirParticipantes(long gastoId, long salaId, List<Long> usuarios);

    void desactivarParticipante(long gastoId, long usuarioId);

    /** Inserta o actualiza; devuelve el gasto con su id. */
    GastoCompartido guardar(GastoCompartido gasto);
}
