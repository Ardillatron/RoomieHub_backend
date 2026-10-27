package roomiehub.vivienda.adapters.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import roomiehub.vivienda.domain.model.finanzas.Dinero;
import roomiehub.vivienda.domain.model.finanzas.GastoCompartido;
import roomiehub.vivienda.domain.repository.GastoRepository;
import roomiehub.vivienda.infrastructure.persistence.GastoJpaEntity;
import roomiehub.vivienda.infrastructure.persistence.GastoJpaRepository;
import roomiehub.vivienda.infrastructure.persistence.ParticipanteGastoJpaEntity;
import roomiehub.vivienda.infrastructure.persistence.ParticipanteGastoJpaRepository;

@Component
public class JpaGastoRepositoryAdapter implements GastoRepository {

    private final GastoJpaRepository gastos;
    private final ParticipanteGastoJpaRepository participantes;

    public JpaGastoRepositoryAdapter(GastoJpaRepository gastos, ParticipanteGastoJpaRepository participantes) {
        this.gastos = gastos;
        this.participantes = participantes;
    }

    @Override
    public Optional<GastoCompartido> buscarPorId(long id) {
        return gastos.findById(id).map(JpaGastoRepositoryAdapter::aDominio);
    }

    @Override
    public List<GastoCompartido> vigentesDeSala(long salaId) {
        return gastos.findBySalaIdAndEliminadoFalseOrderById(salaId).stream()
                .map(JpaGastoRepositoryAdapter::aDominio).toList();
    }

    @Override
    public List<GastoCompartido> conParticipanteActivo(long salaId, long usuarioId) {
        return gastos.conParticipanteActivo(salaId, usuarioId).stream().map(JpaGastoRepositoryAdapter::aDominio).toList();
    }

    @Override
    public List<Long> participantesActivos(long gastoId) {
        return participantes.findByServicioIdAndActivoTrueOrderByOrden(gastoId).stream()
                .map(ParticipanteGastoJpaEntity::getUsuarioId)
                .toList();
    }

    @Override
    public void definirParticipantes(long gastoId, long salaId, List<Long> usuarios) {
        List<ParticipanteGastoJpaEntity> todos = new ArrayList<>(participantes.findByServicioIdOrderByOrden(gastoId));
        Map<Long, ParticipanteGastoJpaEntity> porUsuario = todos.stream()
                .collect(Collectors.toMap(ParticipanteGastoJpaEntity::getUsuarioId, Function.identity()));
        int siguienteOrden = todos.stream().mapToInt(ParticipanteGastoJpaEntity::getOrden).max().orElse(-1) + 1;

        for (ParticipanteGastoJpaEntity p : todos) {
            p.setActivo(usuarios.contains(p.getUsuarioId()));
        }
        for (Long usuarioId : usuarios) {
            if (!porUsuario.containsKey(usuarioId)) {
                ParticipanteGastoJpaEntity nuevo = new ParticipanteGastoJpaEntity();
                nuevo.setServicioId(gastoId);
                nuevo.setSalaId(salaId);
                nuevo.setUsuarioId(usuarioId);
                nuevo.setOrden((short) siguienteOrden++);
                nuevo.setActivo(true);
                todos.add(nuevo);
            }
        }
        participantes.saveAllAndFlush(todos);
    }

    @Override
    public void desactivarParticipante(long gastoId, long usuarioId) {
        participantes.desactivar(gastoId, usuarioId);
    }

    @Override
    public GastoCompartido guardar(GastoCompartido g) {
        GastoJpaEntity e = new GastoJpaEntity();
        e.setId(g.id());
        e.setSalaId(g.salaId());
        e.setConcepto(g.concepto());
        e.setMonto(g.monto().monto());
        e.setMoneda(g.monto().moneda());
        e.setFrecuencia(g.frecuencia());
        e.setFechaInicio(g.fechaInicio());
        e.setGenerarDesde(g.generarDesde());
        e.setEliminado(g.eliminado());
        e.setEliminadoPor(g.eliminadoPor());
        e.setEliminadoEn(g.eliminadoEn());
        return aDominio(gastos.saveAndFlush(e));
    }

    private static GastoCompartido aDominio(GastoJpaEntity e) {
        return new GastoCompartido(e.getId(), e.getSalaId(), e.getConcepto(), new Dinero(e.getMonto(), e.getMoneda()),
                e.getFrecuencia(), e.getFechaInicio(), e.getGenerarDesde(), e.isEliminado(), e.getEliminadoPor(),
                e.getEliminadoEn());
    }
}
