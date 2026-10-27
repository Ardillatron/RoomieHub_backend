package roomiehub.vivienda.domain.repository;

import java.util.List;
import java.util.Optional;

import roomiehub.vivienda.domain.model.sala.CodigoInvitacion;
import roomiehub.vivienda.domain.model.sala.EstadoSala;
import roomiehub.vivienda.domain.model.sala.Sala;

public interface SalaRepository {

    /** Lo mínimo para unirse sin ser miembro todavía (CU-03); no expone el contenido de la sala. */
    record SalaPorCodigo(long salaId, EstadoSala estado, int miembrosActivos) {}

    Optional<Sala> buscarPorId(long id);

    Optional<SalaPorCodigo> buscarPorCodigo(CodigoInvitacion codigo);

    /** Inserta o actualiza; devuelve la sala con su id. */
    Sala guardar(Sala sala);

    /** RN-10: borrado definitivo; las tablas hijas se borran en cascada. */
    void borrar(long id);

    /**
     * Serializa, hasta el fin de la transacción, las entradas y salidas de una sala, para que dos
     * personas no tomen el último lugar (RN-01) ni dejen la sala vacía sin borrarla (RN-10).
     */
    void bloquearMembresias(long salaId);

    /**
     * Serializa las altas de un mismo usuario (RN-02). Si se toman los dos candados, este va primero,
     * para que dos transacciones nunca se esperen en orden inverso.
     */
    void bloquearMembresiasDeUsuario(long usuarioId);

    /** Salas que el proceso programado debe revisar; funciona sin contexto de usuario ni de sala. */
    List<Long> conTrabajoPendiente();

    /** @return false si otra transacción ya procesa esta sala (el cron y el disparador externo coinciden). */
    boolean intentarBloquearProcesamiento(long salaId);
}
