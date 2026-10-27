package roomiehub.vivienda.domain.repository;

import java.util.List;
import java.util.Optional;

import roomiehub.vivienda.domain.model.sala.Membresia;

public interface MembresiaRepository {

    List<Membresia> activasDeSala(long salaId);

    List<Membresia> activasDeUsuario(long usuarioId);

    /** Activa o no: quien fue expulsado conserva su fila y la reactiva al volver (RN-07). */
    Optional<Membresia> buscar(long salaId, long usuarioId);

    /** Se escribe de inmediato: el orden importa por RLS y por el índice de un solo Administrador. */
    void guardar(Membresia membresia);
}
