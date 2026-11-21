package roomiehub.vivienda.application.usecase.finanzas;

import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.port.NotificadorPort;
import roomiehub.vivienda.application.usecase.sala.AccesoASala;
import roomiehub.vivienda.domain.exception.OperacionNoPermitidaException;
import roomiehub.vivienda.domain.exception.RecursoNoEncontradoException;
import roomiehub.vivienda.domain.model.finanzas.Deuda;
import roomiehub.vivienda.domain.model.finanzas.GastoCompartido;
import roomiehub.vivienda.domain.repository.DeudaRepository;
import roomiehub.vivienda.domain.repository.GastoRepository;

// CU-21 avisar no cambia el estado salvo que no quede nadie mas que confirme
public class AvisarPago {

    private final AccesoASala acceso;
    private final DeudaRepository deudas;
    private final GastoRepository gastos;
    private final NotificadorPort notificador;
    private final Clock reloj;

    public AvisarPago(AccesoASala acceso, DeudaRepository deudas, GastoRepository gastos, NotificadorPort notificador,
                      Clock reloj) {
        this.acceso = acceso;
        this.deudas = deudas;
        this.gastos = gastos;
        this.notificador = notificador;
        this.reloj = reloj;
    }

    @Transactional
    public Deuda ejecutar(long usuarioId, long deudaId) {
        Deuda deuda = deudaDeLaSala(deudas, deudaId);
        acceso.exigirMiembroDeSalaActiva(deuda.salaId(), usuarioId);
        if (deuda.deudorId() != usuarioId) {
            throw new OperacionNoPermitidaException("Solo el deudor avisa que pagó");
        }
        GastoCompartido gasto = gastos.buscarPorId(deuda.servicioId()).orElseThrow();
        gasto.exigirVigente();

        Set<Long> activos = Set.copyOf(gastos.participantesActivos(gasto.id()));
        boolean aprobada = deuda.avisarPago(reloj.instant(), activos);
        deudas.guardar(deuda);

        if (aprobada) {
            notificarAprobada(notificador, deuda, activos, gasto);
        } else {
            notificador.notificar(List.copyOf(deuda.faltanPorConfirmar(activos)), gasto.concepto(),
                    "Avisaron que pagaron su parte: confirma el pago");
        }
        return deuda;
    }

    // Una deuda de otra sala no se ve por RLS y responde igual que una que no existe
    static Deuda deudaDeLaSala(DeudaRepository deudas, long deudaId) {
        return deudas.buscarPorId(deudaId).orElseThrow(() -> new RecursoNoEncontradoException("Deuda no encontrada"));
    }

    // Avisa a los participantes del gasto y al deudor que la deuda quedo aprobada
    static void notificarAprobada(NotificadorPort notificador, Deuda deuda, Set<Long> activos, GastoCompartido gasto) {
        Set<Long> destinatarios = new HashSet<>(activos);
        destinatarios.add(deuda.deudorId());
        notificador.notificar(List.copyOf(destinatarios), gasto.concepto(), "Un pago quedó aprobado");
    }
}
