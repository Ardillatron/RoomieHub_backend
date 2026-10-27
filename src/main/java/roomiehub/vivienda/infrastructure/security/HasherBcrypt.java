package roomiehub.vivienda.infrastructure.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import roomiehub.vivienda.application.port.HasherDeContrasenasPort;

@Component
public class HasherBcrypt implements HasherDeContrasenasPort {

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Override
    public String hash(String contrasena) {
        return bcrypt.encode(contrasena);
    }

    @Override
    public boolean coincide(String contrasena, String hash) {
        return contrasena != null && bcrypt.matches(contrasena, hash);
    }
}
