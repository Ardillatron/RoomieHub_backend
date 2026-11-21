package roomiehub.vivienda.infrastructure.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import roomiehub.vivienda.application.port.NotificadorPort;
import roomiehub.vivienda.domain.repository.DispositivoRepository;
import roomiehub.vivienda.infrastructure.config.TrasElCommit;

/* Push con la API HTTP v1 de FCM sin el SDK de Firebase
   el JWT de la cuenta de servicio lo firma Nimbus que ya viene con Spring Security
   envia despues del commit en otro hilo y sin credenciales solo escribe en el log */
@Component
public class FirebaseNotificador implements NotificadorPort {

    private static final Logger log = LoggerFactory.getLogger(FirebaseNotificador.class);
    private static final String ALCANCE = "https://www.googleapis.com/auth/firebase.messaging";
    // FCM rechaza mensajes de mas de 4 KB y un mensaje de chat puede tener 4000 caracteres
    private static final int MAXIMO_CUERPO = 200;

    private final DispositivoRepository dispositivos;
    private final Clock reloj;
    private final RestClient http = RestClient.create();
    // Un solo hilo para enviar en orden y que el token no necesite candados
    private final ExecutorService envios = Executors.newSingleThreadExecutor();
    private final String proyecto;
    private final String cuentaDeServicio;
    private final String urlDeToken;
    private final RSAPrivateKey llave;
    private String acceso;
    private Instant accesoVence = Instant.MIN;

    // credenciales es la ruta al JSON de la cuenta de servicio de Firebase
    public FirebaseNotificador(@Value("${app.firebase.credenciales:}") String credenciales,
                               DispositivoRepository dispositivos, Clock reloj)
            throws IOException, ParseException, GeneralSecurityException {
        this.dispositivos = dispositivos;
        this.reloj = reloj;
        if (credenciales.isBlank()) {
            proyecto = cuentaDeServicio = urlDeToken = null;
            llave = null;
            return;
        }
        Map<String, Object> json = JSONObjectUtils.parse(Files.readString(Path.of(credenciales.trim())));
        proyecto = (String) json.get("project_id");
        cuentaDeServicio = (String) json.get("client_email");
        urlDeToken = (String) json.get("token_uri");
        String pem = ((String) json.get("private_key"))
                .replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        llave = (RSAPrivateKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem)));
    }

    @Override
    public void notificar(Collection<Long> usuarios, String titulo, String cuerpo) {
        if (usuarios.isEmpty()) {
            return;
        }
        if (proyecto == null) {
            log.info("Notificación a {}: {} — {}", usuarios, titulo, cuerpo);
            return;
        }
        List<Long> destinatarios = List.copyOf(usuarios);
        String recortado = cuerpo.length() > MAXIMO_CUERPO ? cuerpo.substring(0, MAXIMO_CUERPO - 1) + "…" : cuerpo;
        TrasElCommit.ejecutar(() -> envios.execute(() -> enviar(destinatarios, titulo, recortado)));
    }

    private void enviar(List<Long> usuarios, String titulo, String cuerpo) {
        try {
            String token = accesoFcm();
            for (String dispositivo : dispositivos.tokensDe(usuarios)) {
                enviarA(dispositivo, titulo, cuerpo, token);
            }
        } catch (RuntimeException | JOSEException e) {
            // Una notificacion perdida no deshace la accion que la genero
            log.error("No se pudo enviar la notificación push", e);
        }
    }

    void enviarA(String dispositivo, String titulo, String cuerpo, String token) {
        // Prioridad alta porque con prioridad normal nunca llegaron al telefono
        Map<String, Object> mensaje = Map.of("message", Map.of("token", dispositivo,
                "notification", Map.of("title", titulo, "body", cuerpo),
                "android", Map.of("priority", "HIGH")));
        http.post().uri("https://fcm.googleapis.com/v1/projects/{proyecto}/messages:send", proyecto)
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(mensaje)
                .exchange((peticion, respuesta) -> {
                    if (respuesta.getStatusCode().value() == 404) {
                        dispositivos.borrarToken(dispositivo); // UNREGISTERED significa que la app ya no esta en ese telefono
                    } else if (respuesta.getStatusCode().isError()) {
                        log.warn("FCM respondió {} al enviar a un dispositivo", respuesta.getStatusCode());
                    }
                    return null;
                });
    }

    // Token OAuth de una hora que se pide con el JWT de la cuenta de servicio y se reusa
    String accesoFcm() throws JOSEException {
        Instant ahora = reloj.instant();
        if (ahora.isBefore(accesoVence)) {
            return acceso;
        }
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), new JWTClaimsSet.Builder()
                .issuer(cuentaDeServicio)
                .audience(urlDeToken)
                .claim("scope", ALCANCE)
                .issueTime(Date.from(ahora))
                .expirationTime(Date.from(ahora.plusSeconds(3600)))
                .build());
        jwt.sign(new RSASSASigner(llave));
        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
        formulario.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
        formulario.add("assertion", jwt.serialize());
        Map<?, ?> respuesta = http.post().uri(urlDeToken)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formulario)
                .retrieve()
                .body(Map.class);
        acceso = (String) respuesta.get("access_token");
        // Un minuto de margen para no usarlo justo cuando vence
        accesoVence = ahora.plusSeconds(((Number) respuesta.get("expires_in")).longValue() - 60);
        return acceso;
    }

    @PreDestroy
    void cerrar() {
        envios.shutdown();
    }
}
