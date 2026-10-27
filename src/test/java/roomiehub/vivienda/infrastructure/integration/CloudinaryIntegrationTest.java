package roomiehub.vivienda.infrastructure.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.util.Map;
import java.util.Properties;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import roomiehub.vivienda.application.port.AlmacenamientoImagenesPort.FirmaDeSubida;

/**
 * Contra la cuenta real de Cloudinary (sube una imagen de prueba y la borra):
 * {@code ./mvnw test -Dcloudinary=true -Dtest=CloudinaryIntegrationTest}. Lee CLOUDINARY_URL de local.properties.
 */
@EnabledIfSystemProperty(named = "cloudinary", matches = "true")
class CloudinaryIntegrationTest {

    final CloudinaryAlmacenamiento almacenamiento = new CloudinaryAlmacenamiento(cloudinaryUrl(), Clock.systemUTC());
    final HttpClient cliente = HttpClient.newHttpClient();

    @Test
    void subirConFirmaVerConUrlFirmadaYBorrar() throws Exception {
        String carpeta = "roomiehub/pruebas/" + java.util.UUID.randomUUID();
        FirmaDeSubida firma = almacenamiento.firmarSubida(carpeta);
        Map<?, ?> subida = subir(firma, firma.parametros());
        assertEquals(firma.imagenId(), subida.get("public_id"));
        assertEquals("authenticated", subida.get("type"));
        assertEquals(1920, subida.get("width"), "la imagen de 3000 px se limita a 1920 al subir");

        try {
            String url = almacenamiento.urlDeEntrega(firma.imagenId());
            assertEquals(200, get(url), "la URL firmada entrega la imagen");
            assertEquals(200, get(almacenamiento.urlDeMiniatura(firma.imagenId())), "y su miniatura");
            assertEquals(401, get(url.replaceFirst("s--[^/]{8}--", "s--AAAAAAAA--")), "una firma alterada no");
            assertTrue(get("https://res.cloudinary.com/" + url.split("/")[3] + "/image/upload/" + firma.imagenId()) >= 400,
                    "sin firma tampoco");
        } finally {
            almacenamiento.borrarCarpeta(carpeta);
        }
    }

    @Test
    void cambiarUnParametroFirmadoInvalidaLaFirma() throws Exception {
        FirmaDeSubida firma = almacenamiento.firmarSubida("roomiehub/pruebas");
        Map<String, String> alterados = new java.util.TreeMap<>(firma.parametros());
        alterados.put("transformation", "c_limit,h_8000,w_8000");
        HttpStatusCode codigo = RestClient.create().post().uri(firma.urlDeSubida())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(formulario(alterados))
                .exchange((pet, resp) -> resp.getStatusCode());
        assertEquals(401, codigo.value());
    }

    Map<?, ?> subir(FirmaDeSubida firma, Map<String, String> parametros) throws Exception {
        return RestClient.create().post().uri(firma.urlDeSubida())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(formulario(parametros))
                .retrieve()
                .body(Map.class);
    }

    static MultiValueMap<String, Object> formulario(Map<String, String> parametros) throws Exception {
        MultiValueMap<String, Object> cuerpo = new LinkedMultiValueMap<>();
        parametros.forEach(cuerpo::add);
        BufferedImage imagen = new BufferedImage(3000, 40, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", png);
        cuerpo.add("file", new ByteArrayResource(png.toByteArray()) {
            @Override public String getFilename() { return "prueba.png"; }
        });
        return cuerpo;
    }

    int get(String url) throws Exception {
        return cliente.send(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.discarding())
                .statusCode();
    }

    static String cloudinaryUrl() {
        try (Reader r = new FileReader("local.properties")) {
            Properties p = new Properties();
            p.load(r);
            return p.getProperty("CLOUDINARY_URL", "");
        } catch (Exception e) {
            return "";
        }
    }
}
