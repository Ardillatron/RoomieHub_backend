package roomiehub.vivienda.adapters.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;

import roomiehub.vivienda.domain.model.convivencia.Nota;
import roomiehub.vivienda.domain.repository.NotaRepository;
import roomiehub.vivienda.infrastructure.persistence.NotaJpaEntity;
import roomiehub.vivienda.infrastructure.persistence.NotaJpaRepository;

@Component
public class JpaNotaRepositoryAdapter implements NotaRepository {

    private final NotaJpaRepository jpa;

    public JpaNotaRepositoryAdapter(NotaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Nota guardar(Nota n) {
        NotaJpaEntity e = new NotaJpaEntity();
        e.setId(n.id());
        e.setSalaId(n.salaId());
        e.setAutorId(n.autorId());
        e.setTexto(n.texto());
        e.setImagenId(n.imagenId());
        e.setFijadaHasta(n.fijadaHasta());
        e.setEliminada(n.eliminada());
        e.setCreadoEn(n.creadoEn());
        e.setClientUuid(n.clientUuid());
        return aDominio(jpa.saveAndFlush(e));
    }

    @Override
    public Optional<Nota> buscarPorId(long id) {
        return jpa.findById(id).map(JpaNotaRepositoryAdapter::aDominio);
    }

    @Override
    public Optional<Nota> buscarPorClientUuid(UUID clientUuid) {
        return jpa.findByClientUuid(clientUuid).map(JpaNotaRepositoryAdapter::aDominio);
    }

    @Override
    public List<Nota> noFijadasDeSala(long salaId, Instant ahora, Long antesDe, int limite) {
        List<NotaJpaEntity> pagina = antesDe == null
                ? jpa.noFijadas(salaId, ahora, Limit.of(limite))
                : jpa.noFijadasAntesDe(salaId, ahora, antesDe, Limit.of(limite));
        return pagina.stream().map(JpaNotaRepositoryAdapter::aDominio).toList();
    }

    @Override
    public List<Nota> fijadasDeSala(long salaId, Instant ahora) {
        return jpa.findBySalaIdAndEliminadaFalseAndFijadaHastaAfterOrderByFijadaHastaDesc(salaId, ahora).stream()
                .map(JpaNotaRepositoryAdapter::aDominio).toList();
    }

    @Override
    public void desfijarVencidas(long salaId, Instant ahora) {
        jpa.desfijarVencidas(salaId, ahora);
    }

    private static Nota aDominio(NotaJpaEntity e) {
        return new Nota(e.getId(), e.getSalaId(), e.getAutorId(), e.getTexto(), e.getImagenId(), e.getCreadoEn(),
                e.getClientUuid(), e.getFijadaHasta(), e.isEliminada());
    }
}
