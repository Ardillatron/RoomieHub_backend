package roomiehub.vivienda.application.usecase.convivencia;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import roomiehub.vivienda.application.port.AlmacenamientoImagenesPort;
import roomiehub.vivienda.application.usecase.sala.AccesoASala;
import roomiehub.vivienda.domain.model.convivencia.Nota;
import roomiehub.vivienda.domain.model.convivencia.TipoReaccion;
import roomiehub.vivienda.domain.model.cuenta.Usuario;
import roomiehub.vivienda.domain.repository.NotaRepository;
import roomiehub.vivienda.domain.repository.ReaccionRepository;
import roomiehub.vivienda.domain.repository.ReaccionRepository.Conteo;
import roomiehub.vivienda.domain.repository.UsuarioRepository;

// En el tablon se ven las reacciones y las fijadas van arriba
public class ConsultarTablon {

    /* Imagenes firmadas o null si la nota no tiene
       la miniatura es para el tablon y la completa para ampliar */
    public record NotaVista(Nota nota, String autor, long likes, long dislikes, TipoReaccion miReaccion,
                            boolean fijada, String imagenUrl, String imagenMiniaturaUrl) {

        static NotaVista de(Nota n, String autor, long likes, long dislikes, TipoReaccion miReaccion, boolean fijada,
                            AlmacenamientoImagenesPort almacenamiento) {
            return n.imagenId() == null
                    ? new NotaVista(n, autor, likes, dislikes, miReaccion, fijada, null, null)
                    : new NotaVista(n, autor, likes, dislikes, miReaccion, fijada,
                            almacenamiento.urlDeEntrega(n.imagenId()), almacenamiento.urlDeMiniatura(n.imagenId()));
        }
    }

    // fijadas solo viene en la primera pagina y recientes no las repite
    public record Tablon(List<NotaVista> fijadas, List<NotaVista> recientes) {}

    private static final int LIMITE_MAXIMO = 100;

    private final AccesoASala acceso;
    private final NotaRepository notas;
    private final ReaccionRepository reacciones;
    private final UsuarioRepository usuarios;
    private final AlmacenamientoImagenesPort almacenamiento;
    private final Clock reloj;

    public ConsultarTablon(AccesoASala acceso, NotaRepository notas, ReaccionRepository reacciones,
                           UsuarioRepository usuarios, AlmacenamientoImagenesPort almacenamiento, Clock reloj) {
        this.acceso = acceso;
        this.notas = notas;
        this.reacciones = reacciones;
        this.usuarios = usuarios;
        this.almacenamiento = almacenamiento;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public Tablon ejecutar(long usuarioId, long salaId, Long antesDe, int limite) {
        acceso.exigirMiembro(salaId, usuarioId);
        Instant ahora = reloj.instant();
        List<Nota> fijadas = antesDe == null ? notas.fijadasDeSala(salaId, ahora) : List.of();
        List<Nota> recientes = notas.noFijadasDeSala(salaId, ahora, antesDe,
                Math.max(1, Math.min(limite, LIMITE_MAXIMO)));

        List<Nota> todas = new ArrayList<>(fijadas);
        todas.addAll(recientes);
        List<Long> ids = todas.stream().map(Nota::id).distinct().toList();
        Map<Long, Conteo> conteos = reacciones.conteos(ids);
        Map<Long, TipoReaccion> mias = reacciones.deUsuario(ids, usuarioId);
        Map<Long, String> autores = usuarios.buscarPorIds(todas.stream().map(Nota::autorId).distinct().toList())
                .stream().collect(Collectors.toMap(Usuario::id, Usuario::nombre));

        Function<Nota, NotaVista> vista = n -> {
            Conteo c = conteos.getOrDefault(n.id(), new Conteo(0, 0));
            return NotaVista.de(n, autores.get(n.autorId()), c.likes(), c.dislikes(), mias.get(n.id()),
                    n.estaFijada(ahora), almacenamiento);
        };
        return new Tablon(fijadas.stream().map(vista).toList(), recientes.stream().map(vista).toList());
    }
}
