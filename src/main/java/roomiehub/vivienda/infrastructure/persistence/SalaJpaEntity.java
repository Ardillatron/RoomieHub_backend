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
import roomiehub.vivienda.domain.model.sala.EstadoSala;

@Entity
@Table(name = "salas")
@Getter
@Setter
public class SalaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "creador_id", nullable = false)
    private Long creadorId;

    @Column(name = "zona_horaria", nullable = false)
    private String zonaHoraria;

    @Column(name = "codigo_invitacion", nullable = false)
    private String codigoInvitacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoSala estado;

    @Column(name = "desactivada_en")
    private Instant desactivadaEn;
}
