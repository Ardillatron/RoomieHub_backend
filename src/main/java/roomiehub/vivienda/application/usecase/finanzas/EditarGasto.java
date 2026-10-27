package roomiehub.vivienda.application.usecase.finanzas;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.usecase.sala.AccesoASala;
import roomiehub.vivienda.domain.exception.RecursoNoEncontradoException;
import roomiehub.vivienda.domain.model.finanzas.Deuda;
import roomiehub.vivienda.domain.model.finanzas.Dinero;
import roomiehub.vivienda.domain.model.finanzas.FrecuenciaGasto;
import roomiehub.vivienda.domain.model.finanzas.GastoCompartido;
import roomiehub.vivienda.domain.model.sala.Sala;
import roomiehub.vivienda.domain.repository.DeudaRepository;
import roomiehub.vivienda.domain.repository.GastoRepository;
import roomiehub.vivienda.domain.repository.MembresiaRepository;

/** CU-20 (editar): RN-24, solo afecta a los periodos futuros. La moneda no se cambia. */
public class EditarGasto {

    private final AccesoASala acceso;
    private final GastoRepository gastos;
    private final DeudaRepository deudas;
    private final MembresiaRepository membresias;
    private final Clock reloj;

    public EditarGasto(AccesoASala acceso, GastoRepository gastos, DeudaRepository deudas,
                       MembresiaRepository membresias, Clock reloj) {
        this.acceso = acceso;
        this.gastos = gastos;
        this.deudas = deudas;
        this.membresias = membresias;
        this.reloj = reloj;
    }

    @Transactional
    public GastoCompartido ejecutar(long adminId, long salaId, long gastoId, String concepto, BigDecimal monto,
                                    FrecuenciaGasto frecuencia, List<Long> participantes) {
        Sala sala = acceso.exigirAdministradorDeSalaActiva(salaId, adminId);
        GastoCompartido gasto = gastoVigente(gastos, salaId, gastoId);
        List<Long> validados = RegistrarGasto.validarParticipantes(membresias.activasDeSala(salaId), participantes);

        gasto.editar(concepto, new Dinero(monto, gasto.monto().moneda()), frecuencia, reloj.instant(),
                sala.zonaHoraria());
        GastoCompartido guardado = gastos.guardar(gasto);
        gastos.definirParticipantes(gasto.id(), salaId, validados);

        // Quien deja de participar ya no cuenta como confirmador requerido de lo pendiente (RN-26).
        // Sus propias deudas pendientes se conservan: corresponden a periodos en los que sí participó.
        Set<Long> activos = Set.copyOf(validados);
        for (Deuda deuda : deudas.pendientesDeGasto(gasto.id())) {
            if (deuda.reevaluar(activos)) {
                deudas.guardar(deuda);
            }
        }
        return guardado;
    }

    static GastoCompartido gastoVigente(GastoRepository gastos, long salaId, long gastoId) {
        GastoCompartido gasto = gastos.buscarPorId(gastoId)
                .filter(g -> g.salaId() == salaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Gasto no encontrado"));
        gasto.exigirVigente();
        return gasto;
    }
}
