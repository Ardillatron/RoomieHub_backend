package roomiehub.vivienda.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import roomiehub.vivienda.application.port.VerificadorGooglePort.IdentidadGoogle;
import roomiehub.vivienda.domain.exception.CredencialesInvalidasException;

/** Las mismas validaciones que con Google, pero con una llave RSA local en lugar de las de Google. */
class GoogleIdTokenVerificadorTest {

    static final String CLIENT_ID = "app-android.apps.googleusercontent.com";

    final KeyPair llaves = generarLlaves();
    final NimbusJwtEncoder firmante = NimbusJwtEncoder
            .withKeyPair((RSAPublicKey) llaves.getPublic(), (RSAPrivateKey) llaves.getPrivate()).build();
    final GoogleIdTokenVerificador verificador = new GoogleIdTokenVerificador(
            NimbusJwtDecoder.withPublicKey((RSAPublicKey) llaves.getPublic()).build(), List.of(CLIENT_ID, ""));

    @Test
    void tokenValidoDevuelveLaIdentidad() {
        IdentidadGoogle id = verificador.verificar(token(c -> {}));
        assertEquals("sub-123", id.sub());
        assertEquals("ana@gmail.com", id.email());
        assertTrue(id.emailVerificado());
        assertEquals("Ana", id.nombre());
    }

    @Test
    void aceptaElEmisorSinHttps() {
        assertEquals("sub-123", verificador.verificar(token(c -> c.issuer("accounts.google.com"))).sub());
    }

    @Test
    void correoSinVerificarSeReporta() {
        assertFalse(verificador.verificar(token(c -> c.claim("email_verified", false))).emailVerificado());
    }

    @Test
    void rechazaOtraAudienciaOtroEmisorTokenVencidoYBasura() {
        assertThrows(CredencialesInvalidasException.class,
                () -> verificador.verificar(token(c -> c.audience(List.of("otra-app")))));
        assertThrows(CredencialesInvalidasException.class,
                () -> verificador.verificar(token(c -> c.issuer("https://evil.example.com"))));
        assertThrows(CredencialesInvalidasException.class, () -> verificador.verificar(token(c -> c
                .issuedAt(Instant.now().minusSeconds(7200)).expiresAt(Instant.now().minusSeconds(3600)))));
        assertThrows(CredencialesInvalidasException.class, () -> verificador.verificar("no.es.jwt"));
    }

    @Test
    void sinClientIdsConfiguradosNadaPasa() {
        var sinConfigurar = new GoogleIdTokenVerificador(
                NimbusJwtDecoder.withPublicKey((RSAPublicKey) llaves.getPublic()).build(), List.of(""));
        assertThrows(CredencialesInvalidasException.class, () -> sinConfigurar.verificar(token(c -> {})));
    }

    String token(Consumer<JwtClaimsSet.Builder> ajuste) {
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("https://accounts.google.com")
                .audience(List.of(CLIENT_ID))
                .subject("sub-123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("email", "ana@gmail.com")
                .claim("email_verified", true)
                .claim("name", "Ana");
        ajuste.accept(claims);
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return firmante.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }

    static KeyPair generarLlaves() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            return gen.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
