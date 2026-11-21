package roomiehub.vivienda.domain.model.tareas;

import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.Objects;

import roomiehub.vivienda.domain.exception.DatoInvalidoException;

/* Una tarea se repite cada N dias de 1 a 365 o cada mes
   se escribe como periodo ISO 8601 como P3D o P1M */
public record Frecuencia(Period paso) {

    public static final int MAXIMO_DIAS = 365;

    public static final Frecuencia DIARIA = cadaDias(1);
    public static final Frecuencia SEMANAL = cadaDias(7);
    public static final Frecuencia MENSUAL = new Frecuencia(Period.ofMonths(1));

    public Frecuencia {
        Objects.requireNonNull(paso, "paso");
        boolean enDias = paso.getYears() == 0 && paso.getMonths() == 0
                && paso.getDays() >= 1 && paso.getDays() <= MAXIMO_DIAS;
        if (!enDias && !paso.equals(Period.ofMonths(1))) {
            throw new DatoInvalidoException("Frecuencia inválida: " + paso + " (cada 1 a " + MAXIMO_DIAS
                    + " días, o cada mes)");
        }
    }

    public static Frecuencia cadaDias(int dias) {
        return new Frecuencia(Period.ofDays(dias));
    }

    // P1W se guarda como P7D para que la misma frecuencia sea igual
    public static Frecuencia de(String iso) {
        try {
            return new Frecuencia(Period.parse(Objects.requireNonNull(iso, "frecuencia")));
        } catch (DateTimeParseException e) {
            throw new DatoInvalidoException("Frecuencia inválida: " + iso);
        }
    }

    @Override
    public String toString() {
        return paso.toString();
    }
}
