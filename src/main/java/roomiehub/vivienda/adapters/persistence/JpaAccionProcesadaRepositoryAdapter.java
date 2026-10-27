package roomiehub.vivienda.adapters.persistence;

import java.util.UUID;

import org.springframework.stereotype.Component;

import roomiehub.vivienda.domain.repository.AccionProcesadaRepository;
import roomiehub.vivienda.infrastructure.persistence.AccionProcesadaJpaRepository;

@Component
public class JpaAccionProcesadaRepositoryAdapter implements AccionProcesadaRepository {

    private final AccionProcesadaJpaRepository jpa;

    public JpaAccionProcesadaRepositoryAdapter(AccionProcesadaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean existe(UUID uuid) {
        return jpa.existsById(uuid);
    }

    @Override
    public void registrar(UUID uuid, long usuarioId, long salaId) {
        jpa.registrar(uuid, usuarioId, salaId);
    }
}
