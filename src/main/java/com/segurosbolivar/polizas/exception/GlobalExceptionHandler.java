package com.segurosbolivar.polizas.exception;

import com.segurosbolivar.polizas.controller.PolizaController;
import com.segurosbolivar.polizas.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/** Traduce excepciones a respuestas HTTP con un formato de error uniforme. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException e, HttpServletRequest req) {
        return responder(HttpStatus.NOT_FOUND, e.getMessage(), req);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponse> reglaNegocio(ReglaNegocioException e, HttpServletRequest req) {
        log.warn("Regla de negocio violada en {}: {}", req.getRequestURI(), e.getMessage());
        return responder(HttpStatus.CONFLICT, e.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException e, HttpServletRequest req) {
        String mensaje = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return responder(HttpStatus.BAD_REQUEST, mensaje, req);
    }

    @ExceptionHandler({
            PolizaController.FiltroInvalidoException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ErrorResponse> peticionInvalida(Exception e, HttpServletRequest req) {
        String mensaje = e instanceof PolizaController.FiltroInvalidoException
                ? e.getMessage()
                : "Petición mal formada";
        return responder(HttpStatus.BAD_REQUEST, mensaje, req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> inesperado(Exception e, HttpServletRequest req) {
        log.error("Error inesperado en {}", req.getRequestURI(), e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", req);
    }

    private ResponseEntity<ErrorResponse> responder(HttpStatus status, String mensaje, HttpServletRequest req) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.de(status.value(), status.getReasonPhrase(), mensaje, req.getRequestURI()));
    }
}
