package roomiehub.vivienda.application.usecase.finanzas;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import roomiehub.vivienda.domain.model.Periodo;
import roomiehub.vivienda.domain.model.finanzas.Deuda;
import roomiehub.vivienda.domain.model.finanzas.Dinero;
import roomiehub.vivienda.domain.model.finanzas.GastoCompartido;
import roomiehub.vivienda.domain.repository.DeudaRepository;
import roomiehub.vivienda.domain.repository.GastoRepository;
import roomiehub.vivienda.domain.service.CalculadoraDeReparto;

/**
 * RN-22 y RN-23: una deuda por participante en cada periodo iniciado, con los centavos rotando. Atiende
 * también los periodos que el proceso no alcanzó (RN-41) y nunca genera dos veces el mismo periodo.
 * Corre dentro de la transacción de quien lo llama.
 */
public class GeneradorDeDeudas {

    private final GastoRepository gastos;
    private final DeudaRepository deudas;

    public GeneradorDeDeudas(GastoRepository gastos, DeudaRepository deudas) {
        this.gastos = gastos;
        this.deudas = deudas;
    }

    /** @return cuántas deudas generó */
    public int generar(GastoCompartido gasto, ZoneId zona, Instant ahora) {
        List<Long> participantes = gastos.participantesActivos(gasto.id());
        List<Periodo> periodos = gasto.periodosIniciados(ahora, zona);
        if (participantes.isEmpty() || periodos.isEmpty()) {
            return 0;
        }
        int generadas = 0;
        for (Periodo periodo : periodos) {
            LocalDate dia = LocalDate.ofInstant(periodo.inicio(), zona);
            if (deudas.existenDelPeriodo(gasto.id(), dia)) {
                continue;
            }
            List<Dinero> partes = CalculadoraDeReparto.repartir(gasto.monto(), participantes.size(), periodo.numero());
            for (int i = 0; i < participantes.size(); i++) {
                deudas.guardar(Deuda.nueva(gasto.id(), gasto.salaId(), dia, participantes.get(i), partes.get(i), null));
                generadas++;
            }
        }
        // Los periodos anteriores al actual ya quedaron atendidos: la próxima vez no se revisan.
        gasto.avanzarGeneracionHasta(LocalDate.ofInstant(periodos.get(periodos.size() - 1).inicio(), zona));
        gastos.guardar(gasto);
        return generadas;
    }
}
