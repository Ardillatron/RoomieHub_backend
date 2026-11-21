package roomiehub.vivienda.application.usecase.convivencia;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.port.AlmacenamientoImagenesPort;
import roomiehub.vivienda.application.port.NotificadorPort;
import roomiehub.vivienda.application.usecase.convivencia.ConsultarTablon.NotaVista;
import roomiehub.vivienda.application.usecase.sala.AccesoASala;
import roomiehub.vivienda.domain.exception.ConflictoDeEstadoException;
import roomiehub.vivienda.domain.model.convivencia.Nota;
import roomiehub.vivienda.domain.model.sala.Membresia;
import roomiehub.vivienda.domain.repository.NotaRepository;

/* CU-09 crea una nota con imagen opcional ya subida
   con el UUID de la app repetir la accion regresa la nota ya creada */
public class CrearNota {

    private final AccesoASala acceso;
    private final NotaRepository notas;
    private final AlmacenamientoImagenesPort almacenamiento;
    private final NotificadorPort notificador;
    private final Clock reloj;

    public CrearNota(AccesoASala acceso, NotaRepository notas, AlmacenamientoImagenesPort almacenamiento,
                     NotificadorPort notificador, Clock reloj) {
        this.acceso = acceso;
        this.notas = notas;
        this.almacenamiento = almacenamiento;
        this.notificador = notificador;
        this.reloj = reloj;
    }

    @Transactional
    public NotaVista ejecutar(long usuarioId, long salaId, String texto, String imagenId, UUID clientUuid) {
        Optional<Nota> yaCreada = clientUuid == null ? Optional.empty() : notas.buscarPorClientUuid(clientUuid);
        if (yaCreada.isPresent()) {
            if (yaCreada.get().autorId() != usuarioId || yaCreada.get().salaId() != salaId) {
                throw new ConflictoDeEstadoException("Ese identificador de acción ya se usó");
            }
            return vista(yaCreada.get());
        }
        acceso.exigirMiembroDeSalaActiva(salaId, usuarioId);
        if (imagenId != null) {
            FirmarSubidaDeImagen.exigirImagenDeLaSala(imagenId, salaId);
        }

        Nota nota = notas.guardar(Nota.nueva(salaId, usuarioId, texto, imagenId, clientUuid, reloj.instant()));
        List<Long> otros = acceso.exigirMiembro(salaId, usuarioId).stream()
                .map(Membresia::usuarioId).filter(id -> id != usuarioId).toList();
        notificador.notificar(otros, "Nueva nota en el tablón", nota.texto().isEmpty() ? "Imagen" : nota.texto());
        return vista(nota);
    }

    private NotaVista vista(Nota nota) {
        return NotaVista.de(nota, null, 0, 0, null, false, almacenamiento);
    }
}
