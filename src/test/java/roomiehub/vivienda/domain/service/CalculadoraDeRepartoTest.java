package roomiehub.vivienda.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import roomiehub.vivienda.domain.exception.DatoInvalidoException;
import roomiehub.vivienda.domain.model.finanzas.Dinero;
import roomiehub.vivienda.domain.model.finanzas.Moneda;

class CalculadoraDeRepartoTest {

    static List<String> montos(String total, int n, long periodo) {
        return CalculadoraDeReparto.repartir(Dinero.de(total, Moneda.MXN), n, periodo).stream()
                .map(d -> d.monto().toPlainString())
                .toList();
    }

    @Test
    void ejemploDelDocumentoRotaElCentavo() {
        assertEquals(List.of("33.34", "33.33", "33.33"), montos("100", 3, 0)); // noviembre
        assertEquals(List.of("33.33", "33.34", "33.33"), montos("100", 3, 1)); // diciembre
        assertEquals(List.of("33.33", "33.33", "33.34"), montos("100", 3, 2));
        assertEquals(List.of("33.34", "33.33", "33.33"), montos("100", 3, 3));
    }

    @Test
    void internetEntreCuatro() {
        assertEquals(List.of("150.00", "150.00", "150.00", "150.00"), montos("600", 4, 0));
    }

    @Test
    void variosCentavosSobrantesEnVueltaCircular() {
        // 0.05 entre 3: sobran 2 centavos; en el periodo 2 los reciben las posiciones 2 y 0.
        assertEquals(List.of("0.02", "0.01", "0.02"), montos("0.05", 3, 2));
    }

    @Test
    void laSumaSiempreCuadra() {
        for (long centavos = 0; centavos < 500; centavos += 7) {
            for (int n = 1; n <= 6; n++) {
                for (long periodo = 0; periodo < 8; periodo++) {
                    Dinero total = Dinero.deCentavos(centavos, Moneda.USD);
                    List<Dinero> partes = CalculadoraDeReparto.repartir(total, n, periodo);
                    assertEquals(centavos, partes.stream().mapToLong(Dinero::centavos).sum());
                    long max = partes.stream().mapToLong(Dinero::centavos).max().orElseThrow();
                    long min = partes.stream().mapToLong(Dinero::centavos).min().orElseThrow();
                    assertEquals(true, max - min <= 1, "equitativo");
                }
            }
        }
    }

    @Test
    void sinParticipantesEsInvalido() {
        assertThrows(DatoInvalidoException.class, () -> montos("10", 0, 0));
    }
}
