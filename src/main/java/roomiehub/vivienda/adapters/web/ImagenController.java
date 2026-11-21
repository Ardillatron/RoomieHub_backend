package roomiehub.vivienda.adapters.web;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import roomiehub.vivienda.application.port.AlmacenamientoImagenesPort.FirmaDeSubida;
import roomiehub.vivienda.application.usecase.convivencia.FirmarSubidaDeImagen;

/* La app pide la firma sube la imagen directo a urlDeSubida
   y luego crea la nota o el mensaje con imagenId */
@RestController
public class ImagenController {

    public record FirmaResponse(String urlDeSubida, String imagenId, Map<String, String> parametros) {}

    private final FirmarSubidaDeImagen firmarSubida;

    public ImagenController(FirmarSubidaDeImagen firmarSubida) {
        this.firmarSubida = firmarSubida;
    }

    @PostMapping("/salas/{salaId}/imagenes/firma")
    public FirmaResponse firmar(@AuthenticationPrincipal Jwt jwt, @PathVariable long salaId) {
        FirmaDeSubida f = firmarSubida.ejecutar(Long.parseLong(jwt.getSubject()), salaId);
        return new FirmaResponse(f.urlDeSubida(), f.imagenId(), f.parametros());
    }
}
