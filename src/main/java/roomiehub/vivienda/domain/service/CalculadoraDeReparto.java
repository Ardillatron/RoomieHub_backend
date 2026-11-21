package roomiehub.vivienda.domain.service;

import java.util.ArrayList;
import java.util.List;

import roomiehub.vivienda.domain.exception.DatoInvalidoException;
import roomiehub.vivienda.domain.model.finanzas.Deuda;
import roomiehub.vivienda.domain.model.finanzas.Dinero;

/* Reparte en centavos por partes iguales
   los centavos que sobran los reciben los participantes desde la posicion k mod N */
public final class CalculadoraDeReparto {

    private CalculadoraDeReparto() {}

    // Regresa el monto de cada participante en el mismo orden
    public static List<Dinero> repartir(Dinero total, int participantes, long numeroPeriodo) {
        if (participantes < 1) {
            throw new DatoInvalidoException("Un gasto necesita al menos un participante");
        }
        long centavos = total.centavos();
        long base = centavos / participantes;
        long sobrantes = centavos % participantes;
        long primero = Math.floorMod(numeroPeriodo, participantes);

        List<Dinero> montos = new ArrayList<>(participantes);
        for (int i = 0; i < participantes; i++) {
            long extra = Math.floorMod(i - primero, participantes) < sobrantes ? 1 : 0;
            montos.add(Dinero.deCentavos(base + extra, total.moneda()));
        }
        return montos;
    }

    /* La deuda pendiente de quien sale pasa a Repartida y su monto se divide entre los que quedan
       como deudas nuevas que apuntan a la original y si no queda nadie no genera deudas
       restantes son los participantes activos que quedan en el orden del gasto */
    public static List<Deuda> repartirDeuda(Deuda original, List<Long> restantes, long numeroPeriodo) {
        original.marcarRepartida();
        if (restantes.isEmpty()) {
            return List.of();
        }
        List<Dinero> partes = repartir(original.monto(), restantes.size(), numeroPeriodo);
        List<Deuda> nuevas = new ArrayList<>(restantes.size());
        for (int i = 0; i < restantes.size(); i++) {
            nuevas.add(Deuda.nueva(original.servicioId(), original.salaId(), original.periodo(), restantes.get(i),
                    partes.get(i), original.id()));
        }
        return nuevas;
    }
}
