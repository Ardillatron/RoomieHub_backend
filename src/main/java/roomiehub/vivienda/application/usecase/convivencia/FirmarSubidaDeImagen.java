package roomiehub.vivienda.application.usecase.convivencia;

import java.util.regex.Pattern;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.port.AlmacenamientoImagenesPort;
import roomiehub.vivienda.application.port.AlmacenamientoImagenesPort.FirmaDeSubida;
import roomiehub.vivienda.application.usecase.sala.AccesoASala;
import roomiehub.vivienda.domain.exception.DatoInvalidoException;

/* La nota o mensaje con imagen primero sube la imagen y luego se envia con su id
   cada sala tiene su carpeta y una imagen solo sirve en su sala */
public class FirmarSubidaDeImagen {

    private static final Pattern UUID = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    private final AccesoASala acceso;
    private final AlmacenamientoImagenesPort almacenamiento;

    public FirmarSubidaDeImagen(AccesoASala acceso, AlmacenamientoImagenesPort almacenamiento) {
        this.acceso = acceso;
        this.almacenamiento = almacenamiento;
    }

    @Transactional(readOnly = true)
    public FirmaDeSubida ejecutar(long usuarioId, long salaId) {
        acceso.exigirMiembroDeSalaActiva(salaId, usuarioId);
        return almacenamiento.firmarSubida(carpeta(salaId));
    }

    // Rechaza ids inventados o firmados para otra sala
    public static void exigirImagenDeLaSala(String imagenId, long salaId) {
        String prefijo = carpeta(salaId) + "/";
        if (!imagenId.startsWith(prefijo) || !UUID.matcher(imagenId.substring(prefijo.length())).matches()) {
            throw new DatoInvalidoException("La imagen no corresponde a esta sala");
        }
    }

    public static String carpeta(long salaId) {
        return "roomiehub/salas/" + salaId;
    }
}
