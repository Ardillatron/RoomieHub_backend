package roomiehub.vivienda.application.usecase.sala;

import java.time.Instant;
import java.util.List;

import roomiehub.vivienda.domain.model.sala.Rol;
import roomiehub.vivienda.domain.model.sala.Sala;

/** Una sala vista por uno de sus miembros; {@code miembros} va vacío en los listados. */
public record SalaDeUsuario(Sala sala, Rol miRol, List<Miembro> miembros) {

    public record Miembro(long usuarioId, String nombre, String fotoUrl, Rol rol, Instant unidoEn) {}
}
