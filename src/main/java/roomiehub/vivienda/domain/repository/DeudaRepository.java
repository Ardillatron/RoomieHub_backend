package roomiehub.vivienda.domain.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import roomiehub.vivienda.domain.model.finanzas.Deuda;
import roomiehub.vivienda.domain.model.finanzas.EstadoDeuda;

public interface DeudaRepository {

    /** Con sus confirmaciones. */
    Optional<Deuda> buscarPorId(long id);

    /** Las deudas pendientes del gasto, con sus confirmaciones. */
    List<Deuda> pendientesDeGasto(long gastoId);

    /** Las de la sala en ese estado, del periodo más reciente al más viejo, con sus confirmaciones. */
    List<Deuda> deSala(long salaId, EstadoDeuda estado);

    /** CU-25: si el periodo ya generó sus deudas normales (las de reparto no cuentan). */
    boolean existenDelPeriodo(long gastoId, LocalDate periodo);

    /**
     * Inserta una deuda nueva o actualiza una existente (estado, aviso, recordatorios) e inserta sus
     * confirmaciones nuevas. Se escribe de inmediato.
     */
    void guardar(Deuda deuda);
}
