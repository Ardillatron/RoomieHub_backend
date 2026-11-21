package roomiehub.vivienda.application.usecase.convivencia;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.port.ChatEnVivoPort;
import roomiehub.vivienda.application.usecase.sala.AccesoASala;
import roomiehub.vivienda.domain.model.convivencia.MensajeChat;
import roomiehub.vivienda.domain.model.sala.Membresia;
import roomiehub.vivienda.domain.repository.MensajeRepository;

// CU-15 solo el admin borra el mensaje y desaparece en vivo de los chats abiertos
public class EliminarMensaje {

    private final AccesoASala acceso;
    private final MensajeRepository mensajes;
    private final ChatEnVivoPort chatEnVivo;

    public EliminarMensaje(AccesoASala acceso, MensajeRepository mensajes, ChatEnVivoPort chatEnVivo) {
        this.acceso = acceso;
        this.mensajes = mensajes;
        this.chatEnVivo = chatEnVivo;
    }

    @Transactional
    public void ejecutar(long adminId, long mensajeId) {
        MensajeChat mensaje = ReportarMensaje.mensajeVisible(mensajes, mensajeId);
        acceso.exigirAdministradorDeSalaActiva(mensaje.salaId(), adminId);
        mensaje.eliminar();
        mensajes.guardar(mensaje);

        List<Long> miembros = acceso.exigirMiembro(mensaje.salaId(), adminId).stream()
                .map(Membresia::usuarioId).toList();
        chatEnVivo.publicarEliminacion(miembros, mensaje.salaId(), mensajeId);
    }
}
