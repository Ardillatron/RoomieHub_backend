package roomiehub.vivienda.application.usecase.sala;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.domain.exception.RecursoNoEncontradoException;
import roomiehub.vivienda.domain.model.cuenta.Usuario;
import roomiehub.vivienda.domain.model.sala.Membresia;
import roomiehub.vivienda.domain.repository.MembresiaRepository;
import roomiehub.vivienda.domain.repository.SalaRepository;
import roomiehub.vivienda.domain.repository.UsuarioRepository;

// CU-04 las salas del usuario y el detalle de una con codigo y miembros
public class ConsultarSalas {

    private final SalaRepository salas;
    private final MembresiaRepository membresias;
    private final UsuarioRepository usuarios;

    public ConsultarSalas(SalaRepository salas, MembresiaRepository membresias, UsuarioRepository usuarios) {
        this.salas = salas;
        this.membresias = membresias;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public List<SalaDeUsuario> misSalas(long usuarioId) {
        return membresias.activasDeUsuario(usuarioId).stream()
                .map(m -> new SalaDeUsuario(salas.buscarPorId(m.salaId()).orElseThrow(), m.rol(), List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public SalaDeUsuario detalle(long usuarioId, long salaId) {
        // RLS solo regresa membresias de salas donde el usuario es miembro activo
        List<Membresia> activas = membresias.activasDeSala(salaId);
        Membresia mia = activas.stream()
                .filter(m -> m.usuarioId() == usuarioId)
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Sala no encontrada"));
        Map<Long, Usuario> personas = usuarios.buscarPorIds(activas.stream().map(Membresia::usuarioId).toList())
                .stream().collect(Collectors.toMap(Usuario::id, Function.identity()));
        List<SalaDeUsuario.Miembro> miembros = activas.stream()
                .map(m -> new SalaDeUsuario.Miembro(m.usuarioId(), personas.get(m.usuarioId()).nombre(),
                        personas.get(m.usuarioId()).fotoUrl(), m.rol(), m.unidoEn()))
                .toList();
        return new SalaDeUsuario(salas.buscarPorId(salaId).orElseThrow(), mia.rol(), miembros);
    }
}
