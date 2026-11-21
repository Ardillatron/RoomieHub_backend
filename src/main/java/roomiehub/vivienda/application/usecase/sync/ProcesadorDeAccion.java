package roomiehub.vivienda.application.usecase.sync;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.usecase.convivencia.CrearNota;
import roomiehub.vivienda.application.usecase.convivencia.EnviarMensaje;
import roomiehub.vivienda.application.usecase.finanzas.AvisarPago;
import roomiehub.vivienda.application.usecase.finanzas.ConfirmarPago;
import roomiehub.vivienda.application.usecase.tareas.CompletarTarea;
import roomiehub.vivienda.domain.exception.DatoInvalidoException;
import roomiehub.vivienda.domain.model.sala.Membresia;
import roomiehub.vivienda.domain.repository.AccionProcesadaRepository;
import roomiehub.vivienda.domain.repository.MembresiaRepository;

/* Una accion una transaccion y si falla se deshace completa sin afectar a las demas
   es otro bean porque Spring no abre la transaccion si se llama desde la misma clase */
public class ProcesadorDeAccion {

    private final MembresiaRepository membresias;
    private final AccionProcesadaRepository procesadas;
    private final CrearNota crearNota;
    private final EnviarMensaje enviarMensaje;
    private final AvisarPago avisarPago;
    private final ConfirmarPago confirmarPago;
    private final CompletarTarea completarTarea;

    public ProcesadorDeAccion(MembresiaRepository membresias, AccionProcesadaRepository procesadas,
                              CrearNota crearNota, EnviarMensaje enviarMensaje, AvisarPago avisarPago,
                              ConfirmarPago confirmarPago, CompletarTarea completarTarea) {
        this.membresias = membresias;
        this.procesadas = procesadas;
        this.crearNota = crearNota;
        this.enviarMensaje = enviarMensaje;
        this.avisarPago = avisarPago;
        this.confirmarPago = confirmarPago;
        this.completarTarea = completarTarea;
    }

    // Dentro de una transaccion para que RLS sepa quien pregunta
    @Transactional(readOnly = true)
    public Set<Long> salasActivas(long usuarioId) {
        return membresias.activasDeUsuario(usuarioId).stream().map(Membresia::salaId).collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public boolean yaProcesada(UUID uuid) {
        return procesadas.existe(uuid);
    }

    // Aplica la accion con su caso de uso y guarda el UUID y regresa la nota o mensaje creado
    @Transactional
    public Long aplicar(long usuarioId, AccionOffline a) {
        Long recursoId = switch (a.tipo()) {
            case CREAR_NOTA -> crearNota.ejecutar(usuarioId, a.salaId(), a.texto(), a.imagenId(), a.uuid())
                    .nota().id();
            case ENVIAR_MENSAJE -> enviarMensaje.ejecutar(usuarioId, a.salaId(), a.texto(), a.imagenId(),
                    a.notaRespondidaId(), a.uuid()).mensaje().id();
            case AVISAR_PAGO -> {
                exigirDeLaSala(avisarPago.ejecutar(usuarioId, requerido(a.deudaId(), "deudaId")).salaId(), a);
                yield null;
            }
            case CONFIRMAR_PAGO -> {
                exigirDeLaSala(confirmarPago.ejecutar(usuarioId, requerido(a.deudaId(), "deudaId")).salaId(), a);
                yield null;
            }
            case COMPLETAR_TAREA -> {
                exigirDeLaSala(completarTarea.ejecutar(usuarioId, requerido(a.asignacionId(), "asignacionId"))
                        .salaId(), a);
                yield null;
            }
        };
        procesadas.registrar(a.uuid(), usuarioId, a.salaId());
        return recursoId;
    }

    // La sala la manda el telefono y debe ser la misma del recurso
    private static void exigirDeLaSala(long salaDelRecurso, AccionOffline a) {
        if (salaDelRecurso != a.salaId()) {
            throw new DatoInvalidoException("La acción no corresponde a esa sala");
        }
    }

    private static long requerido(Long valor, String campo) {
        if (valor == null) {
            throw new DatoInvalidoException("Falta " + campo);
        }
        return valor;
    }
}
