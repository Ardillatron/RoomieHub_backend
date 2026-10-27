package roomiehub.vivienda.domain.service;

import java.util.List;

import roomiehub.vivienda.domain.exception.CapacidadExcedidaException;
import roomiehub.vivienda.domain.exception.LimiteMembresiasException;
import roomiehub.vivienda.domain.exception.OperacionNoPermitidaException;
import roomiehub.vivienda.domain.model.sala.Membresia;
import roomiehub.vivienda.domain.model.sala.Rol;

public final class ReglasDeMembresia {

    public static final int CAPACIDAD_MAXIMA = 6;     // RN-01
    public static final int MEMBRESIAS_MAXIMAS = 2;   // RN-02

    private ReglasDeMembresia() {}

    /** RN-02, al crear o al unirse. Una sala desactivada sigue contando mientras exista (RN-09). */
    public static void validarLimiteDeMembresias(int membresiasActivasDelUsuario) {
        if (membresiasActivasDelUsuario >= MEMBRESIAS_MAXIMAS) {
            throw new LimiteMembresiasException();
        }
    }

    /** RN-01 y RN-02, al unirse. */
    public static void validarIngreso(int miembrosActivosDeLaSala, int membresiasActivasDelUsuario) {
        if (miembrosActivosDeLaSala >= CAPACIDAD_MAXIMA) {
            throw new CapacidadExcedidaException();
        }
        validarLimiteDeMembresias(membresiasActivasDelUsuario);
    }

    /** Sección 3: las acciones administrativas solo las hace el Administrador activo. */
    public static Membresia exigirAdministrador(List<Membresia> activas, long usuarioId) {
        return activas.stream()
                .filter(m -> m.usuarioId() == usuarioId && m.rol() == Rol.ADMIN)
                .findFirst()
                .orElseThrow(() -> new OperacionNoPermitidaException("Solo el Administrador puede hacer esto"));
    }
}
