package roomiehub.vivienda.application.usecase.finanzas;

import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.port.NotificadorPort;
import roomiehub.vivienda.application.usecase.sala.AccesoASala;
import roomiehub.vivienda.domain.model.finanzas.Deuda;
import roomiehub.vivienda.domain.model.finanzas.GastoCompartido;
import roomiehub.vivienda.domain.repository.DeudaRepository;
import roomiehub.vivienda.domain.repository.GastoRepository;

/* CU-22 y CU-26 la confirmacion cuenta aunque el deudor no haya avisado
   con la ultima confirmacion la deuda pasa a Aprobada */
public class ConfirmarPago {

    private final AccesoASala acceso;
    private final DeudaRepository deudas;
    private final GastoRepository gastos;
    private final NotificadorPort notificador;

    public ConfirmarPago(AccesoASala acceso, DeudaRepository deudas, GastoRepository gastos,
                         NotificadorPort notificador) {
        this.acceso = acceso;
        this.deudas = deudas;
        this.gastos = gastos;
        this.notificador = notificador;
    }

    @Transactional
    public Deuda ejecutar(long usuarioId, long deudaId) {
        Deuda deuda = AvisarPago.deudaDeLaSala(deudas, deudaId);
        acceso.exigirMiembroDeSalaActiva(deuda.salaId(), usuarioId);
        GastoCompartido gasto = gastos.buscarPorId(deuda.servicioId()).orElseThrow();
        gasto.exigirVigente();

        Set<Long> activos = Set.copyOf(gastos.participantesActivos(gasto.id()));
        boolean aprobada = deuda.confirmar(usuarioId, activos);
        deudas.guardar(deuda);
        if (aprobada) {
            AvisarPago.notificarAprobada(notificador, deuda, activos, gasto);
        }
        return deuda;
    }
}
