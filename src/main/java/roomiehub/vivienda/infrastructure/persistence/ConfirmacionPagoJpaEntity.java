package roomiehub.vivienda.infrastructure.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "confirmaciones_pago")
@Getter
@Setter
public class ConfirmacionPagoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deuda_id", nullable = false)
    private Long deudaId;

    @Column(name = "sala_id", nullable = false)
    private Long salaId;

    @Column(name = "confirmador_id", nullable = false)
    private Long confirmadorId;

    @Column(name = "client_uuid")
    private UUID clientUuid;
}
