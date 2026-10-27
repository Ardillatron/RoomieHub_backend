package roomiehub.vivienda.adapters.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import roomiehub.vivienda.domain.repository.DispositivoRepository;
import roomiehub.vivienda.infrastructure.persistence.DispositivoJpaRepository;

@Component
public class JpaDispositivoRepositoryAdapter implements DispositivoRepository {

    private final DispositivoJpaRepository jpa;

    public JpaDispositivoRepositoryAdapter(DispositivoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void registrar(long usuarioId, String fcmToken, String plataforma, Instant ahora) {
        jpa.registrar(usuarioId, fcmToken, plataforma, ahora);
    }

    @Override
    public void borrarDeUsuario(long usuarioId) {
        jpa.borrarDeUsuario(usuarioId);
    }

    @Override
    public List<String> tokensDe(Collection<Long> usuarios) {
        return jpa.tokensDe(usuarios);
    }

    @Override
    public void borrarToken(String fcmToken) {
        jpa.borrarToken(fcmToken);
    }
}
