package com.segurosbolivar.polizas.controller;

import com.segurosbolivar.polizas.dto.CoreEventoRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Simula el servicio agnóstico de edición expuesto por la capa media (WebLogic)
 * que mantiene actualizado el CORE. Su único propósito es dejar en los logs
 * que la operación se intentó enviar al CORE.
 */
@RestController
@RequestMapping("/core-mock")
public class CoreMockController {

    private static final Logger log = LoggerFactory.getLogger(CoreMockController.class);

    @PostMapping("/evento")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> recibirEvento(@Valid @RequestBody CoreEventoRequest evento) {
        log.info("[CORE-MOCK] Evento recibido: evento={}, polizaId={}", evento.evento(), evento.polizaId());
        return Map.of("registrado", true, "evento", evento.evento(), "polizaId", evento.polizaId());
    }
}
