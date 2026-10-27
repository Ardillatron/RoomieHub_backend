package roomiehub.vivienda.domain.model.convivencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import roomiehub.vivienda.domain.exception.DatoInvalidoException;
import roomiehub.vivienda.domain.exception.RecursoNoEncontradoException;

class MensajeChatTest {

    static final Instant AHORA = Instant.parse("2026-11-01T12:00:00Z");

    @Test
    void textoOImagen() {
        assertEquals("Hola", MensajeChat.nuevo(1, 2, "  Hola ", null, null, null, AHORA).texto());
        assertNull(MensajeChat.nuevo(1, 2, "  ", "roomiehub/salas/1/x", null, null, AHORA).texto(), "solo imagen");
        assertThrows(DatoInvalidoException.class, () -> MensajeChat.nuevo(1, 2, " ", null, null, null, AHORA));
        assertThrows(DatoInvalidoException.class,
                () -> MensajeChat.nuevo(1, 2, "x".repeat(4001), null, null, null, AHORA));
    }

    @Test
    void unMensajeEliminadoYaNoAdmiteNada() {
        MensajeChat m = MensajeChat.nuevo(1, 2, "Hola", null, null, null, AHORA);
        m.eliminar();
        assertThrows(RecursoNoEncontradoException.class, m::exigirVisible);
        assertThrows(RecursoNoEncontradoException.class, m::eliminar);
    }

    @Test
    void unReporteDeMensaje() {
        Reporte r = Reporte.deMensaje(1, 9, 2, "Spam", AHORA);
        assertEquals(9L, r.mensajeId());
        assertNull(r.notaId());
    }
}
