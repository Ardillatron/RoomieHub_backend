package roomiehub.vivienda.domain.model.sala;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import roomiehub.vivienda.domain.exception.CapacidadExcedidaException;
import roomiehub.vivienda.domain.exception.ConflictoDeEstadoException;
import roomiehub.vivienda.domain.exception.DatoInvalidoException;
import roomiehub.vivienda.domain.exception.LimiteMembresiasException;
import roomiehub.vivienda.domain.exception.OperacionNoPermitidaException;
import roomiehub.vivienda.domain.service.ReglasDeMembresia;

class SalaDominioTest {

    static final Instant AHORA = Instant.parse("2026-11-01T12:00:00Z");

    static Sala sala() {
        return Sala.nueva("  Depa Centro ", 1, "America/Mexico_City", CodigoInvitacion.de("ABCD2345"));
    }

    @Test
    void codigoDeInvitacion() {
        assertEquals("ABCD2345", CodigoInvitacion.de(" abcd2345 ").valor());
        assertThrows(DatoInvalidoException.class, () -> CodigoInvitacion.de("ABCD234O"), "sin O ni 0");
        assertThrows(DatoInvalidoException.class, () -> CodigoInvitacion.de("ABC"));
        assertThrows(DatoInvalidoException.class, () -> CodigoInvitacion.de(null));
        String generado = CodigoInvitacion.generar(new SplittableRandom(1)).valor();
        assertEquals(8, generado.length());
        assertEquals(generado, CodigoInvitacion.de(generado).valor(), "lo generado siempre es válido");
    }

    @Test
    void salaNuevaValidaNombreYZona() {
        assertEquals("Depa Centro", sala().nombre());
        assertEquals(EstadoSala.ACTIVA, sala().estado());
        assertThrows(DatoInvalidoException.class,
                () -> Sala.nueva("Depa", 1, "Marte/Olympus", CodigoInvitacion.de("ABCD2345")));
        assertThrows(DatoInvalidoException.class,
                () -> Sala.nueva(" ", 1, "America/Mexico_City", CodigoInvitacion.de("ABCD2345")));
    }

    @Test
    void desactivarYReactivarDentroDelPlazo() {
        Sala s = sala();
        s.desactivar(AHORA);
        assertEquals(AHORA, s.desactivadaEn());
        assertThrows(OperacionNoPermitidaException.class, s::exigirActiva, "solo lectura");
        assertThrows(ConflictoDeEstadoException.class, () -> s.desactivar(AHORA));

        s.reactivar(AHORA.plus(Duration.ofDays(14)));
        assertEquals(EstadoSala.ACTIVA, s.estado());
        assertNull(s.desactivadaEn());
        s.exigirActiva();
        assertThrows(ConflictoDeEstadoException.class, () -> s.reactivar(AHORA));
    }

    @Test
    void pasados15DiasYaNoSeReactiva() {
        Sala s = sala();
        s.desactivar(AHORA);
        assertThrows(ConflictoDeEstadoException.class, () -> s.reactivar(AHORA.plus(Duration.ofDays(15))));
    }

    @Test
    void capacidadYLimiteDeMembresias() {
        ReglasDeMembresia.validarIngreso(5, 1);
        assertThrows(CapacidadExcedidaException.class, () -> ReglasDeMembresia.validarIngreso(6, 0));
        assertThrows(LimiteMembresiasException.class, () -> ReglasDeMembresia.validarIngreso(1, 2));
        assertThrows(LimiteMembresiasException.class, () -> ReglasDeMembresia.validarLimiteDeMembresias(2));
    }

    @Test
    void soloElAdministradorActivoHaceAccionesAdministrativas() {
        Membresia admin = new Membresia(1L, 10, 1, Rol.ADMIN, true, AHORA);
        Membresia habitante = new Membresia(2L, 20, 1, Rol.HABITANTE, true, AHORA);
        assertEquals(admin, ReglasDeMembresia.exigirAdministrador(List.of(admin, habitante), 10));
        assertThrows(OperacionNoPermitidaException.class,
                () -> ReglasDeMembresia.exigirAdministrador(List.of(admin, habitante), 20));
    }

    @Test
    void quienVuelveEntraComoHabitanteConAntiguedadNueva() {
        Membresia m = new Membresia(1L, 10, 1, Rol.ADMIN, false, AHORA);
        Instant despues = AHORA.plus(Duration.ofDays(30));
        m.reactivar(despues);
        assertTrue(m.activo());
        assertEquals(Rol.HABITANTE, m.rol());
        assertEquals(despues, m.unidoEn());
    }
}
