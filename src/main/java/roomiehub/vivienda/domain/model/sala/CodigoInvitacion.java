package roomiehub.vivienda.domain.model.sala;

import java.util.Locale;
import java.util.random.RandomGenerator;

import roomiehub.vivienda.domain.exception.DatoInvalidoException;

/**
 * RN-05: código fijo de la sala; el QR codifica este mismo texto. 8 caracteres sin los que se
 * confunden al dictarlos (0/O, 1/I/L): 32^8 ≈ 10^12 combinaciones.
 */
public record CodigoInvitacion(String valor) {

    private static final String ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int LONGITUD = 8;

    public CodigoInvitacion {
        if (valor == null || valor.length() != LONGITUD || !valor.chars().allMatch(c -> ALFABETO.indexOf(c) >= 0)) {
            throw new DatoInvalidoException("Código de invitación inválido");
        }
    }

    /** Acepta lo que escribe la persona: minúsculas y espacios alrededor. */
    public static CodigoInvitacion de(String texto) {
        return new CodigoInvitacion(texto == null ? null : texto.trim().toUpperCase(Locale.ROOT));
    }

    public static CodigoInvitacion generar(RandomGenerator azar) {
        StringBuilder sb = new StringBuilder(LONGITUD);
        for (int i = 0; i < LONGITUD; i++) {
            sb.append(ALFABETO.charAt(azar.nextInt(ALFABETO.length())));
        }
        return new CodigoInvitacion(sb.toString());
    }
}
