package roomiehub.vivienda.application.usecase.cuenta;

import java.time.Clock;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.domain.exception.DatoInvalidoException;
import roomiehub.vivienda.domain.repository.DispositivoRepository;

/** Sección 10: la app manda su token de FCM al iniciar sesión y cada vez que FCM lo renueva. */
public class RegistrarDispositivo {

    private static final Set<String> PLATAFORMAS = Set.of("ANDROID", "IOS");
    // Los tokens de FCM rondan los 160 caracteres; el tope solo evita guardar basura enorme.
    private static final int LARGO_MAXIMO = 4096;

    private final DispositivoRepository dispositivos;
    private final Clock reloj;

    public RegistrarDispositivo(DispositivoRepository dispositivos, Clock reloj) {
        this.dispositivos = dispositivos;
        this.reloj = reloj;
    }

    @Transactional
    public void ejecutar(long usuarioId, String fcmToken, String plataforma) {
        if (fcmToken == null || fcmToken.isBlank() || fcmToken.length() > LARGO_MAXIMO) {
            throw new DatoInvalidoException("Token de dispositivo inválido");
        }
        if (plataforma == null || !PLATAFORMAS.contains(plataforma)) {
            throw new DatoInvalidoException("La plataforma debe ser ANDROID o IOS");
        }
        dispositivos.registrar(usuarioId, fcmToken.strip(), plataforma, reloj.instant());
    }
}
