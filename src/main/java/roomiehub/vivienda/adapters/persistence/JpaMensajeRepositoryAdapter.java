package roomiehub.vivienda.adapters.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;

import roomiehub.vivienda.domain.model.convivencia.MensajeChat;
import roomiehub.vivienda.domain.repository.MensajeRepository;
import roomiehub.vivienda.infrastructure.persistence.MensajeJpaEntity;
import roomiehub.vivienda.infrastructure.persistence.MensajeJpaRepository;

@Component
public class JpaMensajeRepositoryAdapter implements MensajeRepository {

    private final MensajeJpaRepository jpa;

    public JpaMensajeRepositoryAdapter(MensajeJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public MensajeChat guardar(MensajeChat m) {
        MensajeJpaEntity e = new MensajeJpaEntity();
        e.setId(m.id());
        e.setSalaId(m.salaId());
        e.setAutorId(m.autorId());
        e.setTexto(m.texto());
        e.setImagenId(m.imagenId());
        e.setNotaRespondidaId(m.notaRespondidaId());
        e.setEliminado(m.eliminado());
        e.setCreadoEn(m.creadoEn());
        e.setClientUuid(m.clientUuid());
        return aDominio(jpa.saveAndFlush(e));
    }

    @Override
    public Optional<MensajeChat> buscarPorId(long id) {
        return jpa.findById(id).map(JpaMensajeRepositoryAdapter::aDominio);
    }

    @Override
    public Optional<MensajeChat> buscarPorClientUuid(UUID clientUuid) {
        return jpa.findByClientUuid(clientUuid).map(JpaMensajeRepositoryAdapter::aDominio);
    }

    @Override
    public List<MensajeChat> visiblesDeSala(long salaId, Long antesDe, int limite) {
        List<MensajeJpaEntity> pagina = antesDe == null
                ? jpa.findBySalaIdAndEliminadoFalseOrderByIdDesc(salaId, Limit.of(limite))
                : jpa.findBySalaIdAndEliminadoFalseAndIdLessThanOrderByIdDesc(salaId, antesDe, Limit.of(limite));
        return pagina.stream().map(JpaMensajeRepositoryAdapter::aDominio).toList();
    }

    private static MensajeChat aDominio(MensajeJpaEntity e) {
        return new MensajeChat(e.getId(), e.getSalaId(), e.getAutorId(), e.getTexto(), e.getImagenId(),
                e.getNotaRespondidaId(), e.getCreadoEn(), e.getClientUuid(), e.isEliminado());
    }
}
