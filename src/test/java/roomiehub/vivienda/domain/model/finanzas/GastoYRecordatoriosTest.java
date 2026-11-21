package roomiehub.vivienda.domain.model.finanzas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import roomiehub.vivienda.domain.exception.ConflictoDeEstadoException;
import roomiehub.vivienda.domain.exception.DatoInvalidoException;
import roomiehub.vivienda.domain.exception.RecursoNoEncontradoException;
import roomiehub.vivienda.domain.model.Periodo;

class GastoYRecordatoriosTest {

    static final ZoneId CDMX = ZoneId.of("America/Mexico_City");
    static final Instant MEDIADOS_DE_NOVIEMBRE = Instant.parse("2026-11-15T18:00:00Z");

    static GastoCompartido internet(LocalDate inicio) {
        return GastoCompartido.nuevo(1, "Internet", Dinero.de("600", Moneda.MXN), FrecuenciaGasto.MENSUAL, inicio);
    }

    static List<LocalDate> dias(List<Periodo> periodos) {
        return periodos.stream().map(p -> LocalDate.ofInstant(p.inicio(), CDMX)).toList();
    }

    @Test
    void generaTodosLosPeriodosIniciadosIncluidosLosAtrasados() {
        List<Periodo> periodos = internet(LocalDate.of(2026, 9, 1)).periodosIniciados(MEDIADOS_DE_NOVIEMBRE, CDMX);
        assertEquals(List.of(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1)),
                dias(periodos));
        assertEquals(List.of(0L, 1L, 2L), periodos.stream().map(Periodo::numero).toList());
        assertTrue(internet(LocalDate.of(2026, 12, 1)).periodosIniciados(MEDIADOS_DE_NOVIEMBRE, CDMX).isEmpty());
    }

    @Test
    void avanzarLaGeneracionSaltaLoAnterior() {
        GastoCompartido g = internet(LocalDate.of(2026, 9, 1));
        g.avanzarGeneracionHasta(LocalDate.of(2026, 10, 20)); // reactivada el 20 de octubre
        assertEquals(List.of(LocalDate.of(2026, 11, 1)), dias(g.periodosIniciados(MEDIADOS_DE_NOVIEMBRE, CDMX)),
                "octubre empezó con la sala desactivada: no se cobra");
        g.avanzarGeneracionHasta(LocalDate.of(2026, 1, 1));
        assertEquals(LocalDate.of(2026, 10, 20), g.generarDesde(), "nunca retrocede");
    }

    @Test
    void unGastoUnicoSoloTieneUnPeriodo() {
        var pizza = GastoCompartido.nuevo(1, "Pizza", Dinero.de("300", Moneda.MXN), FrecuenciaGasto.UNICA,
                LocalDate.of(2026, 11, 10));
        assertEquals(List.of(LocalDate.of(2026, 11, 10)), dias(pizza.periodosIniciados(MEDIADOS_DE_NOVIEMBRE, CDMX)));
        assertTrue(pizza.periodosIniciados(Instant.parse("2026-11-09T12:00:00Z"), CDMX).isEmpty());
    }

    @Test
    void editarNoCambiaLaMonedaYLaFrecuenciaNuevaEmpiezaAlCerrarElPeriodo() {
        GastoCompartido g = internet(LocalDate.of(2026, 9, 1));
        assertThrows(DatoInvalidoException.class, () -> g.editar("Internet", Dinero.de("600", Moneda.USD),
                FrecuenciaGasto.MENSUAL, MEDIADOS_DE_NOVIEMBRE, CDMX));
        assertThrows(DatoInvalidoException.class, () -> g.editar("Internet", Dinero.de("0", Moneda.MXN),
                FrecuenciaGasto.MENSUAL, MEDIADOS_DE_NOVIEMBRE, CDMX));

        g.editar("Internet fibra", Dinero.de("700", Moneda.MXN), FrecuenciaGasto.SEMANAL, MEDIADOS_DE_NOVIEMBRE, CDMX);
        assertEquals(LocalDate.of(2026, 12, 1), g.fechaInicio(), "cuando cierra el mes en curso");
        assertEquals(LocalDate.of(2026, 12, 1), g.generarDesde());
        assertTrue(g.periodosIniciados(MEDIADOS_DE_NOVIEMBRE, CDMX).isEmpty());
        assertThrows(ConflictoDeEstadoException.class, () -> g.editar("Internet", Dinero.de("700", Moneda.MXN),
                FrecuenciaGasto.UNICA, MEDIADOS_DE_NOVIEMBRE, CDMX));
    }

    @Test
    void unGastoEliminadoYaNoAdmiteAcciones() {
        GastoCompartido g = internet(LocalDate.of(2026, 9, 1));
        g.exigirVigente();
        g.eliminar(5, MEDIADOS_DE_NOVIEMBRE);
        assertThrows(RecursoNoEncontradoException.class, g::exigirVigente);
    }

    // Recordatorios

    static final Instant CREADA = Instant.parse("2026-11-01T06:00:00Z");

    static Deuda pendienteCreadaEl1() {
        return new Deuda(1L, 7, 9, LocalDate.of(2026, 11, 1), 1, Dinero.de("150", Moneda.MXN), null,
                EstadoDeuda.PENDIENTE, null, Set.of(), CREADA, 0, null, null);
    }

    @Test
    void alDeudorALos3DiasLuegoCada7YMaximo3() {
        Deuda d = pendienteCreadaEl1();
        assertFalse(d.tocaRecordarAlDeudor(CREADA.plus(Duration.ofDays(2))));
        assertTrue(d.tocaRecordarAlDeudor(CREADA.plus(Duration.ofDays(3))));
        assertFalse(d.tocaRecordarAlDeudor(CREADA.plus(Duration.ofDays(9))), "todavía no pasan 7 días");
        assertTrue(d.tocaRecordarAlDeudor(CREADA.plus(Duration.ofDays(10))));
        assertTrue(d.tocaRecordarAlDeudor(CREADA.plus(Duration.ofDays(17))));
        assertFalse(d.tocaRecordarAlDeudor(CREADA.plus(Duration.ofDays(60))), "máximo 3 por periodo");
        assertEquals(3, d.recordatoriosEnviados());
    }

    @Test
    void siYaAvisoNoSeLeRecuerdaYAQuienFaltaSeLeRecuerdaUnaVezALas24Horas() {
        Deuda d = pendienteCreadaEl1();
        Instant aviso = CREADA.plus(Duration.ofDays(1));
        d.avisarPago(aviso, Set.of(1L, 2L, 3L));
        d.confirmar(2, Set.of(1L, 2L, 3L));
        assertFalse(d.tocaRecordarAlDeudor(CREADA.plus(Duration.ofDays(5))));

        assertEquals(Set.of(), d.recordarConfirmaciones(aviso.plus(Duration.ofHours(23)), Set.of(1L, 2L, 3L)));
        assertEquals(Set.of(3L), d.recordarConfirmaciones(aviso.plus(Duration.ofHours(24)), Set.of(1L, 2L, 3L)),
                "solo a quien falta, no a quien ya confirmó");
        assertEquals(Set.of(), d.recordarConfirmaciones(aviso.plus(Duration.ofDays(5)), Set.of(1L, 2L, 3L)),
                "una sola vez");
    }
}
