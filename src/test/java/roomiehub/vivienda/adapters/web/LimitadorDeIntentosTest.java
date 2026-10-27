package roomiehub.vivienda.adapters.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class LimitadorDeIntentosTest {

    Instant ahora = Instant.parse("2026-11-01T12:00:00Z");
    final Clock reloj = new Clock() {
        @Override public Instant instant() { return ahora; }
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId z) { return this; }
    };
    final LimitadorDeIntentos limitador = new LimitadorDeIntentos(3, Duration.ofMinutes(15), reloj);

    @Test
    void bloqueaAlPasarElMaximoHastaQueTerminaLaVentana() {
        assertEquals(0, limitador.registrar("ana"));
        assertEquals(0, limitador.registrar("ana"));
        assertEquals(0, limitador.bloqueadoPor("ana"));
        assertEquals(0, limitador.registrar("ana"));
        assertEquals(900, limitador.bloqueadoPor("ana"), "3 de 3: el siguiente intento ya no se permite");
        assertEquals(900, limitador.registrar("ana"));
        assertEquals(0, limitador.bloqueadoPor("beto"), "las claves son independientes");

        ahora = ahora.plus(Duration.ofMinutes(10));
        assertEquals(300, limitador.bloqueadoPor("ana"));
        ahora = ahora.plus(Duration.ofMinutes(5));
        assertEquals(0, limitador.bloqueadoPor("ana"), "la ventana terminó");
        assertEquals(0, limitador.registrar("ana"));
    }

    @Test
    void olvidarReiniciaLaCuenta() {
        for (int i = 0; i < 3; i++) limitador.registrar("ana");
        limitador.olvidar("ana");
        assertEquals(0, limitador.bloqueadoPor("ana"));
    }
}
