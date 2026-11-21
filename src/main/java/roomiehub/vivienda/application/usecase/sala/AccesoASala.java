package roomiehub.vivienda.application.usecase.sala;

import java.util.List;

import roomiehub.vivienda.domain.exception.RecursoNoEncontradoException;
import roomiehub.vivienda.domain.model.sala.Membresia;
import roomiehub.vivienda.domain.model.sala.Sala;
import roomiehub.vivienda.domain.repository.MembresiaRepository;
import roomiehub.vivienda.domain.repository.SalaRepository;
import roomiehub.vivienda.domain.service.ReglasDeMembresia;

// Revisiones de acceso que repiten los casos de uso de una sala
public class AccesoASala {

    private final SalaRepository salas;
    private final MembresiaRepository membresias;

    public AccesoASala(SalaRepository salas, MembresiaRepository membresias) {
        this.salas = salas;
        this.membresias = membresias;
    }

    // Regresa las membresias activas y un no miembro recibe 404 para no revelar salas
    public List<Membresia> exigirMiembro(long salaId, long usuarioId) {
        List<Membresia> activas = membresias.activasDeSala(salaId);
        if (activas.stream().noneMatch(m -> m.usuarioId() == usuarioId)) {
            throw new RecursoNoEncontradoException("Sala no encontrada");
        }
        return activas;
    }

    // Para lo que cambia la sala pide miembro y sala activa
    public Sala exigirMiembroDeSalaActiva(long salaId, long usuarioId) {
        exigirMiembro(salaId, usuarioId);
        Sala sala = salas.buscarPorId(salaId).orElseThrow();
        sala.exigirActiva();
        return sala;
    }

    // Para acciones de admin pide admin y sala activa
    public Sala exigirAdministradorDeSalaActiva(long salaId, long usuarioId) {
        ReglasDeMembresia.exigirAdministrador(exigirMiembro(salaId, usuarioId), usuarioId);
        Sala sala = salas.buscarPorId(salaId).orElseThrow();
        sala.exigirActiva();
        return sala;
    }
}
