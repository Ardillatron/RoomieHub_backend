package roomiehub.vivienda.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import roomiehub.vivienda.domain.model.Periodo;
import roomiehub.vivienda.domain.model.finanzas.FrecuenciaGasto;
import roomiehub.vivienda.domain.model.tareas.Frecuencia;

class CalendarioDePeriodosTest {

    static final ZoneId CDMX = ZoneId.of("America/Mexico_City");

    static Instant local(String fechaHora, ZoneId zona) {
        return ZonedDateTime.of(java.time.LocalDateTime.parse(fechaHora), zona).toInstant();
    }

    @Test
    void mensualDesdeEl31UsaElUltimoDiaYVuelveAl31() {
        var cal = new CalendarioDePeriodos(LocalDate.of(2026, 1, 31), Frecuencia.MENSUAL.paso(), CDMX);
        assertEquals(local("2026-04-30T00:00", CDMX), cal.periodo(3).inicio());
        assertEquals(local("2026-05-31T00:00", CDMX), cal.periodo(3).fin());
        assertEquals(local("2026-02-28T00:00", CDMX), cal.periodo(1).inicio());
    }

    @Test
    void elPeriodoIncluyeSuInicioYExcluyeSuFin() {
        var cal = new CalendarioDePeriodos(LocalDate.of(2026, 10, 5), Frecuencia.SEMANAL.paso(), CDMX);
        Periodo p = cal.periodoQueContiene(local("2026-10-12T00:00", CDMX)).orElseThrow();
        assertEquals(1, p.numero());
        assertEquals(0, cal.periodoQueContiene(local("2026-10-11T23:59:59", CDMX)).orElseThrow().numero());
        assertTrue(p.contiene(p.inicio()));
        assertTrue(!p.contiene(p.fin()));
    }

    @Test
    void antesDeLaFechaDeInicioNoHayPeriodo() {
        var cal = new CalendarioDePeriodos(LocalDate.of(2026, 10, 5), Frecuencia.DIARIA.paso(), CDMX);
        assertTrue(cal.periodoQueContiene(local("2026-10-04T23:00", CDMX)).isEmpty());
    }

    @Test
    void usaLaZonaDeLaSalaNoUtc() {
        // 6 de octubre a las 03 00 UTC sigue siendo 5 de octubre en CDMX
        var cal = new CalendarioDePeriodos(LocalDate.of(2026, 10, 1), Frecuencia.DIARIA.paso(), CDMX);
        assertEquals(4, cal.periodoQueContiene(Instant.parse("2026-10-06T03:00:00Z")).orElseThrow().numero());
    }

    @Test
    void bimestralYPeriodoLejano() {
        var cal = new CalendarioDePeriodos(LocalDate.of(2026, 1, 15),
                FrecuenciaGasto.BIMESTRAL.paso().orElseThrow(), CDMX);
        Periodo p = cal.periodoQueContiene(local("2028-06-01T12:00", CDMX)).orElseThrow();
        assertEquals(14, p.numero()); // 28 meses despues

        assertEquals(local("2028-05-15T00:00", CDMX), p.inicio());
    }

    @Test
    void cambioDeHorarioNoRompeLosLimites() {
        ZoneId nuevaYork = ZoneId.of("America/New_York"); // el 8 de marzo de 2026 se adelanta una hora
        var cal = new CalendarioDePeriodos(LocalDate.of(2026, 3, 1), Frecuencia.DIARIA.paso(), nuevaYork);
        Periodo p = cal.periodo(7);
        assertEquals(local("2026-03-08T00:00", nuevaYork), p.inicio());
        assertEquals(23 * 3600, p.fin().getEpochSecond() - p.inicio().getEpochSecond());
    }

    @Test
    void unicaNoTienePaso() {
        assertTrue(FrecuenciaGasto.UNICA.paso().isEmpty());
    }
}
