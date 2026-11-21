package roomiehub.vivienda.application.usecase.cuenta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import roomiehub.vivienda.application.port.HasherDeContrasenasPort;
import roomiehub.vivienda.application.port.VerificadorGooglePort;
import roomiehub.vivienda.application.port.VerificadorGooglePort.IdentidadGoogle;
import roomiehub.vivienda.domain.exception.CorreoYaRegistradoException;
import roomiehub.vivienda.domain.exception.CredencialesInvalidasException;
import roomiehub.vivienda.domain.exception.DatoInvalidoException;
import roomiehub.vivienda.domain.model.cuenta.RefreshToken;
import roomiehub.vivienda.domain.model.cuenta.Usuario;
import roomiehub.vivienda.domain.repository.DispositivoRepository;
import roomiehub.vivienda.domain.repository.RefreshTokenRepository;
import roomiehub.vivienda.domain.repository.UsuarioRepository;

class CuentaUseCasesTest {

    final RelojMovible reloj = new RelojMovible(Instant.parse("2026-11-01T12:00:00Z"));
    final UsuariosEnMemoria usuarios = new UsuariosEnMemoria();
    final TokensEnMemoria tokens = new TokensEnMemoria();
    final Map<String, IdentidadGoogle> tokensGoogle = new HashMap<>();
    final HasherDeContrasenasPort hasher = new HasherDeContrasenasPort() {
        public String hash(String c) { return "h:" + c; }
        public boolean coincide(String c, String h) { return h.equals("h:" + c); }
    };
    final VerificadorGooglePort google = idToken -> Optional.ofNullable(tokensGoogle.get(idToken))
            .orElseThrow(CredencialesInvalidasException::new);

    final EmisorDeSesiones sesiones = new EmisorDeSesiones((id, desde, hasta) -> "jwt-" + id, tokens, reloj);
    final RegistrarUsuario registrar = new RegistrarUsuario(usuarios, hasher, sesiones);
    final IniciarSesion login = new IniciarSesion(usuarios, hasher, sesiones);
    final IniciarSesionConGoogle conGoogle = new IniciarSesionConGoogle(google, usuarios, sesiones, tokens, reloj);
    final RenovarSesion renovar = new RenovarSesion(tokens, usuarios, sesiones, reloj);
    final Set<String> tokensPush = new HashSet<>();
    final CerrarSesion cerrar = new CerrarSesion(tokens, new DispositivoRepository() {
        @Override public void registrar(long usuarioId, String fcmToken, String plataforma, Instant ahora) {
            tokensPush.add(fcmToken);
        }
        @Override public void borrarDeUsuario(long usuarioId) {}
        @Override public List<String> tokensDe(Collection<Long> usuarios) { return List.copyOf(tokensPush); }
        @Override public void borrarToken(String fcmToken) { tokensPush.remove(fcmToken); }
    }, reloj);

    @Test
    void registroNormalizaElCorreoYGuardaSoloElHashDelRefresh() {
        Sesion s = registrar.ejecutar("  Ana@Mail.COM ", "secreta123", "Ana", "Pixel 8");
        assertEquals("ana@mail.com", s.usuario().email());
        assertEquals("h:secreta123", s.usuario().passwordHash());
        assertEquals("jwt-1", s.accessToken());
        assertEquals(reloj.instant().plus(Duration.ofMinutes(15)), s.accesoExpiraEn());
        assertTrue(tokens.buscarPorHash(s.refreshToken()).isEmpty(), "el token en claro no se guarda");
        RefreshToken guardado = tokens.buscarPorHash(EmisorDeSesiones.hash(s.refreshToken())).orElseThrow();
        assertEquals(reloj.instant().plus(Duration.ofDays(30)), guardado.expiraEn());
        assertEquals("Pixel 8", guardado.dispositivo());
    }

    @Test
    void registroRechazaCorreoRepetidoYContrasenasInvalidas() {
        registrar.ejecutar("ana@mail.com", "secreta123", "Ana", null);
        assertThrows(CorreoYaRegistradoException.class, () -> registrar.ejecutar("ANA@mail.com", "otra12345", "A", null));
        assertThrows(DatoInvalidoException.class, () -> registrar.ejecutar("b@mail.com", "corta", "B", null));
        String ñ37 = "ñ".repeat(37); // 37 caracteres pero 74 bytes y BCrypt ignoraria el final
        assertThrows(DatoInvalidoException.class, () -> registrar.ejecutar("b@mail.com", ñ37, "B", null));
    }

