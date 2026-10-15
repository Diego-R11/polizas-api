package com.segurosbolivar.polizas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearRiesgoRequest(
        @NotBlank(message = "La dirección del inmueble es obligatoria")
        @Size(max = 200)
        String direccionInmueble,

        @NotBlank(message = "El arrendatario es obligatorio")
        @Size(max = 150)
        String arrendatario
) {
}
