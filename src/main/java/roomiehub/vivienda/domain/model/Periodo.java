package roomiehub.vivienda.domain.model;

import java.time.Instant;

/**
 * RN-40: periodo número {@code numero} (0 = el que empieza en la fecha de inicio),
 * de {@code inicio} incluido a {@code fin} excluido, en UTC.
 */
public record Periodo(long numero, Instant inicio, Instant fin) {

    public boolean contiene(Instant instante) {
        return !instante.isBefore(inicio) && instante.isBefore(fin);
    }
}
