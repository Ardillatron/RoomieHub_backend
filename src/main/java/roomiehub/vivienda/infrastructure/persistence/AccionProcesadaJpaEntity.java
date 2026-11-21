package roomiehub.vivienda.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "acciones_procesadas")
@Getter
@Setter
public class AccionProcesadaJpaEntity {

    @Id
    @Column(name = "client_uuid")
    private UUID clientUuid;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "sala_id", nullable = false)
    private Long salaId;

    // Lo pone PostgreSQL con DEFAULT now
    @Column(name = "procesada_en", insertable = false, updatable = false)
    private Instant procesadaEn;
}
