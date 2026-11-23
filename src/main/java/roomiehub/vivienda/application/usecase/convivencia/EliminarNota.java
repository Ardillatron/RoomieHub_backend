package roomiehub.vivienda.application.usecase.convivencia;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.usecase.sala.AccesoASala;
import roomiehub.vivienda.domain.model.convivencia.Nota;
import roomiehub.vivienda.domain.repository.NotaRepository;

// CU-13 el admin o quien la escribio borra la nota y sus reportes se quedan
public class EliminarNota {

    private final AccesoASala acceso;
    private final NotaRepository notas;

    public EliminarNota(AccesoASala acceso, NotaRepository notas) {
        this.acceso = acceso;
        this.notas = notas;
    }

    @Transactional
    public void ejecutar(long usuarioId, long notaId) {
        Nota nota = ReaccionarNota.notaVisible(notas, notaId);
        if (nota.autorId() == usuarioId) {
            acceso.exigirMiembroDeSalaActiva(nota.salaId(), usuarioId);
        } else {
            acceso.exigirAdministradorDeSalaActiva(nota.salaId(), usuarioId);
        }
        nota.eliminar();
        notas.guardar(nota);
    }
}
