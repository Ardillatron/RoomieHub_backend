package roomiehub.vivienda.application.usecase.convivencia;

import java.time.Instant;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.domain.repository.NotaRepository;

/** CU-30 (Sistema): RN-31. Al leer, una nota con el plazo vencido ya no cuenta como fijada; esto limpia el dato. */
public class DesfijarNotasVencidas {

    private final NotaRepository notas;

    public DesfijarNotasVencidas(NotaRepository notas) {
        this.notas = notas;
    }

    @Transactional
    public void ejecutar(long salaId, Instant ahora) {
        notas.desfijarVencidas(salaId, ahora);
    }
}
