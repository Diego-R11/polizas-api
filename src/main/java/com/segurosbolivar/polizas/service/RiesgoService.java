package com.segurosbolivar.polizas.service;

import com.segurosbolivar.polizas.domain.Riesgo;
import com.segurosbolivar.polizas.domain.TipoPoliza;
import com.segurosbolivar.polizas.dto.RiesgoResponse;
import com.segurosbolivar.polizas.exception.RecursoNoEncontradoException;
import com.segurosbolivar.polizas.exception.ReglaNegocioException;
import com.segurosbolivar.polizas.integration.CoreGateway;
import com.segurosbolivar.polizas.repository.RiesgoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiesgoService {

    private final RiesgoRepository riesgoRepository;
    private final CoreGateway coreGateway;

    public RiesgoService(RiesgoRepository riesgoRepository, CoreGateway coreGateway) {
        this.riesgoRepository = riesgoRepository;
        this.coreGateway = coreGateway;
    }

    /**
     * Cancela un riesgo de una póliza colectiva.
     * En una individual el único riesgo es la póliza misma, por eso se
     * pide cancelar la póliza (que cancela su riesgo en cascada).
     */
    @Transactional
    public RiesgoResponse cancelar(Long riesgoId) {
        Riesgo riesgo = riesgoRepository.findById(riesgoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el riesgo " + riesgoId));
        if (riesgo.getPoliza().getTipo() == TipoPoliza.INDIVIDUAL) {
            throw new ReglaNegocioException(
                    "El riesgo pertenece a una póliza individual: cancele la póliza " + riesgo.getPoliza().getId());
        }
        riesgo.cancelar();
        riesgoRepository.save(riesgo);
        coreGateway.notificarActualizacion(riesgo.getPoliza().getId());
        return RiesgoResponse.desde(riesgo);
    }
}
