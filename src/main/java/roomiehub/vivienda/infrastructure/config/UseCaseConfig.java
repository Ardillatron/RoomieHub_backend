package roomiehub.vivienda.infrastructure.config;

import java.security.SecureRandom;
import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import roomiehub.vivienda.application.port.AlmacenamientoImagenesPort;
import roomiehub.vivienda.application.port.ChatEnVivoPort;
import roomiehub.vivienda.application.port.EmisorDeTokensPort;
import roomiehub.vivienda.application.port.HasherDeContrasenasPort;
import roomiehub.vivienda.application.port.NotificadorPort;
import roomiehub.vivienda.application.port.VerificadorGooglePort;
import roomiehub.vivienda.application.usecase.cuenta.CerrarSesion;
import roomiehub.vivienda.application.usecase.cuenta.EliminarCuenta;
import roomiehub.vivienda.application.usecase.cuenta.EmisorDeSesiones;
import roomiehub.vivienda.application.usecase.cuenta.IniciarSesion;
import roomiehub.vivienda.application.usecase.cuenta.IniciarSesionConGoogle;
import roomiehub.vivienda.application.usecase.cuenta.ObtenerPerfil;
import roomiehub.vivienda.application.usecase.cuenta.RegistrarDispositivo;
import roomiehub.vivienda.application.usecase.cuenta.RegistrarUsuario;
import roomiehub.vivienda.application.usecase.cuenta.RenovarSesion;
import roomiehub.vivienda.application.usecase.convivencia.ConsultarChat;
import roomiehub.vivienda.application.usecase.convivencia.ConsultarTablon;
import roomiehub.vivienda.application.usecase.convivencia.EliminarMensaje;
import roomiehub.vivienda.application.usecase.convivencia.EnviarMensaje;
import roomiehub.vivienda.application.usecase.convivencia.ReportarMensaje;
import roomiehub.vivienda.application.usecase.convivencia.VistasDeMensajes;
import roomiehub.vivienda.application.usecase.convivencia.CrearNota;
import roomiehub.vivienda.application.usecase.convivencia.DesfijarNotasVencidas;
import roomiehub.vivienda.application.usecase.convivencia.EliminarNota;
import roomiehub.vivienda.application.usecase.convivencia.FijarNota;
import roomiehub.vivienda.application.usecase.convivencia.FirmarSubidaDeImagen;
import roomiehub.vivienda.application.usecase.convivencia.ModerarReportes;
import roomiehub.vivienda.application.usecase.convivencia.ReaccionarNota;
import roomiehub.vivienda.application.usecase.convivencia.ReportarNota;
import roomiehub.vivienda.application.usecase.finanzas.AvisarPago;
import roomiehub.vivienda.application.usecase.finanzas.ConfirmarPago;
import roomiehub.vivienda.application.usecase.finanzas.ConsultarFinanzas;
import roomiehub.vivienda.application.usecase.finanzas.EditarGasto;
import roomiehub.vivienda.application.usecase.finanzas.EliminarGasto;
import roomiehub.vivienda.application.usecase.finanzas.EnviarRecordatorios;
import roomiehub.vivienda.application.usecase.finanzas.GenerarDeudasDelPeriodo;
import roomiehub.vivienda.application.usecase.finanzas.GeneradorDeDeudas;
import roomiehub.vivienda.application.usecase.finanzas.RegistrarGasto;
import roomiehub.vivienda.application.usecase.sala.AbandonarSala;
import roomiehub.vivienda.application.usecase.sala.AccesoASala;
import roomiehub.vivienda.application.usecase.sala.BorrarSalaVencida;
import roomiehub.vivienda.application.usecase.sala.ConsultarSalas;
import roomiehub.vivienda.application.usecase.sala.CrearSala;
import roomiehub.vivienda.application.usecase.sala.DesactivarSala;
import roomiehub.vivienda.application.usecase.sala.ExpulsarMiembro;
import roomiehub.vivienda.application.usecase.sala.ReactivarSala;
import roomiehub.vivienda.application.usecase.sala.SalidaDeMiembro;
import roomiehub.vivienda.application.usecase.sala.TransferirAdministrador;
import roomiehub.vivienda.application.usecase.sala.UnirseASala;
import roomiehub.vivienda.application.usecase.sync.ProcesadorDeAccion;
import roomiehub.vivienda.application.usecase.sync.SincronizarAccionesOffline;
import roomiehub.vivienda.application.usecase.tareas.CompletarTarea;
import roomiehub.vivienda.application.usecase.tareas.ConsultarTareas;
import roomiehub.vivienda.application.usecase.tareas.CrearTarea;
import roomiehub.vivienda.application.usecase.tareas.EditarTarea;
import roomiehub.vivienda.application.usecase.tareas.EliminarTarea;
import roomiehub.vivienda.application.usecase.tareas.PenalizarVencidas;
import roomiehub.vivienda.application.usecase.tareas.ReasignarSinAsignar;
import roomiehub.vivienda.application.usecase.tareas.RolearTareas;
import roomiehub.vivienda.domain.repository.AccionProcesadaRepository;
import roomiehub.vivienda.domain.repository.AsignacionRepository;
import roomiehub.vivienda.domain.repository.DeudaRepository;
import roomiehub.vivienda.domain.repository.DispositivoRepository;
import roomiehub.vivienda.domain.repository.GastoRepository;
import roomiehub.vivienda.domain.repository.MembresiaRepository;
import roomiehub.vivienda.domain.repository.MensajeRepository;
import roomiehub.vivienda.domain.repository.NotaRepository;
import roomiehub.vivienda.domain.repository.PuntosRepository;
import roomiehub.vivienda.domain.repository.ReaccionRepository;
import roomiehub.vivienda.domain.repository.ReporteRepository;
import roomiehub.vivienda.domain.repository.RefreshTokenRepository;
import roomiehub.vivienda.domain.repository.SalaRepository;
import roomiehub.vivienda.domain.repository.TareaRepository;
import roomiehub.vivienda.domain.repository.UsuarioRepository;
import roomiehub.vivienda.domain.service.AsignadorDeTareas;

