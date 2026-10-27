package roomiehub.vivienda.domain.model.tareas;

import java.time.Period;

/** RN-39: frecuencias de una tarea. */
public enum Frecuencia {
    DIARIA(Period.ofDays(1)),
    SEMANAL(Period.ofWeeks(1)),
    MENSUAL(Period.ofMonths(1));

    private final Period paso;

    Frecuencia(Period paso) {
        this.paso = paso;
    }

    public Period paso() {
        return paso;
    }
}
