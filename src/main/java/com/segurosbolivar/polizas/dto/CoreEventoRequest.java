package com.segurosbolivar.polizas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Cuerpo del evento enviado al CORE: { "evento": "ACTUALIZACION", "polizaId": 555 } */
public record CoreEventoRequest(
        @NotBlank String evento,
        @NotNull Long polizaId
) {
}
