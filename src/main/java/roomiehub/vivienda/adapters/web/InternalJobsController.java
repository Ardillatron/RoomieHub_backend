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

/* El disparador externo llama aqui cada 15 minutos y despierta la instancia de Render
   sin secreto configurado el endpoint no existe y responde 404 */
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
        // Compara en tiempo constante para no revelar el secreto
        if (recibido == null || !MessageDigest.isEqual(secreto, recibido.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        int fallidas = tareasProgramadas.ejecutar();
        return fallidas == 0 ? ResponseEntity.noContent().build()
                : ResponseEntity.internalServerError().body(fallidas + " salas fallaron; ver el log");
    }
}
