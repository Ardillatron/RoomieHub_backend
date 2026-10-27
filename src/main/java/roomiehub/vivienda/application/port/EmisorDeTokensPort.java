package roomiehub.vivienda.application.port;

import java.time.Instant;

/** Firma el JWT de acceso. La duración la decide la Aplicación, no la infraestructura. */
public interface EmisorDeTokensPort {

    String emitirAcceso(long usuarioId, Instant emitidoEn, Instant expiraEn);
}
