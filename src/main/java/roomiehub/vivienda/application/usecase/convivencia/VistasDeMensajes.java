package roomiehub.vivienda.application.usecase.convivencia;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import roomiehub.vivienda.application.port.AlmacenamientoImagenesPort;
import roomiehub.vivienda.application.usecase.convivencia.MensajeVista.NotaCitada;
import roomiehub.vivienda.domain.model.convivencia.MensajeChat;
import roomiehub.vivienda.domain.model.convivencia.Nota;
import roomiehub.vivienda.domain.model.cuenta.Usuario;
import roomiehub.vivienda.domain.repository.NotaRepository;
import roomiehub.vivienda.domain.repository.UsuarioRepository;

// Arma varios mensajes a la vez cargando autores y notas citadas una sola vez
public class VistasDeMensajes {

    private final UsuarioRepository usuarios;
    private final NotaRepository notas;
    private final AlmacenamientoImagenesPort almacenamiento;

    public VistasDeMensajes(UsuarioRepository usuarios, NotaRepository notas, AlmacenamientoImagenesPort almacenamiento) {
        this.usuarios = usuarios;
        this.notas = notas;
        this.almacenamiento = almacenamiento;
    }

    public List<MensajeVista> de(List<MensajeChat> mensajes) {
        Map<Long, String> autores = usuarios.buscarPorIds(mensajes.stream().map(MensajeChat::autorId).distinct().toList())
                .stream().collect(Collectors.toMap(Usuario::id, Usuario::nombre));
        Map<Long, Optional<Nota>> citadas = new HashMap<>();
        return mensajes.stream().map(m -> new MensajeVista(m, autores.get(m.autorId()),
                        m.imagenId() == null ? null : almacenamiento.urlDeEntrega(m.imagenId()),
                        m.imagenId() == null ? null : almacenamiento.urlDeMiniatura(m.imagenId()),
                        m.notaRespondidaId() == null ? null : citada(m.notaRespondidaId(),
                                citadas.computeIfAbsent(m.notaRespondidaId(), notas::buscarPorId))))
                .toList();
    }

    private NotaCitada citada(long notaId, Optional<Nota> nota) {
        return nota.filter(n -> !n.eliminada())
                .map(n -> new NotaCitada(notaId, n.texto(),
                        n.imagenId() == null ? null : almacenamiento.urlDeMiniatura(n.imagenId())))
                .orElse(new NotaCitada(notaId, null, null));
    }
}
