package roomiehub.vivienda.domain.model.finanzas;

import java.time.Period;
import java.util.Optional;

// Frecuencias de un gasto y UNICA genera sus deudas una vez en su fecha de inicio
public enum FrecuenciaGasto {
    UNICA(null),
    SEMANAL(Period.ofWeeks(1)),
    MENSUAL(Period.ofMonths(1)),
    BIMESTRAL(Period.ofMonths(2));

    private final Period paso;

    FrecuenciaGasto(Period paso) {
        this.paso = paso;
    }

    // Vacio para UNICA porque no se repite
    public Optional<Period> paso() {
        return Optional.ofNullable(paso);
    }
}
