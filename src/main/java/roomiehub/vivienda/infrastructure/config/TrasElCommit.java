package roomiehub.vivienda.infrastructure.config;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/* Para efectos fuera de la base como WebSocket push o Cloudinary
   si la transaccion se deshace no deben pasar y sin transaccion corre de inmediato */
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
