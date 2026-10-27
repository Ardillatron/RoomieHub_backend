package roomiehub.vivienda.application.usecase.tareas;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.usecase.sala.AccesoASala;
import roomiehub.vivienda.domain.model.cuenta.Usuario;
import roomiehub.vivienda.domain.model.sala.Membresia;
import roomiehub.vivienda.domain.model.tareas.AsignacionTarea;
import roomiehub.vivienda.domain.model.tareas.Tarea;
import roomiehub.vivienda.domain.repository.AsignacionRepository;
import roomiehub.vivienda.domain.repository.PuntosRepository;
import roomiehub.vivienda.domain.repository.SalaRepository;
import roomiehub.vivienda.domain.repository.TareaRepository;
import roomiehub.vivienda.domain.repository.UsuarioRepository;
import roomiehub.vivienda.domain.service.CalculadoraDePuntos;

/** Lo que cualquier miembro puede ver de las tareas: catálogo, asignaciones vigentes y balance (CU-18). */
public class ConsultarTareas {

    public record AsignacionConNombres(AsignacionTarea asignacion, String tarea, String responsable) {}

    /** RN-17: el visible se reinicia cada trimestre; el histórico es el que usa el algoritmo. */
    public record Balance(long usuarioId, String nombre, int visible, int historico) {}

    private final AccesoASala acceso;
    private final SalaRepository salas;
    private final TareaRepository tareas;
    private final AsignacionRepository asignaciones;
    private final PuntosRepository puntos;
    private final UsuarioRepository usuarios;
    private final Clock reloj;

    public ConsultarTareas(AccesoASala acceso, SalaRepository salas, TareaRepository tareas,
                           AsignacionRepository asignaciones, PuntosRepository puntos, UsuarioRepository usuarios,
                           Clock reloj) {
        this.acceso = acceso;
        this.salas = salas;
        this.tareas = tareas;
        this.asignaciones = asignaciones;
        this.puntos = puntos;
        this.usuarios = usuarios;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public List<Tarea> catalogo(long usuarioId, long salaId) {
        acceso.exigirMiembro(salaId, usuarioId);
        return tareas.activasDeSala(salaId);
    }

    /** Las de periodos vigentes y las recién cerradas que aún están dentro del margen de RN-16. */
    @Transactional(readOnly = true)
    public List<AsignacionConNombres> asignaciones(long usuarioId, long salaId) {
        acceso.exigirMiembro(salaId, usuarioId);
        List<AsignacionTarea> recientes = asignaciones.recientesDeSala(salaId,
                reloj.instant().minus(AsignacionTarea.GRACIA));
        Map<Long, String> nombresDeTareas = recientes.stream().map(AsignacionTarea::tareaId).distinct()
                .collect(Collectors.toMap(Function.identity(), id -> tareas.buscarPorId(id).orElseThrow().nombre()));
        Map<Long, String> personas = nombres(recientes.stream().map(AsignacionTarea::usuarioId)
                .filter(id -> id != null).distinct().toList());
        return recientes.stream()
                .map(a -> new AsignacionConNombres(a, nombresDeTareas.get(a.tareaId()),
                        a.usuarioId() == null ? null : personas.get(a.usuarioId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Balance> balance(long usuarioId, long salaId) {
        List<Membresia> miembros = acceso.exigirMiembro(salaId, usuarioId);
        Instant desde = CalculadoraDePuntos.inicioDelTrimestre(reloj.instant(),
                salas.buscarPorId(salaId).orElseThrow().zonaHoraria());
        Map<Long, Integer> visibles = puntos.visibles(salaId, desde);
        Map<Long, Integer> historicos = puntos.historicos(salaId);
        Map<Long, String> personas = nombres(miembros.stream().map(Membresia::usuarioId).toList());
        return miembros.stream()
                .map(m -> new Balance(m.usuarioId(), personas.get(m.usuarioId()),
                        visibles.getOrDefault(m.usuarioId(), 0), historicos.getOrDefault(m.usuarioId(), 0)))
                .toList();
    }

    private Map<Long, String> nombres(List<Long> ids) {
        return usuarios.buscarPorIds(ids).stream().collect(Collectors.toMap(Usuario::id, Usuario::nombre));
    }
}
