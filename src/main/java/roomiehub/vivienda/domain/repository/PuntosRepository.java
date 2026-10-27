package roomiehub.vivienda.domain.repository;

import java.time.Instant;
import java.util.Map;

import roomiehub.vivienda.domain.model.tareas.MovimientoPuntos;

public interface PuntosRepository {

    /** Puntaje histórico (RN-17) de cada usuario con movimientos en la sala; sin movimientos = 0. */
    Map<Long, Integer> historicos(long salaId);

    /** RN-17: balance visible, solo COMPLETADA y PENALIZACION desde {@code desde}. */
    Map<Long, Integer> visibles(long salaId, Instant desde);

    void registrar(MovimientoPuntos movimiento);
}
