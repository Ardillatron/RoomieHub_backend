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

/**
 * Es el único lugar que conoce Cloudinary (sección 2). Las firmas se calculan aquí con SHA-1, como pide
 * Cloudinary, sin su SDK: son pocas líneas y así no se agrega una dependencia.
 */
@Component
public class CloudinaryAlmacenamiento implements AlmacenamientoImagenesPort {

    private static final Pattern URL = Pattern.compile("^cloudinary://([^:]+):([^@]+)@(.+)$");
    /** RN-34: hasta 1080p. Se limita el lado mayor a 1920 px, sea horizontal o vertical. */
    static final String LIMITE_AL_SUBIR = "c_limit,h_1920,w_1920";
    /** Formato y calidad automáticos al entregar: menos datos para el teléfono y menos créditos. */
    static final String AL_ENTREGAR = "f_auto,q_auto";
    /** Para el tablón: lado mayor de 600 px; la completa solo se descarga al ampliar. */
    static final String MINIATURA = "c_limit,h_600,w_600/" + AL_ENTREGAR;
    private static final String FORMATOS = "jpg,jpeg,png,webp,heic";
    private static final Logger log = LoggerFactory.getLogger(CloudinaryAlmacenamiento.class);

    private final String apiKey;
    private final String apiSecret;
    private final String cloudName;
    private final Clock reloj;
    private final RestClient http = RestClient.create();

    /** @param cloudinaryUrl la "API environment variable" de Cloudinary; vacía = imágenes deshabilitadas */
    public CloudinaryAlmacenamiento(@Value("${app.cloudinary.url:}") String cloudinaryUrl, Clock reloj) {
        this.reloj = reloj;
        if (cloudinaryUrl.isBlank()) {
            apiKey = apiSecret = cloudName = null;
            return;
        }
        Matcher m = URL.matcher(cloudinaryUrl.trim());
        if (!m.matches()) {
            // Sin mostrar el valor: contiene el secreto.
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
            return; // sin Cloudinary no se subió ninguna imagen
        }
        // Si la transacción se deshace, la sala sigue existiendo y sus imágenes también.
        TrasElCommit.ejecutar(() -> {
            try {
                // La Admin API borra hasta 1000 por llamada y responde partial=true si quedaron más.
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
                // shortcut: si falla, las imágenes quedan huérfanas; agregar reintentos si pasa seguido.
                log.error("No se pudieron borrar las imágenes de {}", carpeta, e);
            }
        });
    }

    /** Firma de una petición a la API: parámetros ordenados, "k=v" unidos con "&", más el secreto, en SHA-1. */
    static String firmaDeParametros(Map<String, String> parametros, String secreto) {
        String aFirmar = new TreeMap<>(parametros).entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
        return HexFormat.of().formatHex(sha1(aFirmar + secreto));
    }

    /** Firma de una URL de entrega: los primeros 8 caracteres del SHA-1 en Base64 para URL. */
    static String firmaDeUrl(String ruta, String secreto) {
        return Base64.getUrlEncoder().encodeToString(sha1(ruta + secreto)).substring(0, 8);
    }

    private static byte[] sha1(String texto) {
        try {
            return MessageDigest.getInstance("SHA-1").digest(texto.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e); // SHA-1 existe en toda JVM
        }
    }

    private void exigirConfigurado() {
        if (cloudName == null) {
            throw new ServicioNoDisponibleException("Las imágenes no están configuradas (falta CLOUDINARY_URL)");
        }
    }
}
