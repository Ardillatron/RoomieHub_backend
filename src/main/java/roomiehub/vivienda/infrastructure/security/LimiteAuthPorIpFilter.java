package roomiehub.vivienda.infrastructure.security;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import roomiehub.vivienda.adapters.web.LimitadorDeIntentos;

/* Frena el abuso de /auth desde una misma IP como adivinar contraseñas o registros masivos
   detras del proxy de Render la IP real llega por X Forwarded For */
@Component
public class LimiteAuthPorIpFilter extends OncePerRequestFilter {

    private final LimitadorDeIntentos porIp;

    public LimiteAuthPorIpFilter(@Value("${app.limite.auth-por-ip-por-minuto:30}") int maximo, Clock reloj) {
        this.porIp = new LimitadorDeIntentos(maximo, Duration.ofMinutes(1), reloj);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long espera = porIp.registrar(request.getRemoteAddr());
        if (espera > 0) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", Long.toString(espera));
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write(
                    "{\"status\":429,\"title\":\"Too Many Requests\",\"detail\":\"Demasiados intentos; espera un momento\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
