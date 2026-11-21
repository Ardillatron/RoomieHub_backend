package roomiehub.vivienda.infrastructure.security;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import roomiehub.vivienda.application.port.VerificadorGooglePort;
import roomiehub.vivienda.domain.exception.CredencialesInvalidasException;

// Valida el token de Google Sign In con las llaves publicas de Google
@Component
public class GoogleIdTokenVerificador implements VerificadorGooglePort {

    private static final String LLAVES_GOOGLE = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> EMISORES = Set.of("accounts.google.com", "https://accounts.google.com");

    private final JwtDecoder decoder;

    // clientIds son los client ID de OAuth de la app separados por comas
    @Autowired
    public GoogleIdTokenVerificador(@Value("${app.google.client-ids:}") List<String> clientIds) {
        this(NimbusJwtDecoder.withJwkSetUri(LLAVES_GOOGLE).build(), clientIds);
    }

    // Para tests un decodificador con otras llaves y las mismas validaciones
    GoogleIdTokenVerificador(NimbusJwtDecoder decoder, List<String> clientIds) {
        List<String> audiencias = clientIds.stream().filter(id -> !id.isBlank()).toList();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                exigir(jwt -> EMISORES.contains(jwt.getClaimAsString("iss")), "emisor"),
                exigir(jwt -> jwt.getAudience() != null && jwt.getAudience().stream().anyMatch(audiencias::contains),
                        "audiencia")));
        this.decoder = decoder;
    }

    @Override
    public IdentidadGoogle verificar(String idToken) {
        try {
            Jwt jwt = decoder.decode(idToken);
            return new IdentidadGoogle(jwt.getSubject(), jwt.getClaimAsString("email"),
                    Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")),
                    jwt.getClaimAsString("name"), jwt.getClaimAsString("picture"));
        } catch (JwtException e) {
            throw new CredencialesInvalidasException();
        }
    }

    private static OAuth2TokenValidator<Jwt> exigir(Predicate<Jwt> condicion, String que) {
        return jwt -> condicion.test(jwt)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Token de Google: " + que, null));
    }
}
