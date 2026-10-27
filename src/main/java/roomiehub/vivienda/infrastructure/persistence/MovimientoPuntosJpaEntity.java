package roomiehub.vivienda.infrastructure.persistence;

import java.time.Instant;

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
import roomiehub.vivienda.domain.model.tareas.TipoMovimiento;

/** creado_en lo pone PostgreSQL (DEFAULT now()). */
@Entity
@Table(name = "movimientos_puntos")
@Getter
@Setter
public class MovimientoPuntosJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "sala_id", nullable = false)
    private Long salaId;

    @Column(name = "asignacion_id")
    private Long asignacionId;

    @Column(nullable = false)
    private Integer puntos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimiento tipo;

    @Column(name = "creado_en", insertable = false, updatable = false)
    private Instant creadoEn;
}
