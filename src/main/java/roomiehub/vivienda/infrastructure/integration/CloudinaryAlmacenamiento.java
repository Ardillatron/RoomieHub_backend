package roomiehub.vivienda.infrastructure.integration;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import roomiehub.vivienda.application.port.AlmacenamientoImagenesPort;
import roomiehub.vivienda.application.port.ServicioNoDisponibleException;
import roomiehub.vivienda.infrastructure.config.TrasElCommit;

/* Unico lugar que conoce Cloudinary
   las firmas se calculan aqui con SHA 1 sin su SDK para no agregar una dependencia */
@Component
public class CloudinaryAlmacenamiento implements AlmacenamientoImagenesPort {

    private static final Pattern URL = Pattern.compile("^cloudinary://([^:]+):([^@]+)@(.+)$");
    // Hasta 1080p y el lado mayor se limita a 1920 px
    static final String LIMITE_AL_SUBIR = "c_limit,h_1920,w_1920";
    // Formato y calidad automaticos para mandar menos datos al telefono
    static final String AL_ENTREGAR = "f_auto,q_auto";
    // Para el tablon el lado mayor es de 600 px y la completa solo se baja al ampliar
    static final String MINIATURA = "c_limit,h_600,w_600/" + AL_ENTREGAR;
    private static final String FORMATOS = "jpg,jpeg,png,webp,heic";
    private static final Logger log = LoggerFactory.getLogger(CloudinaryAlmacenamiento.class);

    private final String apiKey;
    private final String apiSecret;
    private final String cloudName;
    private final Clock reloj;
    private final RestClient http = RestClient.create();

    // cloudinaryUrl es la variable de Cloudinary y vacia apaga las imagenes
    public CloudinaryAlmacenamiento(@Value("${app.cloudinary.url:}") String cloudinaryUrl, Clock reloj) {
        this.reloj = reloj;
        if (cloudinaryUrl.isBlank()) {
            apiKey = apiSecret = cloudName = null;
            return;
        }
        Matcher m = URL.matcher(cloudinaryUrl.trim());
        if (!m.matches()) {
            // Sin mostrar el valor porque tiene el secreto
            throw new IllegalStateException("app.cloudinary.url no tiene la forma cloudinary://key:secret@cloud_name");
        }
        apiKey = m.group(1);
        apiSecret = m.group(2);
        cloudName = m.group(3);
    }

    @Override
    public FirmaDeSubida firmarSubida(String carpeta) {
        exigirConfigurado();
        String imagenId = carpeta + "/" + UUID.randomUUID();
        Map<String, String> firmados = new TreeMap<>(Map.of(
                "public_id", imagenId,
                "type", "authenticated",
                "transformation", LIMITE_AL_SUBIR,
                "allowed_formats", FORMATOS,
                "timestamp", Long.toString(reloj.instant().getEpochSecond())));
        Map<String, String> parametros = new TreeMap<>(firmados);
        parametros.put("signature", firmaDeParametros(firmados, apiSecret));
        parametros.put("api_key", apiKey);
        return new FirmaDeSubida("https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload", imagenId, parametros);
    }

    @Override
    public String urlDeEntrega(String imagenId) {
        return urlFirmada(AL_ENTREGAR, imagenId);
    }

    @Override
    public String urlDeMiniatura(String imagenId) {
        return urlFirmada(MINIATURA, imagenId);
    }

    private String urlFirmada(String transformacion, String imagenId) {
        exigirConfigurado();
        String ruta = transformacion + "/" + imagenId;
        return "https://res.cloudinary.com/" + cloudName + "/image/authenticated/s--"
                + firmaDeUrl(ruta, apiSecret) + "--/" + ruta;
    }

    @Override
    public void borrarCarpeta(String carpeta) {
        if (cloudName == null) {
            return; // sin Cloudinary no se subio ninguna imagen
        }
        // Si la transaccion se deshace la sala y sus imagenes siguen
        TrasElCommit.ejecutar(() -> {
            try {
                // La API borra hasta 1000 por llamada y avisa si quedaron mas
                Map<?, ?> respuesta;
                do {
                    respuesta = http.delete()
                            .uri("https://api.cloudinary.com/v1_1/{nube}/resources/image/authenticated"
                                    + "?prefix={prefijo}&invalidate=true", cloudName, carpeta + "/")
                            .headers(h -> h.setBasicAuth(apiKey, apiSecret))
                            .retrieve()
                            .body(Map.class);
                } while (respuesta != null && Boolean.TRUE.equals(respuesta.get("partial")));
            } catch (RestClientException e) {
                // shortcut si falla las imagenes quedan huerfanas y agregar reintentos si pasa seguido
                log.error("No se pudieron borrar las imágenes de {}", carpeta, e);
            }
        });
    }

    // Firma de una peticion a la API con parametros ordenados mas el secreto en SHA 1
    static String firmaDeParametros(Map<String, String> parametros, String secreto) {
        String aFirmar = new TreeMap<>(parametros).entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
        return HexFormat.of().formatHex(sha1(aFirmar + secreto));
    }

    // Firma de una URL con los primeros 8 caracteres del SHA 1 en Base64
    static String firmaDeUrl(String ruta, String secreto) {
        return Base64.getUrlEncoder().encodeToString(sha1(ruta + secreto)).substring(0, 8);
    }

    private static byte[] sha1(String texto) {
        try {
            return MessageDigest.getInstance("SHA-1").digest(texto.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e); // toda JVM tiene SHA 1
        }
    }

    private void exigirConfigurado() {
        if (cloudName == null) {
            throw new ServicioNoDisponibleException("Las imágenes no están configuradas (falta CLOUDINARY_URL)");
        }
    }
}
