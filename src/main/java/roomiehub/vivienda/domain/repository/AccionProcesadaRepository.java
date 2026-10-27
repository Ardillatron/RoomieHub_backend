package roomiehub.vivienda.domain.repository;

import java.util.UUID;

/** RN-35: registro de idempotencia de la cola offline (acciones_procesadas). */
public interface AccionProcesadaRepository {

    boolean existe(UUID uuid);

    void registrar(UUID uuid, long usuarioId, long salaId);
}
