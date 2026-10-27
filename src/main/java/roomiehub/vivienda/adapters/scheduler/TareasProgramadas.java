package roomiehub.vivienda.adapters.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import roomiehub.vivienda.domain.repository.SalaRepository;

/**
 * CU-23, CU-24, CU-25, CU-29, CU-30 y los recordatorios de la sección 10. Corre con el cron mientras la instancia está despierta, y también cuando un
 * disparador externo llama a POST /internal/jobs/ejecutar (sección 12), porque un @Scheduled no despierta
 * una instancia dormida. Los procesos son idempotentes, así que coincidir no duplica nada.
 */
@Component
public class TareasProgramadas {

    private static final Logger log = LoggerFactory.getLogger(TareasProgramadas.class);

    private final SalaRepository salas;
    private final ProcesadorSala procesador;

    public TareasProgramadas(SalaRepository salas, ProcesadorSala procesador) {
        this.salas = salas;
        this.procesador = procesador;
    }

    /** @return cuántas salas fallaron */
    @Scheduled(cron = "${app.jobs.cron:0 */15 * * * *}")
    public int ejecutar() {
        int fallidas = 0;
        for (long salaId : salas.conTrabajoPendiente()) {
            try {
                procesador.procesar(salaId);
            } catch (Exception e) {
                // Una sala con error no detiene a las demás.
                fallidas++;
                log.error("Falló el procesamiento de la sala {}", salaId, e);
            }
        }
        return fallidas;
    }
}
