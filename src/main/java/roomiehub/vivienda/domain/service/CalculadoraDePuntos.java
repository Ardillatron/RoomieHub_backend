package roomiehub.vivienda.domain.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;

import roomiehub.vivienda.domain.exception.DatoInvalidoException;

public final class CalculadoraDePuntos {

    private CalculadoraDePuntos() {}

    /**
     * RN-18: promedio de los miembros actuales más la tarea más alta, redondeado hacia arriba.
     * El creador de la sala (sin miembros) inicia en 0; sin tareas, la más alta cuenta como 0.
     */
    public static int puntajeInicial(Collection<Integer> puntajesActuales, int valorTareaMasAlta) {
        if (valorTareaMasAlta < 0 || valorTareaMasAlta > 10) {
            throw new DatoInvalidoException("Valor de tarea fuera de rango: " + valorTareaMasAlta);
        }
        if (puntajesActuales.isEmpty()) {
            return 0;
        }
        long n = puntajesActuales.size();
        long numerador = puntajesActuales.stream().mapToLong(Integer::longValue).sum()
                + valorTareaMasAlta * n;
        return Math.toIntExact(-Math.floorDiv(-numerador, n)); // ⌈numerador / n⌉, también con negativos
    }

    /** RN-17: el balance visible cuenta desde el inicio del trimestre (ene, abr, jul, oct) en la zona de la sala. */
    public static Instant inicioDelTrimestre(Instant ahora, ZoneId zona) {
        LocalDate hoy = ahora.atZone(zona).toLocalDate();
        int primerMes = (hoy.getMonthValue() - 1) / 3 * 3 + 1;
        return LocalDate.of(hoy.getYear(), primerMes, 1).atStartOfDay(zona).toInstant();
    }
}
