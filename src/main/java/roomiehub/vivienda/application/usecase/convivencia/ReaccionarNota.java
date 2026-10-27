package roomiehub.vivienda.application.usecase.convivencia;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.usecase.sala.AccesoASala;
import roomiehub.vivienda.domain.exception.RecursoNoEncontradoException;
import roomiehub.vivienda.domain.model.convivencia.Nota;
import roomiehub.vivienda.domain.model.convivencia.TipoReaccion;
import roomiehub.vivienda.domain.repository.NotaRepository;
import roomiehub.vivienda.domain.repository.ReaccionRepository;

/** CU-10: RN-29, like o dislike; reaccionar otra vez cambia el tipo y se puede quitar. */
public class ReaccionarNota {

    private final AccesoASala acceso;
    private final NotaRepository notas;
    private final ReaccionRepository reacciones;

    public ReaccionarNota(AccesoASala acceso, NotaRepository notas, ReaccionRepository reacciones) {
        this.acceso = acceso;
        this.notas = notas;
        this.reacciones = reacciones;
    }

    @Transactional
    public void reaccionar(long usuarioId, long notaId, TipoReaccion tipo) {
        Nota nota = notaVisible(notas, notaId);
        acceso.exigirMiembroDeSalaActiva(nota.salaId(), usuarioId);
        reacciones.reaccionar(notaId, nota.salaId(), usuarioId, tipo);
    }

    @Transactional
    public void quitar(long usuarioId, long notaId) {
        Nota nota = notaVisible(notas, notaId);
        acceso.exigirMiembroDeSalaActiva(nota.salaId(), usuarioId);
        reacciones.quitar(notaId, usuarioId);
    }

    /** RLS: una nota de otra sala no se ve, así que responde igual que una eliminada o inexistente. */
    static Nota notaVisible(NotaRepository notas, long notaId) {
        Nota nota = notas.buscarPorId(notaId).orElseThrow(() -> new RecursoNoEncontradoException("Nota no encontrada"));
        nota.exigirVisible();
        return nota;
    }
}
