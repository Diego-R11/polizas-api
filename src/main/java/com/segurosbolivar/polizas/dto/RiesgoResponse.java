package com.segurosbolivar.polizas.dto;

import com.segurosbolivar.polizas.domain.Riesgo;

public record RiesgoResponse(
        Long id,
        Long polizaId,
        String direccionInmueble,
        String arrendatario,
        String estado
) {
    public static RiesgoResponse desde(Riesgo r) {
        return new RiesgoResponse(
                r.getId(),
                r.getPoliza().getId(),
                r.getDireccionInmueble(),
                r.getArrendatario(),
                r.getEstado().name()
        );
    }
}
