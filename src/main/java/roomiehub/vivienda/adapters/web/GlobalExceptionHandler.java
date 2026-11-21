package roomiehub.vivienda.adapters.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import roomiehub.vivienda.application.port.ServicioNoDisponibleException;
import roomiehub.vivienda.domain.exception.CredencialesInvalidasException;
import roomiehub.vivienda.domain.exception.DatoInvalidoException;
import roomiehub.vivienda.domain.exception.DomainException;
import roomiehub.vivienda.domain.exception.OperacionNoPermitidaException;
import roomiehub.vivienda.domain.exception.RecursoNoEncontradoException;

// Convierte los errores de negocio a respuestas HTTP sin que el dominio sepa de HTTP
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    ProblemDetail reglaDeNegocio(DomainException e) {
        return ProblemDetail.forStatusAndDetail(estado(e), e.getMessage());
    }

    @ExceptionHandler(DemasiadosIntentosException.class)
    ResponseEntity<ProblemDetail> demasiadosIntentos(DemasiadosIntentosException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(e.segundos()))
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, e.getMessage()));
    }

    @ExceptionHandler(ServicioNoDisponibleException.class)
    ProblemDetail servicioNoDisponible(ServicioNoDisponibleException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validacion(MethodArgumentNotValidException e) {
        String detalle = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst()
                .orElse("Petición inválida");
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalle);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail cuerpoIlegible(HttpMessageNotReadableException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es JSON válido");
    }

    // Un id o enum mal escrito en la ruta o en la query
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail tipoInvalido(MethodArgumentTypeMismatchException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getName() + ": valor inválido");
    }

    // Dos peticiones al mismo tiempo que chocan con una restriccion unica como el mismo correo
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail conflicto(DataIntegrityViolationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "El recurso ya existe o cambió; intenta de nuevo");
    }

    // No manda detalles internos y van al log
    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> inesperado(Exception e) {
        // Los errores de Spring ya traen su codigo 4xx
        if (e instanceof ErrorResponse respuesta) {
            return ResponseEntity.status(respuesta.getStatusCode()).headers(respuesta.getHeaders())
                    .body(respuesta.getBody());
        }
        log.error("Error no controlado", e);
        return ResponseEntity.internalServerError()
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno"));
    }

    private static HttpStatus estado(DomainException e) {
        if (e instanceof CredencialesInvalidasException) return HttpStatus.UNAUTHORIZED;
        if (e instanceof OperacionNoPermitidaException) return HttpStatus.FORBIDDEN;
        if (e instanceof DatoInvalidoException) return HttpStatus.BAD_REQUEST;
        if (e instanceof RecursoNoEncontradoException) return HttpStatus.NOT_FOUND;
        // El estado actual no permite la accion
        return HttpStatus.CONFLICT;
    }
}
