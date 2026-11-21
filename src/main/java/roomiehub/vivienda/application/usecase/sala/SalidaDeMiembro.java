package roomiehub.vivienda.application.usecase.sala;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import roomiehub.vivienda.application.port.AlmacenamientoImagenesPort;
import roomiehub.vivienda.application.port.NotificadorPort;
import roomiehub.vivienda.application.usecase.convivencia.FirmarSubidaDeImagen;
import roomiehub.vivienda.domain.exception.OperacionNoPermitidaException;
import roomiehub.vivienda.domain.model.finanzas.Deuda;
import roomiehub.vivienda.domain.model.finanzas.GastoCompartido;
import roomiehub.vivienda.domain.model.sala.Membresia;
import roomiehub.vivienda.domain.model.sala.Rol;
import roomiehub.vivienda.domain.model.sala.Sala;
import roomiehub.vivienda.domain.repository.DeudaRepository;
import roomiehub.vivienda.domain.repository.GastoRepository;
import roomiehub.vivienda.domain.repository.MembresiaRepository;
import roomiehub.vivienda.domain.repository.SalaRepository;
import roomiehub.vivienda.domain.repository.TareaRepository;
import roomiehub.vivienda.domain.service.CalculadoraDeReparto;
import roomiehub.vivienda.domain.service.SucesionDeAdministrador;

/* Lo que pasa cuando alguien deja una sala por su cuenta al borrar su cuenta o por expulsion
   corre dentro de la transaccion del caso de uso
   el orden importa porque RLS solo deja tocar la sala mientras sigue siendo miembro
   por eso su membresia se desactiva al final */
public class SalidaDeMiembro {

    private final SalaRepository salas;
    private final MembresiaRepository membresias;
    private final TareaRepository tareas;
    private final GastoRepository gastos;
    private final DeudaRepository deudas;
    private final NotificadorPort notificador;
    private final AlmacenamientoImagenesPort almacenamiento;

    public SalidaDeMiembro(SalaRepository salas, MembresiaRepository membresias, TareaRepository tareas,
                           GastoRepository gastos, DeudaRepository deudas, NotificadorPort notificador,
                           AlmacenamientoImagenesPort almacenamiento) {
        this.salas = salas;
        this.membresias = membresias;
        this.tareas = tareas;
        this.gastos = gastos;
        this.deudas = deudas;
        this.notificador = notificador;
        this.almacenamiento = almacenamiento;
    }

    public void ejecutar(long salaId, long usuarioId, Instant ahora) {
        salas.bloquearMembresias(salaId);
        List<Membresia> activas = membresias.activasDeSala(salaId);
        Membresia saliente = activas.stream()
                .filter(m -> m.usuarioId() == usuarioId)
                .findFirst()
                .orElseThrow(() -> new OperacionNoPermitidaException("No perteneces a esta sala"));
        List<Membresia> restantes = activas.stream().filter(m -> m != saliente).toList();

        if (restantes.isEmpty()) {
            salas.borrar(salaId); // RN-10
            almacenamiento.borrarCarpeta(FirmarSubidaDeImagen.carpeta(salaId));
            return;
        }
        Sala sala = salas.buscarPorId(salaId).orElseThrow();

        tareas.desasignarPendientes(salaId, usuarioId); // RN-08
        for (GastoCompartido gasto : gastos.conParticipanteActivo(salaId, usuarioId)) {
            salirDelGasto(gasto, sala, usuarioId, ahora);
        }

        if (saliente.rol() == Rol.ADMIN) { // RN-06
            // Primero se le quita el rol porque solo puede haber un admin activo por sala
            saliente.degradar();
            membresias.guardar(saliente);
            Membresia sucesor = SucesionDeAdministrador.sucesor(restantes);
            sucesor.promover();
            membresias.guardar(sucesor);
        }
        saliente.desactivar();
        membresias.guardar(saliente);

        notificador.notificar(restantes.stream().map(Membresia::usuarioId).toList(),
                sala.nombre(), "Un habitante salió de la sala");
    }

    private void salirDelGasto(GastoCompartido gasto, Sala sala, long usuarioId, Instant ahora) {
        List<Long> quedan = gastos.participantesActivos(gasto.id()).stream()
                .filter(id -> id != usuarioId)
                .toList();

        for (Deuda deuda : deudas.pendientesDeGasto(gasto.id())) {
            if (deuda.deudorId() == usuarioId) {
                long periodo = gasto.numeroDePeriodo(deuda.periodo(), sala.zonaHoraria());
                List<Deuda> nuevas = CalculadoraDeReparto.repartirDeuda(deuda, quedan, periodo);
                deudas.guardar(deuda);
                nuevas.forEach(deudas::guardar);
            } else if (deuda.reevaluar(Set.copyOf(quedan))) {
                // Quien salio ya no tiene que confirmar y la deuda pudo quedar completa
                deudas.guardar(deuda);
            }
        }

        gastos.desactivarParticipante(gasto.id(), usuarioId);
        if (quedan.isEmpty()) {
            gasto.eliminar(usuarioId, ahora); // nadie mas comparte este gasto
            gastos.guardar(gasto);
        }
    }
}
