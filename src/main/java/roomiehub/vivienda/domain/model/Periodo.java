package roomiehub.vivienda.domain.model;

import java.time.Instant;

/* Periodo numero numero donde 0 es el que empieza en la fecha de inicio
   va de inicio incluido a fin excluido en UTC */
public record Periodo(long numero, Instant inicio, Instant fin) {

    public boolean contiene(Instant instante) {
        return !instante.isBefore(inicio) && instante.isBefore(fin);
    }
}
