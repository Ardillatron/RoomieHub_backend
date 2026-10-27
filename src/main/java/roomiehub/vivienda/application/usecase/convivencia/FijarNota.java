package roomiehub.vivienda.application.usecase.convivencia;

import java.time.Clock;
import java.time.Instant;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.usecase.sala.AccesoASala;
import roomiehub.vivienda.domain.exception.ConflictoDeEstadoException;
import roomiehub.vivienda.domain.model.convivencia.Nota;
import roomiehub.vivienda.domain.repository.NotaRepository;

/** CU-12: RN-31. Solo el Administrador, hasta 30 días y como máximo 3 fijadas a la vez. */
public class FijarNota {

    private final AccesoASala acceso;
    private final NotaRepository notas;
    private final Clock reloj;

    public FijarNota(AccesoASala acceso, NotaRepository notas, Clock reloj) {
        this.acceso = acceso;
        this.notas = notas;
        this.reloj = reloj;
    }

    @Transactional
    public void fijar(long adminId, long notaId, Instant hasta) {
        Nota nota = ReaccionarNota.notaVisible(notas, notaId);
        acceso.exigirAdministradorDeSalaActiva(nota.salaId(), adminId);
        Instant ahora = reloj.instant();
        // Cambiar el plazo de una ya fijada no cuenta como una fijada más.
        long otrasFijadas = notas.fijadasDeSala(nota.salaId(), ahora).stream()
                .filter(n -> !n.id().equals(notaId))
                .count();
        if (otrasFijadas >= Nota.MAXIMO_FIJADAS) {
            throw new ConflictoDeEstadoException("Ya hay 3 notas fijadas: desfija una primero");
        }
        nota.fijar(hasta, ahora);
        notas.guardar(nota);
    }

    @Transactional
    public void desfijar(long adminId, long notaId) {
        Nota nota = ReaccionarNota.notaVisible(notas, notaId);
        acceso.exigirAdministradorDeSalaActiva(nota.salaId(), adminId);
        nota.desfijar();
        notas.guardar(nota);
    }
}
