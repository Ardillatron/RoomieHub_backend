package roomiehub.vivienda.application.usecase.sala;

import java.time.Instant;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.port.AlmacenamientoImagenesPort;
import roomiehub.vivienda.application.usecase.convivencia.FirmarSubidaDeImagen;
import roomiehub.vivienda.domain.model.sala.EstadoSala;
import roomiehub.vivienda.domain.model.sala.Sala;
import roomiehub.vivienda.domain.repository.SalaRepository;

// CU-29 una sala desactivada se borra a los 15 dias con sus imagenes
public class BorrarSalaVencida {

    private final SalaRepository salas;
    private final AlmacenamientoImagenesPort almacenamiento;

    public BorrarSalaVencida(SalaRepository salas, AlmacenamientoImagenesPort almacenamiento) {
        this.salas = salas;
        this.almacenamiento = almacenamiento;
    }

    // Regresa true si la borro y ya no hay nada mas que procesar
    @Transactional
    public boolean ejecutar(long salaId, Instant ahora) {
        Sala sala = salas.buscarPorId(salaId).orElseThrow();
        if (sala.estado() != EstadoSala.DESACTIVADA
                || ahora.isBefore(sala.desactivadaEn().plus(Sala.PLAZO_REACTIVACION))) {
            return false;
        }
        salas.borrar(salaId);
        almacenamiento.borrarCarpeta(FirmarSubidaDeImagen.carpeta(salaId));
        return true;
    }
}
