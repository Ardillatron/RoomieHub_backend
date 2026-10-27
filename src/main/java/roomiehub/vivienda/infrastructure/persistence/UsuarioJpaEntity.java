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
import roomiehub.vivienda.domain.model.cuenta.ProveedorAuth;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
public class UsuarioJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "proveedor_auth", nullable = false)
    private ProveedorAuth proveedorAuth;

    @Column(name = "google_sub")
    private String googleSub;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "foto_url")
    private String fotoUrl;

    @Column(name = "eliminado_en")
    private Instant eliminadoEn;
}