    @Test
    void loginSoloConLaContrasenaCorrecta() {
        registrar.ejecutar("ana@mail.com", "secreta123", "Ana", null);
        assertEquals("ana@mail.com", login.ejecutar("Ana@Mail.com", "secreta123", null).usuario().email());
        assertThrows(CredencialesInvalidasException.class, () -> login.ejecutar("ana@mail.com", "mala12345", null));
        assertThrows(CredencialesInvalidasException.class, () -> login.ejecutar("nadie@mail.com", "secreta123", null));
    }

    @Test
    void unaCuentaDeGoogleNoEntraConContrasena() {
        tokensGoogle.put("tg", new IdentidadGoogle("sub-1", "g@mail.com", true, "G", null));
        conGoogle.ejecutar("tg", null);
        assertThrows(CredencialesInvalidasException.class, () -> login.ejecutar("g@mail.com", "contrasena-senuelo", null));
    }

    @Test
    void renovarRotaElRefreshToken() {
        Sesion s1 = registrar.ejecutar("ana@mail.com", "secreta123", "Ana", "Pixel");
        Sesion s2 = renovar.ejecutar(s1.refreshToken());
        assertNotEquals(s1.refreshToken(), s2.refreshToken());
        assertEquals("Pixel", tokens.buscarPorHash(EmisorDeSesiones.hash(s2.refreshToken())).orElseThrow().dispositivo());
        renovar.ejecutar(s2.refreshToken());
    }

    @Test
    void reusarUnTokenRotadoCierraTodasLasSesiones() {
        Sesion telefono = registrar.ejecutar("ana@mail.com", "secreta123", "Ana", null);
        Sesion tableta = login.ejecutar("ana@mail.com", "secreta123", null);
        Sesion rotada = renovar.ejecutar(telefono.refreshToken());

        reloj.avanzar(RenovarSesion.GRACIA_REINTENTO.plusSeconds(1));
        assertThrows(CredencialesInvalidasException.class, () -> renovar.ejecutar(telefono.refreshToken()));
        assertThrows(CredencialesInvalidasException.class, () -> renovar.ejecutar(rotada.refreshToken()));
        assertThrows(CredencialesInvalidasException.class, () -> renovar.ejecutar(tableta.refreshToken()));
    }

    @Test
    void unReintentoInmediatoSeRechazaSinCerrarLasDemasSesiones() {
        Sesion telefono = registrar.ejecutar("ana@mail.com", "secreta123", "Ana", null);
        Sesion tableta = login.ejecutar("ana@mail.com", "secreta123", null);
        Sesion rotada = renovar.ejecutar(telefono.refreshToken());

        reloj.avanzar(Duration.ofSeconds(3)); // se perdio la respuesta y el telefono reintenta
        assertThrows(CredencialesInvalidasException.class, () -> renovar.ejecutar(telefono.refreshToken()));
        renovar.ejecutar(rotada.refreshToken());
        renovar.ejecutar(tableta.refreshToken());
    }

    @Test
    void unRefreshVencidoODesconocidoNoSirve() {
        Sesion s = registrar.ejecutar("ana@mail.com", "secreta123", "Ana", null);
        assertThrows(CredencialesInvalidasException.class, () -> renovar.ejecutar("inventado"));
        reloj.avanzar(Duration.ofDays(30));
        assertThrows(CredencialesInvalidasException.class, () -> renovar.ejecutar(s.refreshToken()));
    }

    @Test
    void editarPerfilCambiaElNombreConLasMismasReglas() {
        long id = registrar.ejecutar("ana@mail.com", "secreta123", "Ana", null).usuario().id();
        EditarPerfil editar = new EditarPerfil(new ObtenerPerfil(usuarios), usuarios);

        assertEquals("Ana María", editar.ejecutar(id, "  Ana María ").nombre());
        assertEquals("Ana María", usuarios.buscarPorId(id).orElseThrow().nombre());
        assertThrows(DatoInvalidoException.class, () -> editar.ejecutar(id, " "));
    }

    @Test
    void cerrarSesionRevocaYToleraTokensDesconocidos() {
        Sesion s = registrar.ejecutar("ana@mail.com", "secreta123", "Ana", null);
        tokensPush.addAll(List.of("fcm-de-ana", "fcm-de-otro"));
        cerrar.ejecutar("inventado", "fcm-de-otro"); // sin sesion valida no borra nada
        cerrar.ejecutar(s.refreshToken(), "fcm-de-ana");
        assertTrue(tokens.buscarPorHash(EmisorDeSesiones.hash(s.refreshToken())).orElseThrow().revocado());
        assertEquals(Set.of("fcm-de-otro"), tokensPush, "ese teléfono deja de recibir push");
    }

