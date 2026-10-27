package roomiehub.vivienda.infrastructure.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface DispositivoJpaRepository extends JpaRepository<DispositivoJpaEntity, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO dispositivos_usuario (usuario_id, fcm_token, plataforma, actualizado_en)
            VALUES (:usuarioId, :fcmToken, :plataforma, :ahora)
            ON CONFLICT (fcm_token) DO UPDATE
               SET usuario_id = EXCLUDED.usuario_id, plataforma = EXCLUDED.plataforma,
                   actualizado_en = EXCLUDED.actualizado_en
            """, nativeQuery = true)
    int registrar(long usuarioId, String fcmToken, String plataforma, Instant ahora);

    @Modifying
    @Query("DELETE FROM DispositivoJpaEntity d WHERE d.usuarioId = :usuarioId")
    int borrarDeUsuario(long usuarioId);

    @Query("SELECT d.fcmToken FROM DispositivoJpaEntity d WHERE d.usuarioId IN :usuarios")
    List<String> tokensDe(Collection<Long> usuarios);

    // Con su propia transacción: el notificador lo llama desde su hilo, fuera de la del caso de uso.
    @Transactional
    @Modifying
    @Query("DELETE FROM DispositivoJpaEntity d WHERE d.fcmToken = :fcmToken")
    int borrarToken(String fcmToken);
}
