package roomiehub.vivienda.adapters.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import roomiehub.vivienda.application.usecase.finanzas.AvisarPago;
import roomiehub.vivienda.application.usecase.finanzas.ConfirmarPago;
import roomiehub.vivienda.application.usecase.finanzas.ConsultarFinanzas;
import roomiehub.vivienda.application.usecase.finanzas.EditarGasto;
import roomiehub.vivienda.application.usecase.finanzas.EliminarGasto;
import roomiehub.vivienda.application.usecase.finanzas.RegistrarGasto;
import roomiehub.vivienda.domain.model.finanzas.Deuda;
import roomiehub.vivienda.domain.model.finanzas.EstadoDeuda;
import roomiehub.vivienda.domain.model.finanzas.FrecuenciaGasto;
import roomiehub.vivienda.domain.model.finanzas.GastoCompartido;
import roomiehub.vivienda.domain.model.finanzas.Moneda;

// CU-20 a CU-22 los montos viajan como texto para no perder centavos
@RestController
public class FinanzasController {

    public record CrearGastoRequest(@NotBlank @Size(max = 80) String concepto, @NotNull BigDecimal monto,
                                    @NotNull Moneda moneda, @NotNull FrecuenciaGasto frecuencia,
                                    @NotNull LocalDate fechaInicio, @NotEmpty List<Long> participantes) {}

    public record EditarGastoRequest(@NotBlank @Size(max = 80) String concepto, @NotNull BigDecimal monto,
                                     @NotNull FrecuenciaGasto frecuencia, @NotEmpty List<Long> participantes) {}

    public record GastoResponse(long id, String concepto, String monto, Moneda moneda, FrecuenciaGasto frecuencia,
                                LocalDate fechaInicio, List<Long> participantes) {
        static GastoResponse de(GastoCompartido g, List<Long> participantes) {
            return new GastoResponse(g.id(), g.concepto(), g.monto().monto().toPlainString(), g.monto().moneda(),
                    g.frecuencia(), g.fechaInicio(), participantes);
        }
    }

    public record DeudaResponse(long id, long gastoId, String concepto, LocalDate periodo, long deudorId,
                                String deudor, String monto, Moneda moneda, EstadoDeuda estado, Instant avisoPagoEn,
                                Set<Long> confirmadores, Long deudaOrigenId) {
        static DeudaResponse de(Deuda d, String concepto, String deudor) {
            return new DeudaResponse(d.id(), d.servicioId(), concepto, d.periodo(), d.deudorId(), deudor,
                    d.monto().monto().toPlainString(), d.monto().moneda(), d.estado(), d.avisoPagoEn(),
                    d.confirmadores(), d.deudaOrigenId());
        }
    }

    private final RegistrarGasto registrarGasto;
    private final EditarGasto editarGasto;
    private final EliminarGasto eliminarGasto;
    private final AvisarPago avisarPago;
    private final ConfirmarPago confirmarPago;
    private final ConsultarFinanzas consultarFinanzas;

    public FinanzasController(RegistrarGasto registrarGasto, EditarGasto editarGasto, EliminarGasto eliminarGasto,
                              AvisarPago avisarPago, ConfirmarPago confirmarPago, ConsultarFinanzas consultarFinanzas) {
        this.registrarGasto = registrarGasto;
        this.editarGasto = editarGasto;
        this.eliminarGasto = eliminarGasto;
        this.avisarPago = avisarPago;
        this.confirmarPago = confirmarPago;
        this.consultarFinanzas = consultarFinanzas;
    }

    @GetMapping("/salas/{salaId}/gastos")
    public List<GastoResponse> gastos(@AuthenticationPrincipal Jwt jwt, @PathVariable long salaId) {
        return consultarFinanzas.gastos(usuario(jwt), salaId).stream()
                .map(g -> GastoResponse.de(g.gasto(), g.participantes()))
                .toList();
    }

    @PostMapping("/salas/{salaId}/gastos")
    @ResponseStatus(HttpStatus.CREATED)
    public GastoResponse crear(@AuthenticationPrincipal Jwt jwt, @PathVariable long salaId,
                               @Valid @RequestBody CrearGastoRequest r) {
        GastoCompartido g = registrarGasto.ejecutar(usuario(jwt), salaId, r.concepto(), r.monto(), r.moneda(),
                r.frecuencia(), r.fechaInicio(), r.participantes());
        return GastoResponse.de(g, r.participantes());
    }

    @PutMapping("/salas/{salaId}/gastos/{gastoId}")
    public GastoResponse editar(@AuthenticationPrincipal Jwt jwt, @PathVariable long salaId,
                                @PathVariable long gastoId, @Valid @RequestBody EditarGastoRequest r) {
        GastoCompartido g = editarGasto.ejecutar(usuario(jwt), salaId, gastoId, r.concepto(), r.monto(),
                r.frecuencia(), r.participantes());
        return GastoResponse.de(g, r.participantes());
    }

    @DeleteMapping("/salas/{salaId}/gastos/{gastoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@AuthenticationPrincipal Jwt jwt, @PathVariable long salaId, @PathVariable long gastoId) {
        eliminarGasto.ejecutar(usuario(jwt), salaId, gastoId);
    }

    @GetMapping("/salas/{salaId}/deudas")
    public List<DeudaResponse> deudas(@AuthenticationPrincipal Jwt jwt, @PathVariable long salaId,
                                      @RequestParam(defaultValue = "PENDIENTE") EstadoDeuda estado) {
        return consultarFinanzas.deudas(usuario(jwt), salaId, estado).stream()
                .map(d -> DeudaResponse.de(d.deuda(), d.concepto(), d.deudor()))
                .toList();
    }

    @PostMapping("/deudas/{deudaId}/aviso")
    public DeudaResponse avisar(@AuthenticationPrincipal Jwt jwt, @PathVariable long deudaId) {
        return DeudaResponse.de(avisarPago.ejecutar(usuario(jwt), deudaId), null, null);
    }

    @PostMapping("/deudas/{deudaId}/confirmaciones")
    public DeudaResponse confirmar(@AuthenticationPrincipal Jwt jwt, @PathVariable long deudaId) {
        return DeudaResponse.de(confirmarPago.ejecutar(usuario(jwt), deudaId), null, null);
    }

    private static long usuario(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
