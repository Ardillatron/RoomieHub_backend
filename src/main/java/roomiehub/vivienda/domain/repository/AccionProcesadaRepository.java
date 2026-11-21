package roomiehub.vivienda.domain.repository;

import java.util.UUID;

// Registro de acciones offline ya procesadas
public interface AccionProcesadaRepository {

    boolean existe(UUID uuid);

    void registrar(UUID uuid, long usuarioId, long salaId);
}
