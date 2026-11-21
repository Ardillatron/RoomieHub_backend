package roomiehub.vivienda.application.usecase.convivencia;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.port.ChatEnVivoPort;
import roomiehub.vivienda.application.port.NotificadorPort;
import roomiehub.vivienda.application.usecase.sala.AccesoASala;
import roomiehub.vivienda.domain.exception.ConflictoDeEstadoException;
import roomiehub.vivienda.domain.exception.RecursoNoEncontradoException;
import roomiehub.vivienda.domain.model.convivencia.MensajeChat;
import roomiehub.vivienda.domain.model.convivencia.Nota;
import roomiehub.vivienda.domain.model.sala.Membresia;
import roomiehub.vivienda.domain.repository.MensajeRepository;
import roomiehub.vivienda.domain.repository.NotaRepository;

/* CU-14 y CU-11 envia un mensaje que llega en vivo a quien tiene la app abierta y por push a los demas
   con el UUID de la app reenviar el mismo mensaje regresa el ya creado */
public class EnviarMensaje {

    private final AccesoASala acceso;
    private final MensajeRepository mensajes;
    private final NotaRepository notas;
    private final VistasDeMensajes vistas;
    private final ChatEnVivoPort chatEnVivo;
    private final NotificadorPort notificador;
    private final Clock reloj;

    public EnviarMensaje(AccesoASala acceso, MensajeRepository mensajes, NotaRepository notas, VistasDeMensajes vistas,
                         ChatEnVivoPort chatEnVivo, NotificadorPort notificador, Clock reloj) {
        this.acceso = acceso;
        this.mensajes = mensajes;
        this.notas = notas;
        this.vistas = vistas;
        this.chatEnVivo = chatEnVivo;
        this.notificador = notificador;
        this.reloj = reloj;
    }

    @Transactional
    public MensajeVista ejecutar(long usuarioId, long salaId, String texto, String imagenId, Long notaRespondidaId,
                                 UUID clientUuid) {
        Optional<MensajeChat> yaCreado = clientUuid == null ? Optional.empty() : mensajes.buscarPorClientUuid(clientUuid);
        if (yaCreado.isPresent()) {
            if (yaCreado.get().autorId() != usuarioId || yaCreado.get().salaId() != salaId) {
                throw new ConflictoDeEstadoException("Ese identificador de acción ya se usó");
            }
            return vistas.de(List.of(yaCreado.get())).get(0);
        }
        acceso.exigirMiembroDeSalaActiva(salaId, usuarioId);
        if (imagenId != null) {
            FirmarSubidaDeImagen.exigirImagenDeLaSala(imagenId, salaId);
        }
        if (notaRespondidaId != null) {
            Nota respondida = ReaccionarNota.notaVisible(notas, notaRespondidaId);
            if (respondida.salaId() != salaId) {
                throw new RecursoNoEncontradoException("Nota no encontrada");
            }
        }

        MensajeChat mensaje = mensajes.guardar(MensajeChat.nuevo(salaId, usuarioId, texto, imagenId, notaRespondidaId,
                clientUuid, reloj.instant()));
        MensajeVista vista = vistas.de(List.of(mensaje)).get(0);

        List<Long> miembros = acceso.exigirMiembro(salaId, usuarioId).stream().map(Membresia::usuarioId).toList();
        chatEnVivo.publicarMensaje(miembros, vista);
        notificador.notificar(miembros.stream()
                        .filter(id -> id != usuarioId && !chatEnVivo.estaConectado(id))
                        .toList(),
                vista.autor(), mensaje.texto() != null ? mensaje.texto() : "Imagen");
        return vista;
    }
}
