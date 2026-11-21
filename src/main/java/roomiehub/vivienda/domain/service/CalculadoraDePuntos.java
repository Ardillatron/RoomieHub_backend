package roomiehub.vivienda.domain.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;

import roomiehub.vivienda.domain.exception.DatoInvalidoException;

public final class CalculadoraDePuntos {

    private CalculadoraDePuntos() {}

    /* Promedio de los miembros actuales mas la tarea mas alta redondeado hacia arriba
       el creador de la sala empieza en 0 y sin tareas la mas alta cuenta como 0 */
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
        return Math.toIntExact(-Math.floorDiv(-numerador, n)); // division redondeada hacia arriba tambien con negativos
    }

    // El balance visible cuenta desde el inicio del trimestre en la zona de la sala
    public static Instant inicioDelTrimestre(Instant ahora, ZoneId zona) {
        LocalDate hoy = ahora.atZone(zona).toLocalDate();
        int primerMes = (hoy.getMonthValue() - 1) / 3 * 3 + 1;
        return LocalDate.of(hoy.getYear(), primerMes, 1).atStartOfDay(zona).toInstant();
    }
}
