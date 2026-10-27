package roomiehub.vivienda.adapters.web;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import roomiehub.vivienda.application.usecase.sync.AccionOffline;
import roomiehub.vivienda.application.usecase.sync.ResultadoAccion;
import roomiehub.vivienda.application.usecase.sync.SincronizarAccionesOffline;

/**
 * CU-27: la app envía su cola offline completa, en el orden en que la generó, y recibe un resultado por
 * acción. Siempre responde 200 aunque haya rechazos: cada resultado dice qué hacer con su acción.
 */
@RestController
public class SyncController {

    public record AccionRequest(@NotNull UUID uuid, @NotNull AccionOffline.Tipo tipo, @NotNull Long salaId,
                                @Size(max = 4000) String texto, @Size(max = 200) String imagenId,
                                Long notaRespondidaId, Long deudaId, Long asignacionId) {
        AccionOffline aAccion() {
            return new AccionOffline(uuid, tipo, salaId, texto, imagenId, notaRespondidaId, deudaId, asignacionId);
        }
    }

    public record SyncRequest(@NotNull @Size(max = SincronizarAccionesOffline.MAXIMO_POR_LOTE)
                              List<@Valid @NotNull AccionRequest> acciones) {}

    public record SyncResponse(List<ResultadoAccion> resultados, Set<Long> salasExpulsadas) {}

    private final SincronizarAccionesOffline sincronizar;

    public SyncController(SincronizarAccionesOffline sincronizar) {
        this.sincronizar = sincronizar;
    }

    @PostMapping("/sync")
    public SyncResponse sincronizar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SyncRequest r) {
        SincronizarAccionesOffline.Resultado resultado = sincronizar.ejecutar(Long.parseLong(jwt.getSubject()),
                r.acciones().stream().map(AccionRequest::aAccion).toList());
        return new SyncResponse(resultado.resultados(), resultado.salasExpulsadas());
    }
}
