package roomiehub.vivienda.domain.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface DispositivoRepository {

    // El token es unico y si estaba en otra cuenta pasa a esta
    void registrar(long usuarioId, String fcmToken, String plataforma, Instant ahora);

    // Se borran los tokens de push de una cuenta borrada
    void borrarDeUsuario(long usuarioId);

    // Los tokens de push de esos usuarios uno por telefono
    List<String> tokensDe(Collection<Long> usuarios);

    // Se cerro sesion en ese telefono o FCM dijo que el token ya no existe
    void borrarToken(String fcmToken);
}
