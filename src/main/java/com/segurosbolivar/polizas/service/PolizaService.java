package com.segurosbolivar.polizas.service;

import com.segurosbolivar.polizas.domain.EstadoPoliza;
import com.segurosbolivar.polizas.domain.Poliza;
import com.segurosbolivar.polizas.domain.Riesgo;
import com.segurosbolivar.polizas.domain.TipoPoliza;
import com.segurosbolivar.polizas.dto.CrearRiesgoRequest;
import com.segurosbolivar.polizas.dto.PolizaResponse;
import com.segurosbolivar.polizas.dto.RiesgoResponse;
import com.segurosbolivar.polizas.exception.RecursoNoEncontradoException;
import com.segurosbolivar.polizas.exception.ReglaNegocioException;
import com.segurosbolivar.polizas.integration.CoreGateway;
import com.segurosbolivar.polizas.repository.PolizaRepository;
import com.segurosbolivar.polizas.repository.RiesgoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Casos de uso sobre pólizas. Orquesta: busca la póliza, delega la regla
 * de negocio a la entidad, guarda y notifica al CORE.
 */
@Service
public class PolizaService {

    private final PolizaRepository polizaRepository;
    private final RiesgoRepository riesgoRepository;
    private final CoreGateway coreGateway;
    private final BigDecimal ipcVigente;

    public PolizaService(PolizaRepository polizaRepository,
                         RiesgoRepository riesgoRepository,
                         CoreGateway coreGateway,
                         @Value("${app.renovacion.ipc}") BigDecimal ipcVigente) {
        this.polizaRepository = polizaRepository;
        this.riesgoRepository = riesgoRepository;
        this.coreGateway = coreGateway;
        this.ipcVigente = ipcVigente;
    }

    @Transactional(readOnly = true)
    public List<PolizaResponse> listar(TipoPoliza tipo, EstadoPoliza estado) {
        return polizaRepository.buscar(tipo, estado).stream()
                .map(PolizaResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public PolizaResponse obtener(Long id) {
        return PolizaResponse.desde(buscarPoliza(id));
    }

    @Transactional(readOnly = true)
    public List<RiesgoResponse> listarRiesgos(Long polizaId) {
        return buscarPoliza(polizaId).getRiesgos().stream()
                .map(RiesgoResponse::desde)
                .toList();
    }

    @Transactional
    public PolizaResponse renovar(Long id) {
        Poliza poliza = buscarPoliza(id);
        poliza.renovar(ipcVigente);
        polizaRepository.save(poliza);
        coreGateway.notificarActualizacion(poliza.getId());
        return PolizaResponse.desde(poliza);
    }

    @Transactional
    public PolizaResponse cancelar(Long id) {
        Poliza poliza = buscarPoliza(id);
        poliza.cancelar();
        polizaRepository.save(poliza);
        coreGateway.notificarActualizacion(poliza.getId());
        return PolizaResponse.desde(poliza);
    }

    /** Regla: solo se agregan riesgos por este endpoint si la póliza es COLECTIVA. */
    @Transactional
    public RiesgoResponse agregarRiesgo(Long polizaId, CrearRiesgoRequest request) {
        Poliza poliza = buscarPoliza(polizaId);
        if (poliza.getTipo() != TipoPoliza.COLECTIVA) {
            throw new ReglaNegocioException(
                    "Solo se pueden agregar riesgos a pólizas colectivas. La póliza " + polizaId + " es individual");
        }
        Riesgo riesgo = new Riesgo(request.direccionInmueble(), request.arrendatario());
        poliza.agregarRiesgo(riesgo);
        // persist directo del riesgo nuevo: así esta misma instancia recibe su id para la respuesta
        riesgoRepository.save(riesgo);
        coreGateway.notificarActualizacion(poliza.getId());
        return RiesgoResponse.desde(riesgo);
    }

    private Poliza buscarPoliza(Long id) {
        return polizaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la póliza " + id));
    }
}
