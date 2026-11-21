package roomiehub.vivienda.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import roomiehub.vivienda.domain.model.tareas.ValorTarea;
import roomiehub.vivienda.domain.service.AsignadorDeTareas.TareaARolear;

class AsignadorDeTareasTest {

    static final long ELENA = 1, DIEGO = 2, ANA = 3, BETO = 4, CARLA = 5;
    static final long BANOS = 10, BARRER = 11, BASURA = 12;

    static final List<TareaARolear> TAREAS = List.of(
            new TareaARolear(BASURA, new ValorTarea(3)),
            new TareaARolear(BANOS, new ValorTarea(8)),
            new TareaARolear(BARRER, new ValorTarea(5)));

    static final Map<Long, Integer> PUNTAJES = Map.of(ELENA, 15, DIEGO, 20, ANA, 23, BETO, 24, CARLA, 25);

    @Test
    void ejemploDelDocumento() {
        Set<Long> ganadoresDeBasura = new HashSet<>();
        for (long semilla = 0; semilla < 50; semilla++) {
            Map<Long, Long> r = new AsignadorDeTareas(new SplittableRandom(semilla)).asignar(TAREAS, PUNTAJES);
            assertEquals(ELENA, r.get(BANOS));
            assertEquals(DIEGO, r.get(BARRER));
            ganadoresDeBasura.add(r.get(BASURA));
            assertEquals(List.of(BANOS, BARRER, BASURA), List.copyOf(r.keySet()), "de mayor a menor valor");
        }
        // Elena con 15 mas 8 da 23 y Ana con 23 empatan y el sorteo puede dar a cualquiera
        assertEquals(Set.of(ELENA, ANA), ganadoresDeBasura);
    }

    @Test
    void sinMiembrosNoAsignaNada() {
        assertTrue(new AsignadorDeTareas(new SplittableRandom(1)).asignar(TAREAS, Map.of()).isEmpty());
    }

    @Test
    void empatadosRecibenTareasDeValorParecido() {
        List<TareaARolear> tareas = List.of(
                new TareaARolear(1, new ValorTarea(10)),
                new TareaARolear(2, new ValorTarea(9)));
        Map<Long, Long> r = new AsignadorDeTareas(new SplittableRandom(7)).asignar(tareas, Map.of(ANA, 0, BETO, 0));
        assertEquals(Set.of(ANA, BETO), Set.copyOf(r.values()), "una tarea para cada uno");
    }

    @Test
    void puntajesNegativosCuentan() {
        Map<Long, Long> r = new AsignadorDeTareas(new SplittableRandom(1))
                .asignar(List.of(new TareaARolear(1, new ValorTarea(4))), Map.of(ANA, -6, BETO, 0));
        assertEquals(ANA, r.get(1L));
    }
}
