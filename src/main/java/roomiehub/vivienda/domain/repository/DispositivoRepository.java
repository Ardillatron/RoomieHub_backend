package roomiehub.vivienda.domain.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface DispositivoRepository {

    /** El token es único: si ya estaba en otra cuenta (otro login en el mismo teléfono), pasa a esta. */
    void registrar(long usuarioId, String fcmToken, String plataforma, Instant ahora);

    /** RN-37: los tokens de push de una cuenta eliminada se borran. */
    void borrarDeUsuario(long usuarioId);

    /** Sección 10: los tokens de push de esos usuarios, uno por teléfono. */
    List<String> tokensDe(Collection<Long> usuarios);

    /** Se cerró sesión en ese teléfono, o FCM avisó que el token ya no existe (app desinstalada). */
    void borrarToken(String fcmToken);
}
