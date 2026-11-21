package roomiehub.vivienda.adapters.web;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/* Ventana fija por clave con un maximo de eventos por ventana
   shortcut vive en memoria de una sola instancia y con varias hay que usar un almacen compartido */
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

    // Registra un evento y regresa los segundos para reintentar o 0 si se permitio
    public long registrar(String clave) {
        Instant ahora = reloj.instant();
        if (cuentas.size() > LIMPIAR_DESDE) {
            cuentas.values().removeIf(c -> vencida(c, ahora));
        }
        Cuenta cuenta = cuentas.compute(clave, (k, c) ->
                c == null || vencida(c, ahora) ? new Cuenta(ahora, 1) : new Cuenta(c.inicio(), c.eventos() + 1));
        return cuenta.eventos() > maximo ? espera(cuenta, ahora) : 0;
    }

    // Regresa los segundos que faltan si la clave ya se lleno o 0 si no y no registra nada
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
