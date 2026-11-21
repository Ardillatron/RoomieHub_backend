package roomiehub.vivienda.application.usecase.convivencia;

import java.time.Instant;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.domain.repository.NotaRepository;

// CU-30 limpia las notas fijadas cuyo plazo ya vencio
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
