package roomiehub.vivienda.domain.model.tareas;

import java.util.Objects;

// Los puntos son un historial y el puntaje historico es la suma de todos los movimientos
public record MovimientoPuntos(long usuarioId, long salaId, Long asignacionId, int puntos, TipoMovimiento tipo) {

    public MovimientoPuntos {
        Objects.requireNonNull(tipo, "tipo");
    }
}
