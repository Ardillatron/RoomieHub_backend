package roomiehub.vivienda.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface AccionProcesadaJpaRepository extends JpaRepository<AccionProcesadaJpaEntity, UUID> {

    // INSERT directo: con un id asignado, save() haría un SELECT antes, y un repetido debe fallar por la llave.
    @Modifying
    @Query(value = "INSERT INTO acciones_procesadas (client_uuid, usuario_id, sala_id) VALUES (:uuid, :usuarioId, :salaId)",
            nativeQuery = true)
    int registrar(UUID uuid, long usuarioId, long salaId);
}
