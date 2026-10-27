package roomiehub.vivienda.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import roomiehub.vivienda.domain.model.finanzas.EstadoDeuda;

@Entity
@Table(name = "deudas_usuarios")
@Getter
@Setter
public class DeudaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "servicio_id", nullable = false)
    private Long servicioId;

    @Column(name = "sala_id", nullable = false)
    private Long salaId;

    @Column(nullable = false)
    private LocalDate periodo;

    @Column(name = "deudor_id", nullable = false)
    private Long deudorId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoDeuda estado;

    @Column(name = "deuda_origen_id")
    private Long deudaOrigenId;

    @Column(name = "aviso_pago_en")
    private Instant avisoPagoEn;

    @Column(name = "recordatorio_confirmacion_en")
    private Instant recordatorioConfirmacionEn;

    @Column(name = "recordatorios_enviados", nullable = false)
    private Short recordatoriosEnviados = 0;

    @Column(name = "ultimo_recordatorio_en")
    private Instant ultimoRecordatorioEn;

    /** Lo pone PostgreSQL (DEFAULT now()). */
    @Column(name = "creado_en", insertable = false, updatable = false)
    private Instant creadoEn;
}
