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
@Table(name = "chat_mensajes")
@Getter
@Setter
public class MensajeJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sala_id", nullable = false)
    private Long salaId;

    @Column(name = "autor_id", nullable = false)
    private Long autorId;

    private String texto;

    @Column(name = "imagen_id")
    private String imagenId;

    @Column(name = "nota_respondida_id")
    private Long notaRespondidaId;

    @Column(nullable = false)
    private boolean eliminado;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "client_uuid")
    private UUID clientUuid;
}
