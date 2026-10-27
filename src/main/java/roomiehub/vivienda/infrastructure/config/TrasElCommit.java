package roomiehub.vivienda.infrastructure.config;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Para los efectos fuera de la base (WebSocket, push, Cloudinary): si la transacción se deshace, no deben
 * ocurrir. Sin transacción en curso, la acción corre de inmediato.
 */
public final class TrasElCommit {

    private TrasElCommit() {}

    public static void ejecutar(Runnable accion) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            accion.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                accion.run();
            }
        });
    }
}
