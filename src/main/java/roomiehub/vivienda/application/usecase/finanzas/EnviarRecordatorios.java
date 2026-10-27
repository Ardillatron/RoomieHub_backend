package roomiehub.vivienda.application.usecase.finanzas;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.port.NotificadorPort;
import roomiehub.vivienda.domain.model.finanzas.Deuda;
import roomiehub.vivienda.domain.model.finanzas.EstadoDeuda;
import roomiehub.vivienda.domain.model.finanzas.GastoCompartido;
import roomiehub.vivienda.domain.model.sala.EstadoSala;
import roomiehub.vivienda.domain.repository.DeudaRepository;
import roomiehub.vivienda.domain.repository.GastoRepository;
import roomiehub.vivienda.domain.repository.SalaRepository;

/**
 * Sección 10 (Sistema): al deudor a los 3 días si no ha avisado y luego cada 7, máximo 3 por periodo; a
 * quien falte por confirmar, a las 24 horas del aviso. Ni gastos eliminados ni salas desactivadas.
 */
public class EnviarRecordatorios {

    private final SalaRepository salas;
    private final DeudaRepository deudas;
    private final GastoRepository gastos;
    private final NotificadorPort notificador;

    public EnviarRecordatorios(SalaRepository salas, DeudaRepository deudas, GastoRepository gastos,
                               NotificadorPort notificador) {
        this.salas = salas;
        this.deudas = deudas;
        this.gastos = gastos;
        this.notificador = notificador;
    }

    @Transactional
    public void ejecutar(long salaId, Instant ahora) {
        if (salas.buscarPorId(salaId).orElseThrow().estado() == EstadoSala.DESACTIVADA) {
            return;
        }
        Map<Long, GastoCompartido> gastosPorId = new HashMap<>();
        Map<Long, Set<Long>> activosPorGasto = new HashMap<>();
        for (Deuda deuda : deudas.deSala(salaId, EstadoDeuda.PENDIENTE)) {
            GastoCompartido gasto = gastosPorId.computeIfAbsent(deuda.servicioId(),
                    id -> gastos.buscarPorId(id).orElseThrow());
            if (gasto.eliminado()) {
                continue;
            }
            Set<Long> activos = activosPorGasto.computeIfAbsent(gasto.id(),
                    id -> Set.copyOf(gastos.participantesActivos(id)));

            boolean alDeudor = deuda.tocaRecordarAlDeudor(ahora);
            Set<Long> porConfirmar = deuda.recordarConfirmaciones(ahora, activos);
            if (alDeudor) {
                notificador.notificar(List.of(deuda.deudorId()), gasto.concepto(), "Tienes un pago pendiente");
            }
            if (!porConfirmar.isEmpty()) {
                notificador.notificar(List.copyOf(porConfirmar), gasto.concepto(), "Falta que confirmes un pago");
            }
            if (alDeudor || !porConfirmar.isEmpty()) {
                deudas.guardar(deuda);
            }
        }
    }
}
