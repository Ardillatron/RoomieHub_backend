package roomiehub.vivienda.domain.repository;

import java.time.Instant;
import java.util.Map;

import roomiehub.vivienda.domain.model.tareas.MovimientoPuntos;

public interface PuntosRepository {

    // Puntaje historico de cada usuario con movimientos en la sala y sin movimientos es 0
    Map<Long, Integer> historicos(long salaId);

    // Balance visible solo con COMPLETADA y PENALIZACION desde la fecha dada
    Map<Long, Integer> visibles(long salaId, Instant desde);

    void registrar(MovimientoPuntos movimiento);
}
