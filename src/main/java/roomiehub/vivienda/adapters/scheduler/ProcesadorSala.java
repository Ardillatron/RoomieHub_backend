package roomiehub.vivienda.adapters.scheduler;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.usecase.convivencia.DesfijarNotasVencidas;
import roomiehub.vivienda.application.usecase.finanzas.EnviarRecordatorios;
import roomiehub.vivienda.application.usecase.finanzas.GenerarDeudasDelPeriodo;
import roomiehub.vivienda.application.usecase.sala.BorrarSalaVencida;
import roomiehub.vivienda.application.usecase.tareas.PenalizarVencidas;
import roomiehub.vivienda.application.usecase.tareas.RolearTareas;
import roomiehub.vivienda.domain.repository.SalaRepository;
import roomiehub.vivienda.infrastructure.security.ContextoSistemaRls;

/**
 * Una sala, una transacción. Es un bean aparte de TareasProgramadas porque Spring no abre la
 * transacción si el método @Transactional se llama desde la misma clase (sección 9).
 */
@Component
public class ProcesadorSala {

    private final ContextoSistemaRls contexto;
    private final SalaRepository salas;
    private final BorrarSalaVencida borrarSalaVencida;
    private final PenalizarVencidas penalizarVencidas;
    private final RolearTareas rolearTareas;
    private final GenerarDeudasDelPeriodo generarDeudas;
    private final EnviarRecordatorios enviarRecordatorios;
    private final DesfijarNotasVencidas desfijarNotas;
    private final Clock reloj;

    public ProcesadorSala(ContextoSistemaRls contexto, SalaRepository salas, BorrarSalaVencida borrarSalaVencida,
                          PenalizarVencidas penalizarVencidas, RolearTareas rolearTareas,
                          GenerarDeudasDelPeriodo generarDeudas, EnviarRecordatorios enviarRecordatorios,
                          DesfijarNotasVencidas desfijarNotas, Clock reloj) {
        this.desfijarNotas = desfijarNotas;
        this.contexto = contexto;
        this.salas = salas;
        this.borrarSalaVencida = borrarSalaVencida;
        this.penalizarVencidas = penalizarVencidas;
        this.rolearTareas = rolearTareas;
        this.generarDeudas = generarDeudas;
        this.enviarRecordatorios = enviarRecordatorios;
        this.reloj = reloj;
    }

    @Transactional
    public void procesar(long salaId) {
        contexto.entrarComoSistema(salaId);
        if (!salas.intentarBloquearProcesamiento(salaId)) {
            return; // otro proceso ya la está atendiendo
        }
        Instant ahora = reloj.instant();
        if (borrarSalaVencida.ejecutar(salaId, ahora)) {
            return;
        }
        // Primero se cierran los periodos vencidos y después se abren los nuevos.
        penalizarVencidas.ejecutar(salaId, ahora);
        rolearTareas.ejecutar(salaId, ahora);
        generarDeudas.ejecutar(salaId, ahora);
        enviarRecordatorios.ejecutar(salaId, ahora);
        desfijarNotas.ejecutar(salaId, ahora);
    }
}
