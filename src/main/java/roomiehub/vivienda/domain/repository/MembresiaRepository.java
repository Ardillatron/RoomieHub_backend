package roomiehub.vivienda.domain.repository;

import java.util.List;
import java.util.Optional;

import roomiehub.vivienda.domain.model.sala.Membresia;

public interface MembresiaRepository {

    List<Membresia> activasDeSala(long salaId);

    List<Membresia> activasDeUsuario(long usuarioId);

    // Activa o no porque quien fue expulsado conserva su fila y la reactiva al volver
    Optional<Membresia> buscar(long salaId, long usuarioId);

    // Se escribe de inmediato porque el orden importa por RLS y por el indice de un solo admin
    void guardar(Membresia membresia);
}
