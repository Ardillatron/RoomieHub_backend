package roomiehub.vivienda.domain.model.cuenta;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

import roomiehub.vivienda.domain.exception.DatoInvalidoException;

public final class Usuario {

    private final Long id;
    private final ProveedorAuth proveedor;
    private String email;
    private String passwordHash;
    private String googleSub;
    private String nombre;
    private String fotoUrl;
    private Instant eliminadoEn;

    public Usuario(Long id, ProveedorAuth proveedor, String email, String passwordHash, String googleSub,
                   String nombre, String fotoUrl, Instant eliminadoEn) {
        this.id = id;
        this.proveedor = Objects.requireNonNull(proveedor, "proveedor");
        this.email = email;
        this.passwordHash = passwordHash;
        this.googleSub = googleSub;
        this.nombre = validarNombre(nombre);
        this.fotoUrl = fotoUrl;
        this.eliminadoEn = eliminadoEn;
    }

    public static Usuario conCorreo(String email, String passwordHash, String nombre) {
        return new Usuario(null, ProveedorAuth.EMAIL, normalizarCorreo(email),
                Objects.requireNonNull(passwordHash, "passwordHash"), null, nombre, null, null);
    }

    public static Usuario conGoogle(String googleSub, String email, String nombre, String fotoUrl) {
        return new Usuario(null, ProveedorAuth.GOOGLE, normalizarCorreo(email), null,
                Objects.requireNonNull(googleSub, "googleSub"), nombre, fotoUrl, null);
    }

    // Correo sin espacios y en minusculas para guardar y buscar
    public static String normalizarCorreo(String email) {
        if (email == null || email.isBlank()) {
            throw new DatoInvalidoException("El correo es obligatorio");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /* Una cuenta de correo que luego entra con Google con el mismo correo verificado
       el registro nunca verifico el correo y la contraseña deja de servir */
    public void vincularGoogle(String sub) {
        this.googleSub = Objects.requireNonNull(sub, "sub");
        this.passwordHash = null;
    }

    // Se borran los datos personales y deudas puntos y mensajes siguen con este id
    public void anonimizar(Instant ahora) {
        email = null;
        passwordHash = null;
        googleSub = null;
        fotoUrl = null;
        nombre = "Usuario eliminado";
        eliminadoEn = Objects.requireNonNull(ahora, "ahora");
    }

    public void renombrar(String nombre) {
        this.nombre = validarNombre(nombre);
    }

    public boolean tieneContrasena() {
        return passwordHash != null;
    }

    private static String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new DatoInvalidoException("El nombre es obligatorio");
        }
        String limpio = nombre.trim();
        if (limpio.length() > 60) {
            throw new DatoInvalidoException("El nombre tiene a lo más 60 caracteres");
        }
        return limpio;
    }

    public Long id() { return id; }
    public ProveedorAuth proveedor() { return proveedor; }
    public String email() { return email; }
    public String passwordHash() { return passwordHash; }
    public String googleSub() { return googleSub; }
    public String nombre() { return nombre; }
    public String fotoUrl() { return fotoUrl; }
    public Instant eliminadoEn() { return eliminadoEn; }
}