/** Raíz de composición: los casos de uso no llevan @Service, se registran aquí. */
@Configuration
public class UseCaseConfig {

    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }

    @Bean
    EmisorDeSesiones emisorDeSesiones(EmisorDeTokensPort tokens, RefreshTokenRepository refreshTokens, Clock reloj) {
        return new EmisorDeSesiones(tokens, refreshTokens, reloj);
    }

    @Bean
    RegistrarUsuario registrarUsuario(UsuarioRepository usuarios, HasherDeContrasenasPort hasher,
                                      EmisorDeSesiones sesiones) {
        return new RegistrarUsuario(usuarios, hasher, sesiones);
    }

    @Bean
    IniciarSesion iniciarSesion(UsuarioRepository usuarios, HasherDeContrasenasPort hasher,
                                EmisorDeSesiones sesiones) {
        return new IniciarSesion(usuarios, hasher, sesiones);
    }

    @Bean
    IniciarSesionConGoogle iniciarSesionConGoogle(VerificadorGooglePort google, UsuarioRepository usuarios,
                                                  EmisorDeSesiones sesiones, RefreshTokenRepository refreshTokens,
                                                  Clock reloj) {
        return new IniciarSesionConGoogle(google, usuarios, sesiones, refreshTokens, reloj);
    }

    @Bean
    RenovarSesion renovarSesion(RefreshTokenRepository refreshTokens, UsuarioRepository usuarios,
                                EmisorDeSesiones sesiones, Clock reloj) {
        return new RenovarSesion(refreshTokens, usuarios, sesiones, reloj);
    }

    @Bean
    CerrarSesion cerrarSesion(RefreshTokenRepository refreshTokens, DispositivoRepository dispositivos, Clock reloj) {
        return new CerrarSesion(refreshTokens, dispositivos, reloj);
    }

    @Bean
    ObtenerPerfil obtenerPerfil(UsuarioRepository usuarios) {
        return new ObtenerPerfil(usuarios);
    }

    @Bean
    EliminarCuenta eliminarCuenta(UsuarioRepository usuarios, MembresiaRepository membresias, SalidaDeMiembro salida,
                                  RefreshTokenRepository refreshTokens, DispositivoRepository dispositivos, Clock reloj) {
        return new EliminarCuenta(usuarios, membresias, salida, refreshTokens, dispositivos, reloj);
    }

    @Bean
    RegistrarDispositivo registrarDispositivo(DispositivoRepository dispositivos, Clock reloj) {
        return new RegistrarDispositivo(dispositivos, reloj);
    }

    // ─── Salas ───

    @Bean
    SalidaDeMiembro salidaDeMiembro(SalaRepository salas, MembresiaRepository membresias, TareaRepository tareas,
                                    GastoRepository gastos, DeudaRepository deudas, NotificadorPort notificador,
                                    AlmacenamientoImagenesPort almacenamiento) {
        return new SalidaDeMiembro(salas, membresias, tareas, gastos, deudas, notificador, almacenamiento);
    }

    @Bean
    AbandonarSala abandonarSala(SalidaDeMiembro salida, Clock reloj) {
        return new AbandonarSala(salida, reloj);
    }

    @Bean
    CrearSala crearSala(SalaRepository salas, MembresiaRepository membresias, Clock reloj) {
        return new CrearSala(salas, membresias, reloj);
    }

    @Bean
    UnirseASala unirseASala(SalaRepository salas, MembresiaRepository membresias, PuntosRepository puntos,
                            TareaRepository tareas, NotificadorPort notificador, Clock reloj) {
        return new UnirseASala(salas, membresias, puntos, tareas, notificador, reloj);
    }

    @Bean
    ConsultarSalas consultarSalas(SalaRepository salas, MembresiaRepository membresias, UsuarioRepository usuarios) {
        return new ConsultarSalas(salas, membresias, usuarios);
    }

    @Bean
    ExpulsarMiembro expulsarMiembro(SalaRepository salas, MembresiaRepository membresias, SalidaDeMiembro salida,
                                    Clock reloj) {
        return new ExpulsarMiembro(salas, membresias, salida, reloj);
    }

    @Bean
    TransferirAdministrador transferirAdministrador(SalaRepository salas, MembresiaRepository membresias) {
        return new TransferirAdministrador(salas, membresias);
    }

    @Bean
    DesactivarSala desactivarSala(SalaRepository salas, MembresiaRepository membresias, NotificadorPort notificador,
                                  Clock reloj) {
        return new DesactivarSala(salas, membresias, notificador, reloj);
    }

    @Bean
    ReactivarSala reactivarSala(SalaRepository salas, MembresiaRepository membresias,
                                AsignacionRepository asignaciones, GastoRepository gastos,
                                NotificadorPort notificador, Clock reloj) {
        return new ReactivarSala(salas, membresias, asignaciones, gastos, notificador, reloj);
    }

    @Bean
    AccesoASala accesoASala(SalaRepository salas, MembresiaRepository membresias) {
        return new AccesoASala(salas, membresias);
    }

    @Bean
    BorrarSalaVencida borrarSalaVencida(SalaRepository salas, AlmacenamientoImagenesPort almacenamiento) {
        return new BorrarSalaVencida(salas, almacenamiento);
    }

    // ─── Tareas ───

    @Bean
    AsignadorDeTareas asignadorDeTareas() {
        return new AsignadorDeTareas(new SecureRandom()); // los empates se sortean (sección 6)
    }

    @Bean
    CrearTarea crearTarea(AccesoASala acceso, TareaRepository tareas) {
        return new CrearTarea(acceso, tareas);
    }

    @Bean
    EditarTarea editarTarea(AccesoASala acceso, TareaRepository tareas, Clock reloj) {
        return new EditarTarea(acceso, tareas, reloj);
    }

    @Bean
    EliminarTarea eliminarTarea(AccesoASala acceso, TareaRepository tareas, AsignacionRepository asignaciones) {
        return new EliminarTarea(acceso, tareas, asignaciones);
    }

    @Bean
    CompletarTarea completarTarea(AccesoASala acceso, AsignacionRepository asignaciones, PuntosRepository puntos,
                                  TareaRepository tareas, UsuarioRepository usuarios, MembresiaRepository membresias,
                                  NotificadorPort notificador, Clock reloj) {
        return new CompletarTarea(acceso, asignaciones, puntos, tareas, usuarios, membresias, notificador, reloj);
    }

    @Bean
    ConsultarTareas consultarTareas(AccesoASala acceso, SalaRepository salas, TareaRepository tareas,
                                    AsignacionRepository asignaciones, PuntosRepository puntos,
                                    UsuarioRepository usuarios, Clock reloj) {
        return new ConsultarTareas(acceso, salas, tareas, asignaciones, puntos, usuarios, reloj);
    }

    @Bean
    ReasignarSinAsignar reasignarSinAsignar(AccesoASala acceso, AsignacionRepository asignaciones,
                                            MembresiaRepository membresias, PuntosRepository puntos,
                                            AsignadorDeTareas asignador, NotificadorPort notificador, Clock reloj) {
        return new ReasignarSinAsignar(acceso, asignaciones, membresias, puntos, asignador, notificador, reloj);
    }

    @Bean
    RolearTareas rolearTareas(SalaRepository salas, TareaRepository tareas, AsignacionRepository asignaciones,
                              MembresiaRepository membresias, PuntosRepository puntos, AsignadorDeTareas asignador,
                              NotificadorPort notificador) {
        return new RolearTareas(salas, tareas, asignaciones, membresias, puntos, asignador, notificador);
    }

    @Bean
    PenalizarVencidas penalizarVencidas(SalaRepository salas, AsignacionRepository asignaciones,
                                        PuntosRepository puntos) {
        return new PenalizarVencidas(salas, asignaciones, puntos);
    }

    // ─── Finanzas ───

    @Bean
    GeneradorDeDeudas generadorDeDeudas(GastoRepository gastos, DeudaRepository deudas) {
        return new GeneradorDeDeudas(gastos, deudas);
    }

    @Bean
    RegistrarGasto registrarGasto(AccesoASala acceso, GastoRepository gastos, MembresiaRepository membresias,
                                  GeneradorDeDeudas generador, NotificadorPort notificador, Clock reloj) {
        return new RegistrarGasto(acceso, gastos, membresias, generador, notificador, reloj);
    }

    @Bean
    EditarGasto editarGasto(AccesoASala acceso, GastoRepository gastos, DeudaRepository deudas,
                            MembresiaRepository membresias, Clock reloj) {
        return new EditarGasto(acceso, gastos, deudas, membresias, reloj);
    }

    @Bean
    EliminarGasto eliminarGasto(AccesoASala acceso, GastoRepository gastos, Clock reloj) {
        return new EliminarGasto(acceso, gastos, reloj);
    }

    @Bean
    AvisarPago avisarPago(AccesoASala acceso, DeudaRepository deudas, GastoRepository gastos,
                          NotificadorPort notificador, Clock reloj) {
        return new AvisarPago(acceso, deudas, gastos, notificador, reloj);
    }

    @Bean
    ConfirmarPago confirmarPago(AccesoASala acceso, DeudaRepository deudas, GastoRepository gastos,
                                NotificadorPort notificador) {
        return new ConfirmarPago(acceso, deudas, gastos, notificador);
    }

    @Bean
    ConsultarFinanzas consultarFinanzas(AccesoASala acceso, GastoRepository gastos, DeudaRepository deudas,
                                        UsuarioRepository usuarios) {
        return new ConsultarFinanzas(acceso, gastos, deudas, usuarios);
    }

    @Bean
    GenerarDeudasDelPeriodo generarDeudasDelPeriodo(SalaRepository salas, GastoRepository gastos,
                                                    GeneradorDeDeudas generador) {
        return new GenerarDeudasDelPeriodo(salas, gastos, generador);
    }

    @Bean
    EnviarRecordatorios enviarRecordatorios(SalaRepository salas, DeudaRepository deudas, GastoRepository gastos,
                                            NotificadorPort notificador) {
        return new EnviarRecordatorios(salas, deudas, gastos, notificador);
    }

    // ─── Convivencia: tablón ───

    @Bean
    CrearNota crearNota(AccesoASala acceso, NotaRepository notas, AlmacenamientoImagenesPort almacenamiento,
                        NotificadorPort notificador, Clock reloj) {
        return new CrearNota(acceso, notas, almacenamiento, notificador, reloj);
    }

    @Bean
    FirmarSubidaDeImagen firmarSubidaDeImagen(AccesoASala acceso, AlmacenamientoImagenesPort almacenamiento) {
        return new FirmarSubidaDeImagen(acceso, almacenamiento);
    }

    @Bean
    ReaccionarNota reaccionarNota(AccesoASala acceso, NotaRepository notas, ReaccionRepository reacciones) {
        return new ReaccionarNota(acceso, notas, reacciones);
    }

    @Bean
    ReportarNota reportarNota(AccesoASala acceso, NotaRepository notas, ReporteRepository reportes,
                              NotificadorPort notificador, Clock reloj) {
        return new ReportarNota(acceso, notas, reportes, notificador, reloj);
    }

    @Bean
    FijarNota fijarNota(AccesoASala acceso, NotaRepository notas, Clock reloj) {
        return new FijarNota(acceso, notas, reloj);
    }

    @Bean
    EliminarNota eliminarNota(AccesoASala acceso, NotaRepository notas) {
        return new EliminarNota(acceso, notas);
    }

    @Bean
    ConsultarTablon consultarTablon(AccesoASala acceso, NotaRepository notas, ReaccionRepository reacciones,
                                    UsuarioRepository usuarios, AlmacenamientoImagenesPort almacenamiento,
                                    Clock reloj) {
        return new ConsultarTablon(acceso, notas, reacciones, usuarios, almacenamiento, reloj);
    }

    @Bean
    ModerarReportes moderarReportes(AccesoASala acceso, ReporteRepository reportes, NotaRepository notas,
                                    MensajeRepository mensajes, UsuarioRepository usuarios) {
        return new ModerarReportes(acceso, reportes, notas, mensajes, usuarios);
    }

    // ─── Convivencia: chat ───

    @Bean
    VistasDeMensajes vistasDeMensajes(UsuarioRepository usuarios, NotaRepository notas,
                                      AlmacenamientoImagenesPort almacenamiento) {
        return new VistasDeMensajes(usuarios, notas, almacenamiento);
    }

    @Bean
    EnviarMensaje enviarMensaje(AccesoASala acceso, MensajeRepository mensajes, NotaRepository notas,
                                VistasDeMensajes vistas, ChatEnVivoPort chatEnVivo, NotificadorPort notificador,
                                Clock reloj) {
        return new EnviarMensaje(acceso, mensajes, notas, vistas, chatEnVivo, notificador, reloj);
    }

    @Bean
    ConsultarChat consultarChat(AccesoASala acceso, MensajeRepository mensajes, VistasDeMensajes vistas) {
        return new ConsultarChat(acceso, mensajes, vistas);
    }

    @Bean
    EliminarMensaje eliminarMensaje(AccesoASala acceso, MensajeRepository mensajes, ChatEnVivoPort chatEnVivo) {
        return new EliminarMensaje(acceso, mensajes, chatEnVivo);
    }

    @Bean
    ReportarMensaje reportarMensaje(AccesoASala acceso, MensajeRepository mensajes, ReporteRepository reportes,
                                    NotificadorPort notificador, Clock reloj) {
        return new ReportarMensaje(acceso, mensajes, reportes, notificador, reloj);
    }

    // ─── Sincronización offline ───

    @Bean
    ProcesadorDeAccion procesadorDeAccion(MembresiaRepository membresias, AccionProcesadaRepository procesadas,
                                          CrearNota crearNota, EnviarMensaje enviarMensaje, AvisarPago avisarPago,
                                          ConfirmarPago confirmarPago, CompletarTarea completarTarea) {
        return new ProcesadorDeAccion(membresias, procesadas, crearNota, enviarMensaje, avisarPago, confirmarPago,
                completarTarea);
    }

    @Bean
    SincronizarAccionesOffline sincronizarAccionesOffline(ProcesadorDeAccion procesador) {
        return new SincronizarAccionesOffline(procesador);
    }

    @Bean
    DesfijarNotasVencidas desfijarNotasVencidas(NotaRepository notas) {
        return new DesfijarNotasVencidas(notas);
    }
}
