package roomiehub.vivienda.application.usecase.sala;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.port.NotificadorPort;
import roomiehub.vivienda.domain.exception.ConflictoDeEstadoException;
import roomiehub.vivienda.domain.exception.DatoInvalidoException;
import roomiehub.vivienda.domain.exception.OperacionNoPermitidaException;
import roomiehub.vivienda.domain.exception.RecursoNoEncontradoException;
import roomiehub.vivienda.domain.model.sala.CodigoInvitacion;
import roomiehub.vivienda.domain.model.sala.EstadoSala;
import roomiehub.vivienda.domain.model.sala.Membresia;
import roomiehub.vivienda.domain.model.sala.Rol;
import roomiehub.vivienda.domain.model.sala.Sala;
import roomiehub.vivienda.domain.model.tareas.MovimientoPuntos;
import roomiehub.vivienda.domain.model.tareas.TipoMovimiento;
import roomiehub.vivienda.domain.repository.MembresiaRepository;
import roomiehub.vivienda.domain.repository.PuntosRepository;
import roomiehub.vivienda.domain.repository.SalaRepository;
import roomiehub.vivienda.domain.repository.TareaRepository;
import roomiehub.vivienda.domain.repository.SalaRepository.SalaPorCodigo;
import roomiehub.vivienda.domain.service.CalculadoraDePuntos;
import roomiehub.vivienda.domain.service.ReglasDeMembresia;

/** CU-03: RN-01, RN-02, RN-04, RN-07 y RN-18. */
public class UnirseASala {

    private final SalaRepository salas;
    private final MembresiaRepository membresias;
    private final PuntosRepository puntos;
    private final TareaRepository tareas;
    private final NotificadorPort notificador;
    private final Clock reloj;

    public UnirseASala(SalaRepository salas, MembresiaRepository membresias, PuntosRepository puntos,
                       TareaRepository tareas, NotificadorPort notificador, Clock reloj) {
        this.salas = salas;
        this.membresias = membresias;
        this.puntos = puntos;
        this.tareas = tareas;
        this.notificador = notificador;
        this.reloj = reloj;
    }

    @Transactional
    public SalaDeUsuario ejecutar(long usuarioId, String codigoTexto) {
        CodigoInvitacion codigo;
        try {
            codigo = CodigoInvitacion.de(codigoTexto);
        } catch (DatoInvalidoException e) {
            throw noExiste();
        }
        salas.bloquearMembresiasDeUsuario(usuarioId); // primero el del usuario, después el de la sala
        long salaId = salas.buscarPorCodigo(codigo).orElseThrow(UnirseASala::noExiste).salaId();
        salas.bloquearMembresias(salaId);
        // Se vuelve a leer ya con el candado: el conteo de miembros no puede cambiar hasta el commit.
        SalaPorCodigo encontrada = salas.buscarPorCodigo(codigo).orElseThrow(UnirseASala::noExiste);
        if (encontrada.estado() == EstadoSala.DESACTIVADA) {
            throw new OperacionNoPermitidaException("La sala está desactivada: no admite nuevos miembros");
        }

        Optional<Membresia> previa = membresias.buscar(salaId, usuarioId);
        if (previa.filter(Membresia::activo).isPresent()) {
            throw new ConflictoDeEstadoException("Ya perteneces a esta sala");
        }
        ReglasDeMembresia.validarIngreso(encontrada.miembrosActivos(), membresias.activasDeUsuario(usuarioId).size());

        Instant ahora = reloj.instant();
        Membresia membresia = previa.orElseGet(() -> Membresia.nueva(usuarioId, salaId, Rol.HABITANTE, ahora));
        membresia.reactivar(ahora); // RN-04 y RN-07: Habitante, con antigüedad desde hoy
        // Primero la membresía: RLS solo deja leer los puntos de la sala a sus miembros.
        membresias.guardar(membresia);

        List<Membresia> otros = membresias.activasDeSala(salaId).stream()
                .filter(m -> m.usuarioId() != usuarioId)
                .toList();
        asignarPuntajeInicial(usuarioId, salaId, otros, previa.isPresent());

        notificador.notificar(otros.stream().map(Membresia::usuarioId).toList(),
                "Nuevo habitante", "Alguien se unió a la sala");
        Sala sala = salas.buscarPorId(salaId).orElseThrow();
        return new SalaDeUsuario(sala, Rol.HABITANTE, List.of());
    }

    /**
     * RN-18: promedio de los demás más la tarea más alta, redondeado hacia arriba. Quien vuelve conserva
     * su historial y recibe un AJUSTE que lleva su puntaje a ese valor.
     */
    private void asignarPuntajeInicial(long usuarioId, long salaId, List<Membresia> otros, boolean vuelve) {
        Map<Long, Integer> historicos = puntos.historicos(salaId);
        int objetivo = CalculadoraDePuntos.puntajeInicial(
                otros.stream().map(m -> historicos.getOrDefault(m.usuarioId(), 0)).toList(),
                tareas.valorMasAlto(salaId));
        int diferencia = objetivo - historicos.getOrDefault(usuarioId, 0);
        if (diferencia != 0) {
            puntos.registrar(new MovimientoPuntos(usuarioId, salaId, null, diferencia,
                    vuelve ? TipoMovimiento.AJUSTE : TipoMovimiento.INICIAL));
        }
    }

    private static RecursoNoEncontradoException noExiste() {
        return new RecursoNoEncontradoException("No existe una sala con ese código");
    }
}
