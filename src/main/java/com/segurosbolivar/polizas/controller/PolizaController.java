package com.segurosbolivar.polizas.controller;

import com.segurosbolivar.polizas.domain.EstadoPoliza;
import com.segurosbolivar.polizas.domain.TipoPoliza;
import com.segurosbolivar.polizas.dto.CrearRiesgoRequest;
import com.segurosbolivar.polizas.dto.PolizaResponse;
import com.segurosbolivar.polizas.dto.RiesgoResponse;
import com.segurosbolivar.polizas.exception.ReglaNegocioException;
import com.segurosbolivar.polizas.service.PolizaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

/**
 * Capa HTTP: recibe la petición, valida el formato y delega al servicio.
 * No contiene reglas de negocio.
 */
@RestController
@RequestMapping("/polizas")
public class PolizaController {

    private final PolizaService polizaService;

    public PolizaController(PolizaService polizaService) {
        this.polizaService = polizaService;
    }

    /** GET /polizas?tipo=COLECTIVA&estado=ACTIVA (ambos filtros opcionales, sin distinguir mayúsculas). */
    @GetMapping
    public List<PolizaResponse> listar(@RequestParam(required = false) String tipo,
                                       @RequestParam(required = false) String estado) {
        return polizaService.listar(
                aEnum(TipoPoliza.class, tipo, "tipo"),
                aEnum(EstadoPoliza.class, estado, "estado"));
    }

    @GetMapping("/{id}")
    public PolizaResponse obtener(@PathVariable Long id) {
        return polizaService.obtener(id);
    }

    @GetMapping("/{id}/riesgos")
    public List<RiesgoResponse> listarRiesgos(@PathVariable Long id) {
        return polizaService.listarRiesgos(id);
    }

    @PostMapping("/{id}/renovar")
    public PolizaResponse renovar(@PathVariable Long id) {
        return polizaService.renovar(id);
    }

    @PostMapping("/{id}/cancelar")
    public PolizaResponse cancelar(@PathVariable Long id) {
        return polizaService.cancelar(id);
    }

    @PostMapping("/{id}/riesgos")
    @ResponseStatus(HttpStatus.CREATED)
    public RiesgoResponse agregarRiesgo(@PathVariable Long id,
                                        @Valid @RequestBody CrearRiesgoRequest request) {
        return polizaService.agregarRiesgo(id, request);
    }

    private static <E extends Enum<E>> E aEnum(Class<E> tipoEnum, String valor, String nombreParametro) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(tipoEnum, valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new FiltroInvalidoException("Valor inválido para '" + nombreParametro + "': " + valor);
        }
    }

    /** Filtro de consulta mal formado: se responde 400. */
    public static class FiltroInvalidoException extends RuntimeException {
        public FiltroInvalidoException(String mensaje) {
            super(mensaje);
        }
    }
}
