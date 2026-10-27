package roomiehub.vivienda.application.usecase.sala;

import java.time.Clock;

import org.springframework.transaction.annotation.Transactional;

/** CU-05. También se permite en una sala desactivada, para no quedar atado al límite de RN-02. */
public class AbandonarSala {

    private final SalidaDeMiembro salida;
    private final Clock reloj;

    public AbandonarSala(SalidaDeMiembro salida, Clock reloj) {
        this.salida = salida;
        this.reloj = reloj;
    }

    @Transactional
    public void ejecutar(long usuarioId, long salaId) {
        salida.ejecutar(salaId, usuarioId, reloj.instant());
    }
}
