package roomiehub.vivienda.application.usecase.convivencia;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.usecase.sala.AccesoASala;
import roomiehub.vivienda.domain.model.convivencia.Nota;
import roomiehub.vivienda.domain.repository.NotaRepository;

/** CU-13: solo el Administrador; borrado lógico, sus reportes se conservan. */
public class EliminarNota {

    private final AccesoASala acceso;
    private final NotaRepository notas;

    public EliminarNota(AccesoASala acceso, NotaRepository notas) {
        this.acceso = acceso;
        this.notas = notas;
    }

    @Transactional
    public void ejecutar(long adminId, long notaId) {
        Nota nota = ReaccionarNota.notaVisible(notas, notaId);
        acceso.exigirAdministradorDeSalaActiva(nota.salaId(), adminId);
        nota.eliminar();
        notas.guardar(nota);
    }
}