    @Test
    void googleCreaLaCuentaYDespuesLaReconocePorSub() {
        tokensGoogle.put("t1", new IdentidadGoogle("sub-1", "Gabi@Gmail.com", true, null, "foto"));
        Usuario creado = conGoogle.ejecutar("t1", null).usuario();
        assertEquals("gabi", creado.nombre(), "sin nombre en el token usa la parte local del correo");
        assertEquals("gabi@gmail.com", creado.email());

        // Mismo id de Google aunque el correo haya cambiado es la misma cuenta
        tokensGoogle.put("t2", new IdentidadGoogle("sub-1", "nuevo@gmail.com", true, "Gabi", null));
        assertEquals(creado.id(), conGoogle.ejecutar("t2", null).usuario().id());
    }

    @Test
    void googleVinculaLaCuentaDeCorreoSoloConCorreoVerificado() {
        // Alguien registro el correo de Ana antes que ella con su propia contraseña
        Sesion delIntruso = registrar.ejecutar("ana@mail.com", "secreta123", "Ana", null);
        long id = delIntruso.usuario().id();

        tokensGoogle.put("sinVerificar", new IdentidadGoogle("sub-x", "ana@mail.com", false, "Ana", null));
        assertThrows(CredencialesInvalidasException.class, () -> conGoogle.ejecutar("sinVerificar", null));

        tokensGoogle.put("verificado", new IdentidadGoogle("sub-ana", "ana@mail.com", true, "Ana", null));
        Usuario vinculado = conGoogle.ejecutar("verificado", null).usuario();
        assertEquals(id, vinculado.id());
        assertEquals("sub-ana", vinculado.googleSub());
        // Al vincular la contraseña y las sesiones que habia dejan de servir
        assertThrows(CredencialesInvalidasException.class, () -> login.ejecutar("ana@mail.com", "secreta123", null));
        assertThrows(CredencialesInvalidasException.class, () -> renovar.ejecutar(delIntruso.refreshToken()));

        tokensGoogle.put("otraCuenta", new IdentidadGoogle("sub-otro", "ana@mail.com", true, "Ana", null));
        assertThrows(CredencialesInvalidasException.class, () -> conGoogle.ejecutar("otraCuenta", null));
        assertThrows(CredencialesInvalidasException.class, () -> conGoogle.ejecutar("falso", null));
    }

    static final class RelojMovible extends Clock {
        private Instant ahora;
        RelojMovible(Instant ahora) { this.ahora = ahora; }
        void avanzar(Duration d) { ahora = ahora.plus(d); }
        @Override public Instant instant() { return ahora; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
    }

    static final class UsuariosEnMemoria implements UsuarioRepository {
        private final Map<Long, Usuario> porId = new HashMap<>();
        private long siguiente = 1;

        @Override public Usuario guardar(Usuario u) {
            Usuario conId = u.id() != null ? u : new Usuario(siguiente++, u.proveedor(), u.email(), u.passwordHash(),
                    u.googleSub(), u.nombre(), u.fotoUrl(), u.eliminadoEn());
            porId.put(conId.id(), conId);
            return conId;
        }
        @Override public Optional<Usuario> buscarPorId(long id) { return Optional.ofNullable(porId.get(id)); }
        @Override public Optional<Usuario> buscarPorEmail(String email) {
            return porId.values().stream().filter(u -> email.equals(u.email())).findFirst();
        }
        @Override public Optional<Usuario> buscarPorGoogleSub(String sub) {
            return porId.values().stream().filter(u -> sub.equals(u.googleSub())).findFirst();
        }
        @Override public java.util.List<Usuario> buscarPorIds(java.util.Collection<Long> ids) {
            return ids.stream().map(porId::get).filter(java.util.Objects::nonNull).toList();
        }
    }

    static final class TokensEnMemoria implements RefreshTokenRepository {
        private final Map<Long, RefreshToken> porId = new HashMap<>();
        private long siguiente = 1;

        @Override public void guardar(RefreshToken t) {
            long id = siguiente++;
            porId.put(id, new RefreshToken(id, t.usuarioId(), t.tokenHash(), t.dispositivo(), t.expiraEn(), t.revocadoEn()));
        }
        @Override public Optional<RefreshToken> buscarPorHash(String hash) {
            return porId.values().stream().filter(t -> t.tokenHash().equals(hash)).findFirst();
        }
        @Override public boolean revocarSiVigente(long id, Instant ahora) {
            RefreshToken t = porId.get(id);
            if (t.revocado()) return false;
            porId.put(id, revocado(t, ahora));
            return true;
        }
        @Override public void revocarTodos(long usuarioId, Instant ahora) {
            porId.replaceAll((id, t) -> t.usuarioId() == usuarioId && !t.revocado() ? revocado(t, ahora) : t);
        }
        private static RefreshToken revocado(RefreshToken t, Instant ahora) {
            return new RefreshToken(t.id(), t.usuarioId(), t.tokenHash(), t.dispositivo(), t.expiraEn(), ahora);
        }
    }
}
