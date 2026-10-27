package roomiehub.vivienda.domain.model.tareas;

import java.util.Objects;

/** Sección 11: los puntos son un historial; el puntaje histórico es la suma de todos los movimientos. */
public record MovimientoPuntos(long usuarioId, long salaId, Long asignacionId, int puntos, TipoMovimiento tipo) {

    public MovimientoPuntos {
        Objects.requireNonNull(tipo, "tipo");
    }
}
