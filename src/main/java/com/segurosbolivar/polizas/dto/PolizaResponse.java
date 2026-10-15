package com.segurosbolivar.polizas.dto;

import com.segurosbolivar.polizas.domain.Poliza;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Lo que la API expone de una póliza. Desacopla el contrato HTTP de la entidad JPA. */
public record PolizaResponse(
        Long id,
        String tipo,
        String estado,
        String tomador,
        String beneficiario,
        LocalDate fechaInicioVigencia,
        LocalDate fechaFinVigencia,
        int mesesVigencia,
        BigDecimal valorCanon,
        BigDecimal valorPrima,
        long riesgosActivos
) {
    public static PolizaResponse desde(Poliza p) {
        return new PolizaResponse(
                p.getId(),
                p.getTipo().name(),
                p.getEstado().name(),
                p.getTomador(),
                p.getBeneficiario(),
                p.getFechaInicioVigencia(),
                p.getFechaFinVigencia(),
                p.getMesesVigencia(),
                p.getValorCanon(),
                p.getValorPrima(),
                p.getRiesgos().stream().filter(r -> r.estaActivo()).count()
        );
    }
}
