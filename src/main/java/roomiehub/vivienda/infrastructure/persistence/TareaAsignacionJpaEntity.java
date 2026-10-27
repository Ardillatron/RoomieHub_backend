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
import roomiehub.vivienda.domain.model.tareas.EstadoAsignacion;

@Entity
@Table(name = "tarea_asignaciones")
@Getter
@Setter
public class TareaAsignacionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tarea_id", nullable = false)
    private Long tareaId;

    @Column(name = "sala_id", nullable = false)
    private Long salaId;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "periodo_inicio", nullable = false)
    private Instant periodoInicio;

    @Column(name = "periodo_fin", nullable = false)
    private Instant periodoFin;

    @Column(name = "valor_al_asignar", nullable = false)
    private Short valorAlAsignar;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoAsignacion estado;

    @Column(name = "completada_en")
    private Instant completadaEn;
}
