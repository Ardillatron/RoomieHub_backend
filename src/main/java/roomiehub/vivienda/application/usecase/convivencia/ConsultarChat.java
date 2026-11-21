package roomiehub.vivienda.application.usecase.convivencia;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.usecase.sala.AccesoASala;
import roomiehub.vivienda.domain.repository.MensajeRepository;

// El historial se guarda mientras exista la sala y se pide por paginas del mas nuevo al mas viejo
public class ConsultarChat {

    private static final int LIMITE_MAXIMO = 100;

    private final AccesoASala acceso;
    private final MensajeRepository mensajes;
    private final VistasDeMensajes vistas;

    public ConsultarChat(AccesoASala acceso, MensajeRepository mensajes, VistasDeMensajes vistas) {
        this.acceso = acceso;
        this.mensajes = mensajes;
        this.vistas = vistas;
    }

    @Transactional(readOnly = true)
    public List<MensajeVista> ejecutar(long usuarioId, long salaId, Long antesDe, int limite) {
        acceso.exigirMiembro(salaId, usuarioId);
        return vistas.de(mensajes.visiblesDeSala(salaId, antesDe, Math.max(1, Math.min(limite, LIMITE_MAXIMO))));
    }
}
