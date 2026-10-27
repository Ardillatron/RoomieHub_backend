package roomiehub.vivienda.adapters.persistence;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import roomiehub.vivienda.domain.model.finanzas.Deuda;
import roomiehub.vivienda.domain.model.finanzas.Dinero;
import roomiehub.vivienda.domain.model.finanzas.EstadoDeuda;
import roomiehub.vivienda.domain.model.finanzas.Moneda;
import roomiehub.vivienda.domain.repository.DeudaRepository;
import roomiehub.vivienda.infrastructure.persistence.ConfirmacionPagoJpaEntity;
import roomiehub.vivienda.infrastructure.persistence.ConfirmacionPagoJpaRepository;
import roomiehub.vivienda.infrastructure.persistence.DeudaJpaEntity;
import roomiehub.vivienda.infrastructure.persistence.DeudaJpaRepository;
import roomiehub.vivienda.infrastructure.persistence.GastoJpaRepository;

@Component
public class JpaDeudaRepositoryAdapter implements DeudaRepository {

    private final DeudaJpaRepository deudas;
    private final ConfirmacionPagoJpaRepository confirmaciones;
    private final GastoJpaRepository gastos;

    public JpaDeudaRepositoryAdapter(DeudaJpaRepository deudas, ConfirmacionPagoJpaRepository confirmaciones,
                                     GastoJpaRepository gastos) {
        this.deudas = deudas;
        this.confirmaciones = confirmaciones;
        this.gastos = gastos;
    }

    @Override
    public Optional<Deuda> buscarPorId(long id) {
        return deudas.findById(id).map(e -> aDominio(List.of(e)).get(0));
    }

    @Override
    public List<Deuda> pendientesDeGasto(long gastoId) {
        return aDominio(deudas.findByServicioIdAndEstado(gastoId, EstadoDeuda.PENDIENTE));
    }

    @Override
    public List<Deuda> deSala(long salaId, EstadoDeuda estado) {
        return aDominio(deudas.findBySalaIdAndEstadoOrderByPeriodoDescIdAsc(salaId, estado));
    }

    @Override
    public boolean existenDelPeriodo(long gastoId, LocalDate periodo) {
        return deudas.existsByServicioIdAndPeriodoAndDeudaOrigenIdIsNull(gastoId, periodo);
    }

    @Override
    public void guardar(Deuda d) {
        DeudaJpaEntity e = d.id() == null ? new DeudaJpaEntity() : deudas.findById(d.id()).orElseThrow();
        e.setServicioId(d.servicioId());
        e.setSalaId(d.salaId());
        e.setPeriodo(d.periodo());
        e.setDeudorId(d.deudorId());
        e.setMonto(d.monto().monto());
        e.setDeudaOrigenId(d.deudaOrigenId());
        e.setEstado(d.estado());
        e.setAvisoPagoEn(d.avisoPagoEn());
        e.setRecordatoriosEnviados((short) d.recordatoriosEnviados());
        e.setUltimoRecordatorioEn(d.ultimoRecordatorioEn());
        e.setRecordatorioConfirmacionEn(d.recordatorioConfirmacionEn());
        DeudaJpaEntity guardada = deudas.saveAndFlush(e);

        Set<Long> yaGuardados = confirmaciones.findByDeudaIdIn(List.of(guardada.getId())).stream()
                .map(ConfirmacionPagoJpaEntity::getConfirmadorId)
                .collect(Collectors.toSet());
        for (Long confirmador : d.confirmadores()) {
            if (!yaGuardados.contains(confirmador)) {
                ConfirmacionPagoJpaEntity c = new ConfirmacionPagoJpaEntity();
                c.setDeudaId(guardada.getId());
                c.setSalaId(d.salaId());
                c.setConfirmadorId(confirmador);
                confirmaciones.saveAndFlush(c);
            }
        }
    }

    /** Carga las confirmaciones de todas en una consulta, y la moneda de cada gasto una sola vez. */
    private List<Deuda> aDominio(List<DeudaJpaEntity> entidades) {
        if (entidades.isEmpty()) {
            return List.of();
        }
        Map<Long, Set<Long>> confirmadores = confirmaciones
                .findByDeudaIdIn(entidades.stream().map(DeudaJpaEntity::getId).toList()).stream()
                .collect(Collectors.groupingBy(ConfirmacionPagoJpaEntity::getDeudaId,
                        Collectors.mapping(ConfirmacionPagoJpaEntity::getConfirmadorId, Collectors.toSet())));
        Map<Long, Moneda> monedas = new HashMap<>();
        return entidades.stream()
                .map(e -> new Deuda(e.getId(), e.getServicioId(), e.getSalaId(), e.getPeriodo(), e.getDeudorId(),
                        new Dinero(e.getMonto(), monedas.computeIfAbsent(e.getServicioId(),
                                id -> gastos.findById(id).orElseThrow().getMoneda())),
                        e.getDeudaOrigenId(), e.getEstado(), e.getAvisoPagoEn(),
                        confirmadores.getOrDefault(e.getId(), Set.of()), e.getCreadoEn(),
                        e.getRecordatoriosEnviados(), e.getUltimoRecordatorioEn(), e.getRecordatorioConfirmacionEn()))
                .toList();
    }
}
