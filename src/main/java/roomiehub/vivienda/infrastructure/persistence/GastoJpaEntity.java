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
import roomiehub.vivienda.domain.model.finanzas.FrecuenciaGasto;
import roomiehub.vivienda.domain.model.finanzas.Moneda;

@Entity
@Table(name = "servicios_pago")
@Getter
@Setter
public class GastoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sala_id", nullable = false)
    private Long salaId;

    @Column(nullable = false)
    private String concepto;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Moneda moneda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FrecuenciaGasto frecuencia;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "generar_desde", nullable = false)
    private LocalDate generarDesde;

    @Column(nullable = false)
    private boolean eliminado;

    @Column(name = "eliminado_por")
    private Long eliminadoPor;

    @Column(name = "eliminado_en")
    private Instant eliminadoEn;
}
