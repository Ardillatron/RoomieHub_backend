package roomiehub.vivienda.application.usecase.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import roomiehub.vivienda.application.usecase.sync.AccionOffline.Tipo;
import roomiehub.vivienda.application.usecase.sync.ResultadoAccion.Estado;
import roomiehub.vivienda.domain.exception.ConflictoDeEstadoException;
import roomiehub.vivienda.domain.exception.DatoInvalidoException;
import roomiehub.vivienda.domain.exception.RecursoNoEncontradoException;

// Las tres revisiones de la sincronizacion y que responde cada falla con un procesador falso
class SincronizarAccionesOfflineTest {

    static final long SALA = 1, EXPULSADO_DE = 2;

    // El texto de la nota decide que pasa eliminado regla falla o se aplica
    static final class ProcesadorFalso extends ProcesadorDeAccion {
        final Set<UUID> procesadas;
        final List<UUID> aplicadas = new ArrayList<>();

        ProcesadorFalso(Set<UUID> procesadas) {
            super(null, null, null, null, null, null, null);
            this.procesadas = procesadas;
        }

        @Override public Set<Long> salasActivas(long usuarioId) { return Set.of(SALA); }

        @Override public boolean yaProcesada(UUID uuid) { return procesadas.contains(uuid); }

        @Override public Long aplicar(long usuarioId, AccionOffline a) {
            switch (String.valueOf(a.texto())) {
                case "eliminado" -> throw new RecursoNoEncontradoException("El gasto fue eliminado");
                case "regla" -> throw new ConflictoDeEstadoException("El plazo ya venció");
                case "falla" -> throw new IllegalStateException("la base se cayó");
                default -> { aplicadas.add(a.uuid()); return 99L; }
            }
        }
    }

    static AccionOffline nota(long sala, String texto) {
        return new AccionOffline(UUID.randomUUID(), Tipo.CREAR_NOTA, sala, texto, null, null, null, null);
    }

    @Test
    void cadaFallaTieneSuRespuestaYLasDemasSiguen() {
        AccionOffline yaHecha = nota(SALA, "hola");
        ProcesadorFalso procesador = new ProcesadorFalso(Set.of(yaHecha.uuid()));
        List<AccionOffline> cola = List.of(
                nota(SALA, "nueva"), yaHecha, nota(SALA, "eliminado"), nota(SALA, "regla"), nota(SALA, "falla"),
                nota(EXPULSADO_DE, "de la sala ajena"), nota(SALA, "otra nueva"));

        SincronizarAccionesOffline.Resultado r = new SincronizarAccionesOffline(procesador).ejecutar(7, cola);

        assertEquals(List.of(Estado.PROCESADA, Estado.DUPLICADA, Estado.RECHAZADA_RECURSO_ELIMINADO, Estado.RECHAZADA,
                        Estado.ERROR, Estado.RECHAZADA_EXPULSION, Estado.PROCESADA),
                r.resultados().stream().map(ResultadoAccion::estado).toList());
        assertEquals(99L, r.resultados().get(0).recursoId());
        assertEquals("El plazo ya venció", r.resultados().get(3).motivo());
        assertEquals(Set.of(EXPULSADO_DE), r.salasExpulsadas());
        assertEquals(List.of(cola.get(0).uuid(), cola.get(6).uuid()), procesador.aplicadas, "en orden");
    }

    @Test
    void loteDemasiadoGrandeOConUuidRepetido() {
        var sincronizar = new SincronizarAccionesOffline(new ProcesadorFalso(Set.of()));
        assertThrows(DatoInvalidoException.class, () -> sincronizar.ejecutar(7,
                Collections.nCopies(101, nota(SALA, "x"))));
        AccionOffline repetida = nota(SALA, "x");
        assertThrows(DatoInvalidoException.class, () -> sincronizar.ejecutar(7, List.of(repetida, repetida)));
    }
}
