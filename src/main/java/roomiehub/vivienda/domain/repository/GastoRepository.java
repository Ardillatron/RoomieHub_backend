package roomiehub.vivienda.domain.repository;

import java.util.List;
import java.util.Optional;

import roomiehub.vivienda.domain.model.finanzas.GastoCompartido;

public interface GastoRepository {

    // Tambien regresa gastos borrados y quien llama decide
    Optional<GastoCompartido> buscarPorId(long id);

    List<GastoCompartido> vigentesDeSala(long salaId);

    // Gastos vigentes de la sala donde el usuario participa
    List<GastoCompartido> conParticipanteActivo(long salaId, long usuarioId);

    // Ids de los participantes activos en el orden fijo del gasto
    List<Long> participantesActivos(long gastoId);

    /* Deja activos solo estos usuarios
       quien ya estuvo conserva su lugar y quien entra por primera vez va al final */
    void definirParticipantes(long gastoId, long salaId, List<Long> usuarios);

    void desactivarParticipante(long gastoId, long usuarioId);

    // Inserta o actualiza y regresa el gasto con su id
    GastoCompartido guardar(GastoCompartido gasto);
}
