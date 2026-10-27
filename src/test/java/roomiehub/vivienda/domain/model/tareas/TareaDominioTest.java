package roomiehub.vivienda.domain.model.tareas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import roomiehub.vivienda.domain.exception.ConflictoDeEstadoException;
import roomiehub.vivienda.domain.exception.DatoInvalidoException;
import roomiehub.vivienda.domain.exception.OperacionNoPermitidaException;
import roomiehub.vivienda.domain.model.Periodo;
import roomiehub.vivienda.domain.service.CalculadoraDePuntos;

class TareaDominioTest {

    static final ZoneId CDMX = ZoneId.of("America/Mexico_City");
    static final Instant INICIO = Instant.parse("2026-11-02T06:00:00Z");
    static final Instant FIN = INICIO.plus(Duration.ofDays(7));
    static final long ANA = 1, BETO = 2;

    static Tarea banos() {
        return new Tarea(10L, 5, "Lavar baños", new ValorTarea(8), Frecuencia.SEMANAL, LocalDate.of(2026, 11, 2), true);
    }

    static AsignacionTarea asignadaA(Long usuario) {
        return AsignacionTarea.nueva(banos(), new Periodo(0, INICIO, FIN), usuario);
    }

    @Test
    void completarSumaElValorQueTeniaAlAsignarse() {
        AsignacionTarea a = asignadaA(ANA);
        MovimientoPuntos m = a.completar(ANA, FIN.minusSeconds(1));
        assertEquals(8, m.puntos());
        assertEquals(TipoMovimiento.COMPLETADA, m.tipo());
        assertEquals(EstadoAsignacion.COMPLETADA, a.estado());
        assertThrows(ConflictoDeEstadoException.class, () -> a.completar(ANA, FIN), "no se marca dos veces");
    }

    @Test
    void soloElResponsableYDentroDeLas3HorasDeGracia() {
        assertThrows(OperacionNoPermitidaException.class, () -> asignadaA(ANA).completar(BETO, INICIO));
        assertThrows(OperacionNoPermitidaException.class, () -> asignadaA(null).completar(ANA, INICIO));
        asignadaA(ANA).completar(ANA, FIN.plus(Duration.ofHours(3)).minusSeconds(1));
        assertThrows(ConflictoDeEstadoException.class,
                () -> asignadaA(ANA).completar(ANA, FIN.plus(Duration.ofHours(3))));
    }

    @Test
    void vencerPenalizaLaMitadHaciaArriba() {
        AsignacionTarea a = asignadaA(ANA);
        assertThrows(ConflictoDeEstadoException.class, () -> a.vencer(FIN.plus(Duration.ofHours(2))), "aún en gracia");
        MovimientoPuntos m = a.vencer(FIN.plus(Duration.ofHours(3))).orElseThrow();
        assertEquals(-4, m.puntos());
        assertEquals(TipoMovimiento.PENALIZACION, m.tipo());
        assertEquals(EstadoAsignacion.VENCIDA, a.estado());
    }

    @Test
    void sinResponsableVenceSinPenalizar() {
        assertTrue(asignadaA(null).vencer(FIN.plus(Duration.ofDays(1))).isEmpty());
    }

    @Test
    void reasignarSoloLasQueNoTienenResponsable() {
        AsignacionTarea sinResponsable = asignadaA(null);
        sinResponsable.asignarA(BETO);
        assertEquals(BETO, sinResponsable.usuarioId());
        assertThrows(ConflictoDeEstadoException.class, () -> asignadaA(ANA).asignarA(BETO));
    }

    @Test
    void editarCambiaElValorSoloParaLoQueSigue() {
        Tarea t = banos();
        AsignacionTarea actual = AsignacionTarea.nueva(t, new Periodo(0, INICIO, FIN), ANA);
        t.editar("Lavar baños a fondo", new ValorTarea(9), Frecuencia.SEMANAL, INICIO, CDMX);
        assertEquals(8, actual.valorAlAsignar().valor(), "RN-12: la asignación conserva su valor");
        assertEquals(9, AsignacionTarea.nueva(t, new Periodo(1, FIN, FIN.plus(Duration.ofDays(7))), ANA)
                .valorAlAsignar().valor());
        assertEquals(LocalDate.of(2026, 11, 2), t.fechaInicio(), "misma frecuencia: no cambia el calendario");
        assertThrows(DatoInvalidoException.class,
                () -> t.editar(" ", new ValorTarea(3), Frecuencia.SEMANAL, INICIO, CDMX));
    }

    @Test
    void laFrecuenciaNuevaEmpiezaCuandoCierraElPeriodoEnCurso() {
        Tarea t = banos(); // semanal desde el lunes 2 de noviembre
        Instant miercoles = Instant.parse("2026-11-04T18:00:00Z");
        t.editar("Lavar baños", new ValorTarea(8), Frecuencia.DIARIA, miercoles, CDMX);

        assertEquals(Frecuencia.DIARIA, t.frecuencia());
        assertEquals(LocalDate.of(2026, 11, 9), t.fechaInicio(), "cuando cierra la semana en curso");
        assertTrue(t.periodoEn(Instant.parse("2026-11-06T18:00:00Z"), CDMX).isEmpty(),
                "no se empalma con la asignación semanal que sigue vigente");
        assertEquals(Instant.parse("2026-11-09T06:00:00Z"),
                t.periodoEn(Instant.parse("2026-11-09T18:00:00Z"), CDMX).orElseThrow().inicio());
    }

    @Test
    void siLaTareaAunNoEmpiezaConservaSuFechaDeInicio() {
        Tarea t = banos();
        t.editar("Lavar baños", new ValorTarea(8), Frecuencia.MENSUAL, Instant.parse("2026-10-20T12:00:00Z"), CDMX);
        assertEquals(Frecuencia.MENSUAL, t.frecuencia());
        assertEquals(LocalDate.of(2026, 11, 2), t.fechaInicio());
    }

    @Test
    void periodoDeLaTareaEnLaZonaDeLaSala() {
        Periodo p = banos().periodoEn(Instant.parse("2026-11-10T12:00:00Z"), CDMX).orElseThrow();
        assertEquals(1, p.numero());
        assertFalse(banos().periodoEn(Instant.parse("2026-11-01T12:00:00Z"), CDMX).isPresent(), "aún no empieza");
    }

    @Test
    void elTrimestreEmpiezaEnEneroAbrilJulioUOctubreEnLaZonaDeLaSala() {
        assertEquals(Instant.parse("2026-10-01T06:00:00Z"),
                CalculadoraDePuntos.inicioDelTrimestre(Instant.parse("2026-11-15T00:00:00Z"), CDMX));
        // 1 de enero a las 03:00 UTC sigue siendo 31 de diciembre en CDMX
        assertEquals(Instant.parse("2025-10-01T06:00:00Z"),
                CalculadoraDePuntos.inicioDelTrimestre(Instant.parse("2026-01-01T03:00:00Z"), CDMX));
    }
}
