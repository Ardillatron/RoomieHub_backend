package roomiehub.vivienda.domain.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

import roomiehub.vivienda.domain.model.Periodo;

/* Los periodos empiezan a las 00 00 en la zona de la sala
   el periodo k se calcula siempre desde la fecha de inicio
   un inicio el dia 31 cae el 30 en abril y vuelve al 31 en mayo */
public final class CalendarioDePeriodos {

    private final LocalDate fechaInicio;
    private final Period paso;
    private final ZoneId zona;

    public CalendarioDePeriodos(LocalDate fechaInicio, Period paso, ZoneId zona) {
        this.fechaInicio = Objects.requireNonNull(fechaInicio, "fechaInicio");
        this.paso = Objects.requireNonNull(paso, "paso");
        this.zona = Objects.requireNonNull(zona, "zona");
        if (paso.isZero() || paso.isNegative()) {
            throw new IllegalArgumentException("El paso debe ser positivo: " + paso);
        }
    }

    public Periodo periodo(long numero) {
        if (numero < 0) {
            throw new IllegalArgumentException("Número de periodo negativo: " + numero);
        }
        return new Periodo(numero, inicioInstante(numero), inicioInstante(numero + 1));
    }

    // Vacio si el instante es antes de la fecha de inicio
    public Optional<Periodo> periodoQueContiene(Instant instante) {
        LocalDate dia = instante.atZone(zona).toLocalDate();
        if (dia.isBefore(fechaInicio)) {
            return Optional.empty();
        }
        long k = estimacion(dia);
        while (k > 0 && inicioLocal(k).isAfter(dia)) {
            k--;
        }
        while (!inicioLocal(k + 1).isAfter(dia)) {
            k++;
        }
        return Optional.of(periodo(k));
    }

    private long estimacion(LocalDate dia) {
        long meses = paso.toTotalMonths();
        return meses > 0
                ? ChronoUnit.MONTHS.between(fechaInicio, dia) / meses
                : ChronoUnit.DAYS.between(fechaInicio, dia) / paso.getDays();
    }

    private LocalDate inicioLocal(long numero) {
        return fechaInicio.plus(paso.multipliedBy(Math.toIntExact(numero)));
    }

    private Instant inicioInstante(long numero) {
        return inicioLocal(numero).atStartOfDay(zona).toInstant();
    }
}
