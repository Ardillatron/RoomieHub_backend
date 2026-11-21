package roomiehub.vivienda.domain.model.finanzas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import roomiehub.vivienda.domain.exception.DatoInvalidoException;

// Monto con centavos exactos sin double sin decimales de mas ni negativos
public record Dinero(BigDecimal monto, Moneda moneda) {

    private static final BigDecimal MAXIMO = new BigDecimal("9999999999.99"); // NUMERIC 12 2

    public Dinero {
        Objects.requireNonNull(monto, "monto");
        Objects.requireNonNull(moneda, "moneda");
        if (monto.signum() < 0) {
            throw new DatoInvalidoException("Un monto no puede ser negativo: " + monto);
        }
        try {
            monto = monto.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new DatoInvalidoException("Un monto tiene a lo más 2 decimales: " + monto);
        }
        if (monto.compareTo(MAXIMO) > 0) {
            throw new DatoInvalidoException("El monto excede el máximo permitido: " + monto);
        }
    }

    public static Dinero de(String monto, Moneda moneda) {
        try {
            return new Dinero(new BigDecimal(monto), moneda);
        } catch (NumberFormatException e) {
            throw new DatoInvalidoException("Monto inválido: " + monto);
        }
    }

    public static Dinero deCentavos(long centavos, Moneda moneda) {
        return new Dinero(BigDecimal.valueOf(centavos, 2), moneda);
    }

    public long centavos() {
        return monto.movePointRight(2).longValueExact();
    }
}
