package roomiehub.vivienda.infrastructure.persistence;

import java.time.Instant;
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
@Table(name = "notas_tablon")
@Getter
@Setter
public class NotaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sala_id", nullable = false)
    private Long salaId;

    @Column(name = "autor_id", nullable = false)
    private Long autorId;

    @Column(nullable = false)
    private String texto;

    @Column(name = "imagen_id")
    private String imagenId;

    @Column(name = "fijada_hasta")
    private Instant fijadaHasta;

    @Column(nullable = false)
    private boolean eliminada;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "client_uuid")
    private UUID clientUuid;
}
