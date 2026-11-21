package roomiehub.vivienda.application.usecase.sync;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import roomiehub.vivienda.application.usecase.sync.ResultadoAccion.Estado;
import roomiehub.vivienda.domain.exception.DatoInvalidoException;
import roomiehub.vivienda.domain.exception.DomainException;
import roomiehub.vivienda.domain.exception.RecursoNoEncontradoException;

/* CU-27 procesa la cola offline en el orden del telefono
   cada accion revisa miembro activo UUID ya procesado y recurso existente
   cada accion tiene su propia transaccion en ProcesadorDeAccion */
public class SincronizarAccionesOffline {

    public static final int MAXIMO_POR_LOTE = 100;

    // salasExpulsadas son las salas cuyo cache borra la app
    public record Resultado(List<ResultadoAccion> resultados, Set<Long> salasExpulsadas) {}

    private static final Logger log = Logger.getLogger(SincronizarAccionesOffline.class.getName());

    private final ProcesadorDeAccion procesador;

    public SincronizarAccionesOffline(ProcesadorDeAccion procesador) {
        this.procesador = procesador;
    }

    public Resultado ejecutar(long usuarioId, List<AccionOffline> acciones) {
        if (acciones.size() > MAXIMO_POR_LOTE) {
            throw new DatoInvalidoException("Se sincronizan a lo más " + MAXIMO_POR_LOTE + " acciones por lote");
        }
        if (new HashSet<>(acciones.stream().map(AccionOffline::uuid).toList()).size() != acciones.size()) {
            throw new DatoInvalidoException("Hay UUID repetidos en el lote");
        }
        Set<Long> activas = procesador.salasActivas(usuarioId);
        Set<Long> expulsadas = new LinkedHashSet<>();
        List<ResultadoAccion> resultados = new ArrayList<>(acciones.size());

        for (AccionOffline accion : acciones) {
            if (!activas.contains(accion.salaId())) {
                expulsadas.add(accion.salaId());
                resultados.add(ResultadoAccion.de(accion, Estado.RECHAZADA_EXPULSION, "Ya no perteneces a esta sala",
                        null));
            } else if (procesador.yaProcesada(accion.uuid())) {
                resultados.add(ResultadoAccion.de(accion, Estado.DUPLICADA, null, null));
            } else {
                resultados.add(aplicar(usuarioId, accion));
            }
        }
        return new Resultado(resultados, expulsadas);
    }

    private ResultadoAccion aplicar(long usuarioId, AccionOffline accion) {
        try {
            return ResultadoAccion.de(accion, Estado.PROCESADA, null, procesador.aplicar(usuarioId, accion));
        } catch (RecursoNoEncontradoException e) {
            return ResultadoAccion.de(accion, Estado.RECHAZADA_RECURSO_ELIMINADO, e.getMessage(), null);
        } catch (DomainException e) {
            return ResultadoAccion.de(accion, Estado.RECHAZADA, e.getMessage(), null);
        } catch (RuntimeException e) {
            // Si llegan dos envios iguales al reintentar el UUID ya estara y sale DUPLICADA
            log.log(Level.SEVERE, "Falló la acción offline " + accion.uuid(), e);
            return ResultadoAccion.de(accion, Estado.ERROR, "Error del servidor; se reintentará", null);
        }
    }
}
