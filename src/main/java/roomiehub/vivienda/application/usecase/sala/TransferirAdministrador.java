package roomiehub.vivienda.application.usecase.sala;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.domain.exception.ConflictoDeEstadoException;
import roomiehub.vivienda.domain.exception.RecursoNoEncontradoException;
import roomiehub.vivienda.domain.model.sala.Membresia;
import roomiehub.vivienda.domain.repository.MembresiaRepository;
import roomiehub.vivienda.domain.repository.SalaRepository;
import roomiehub.vivienda.domain.service.ReglasDeMembresia;

/** CU-07: RN-03 y RN-06. */
public class TransferirAdministrador {

    private final SalaRepository salas;
    private final MembresiaRepository membresias;

    public TransferirAdministrador(SalaRepository salas, MembresiaRepository membresias) {
        this.salas = salas;
        this.membresias = membresias;
    }

    @Transactional
    public void ejecutar(long adminId, long salaId, long nuevoAdminId) {
        salas.bloquearMembresias(salaId);
        List<Membresia> activas = membresias.activasDeSala(salaId);
        Membresia actual = ReglasDeMembresia.exigirAdministrador(activas, adminId);
        salas.buscarPorId(salaId).orElseThrow().exigirActiva();
        if (nuevoAdminId == adminId) {
            throw new ConflictoDeEstadoException("Ya eres el Administrador");
        }
        Membresia nuevo = activas.stream()
                .filter(m -> m.usuarioId() == nuevoAdminId)
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Esa persona no es miembro de la sala"));

        // Primero se degrada: el índice único solo admite un Administrador activo por sala.
        actual.degradar();
        membresias.guardar(actual);
        nuevo.promover();
        membresias.guardar(nuevo);
    }
}
