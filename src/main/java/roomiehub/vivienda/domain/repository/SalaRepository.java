package roomiehub.vivienda.domain.repository;

import java.util.List;
import java.util.Optional;

import roomiehub.vivienda.domain.model.sala.CodigoInvitacion;
import roomiehub.vivienda.domain.model.sala.EstadoSala;
import roomiehub.vivienda.domain.model.sala.Sala;

public interface SalaRepository {

    // Lo minimo para unirse sin ser miembro todavia sin mostrar el contenido de la sala
    record SalaPorCodigo(long salaId, EstadoSala estado, int miembrosActivos) {}

    Optional<Sala> buscarPorId(long id);

    Optional<SalaPorCodigo> buscarPorCodigo(CodigoInvitacion codigo);

    // Inserta o actualiza y regresa la sala con su id
    Sala guardar(Sala sala);

    // Borra la sala para siempre y las tablas hijas se borran en cascada
    void borrar(long id);

    /* Pone en fila las entradas y salidas de una sala hasta el fin de la transaccion
       para que dos personas no tomen el ultimo lugar ni dejen la sala vacia */
    void bloquearMembresias(long salaId);

    /* Pone en fila las altas de un mismo usuario
       si se toman los dos candados este va primero para que no se esperen al reves */
    void bloquearMembresiasDeUsuario(long usuarioId);

    // Salas que el proceso programado debe revisar sin contexto de usuario ni de sala
    List<Long> conTrabajoPendiente();

    // Regresa false si otra transaccion ya procesa esta sala
    boolean intentarBloquearProcesamiento(long salaId);
}
