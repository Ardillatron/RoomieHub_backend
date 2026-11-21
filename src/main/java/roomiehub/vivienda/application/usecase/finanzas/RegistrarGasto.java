package roomiehub.vivienda.application.usecase.finanzas;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.port.NotificadorPort;
import roomiehub.vivienda.application.usecase.sala.AccesoASala;
import roomiehub.vivienda.domain.exception.DatoInvalidoException;
import roomiehub.vivienda.domain.model.finanzas.Dinero;
import roomiehub.vivienda.domain.model.finanzas.FrecuenciaGasto;
import roomiehub.vivienda.domain.model.finanzas.GastoCompartido;
import roomiehub.vivienda.domain.model.finanzas.Moneda;
import roomiehub.vivienda.domain.model.sala.Membresia;
import roomiehub.vivienda.domain.model.sala.Sala;
import roomiehub.vivienda.domain.repository.GastoRepository;
import roomiehub.vivienda.domain.repository.MembresiaRepository;

// CU-20 crear y las deudas de los periodos ya iniciados se generan de inmediato
public class RegistrarGasto {

    // Con una fecha muy vieja se crearian muchas deudas de golpe
    private static final int MAXIMO_DIAS_ATRAS = 365;

    private final AccesoASala acceso;
    private final GastoRepository gastos;
    private final MembresiaRepository membresias;
    private final GeneradorDeDeudas generador;
    private final NotificadorPort notificador;
    private final Clock reloj;

    public RegistrarGasto(AccesoASala acceso, GastoRepository gastos, MembresiaRepository membresias,
                          GeneradorDeDeudas generador, NotificadorPort notificador, Clock reloj) {
        this.acceso = acceso;
        this.gastos = gastos;
        this.membresias = membresias;
        this.generador = generador;
        this.notificador = notificador;
        this.reloj = reloj;
    }

    @Transactional
    public GastoCompartido ejecutar(long adminId, long salaId, String concepto, BigDecimal monto, Moneda moneda,
                                    FrecuenciaGasto frecuencia, LocalDate fechaInicio, List<Long> participantes) {
        Sala sala = acceso.exigirAdministradorDeSalaActiva(salaId, adminId);
        Instant ahora = reloj.instant();
        if (fechaInicio.isBefore(LocalDate.ofInstant(ahora, sala.zonaHoraria()).minusDays(MAXIMO_DIAS_ATRAS))) {
            throw new DatoInvalidoException("La fecha de inicio no puede tener más de un año de antigüedad");
        }
        List<Long> validados = validarParticipantes(membresias.activasDeSala(salaId), participantes);

        GastoCompartido gasto = gastos.guardar(GastoCompartido.nuevo(salaId, concepto, new Dinero(monto, moneda),
                frecuencia, fechaInicio));
        gastos.definirParticipantes(gasto.id(), salaId, validados);
        generador.generar(gasto, sala.zonaHoraria(), ahora);

        notificador.notificar(validados.stream().filter(id -> id != adminId).toList(),
                "Nuevo gasto compartido", gasto.concepto());
        return gasto;
    }

    // Al menos uno sin repetidos y todos miembros activos y se conserva el orden
    static List<Long> validarParticipantes(List<Membresia> miembros, List<Long> participantes) {
        if (participantes == null || participantes.isEmpty()) {
            throw new DatoInvalidoException("Un gasto necesita al menos un participante");
        }
        if (new HashSet<>(participantes).size() != participantes.size()) {
            throw new DatoInvalidoException("Hay participantes repetidos");
        }
        Set<Long> ids = new HashSet<>(miembros.stream().map(Membresia::usuarioId).toList());
        if (!ids.containsAll(participantes)) {
            throw new DatoInvalidoException("Todos los participantes deben ser miembros activos de la sala");
        }
        return List.copyOf(participantes);
    }
}
