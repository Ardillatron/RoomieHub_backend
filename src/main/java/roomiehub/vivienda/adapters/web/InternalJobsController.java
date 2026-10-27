package roomiehub.vivienda.adapters.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import roomiehub.vivienda.adapters.scheduler.TareasProgramadas;

/**
 * Sección 12: el disparador externo (GitHub Actions o un cron por HTTP) llama aquí cada 15 minutos.
 * Esa petición despierta la instancia de Render. Sin secreto configurado, el endpoint no existe (404).
 */
@RestController
public class InternalJobsController {

    private final TareasProgramadas tareasProgramadas;
    private final byte[] secreto;

    public InternalJobsController(TareasProgramadas tareasProgramadas, @Value("${app.jobs.secreto:}") String secreto) {
        this.tareasProgramadas = tareasProgramadas;
        this.secreto = secreto.getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping("/internal/jobs/ejecutar")
    public ResponseEntity<String> ejecutar(@RequestHeader(value = "X-Jobs-Secret", required = false) String recibido) {
        if (secreto.length == 0) {
            return ResponseEntity.notFound().build();
        }
        // Comparación en tiempo constante, para no revelar el secreto por el tiempo de respuesta.
        if (recibido == null || !MessageDigest.isEqual(secreto, recibido.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        int fallidas = tareasProgramadas.ejecutar();
        return fallidas == 0 ? ResponseEntity.noContent().build()
                : ResponseEntity.internalServerError().body(fallidas + " salas fallaron; ver el log");
    }
}
