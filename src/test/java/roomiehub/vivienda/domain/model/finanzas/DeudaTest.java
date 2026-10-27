package roomiehub.vivienda.domain.model.finanzas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

import roomiehub.vivienda.domain.exception.DatoInvalidoException;
import roomiehub.vivienda.domain.exception.DeudaCerradaException;
import roomiehub.vivienda.domain.exception.OperacionNoPermitidaException;

class DeudaTest {

    static final long ANA = 1, BETO = 2, CARLA = 3, DIEGO = 4;
    static final Set<Long> TODOS = Set.of(ANA, BETO, CARLA, DIEGO);
    static final Instant AHORA = Instant.parse("2026-11-03T18:00:00Z");

    static Deuda deudaDeAna() {
        return Deuda.nueva(7, 9, LocalDate.of(2026, 11, 1), ANA, Dinero.de("150", Moneda.MXN), null);
    }

    @Test
    void ejemploDelDocumentoSeApruebaConLasTresConfirmaciones() {
        Deuda d = deudaDeAna();
        assertFalse(d.avisarPago(AHORA, TODOS), "avisar no cambia el estado");
        assertFalse(d.confirmar(BETO, TODOS));
        assertFalse(d.confirmar(CARLA, TODOS));
        assertTrue(d.confirmar(DIEGO, TODOS));
        assertEquals(EstadoDeuda.APROBADA, d.estado());
    }

    @Test
    void lasConfirmacionesCuentanAunqueElDeudorNoHayaAvisado() {
        Deuda d = deudaDeAna();
        d.confirmar(DIEGO, TODOS);
        d.confirmar(BETO, TODOS);
        assertTrue(d.confirmar(CARLA, TODOS));
        assertEquals(null, d.avisoPagoEn());
    }

    @Test
    void sinOtrosParticipantesSeApruebaAlAvisar() {
        Deuda d = deudaDeAna();
        assertFalse(d.reevaluar(Set.of(ANA)), "sin aviso sigue pendiente");
        assertTrue(d.avisarPago(AHORA, Set.of(ANA)));
    }

    @Test
    void siAlguienSaleYYaReuneLasConfirmacionesSeAprueba() {
        Deuda d = deudaDeAna();
        d.confirmar(BETO, TODOS);
        d.confirmar(CARLA, TODOS);
        assertTrue(d.reevaluar(Set.of(ANA, BETO, CARLA)), "Diego salió");
    }

    @Test
    void elDeudorNoSeConfirmaASiMismoNiConfirmaUnAjeno() {
        Deuda d = deudaDeAna();
        assertThrows(OperacionNoPermitidaException.class, () -> d.confirmar(ANA, TODOS));
        assertThrows(OperacionNoPermitidaException.class, () -> d.confirmar(99, TODOS));
    }

    @Test
    void unaDeudaCerradaNoCambia() {
        Deuda aprobada = deudaDeAna();
        aprobada.avisarPago(AHORA, Set.of(ANA));
        assertThrows(DeudaCerradaException.class, () -> aprobada.confirmar(BETO, TODOS));
        assertThrows(DeudaCerradaException.class, aprobada::marcarRepartida);

        Deuda repartida = deudaDeAna();
        repartida.marcarRepartida();
        assertEquals(EstadoDeuda.REPARTIDA, repartida.estado());
        assertThrows(DeudaCerradaException.class, () -> repartida.avisarPago(AHORA, TODOS));
        assertFalse(repartida.reevaluar(Set.of(ANA)));
    }

    @Test
    void elSegundoAvisoConservaLaHoraDelPrimero() {
        Deuda d = deudaDeAna();
        d.avisarPago(AHORA, TODOS);
        d.avisarPago(AHORA.plusSeconds(60), TODOS);
        assertEquals(AHORA, d.avisoPagoEn());
    }

    @Test
    void dineroRechazaDecimalesDeMasYNegativos() {
        assertEquals("33.30", Dinero.de("33.3", Moneda.MXN).monto().toPlainString());
        assertThrows(DatoInvalidoException.class, () -> Dinero.de("33.333", Moneda.MXN));
        assertThrows(DatoInvalidoException.class, () -> Dinero.de("-1", Moneda.MXN));
        assertThrows(DatoInvalidoException.class, () -> Dinero.de("abc", Moneda.MXN));
        assertThrows(DatoInvalidoException.class, () -> Dinero.de("10000000000.00", Moneda.MXN));
    }
}
