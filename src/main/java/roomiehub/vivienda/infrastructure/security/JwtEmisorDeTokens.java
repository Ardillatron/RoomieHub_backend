package roomiehub.vivienda.infrastructure.security;

import java.time.Instant;

import javax.crypto.SecretKey;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import roomiehub.vivienda.application.port.EmisorDeTokensPort;

@Component
public class JwtEmisorDeTokens implements EmisorDeTokensPort {

    private final JwtEncoder encoder;

    public JwtEmisorDeTokens(SecretKey llaveJwt) {
        this.encoder = NimbusJwtEncoder.withSecretKey(llaveJwt).build();
    }

    @Override
    public String emitirAcceso(long usuarioId, Instant emitidoEn, Instant expiraEn) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(SecurityConfig.EMISOR)
                .subject(Long.toString(usuarioId))
                .issuedAt(emitidoEn)
                .expiresAt(expiraEn)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
