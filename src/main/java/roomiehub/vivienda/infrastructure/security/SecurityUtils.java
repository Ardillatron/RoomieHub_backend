package roomiehub.vivienda.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class SecurityUtils {

    private SecurityUtils() {}

    /** El usuario_id del JWT de la petición actual, o nulo si no hay JWT (por ejemplo, el scheduler). */
    public static Long usuarioActualIdONulo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth instanceof JwtAuthenticationToken jwt ? Long.valueOf(jwt.getToken().getSubject()) : null;
    }
}
