package roomiehub.vivienda.adapters.web;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ventana fija por clave: como mucho {@code maximo} eventos cada {@code ventana}.
 * shortcut: vive en la memoria de una sola instancia; con varias instancias, pasar a un almacén compartido.
 */
public class LimitadorDeIntentos {

    private record Cuenta(Instant inicio, int eventos) {}

    private static final int LIMPIAR_DESDE = 10_000;

    private final ConcurrentHashMap<String, Cuenta> cuentas = new ConcurrentHashMap<>();
    private final int maximo;
    private final Duration ventana;
    private final Clock reloj;

    public LimitadorDeIntentos(int maximo, Duration ventana, Clock reloj) {
        this.maximo = maximo;
        this.ventana = ventana;
        this.reloj = reloj;
    }

    /** Registra un evento. @return segundos que faltan para poder reintentar, o 0 si se permitió. */
    public long registrar(String clave) {
        Instant ahora = reloj.instant();
        if (cuentas.size() > LIMPIAR_DESDE) {
            cuentas.values().removeIf(c -> vencida(c, ahora));
        }
        Cuenta cuenta = cuentas.compute(clave, (k, c) ->
                c == null || vencida(c, ahora) ? new Cuenta(ahora, 1) : new Cuenta(c.inicio(), c.eventos() + 1));
        return cuenta.eventos() > maximo ? espera(cuenta, ahora) : 0;
    }

    /** @return segundos que faltan si la clave ya agotó su ventana, o 0 si no. No registra nada. */
    public long bloqueadoPor(String clave) {
        Instant ahora = reloj.instant();
        Cuenta cuenta = cuentas.get(clave);
        return cuenta != null && !vencida(cuenta, ahora) && cuenta.eventos() >= maximo ? espera(cuenta, ahora) : 0;
    }

    public void olvidar(String clave) {
        cuentas.remove(clave);
    }

    private boolean vencida(Cuenta c, Instant ahora) {
        return !ahora.isBefore(c.inicio().plus(ventana));
    }

    private long espera(Cuenta c, Instant ahora) {
        return Math.max(1, Duration.between(ahora, c.inicio().plus(ventana)).toSeconds());
    }
}
