package roomiehub.vivienda.domain.repository;

import java.util.List;
import java.util.Optional;

import roomiehub.vivienda.domain.model.convivencia.Reporte;

public interface ReporteRepository {

    Reporte guardar(Reporte reporte);

    Optional<Reporte> buscarPorId(long id);

    /** Los no resueltos, del más viejo al más nuevo. */
    List<Reporte> pendientesDeSala(long salaId);
}
