package roomiehub.vivienda.adapters.persistence;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import roomiehub.vivienda.domain.model.sala.CodigoInvitacion;
import roomiehub.vivienda.domain.model.sala.EstadoSala;
import roomiehub.vivienda.domain.model.sala.Sala;
import roomiehub.vivienda.domain.repository.SalaRepository;
import roomiehub.vivienda.infrastructure.persistence.SalaJpaEntity;
import roomiehub.vivienda.infrastructure.persistence.SalaJpaRepository;

@Component
public class JpaSalaRepositoryAdapter implements SalaRepository {

    private final SalaJpaRepository jpa;

    public JpaSalaRepositoryAdapter(SalaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Sala> buscarPorId(long id) {
        return jpa.findById(id).map(JpaSalaRepositoryAdapter::aDominio);
    }

    @Override
    public Optional<SalaPorCodigo> buscarPorCodigo(CodigoInvitacion codigo) {
        return jpa.buscarPorCodigo(codigo.valor()).map(v ->
                new SalaPorCodigo(v.getSalaId(), EstadoSala.valueOf(v.getEstado()), v.getMiembrosActivos()));
    }

    @Override
    public Sala guardar(Sala s) {
        SalaJpaEntity e = new SalaJpaEntity();
        e.setId(s.id());
        e.setNombre(s.nombre());
        e.setCreadorId(s.creadorId());
        e.setZonaHoraria(s.zonaHoraria().getId());
        e.setCodigoInvitacion(s.codigoInvitacion());
        e.setEstado(s.estado());
        e.setDesactivadaEn(s.desactivadaEn());
        return aDominio(jpa.saveAndFlush(e));
    }

    @Override
    public void borrar(long id) {
        jpa.borrar(id);
    }

    @Override
    public void bloquearMembresias(long salaId) {
        jpa.bloquearMembresias(salaId);
    }

    @Override
    public void bloquearMembresiasDeUsuario(long usuarioId) {
        jpa.bloquearMembresiasDeUsuario(usuarioId);
    }

    @Override
    public List<Long> conTrabajoPendiente() {
        return jpa.conTrabajoPendiente();
    }

    @Override
    public boolean intentarBloquearProcesamiento(long salaId) {
        return jpa.intentarBloquearProcesamiento(salaId);
    }

    private static Sala aDominio(SalaJpaEntity e) {
        return new Sala(e.getId(), e.getNombre(), e.getCreadorId(), ZoneId.of(e.getZonaHoraria()),
                e.getCodigoInvitacion(), e.getEstado(), e.getDesactivadaEn());
    }
}
