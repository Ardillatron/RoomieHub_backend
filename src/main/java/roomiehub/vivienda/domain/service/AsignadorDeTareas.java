package roomiehub.vivienda.domain.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.random.RandomGenerator;

import roomiehub.vivienda.domain.model.tareas.ValorTarea;

/**
 * Sección 6: cada tarea, de mayor a menor valor, va al miembro con el menor puntaje proyectado
 * (histórico + lo ya asignado en este roleo). Los empates se sortean.
 */
public final class AsignadorDeTareas {

    public record TareaARolear(long tareaId, ValorTarea valor) {}

    private static final Comparator<TareaARolear> MAYOR_VALOR_PRIMERO =
            Comparator.comparingInt((TareaARolear t) -> t.valor().valor()).reversed()
                    .thenComparingLong(TareaARolear::tareaId);

    private final RandomGenerator azar;

    public AsignadorDeTareas(RandomGenerator azar) {
        this.azar = Objects.requireNonNull(azar, "azar");
    }

    /**
     * @param puntajeHistorico usuarioId → puntaje histórico de cada miembro activo
     * @return tareaId → usuarioId, en el orden en que se asignaron; vacío si no hay miembros
     */
    public Map<Long, Long> asignar(List<TareaARolear> tareas, Map<Long, Integer> puntajeHistorico) {
        Map<Long, Long> asignaciones = new LinkedHashMap<>();
        if (puntajeHistorico.isEmpty()) {
            return asignaciones;
        }
        Map<Long, Long> proyectado = new HashMap<>();
        puntajeHistorico.forEach((usuario, puntaje) -> proyectado.put(usuario, (long) puntaje));

        for (TareaARolear tarea : tareas.stream().sorted(MAYOR_VALOR_PRIMERO).toList()) {
            long menor = proyectado.values().stream().mapToLong(Long::longValue).min().orElseThrow();
            // Ordenados para que el sorteo sea reproducible con una semilla fija.
            List<Long> empatados = proyectado.entrySet().stream()
                    .filter(e -> e.getValue() == menor)
                    .map(Map.Entry::getKey)
                    .sorted()
                    .toList();
            long elegido = empatados.get(azar.nextInt(empatados.size()));
            proyectado.merge(elegido, (long) tarea.valor().valor(), Long::sum);
            asignaciones.put(tarea.tareaId(), elegido);
        }
        return asignaciones;
    }
}
