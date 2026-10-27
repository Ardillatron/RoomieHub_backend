package roomiehub.vivienda.application.usecase.sala;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.domain.model.sala.CodigoInvitacion;
import roomiehub.vivienda.domain.model.sala.Membresia;
import roomiehub.vivienda.domain.model.sala.Rol;
import roomiehub.vivienda.domain.model.sala.Sala;
import roomiehub.vivienda.domain.repository.MembresiaRepository;
import roomiehub.vivienda.domain.repository.SalaRepository;
import roomiehub.vivienda.domain.service.ReglasDeMembresia;

/** CU-02: RN-02, RN-03 y RN-05. El creador inicia con 0 puntos (RN-18), así que no se registra movimiento. */
public class CrearSala {

    private final SalaRepository salas;
    private final MembresiaRepository membresias;
    private final Clock reloj;
    private final SecureRandom azar = new SecureRandom();

    public CrearSala(SalaRepository salas, MembresiaRepository membresias, Clock reloj) {
        this.salas = salas;
        this.membresias = membresias;
        this.reloj = reloj;
    }

    @Transactional
    public SalaDeUsuario ejecutar(long usuarioId, String nombre, String zonaHoraria) {
        salas.bloquearMembresiasDeUsuario(usuarioId);
        ReglasDeMembresia.validarLimiteDeMembresias(membresias.activasDeUsuario(usuarioId).size());

        // Un código repetido choca con la restricción UNIQUE (409); con 10^12 combinaciones es improbable.
        Sala sala = salas.guardar(Sala.nueva(nombre, usuarioId, zonaHoraria, CodigoInvitacion.generar(azar)));
        membresias.guardar(Membresia.nueva(usuarioId, sala.id(), Rol.ADMIN, reloj.instant()));
        return new SalaDeUsuario(sala, Rol.ADMIN, List.of());
    }
}
