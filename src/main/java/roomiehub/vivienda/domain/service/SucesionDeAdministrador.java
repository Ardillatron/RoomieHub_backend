package roomiehub.vivienda.domain.service;

import java.util.Comparator;
import java.util.List;

import roomiehub.vivienda.domain.model.sala.Membresia;

/** RN-06: el rol pasa al miembro activo con mayor antigüedad (unido_en más viejo). */
public final class SucesionDeAdministrador {

    private SucesionDeAdministrador() {}

    public static Membresia sucesor(List<Membresia> candidatos) {
        return candidatos.stream()
                .filter(Membresia::activo)
                .min(Comparator.comparing(Membresia::unidoEn).thenComparing(Membresia::id))
                .orElseThrow(() -> new IllegalStateException("No hay miembros activos para suceder al Administrador"));
    }
}
