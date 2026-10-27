package roomiehub.vivienda.application.usecase.sala;

import java.time.Clock;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.domain.exception.OperacionNoPermitidaException;
import roomiehub.vivienda.domain.exception.RecursoNoEncontradoException;
import roomiehub.vivienda.domain.model.sala.Membresia;
import roomiehub.vivienda.domain.repository.MembresiaRepository;
import roomiehub.vivienda.domain.repository.SalaRepository;
import roomiehub.vivienda.domain.service.ReglasDeMembresia;

/** CU-06: RN-07 y RN-08. El expulsado puede volver con el código. */
public class ExpulsarMiembro {

    private final SalaRepository salas;
    private final MembresiaRepository membresias;
    private final SalidaDeMiembro salida;
    private final Clock reloj;

    public ExpulsarMiembro(SalaRepository salas, MembresiaRepository membresias, SalidaDeMiembro salida, Clock reloj) {
        this.salas = salas;
        this.membresias = membresias;
        this.salida = salida;
        this.reloj = reloj;
    }

    @Transactional
    public void ejecutar(long adminId, long salaId, long expulsadoId) {
        List<Membresia> activas = membresias.activasDeSala(salaId);
        ReglasDeMembresia.exigirAdministrador(activas, adminId);
        if (expulsadoId == adminId) {
            throw new OperacionNoPermitidaException("El Administrador no se expulsa: abandona la sala");
        }
        if (activas.stream().noneMatch(m -> m.usuarioId() == expulsadoId)) {
            throw new RecursoNoEncontradoException("Esa persona no es miembro de la sala");
        }
        salas.buscarPorId(salaId).orElseThrow().exigirActiva();
        salida.ejecutar(salaId, expulsadoId, reloj.instant());
    }
}
