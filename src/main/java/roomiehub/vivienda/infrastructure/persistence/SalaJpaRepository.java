package roomiehub.vivienda.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SalaJpaRepository extends JpaRepository<SalaJpaEntity, Long> {

    interface SalaPorCodigoVista {
        Long getSalaId();
        String getEstado();
        Integer getMiembrosActivos();
    }

    // SECURITY DEFINER funciona aunque el usuario aun no sea miembro
    @Query(value = """
            SELECT sala_id AS salaId, estado, miembros_activos AS miembrosActivos
            FROM buscar_sala_por_codigo(:codigo)""", nativeQuery = true)
    Optional<SalaPorCodigoVista> buscarPorCodigo(String codigo);

    // Borra directo sin cargar la sala y las tablas hijas caen en cascada
    @Modifying
    @Query("DELETE FROM SalaJpaEntity s WHERE s.id = :id")
    int borrar(long id);

    // Candados de transaccion con llave 1 para el cupo de una sala y llave 2 para las membresias de un usuario
    @Query(value = "SELECT CAST(pg_advisory_xact_lock(1, CAST(:salaId AS integer)) AS text)", nativeQuery = true)
    String bloquearMembresias(long salaId);

    @Query(value = "SELECT CAST(pg_advisory_xact_lock(2, CAST(:usuarioId AS integer)) AS text)", nativeQuery = true)
    String bloquearMembresiasDeUsuario(long usuarioId);

    // Llave 3 para el proceso programado de una sala y try la salta si otro proceso ya la tiene
    @Query(value = "SELECT pg_try_advisory_xact_lock(3, CAST(:salaId AS integer))", nativeQuery = true)
    boolean intentarBloquearProcesamiento(long salaId);

    @Query(value = "SELECT salas_con_trabajo_pendiente()", nativeQuery = true)
    List<Long> conTrabajoPendiente();
}
