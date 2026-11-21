package roomiehub.vivienda.adapters.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import roomiehub.vivienda.domain.repository.SalaRepository;

/* Corre los procesos programados de tareas deudas notas salas y recordatorios
   corre con el cron y tambien cuando el disparador externo llama a POST /internal/jobs/ejecutar
   los procesos son idempotentes y si coinciden no duplican nada */
@Component
public class TareasProgramadas {

    private static final Logger log = LoggerFactory.getLogger(TareasProgramadas.class);

    private final SalaRepository salas;
    private final ProcesadorSala procesador;

    public TareasProgramadas(SalaRepository salas, ProcesadorSala procesador) {
        this.salas = salas;
        this.procesador = procesador;
    }

    // Regresa cuantas salas fallaron
    @Scheduled(cron = "${app.jobs.cron:0 */15 * * * *}")
    public int ejecutar() {
        int fallidas = 0;
        for (long salaId : salas.conTrabajoPendiente()) {
            try {
                procesador.procesar(salaId);
            } catch (Exception e) {
                // Si una sala falla las demas siguen
                fallidas++;
                log.error("Falló el procesamiento de la sala {}", salaId, e);
            }
        }
        return fallidas;
    }
}
