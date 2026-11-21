package roomiehub.vivienda.application.port;

import java.time.Instant;

// Firma el JWT de acceso y la duracion la decide la aplicacion
public interface EmisorDeTokensPort {

    String emitirAcceso(long usuarioId, Instant emitidoEn, Instant expiraEn);
}
