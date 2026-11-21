package roomiehub.vivienda.application.usecase.finanzas;

import java.time.Clock;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.usecase.sala.AccesoASala;
import roomiehub.vivienda.domain.model.finanzas.GastoCompartido;
import roomiehub.vivienda.domain.repository.GastoRepository;

// CU-20 eliminar es un borrado logico y sus deudas ya no aceptan avisos ni confirmaciones
public class EliminarGasto {

    private final AccesoASala acceso;
    private final GastoRepository gastos;
    private final Clock reloj;

    public EliminarGasto(AccesoASala acceso, GastoRepository gastos, Clock reloj) {
        this.acceso = acceso;
        this.gastos = gastos;
        this.reloj = reloj;
    }

    @Transactional
    public void ejecutar(long adminId, long salaId, long gastoId) {
        acceso.exigirAdministradorDeSalaActiva(salaId, adminId);
        GastoCompartido gasto = EditarGasto.gastoVigente(gastos, salaId, gastoId);
        gasto.eliminar(adminId, reloj.instant());
        gastos.guardar(gasto);
    }
}
