package roomiehub.vivienda.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;

import roomiehub.vivienda.domain.exception.DeudaCerradaException;
import roomiehub.vivienda.domain.model.cuenta.Usuario;
import roomiehub.vivienda.domain.model.finanzas.Deuda;
import roomiehub.vivienda.domain.model.finanzas.Dinero;
import roomiehub.vivienda.domain.model.finanzas.EstadoDeuda;
import roomiehub.vivienda.domain.model.finanzas.FrecuenciaGasto;
import roomiehub.vivienda.domain.model.finanzas.GastoCompartido;
import roomiehub.vivienda.domain.model.finanzas.Moneda;
import roomiehub.vivienda.domain.model.sala.Membresia;
import roomiehub.vivienda.domain.model.sala.Rol;

/** RN-06, RN-08, RN-22 y RN-37: las piezas de dominio que usa la salida de un miembro. */
class SalidaDominioTest {

    static Membresia miembro(long id, String unidoEn, boolean activo) {
        return new Membresia(id, id * 10, 1, Rol.HABITANTE, activo, Instant.parse(unidoEn));
    }

    @Test
    void elSucesorEsElMiembroActivoMasAntiguo() {
        List<Membresia> candidatos = List.of(
                miembro(1, "2026-03-01T00:00:00Z", true),
                miembro(2, "2026-01-01T00:00:00Z", false), // más antiguo pero inactivo
                miembro(3, "2026-02-01T00:00:00Z", true),
                miembro(4, "2026-02-01T00:00:00Z", true));
        assertEquals(3, SucesionDeAdministrador.sucesor(candidatos).id(), "empate de antigüedad: id menor");
        assertThrows(IllegalStateException.class, () -> SucesionDeAdministrador.sucesor(List.of()));
    }

    @Test
    void laDeudaDeQuienSaleSeRepartePorCentavosEntreLosQueQuedan() {
        Deuda original = new Deuda(50L, 7, 9, LocalDate.of(2026, 11, 1), 1, Dinero.de("33.34", Moneda.MXN),
                null, EstadoDeuda.PENDIENTE, null, java.util.Set.of(), null, 0, null, null);
        List<Deuda> nuevas = CalculadoraDeReparto.repartirDeuda(original, List.of(2L, 3L, 4L), 1);

        assertEquals(EstadoDeuda.REPARTIDA, original.estado());
        // 3334 centavos entre 3 = 1111 y sobra 1; en el periodo 1 lo recibe la posición 1
        assertEquals(List.of("11.11", "11.12", "11.11"),
                nuevas.stream().map(d -> d.monto().monto().toPlainString()).toList());
        assertEquals(List.of(2L, 3L, 4L), nuevas.stream().map(Deuda::deudorId).toList());
        assertTrue(nuevas.stream().allMatch(d -> d.deudaOrigenId() == 50L && d.estado() == EstadoDeuda.PENDIENTE));
        assertThrows(DeudaCerradaException.class, () -> CalculadoraDeReparto.repartirDeuda(original, List.of(2L), 1));
    }

    @Test
    void sinNadieQueQuedeLaDeudaSoloSeCierra() {
        Deuda original = Deuda.nueva(7, 9, LocalDate.of(2026, 11, 1), 1, Dinero.de("10", Moneda.MXN), null);
        assertEquals(List.of(), CalculadoraDeReparto.repartirDeuda(original, List.of(), 0));
        assertEquals(EstadoDeuda.REPARTIDA, original.estado());
    }

    @Test
    void numeroDePeriodoDeUnGasto() {
        ZoneId cdmx = ZoneId.of("America/Mexico_City");
        var mensual = GastoCompartido.nuevo(1, "Internet", Dinero.de("600", Moneda.MXN), FrecuenciaGasto.MENSUAL,
                LocalDate.of(2026, 1, 31));
        assertEquals(3, mensual.numeroDePeriodo(LocalDate.of(2026, 4, 30), cdmx));
        var unica = GastoCompartido.nuevo(1, "Pizza", Dinero.de("300", Moneda.MXN), FrecuenciaGasto.UNICA,
                LocalDate.of(2026, 5, 2));
        assertEquals(0, unica.numeroDePeriodo(LocalDate.of(2026, 5, 2), cdmx));
    }

    @Test
    void anonimizarBorraLosDatosPersonales() {
        Usuario u = new Usuario(5L, roomiehub.vivienda.domain.model.cuenta.ProveedorAuth.GOOGLE, "ana@mail.com", "hash",
                "sub", "Ana", "foto", null);
        Instant ahora = Instant.parse("2026-11-01T00:00:00Z");
        u.anonimizar(ahora);
        assertEquals("Usuario eliminado", u.nombre());
        assertNull(u.email());
        assertNull(u.passwordHash());
        assertNull(u.googleSub());
        assertNull(u.fotoUrl());
        assertEquals(ahora, u.eliminadoEn());
        assertEquals(5L, u.id(), "el id se conserva para el historial");
    }
}
