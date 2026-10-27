package roomiehub.vivienda.domain.model.convivencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import roomiehub.vivienda.domain.exception.ConflictoDeEstadoException;
import roomiehub.vivienda.domain.exception.DatoInvalidoException;
import roomiehub.vivienda.domain.exception.RecursoNoEncontradoException;

class NotaTest {

    static final Instant AHORA = Instant.parse("2026-11-01T12:00:00Z");

    static Nota nota() {
        return Nota.nueva(1, 2, "  Hay pastel en el refri  ", null, null, AHORA);
    }

    @Test
    void textoObligatorioYConLimite() {
        assertEquals("Hay pastel en el refri", nota().texto());
        assertThrows(DatoInvalidoException.class, () -> Nota.nueva(1, 2, "  ", null, null, AHORA));
        assertThrows(DatoInvalidoException.class, () -> Nota.nueva(1, 2, "x".repeat(2001), null, null, AHORA));
    }

    @Test
    void seFijaHasta30Dias() {
        Nota n = nota();
        n.fijar(AHORA.plus(Duration.ofDays(30)), AHORA);
        assertTrue(n.estaFijada(AHORA));
        assertFalse(n.estaFijada(AHORA.plus(Duration.ofDays(30))), "el plazo vence y deja de contar");
        assertThrows(DatoInvalidoException.class, () -> n.fijar(AHORA.plus(Duration.ofDays(31)), AHORA));
        assertThrows(DatoInvalidoException.class, () -> n.fijar(AHORA.minusSeconds(1), AHORA));
        n.desfijar();
        assertFalse(n.estaFijada(AHORA));
    }

    @Test
    void eliminarLaQuitaDelTablonYDeLasFijadas() {
        Nota n = nota();
        n.fijar(AHORA.plus(Duration.ofDays(7)), AHORA);
        n.eliminar();
        assertNull(n.fijadaHasta());
        assertThrows(RecursoNoEncontradoException.class, n::exigirVisible);
        assertThrows(RecursoNoEncontradoException.class, () -> n.fijar(AHORA.plus(Duration.ofDays(1)), AHORA));
    }

    @Test
    void unReporteTieneMotivoYSeResuelveUnaVez() {
        assertThrows(DatoInvalidoException.class, () -> Reporte.deNota(1, 5, 2, " ", AHORA));
        Reporte r = Reporte.deNota(1, 5, 2, "Ofensivo", AHORA);
        r.resolver();
        assertTrue(r.resuelto());
        assertThrows(ConflictoDeEstadoException.class, r::resolver);
    }
}
