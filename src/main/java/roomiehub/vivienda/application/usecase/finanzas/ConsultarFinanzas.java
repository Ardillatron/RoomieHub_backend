package roomiehub.vivienda.application.usecase.finanzas;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.usecase.sala.AccesoASala;
import roomiehub.vivienda.domain.model.cuenta.Usuario;
import roomiehub.vivienda.domain.model.finanzas.Deuda;
import roomiehub.vivienda.domain.model.finanzas.EstadoDeuda;
import roomiehub.vivienda.domain.model.finanzas.GastoCompartido;
import roomiehub.vivienda.domain.repository.DeudaRepository;
import roomiehub.vivienda.domain.repository.GastoRepository;
import roomiehub.vivienda.domain.repository.UsuarioRepository;

/** Lo que cualquier miembro ve de las finanzas de su sala. */
public class ConsultarFinanzas {

    public record GastoConParticipantes(GastoCompartido gasto, List<Long> participantes) {}

    public record DeudaConNombres(Deuda deuda, String concepto, String deudor) {}

    private final AccesoASala acceso;
    private final GastoRepository gastos;
    private final DeudaRepository deudas;
    private final UsuarioRepository usuarios;

    public ConsultarFinanzas(AccesoASala acceso, GastoRepository gastos, DeudaRepository deudas,
                             UsuarioRepository usuarios) {
        this.acceso = acceso;
        this.gastos = gastos;
        this.deudas = deudas;
        this.usuarios = usuarios;
    }

    /** RN-25: los eliminados ya no se muestran. */
    @Transactional(readOnly = true)
    public List<GastoConParticipantes> gastos(long usuarioId, long salaId) {
        acceso.exigirMiembro(salaId, usuarioId);
        return gastos.vigentesDeSala(salaId).stream()
                .map(g -> new GastoConParticipantes(g, gastos.participantesActivos(g.id())))
                .toList();
    }

    /** Del periodo más reciente al más viejo. Las de gastos eliminados siguen visibles: son historial. */
    @Transactional(readOnly = true)
    public List<DeudaConNombres> deudas(long usuarioId, long salaId, EstadoDeuda estado) {
        acceso.exigirMiembro(salaId, usuarioId);
        List<Deuda> lista = deudas.deSala(salaId, estado);
        Map<Long, String> conceptos = new HashMap<>();
        lista.forEach(d -> conceptos.computeIfAbsent(d.servicioId(),
                id -> gastos.buscarPorId(id).orElseThrow().concepto()));
        Map<Long, String> deudores = usuarios.buscarPorIds(lista.stream().map(Deuda::deudorId).distinct().toList())
                .stream().collect(Collectors.toMap(Usuario::id, Usuario::nombre));
        return lista.stream()
                .map(d -> new DeudaConNombres(d, conceptos.get(d.servicioId()), deudores.get(d.deudorId())))
                .toList();
    }
}
