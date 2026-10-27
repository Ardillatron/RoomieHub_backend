package roomiehub.vivienda.application.usecase.finanzas;

import java.time.Instant;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.domain.model.finanzas.GastoCompartido;
import roomiehub.vivienda.domain.model.sala.EstadoSala;
import roomiehub.vivienda.domain.model.sala.Sala;
import roomiehub.vivienda.domain.repository.GastoRepository;
import roomiehub.vivienda.domain.repository.SalaRepository;

/** CU-25 (Sistema): RN-23 y RN-41. Pausado en una sala desactivada (RN-09). */
public class GenerarDeudasDelPeriodo {

    private final SalaRepository salas;
    private final GastoRepository gastos;
    private final GeneradorDeDeudas generador;

    public GenerarDeudasDelPeriodo(SalaRepository salas, GastoRepository gastos, GeneradorDeDeudas generador) {
        this.salas = salas;
        this.gastos = gastos;
        this.generador = generador;
    }

    @Transactional
    public void ejecutar(long salaId, Instant ahora) {
        Sala sala = salas.buscarPorId(salaId).orElseThrow();
        if (sala.estado() == EstadoSala.DESACTIVADA) {
            return;
        }
        for (GastoCompartido gasto : gastos.vigentesDeSala(salaId)) {
            generador.generar(gasto, sala.zonaHoraria(), ahora);
        }
    }
}
