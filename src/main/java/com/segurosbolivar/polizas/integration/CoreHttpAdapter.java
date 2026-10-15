package com.segurosbolivar.polizas.integration;

import com.segurosbolivar.polizas.dto.CoreEventoRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adapter HTTP hacia el CORE. Envía el evento ACTUALIZACION cada vez que
 * cambia el estado de una póliza o de un riesgo.
 *
 * Si el CORE no responde, se registra el error y la operación del usuario
 * no se revierte. En producción esto se reemplazaría por un patrón Outbox
 * con reintentos (ver documento de diseño, módulo 1).
 */
@Component
public class CoreHttpAdapter implements CoreGateway {

    private static final Logger log = LoggerFactory.getLogger(CoreHttpAdapter.class);
    static final String EVENTO_ACTUALIZACION = "ACTUALIZACION";

    private final RestClient restClient;
    private final String coreUrl;
    private final String apiKey;

    public CoreHttpAdapter(RestClient.Builder builder,
                           @Value("${app.core.url}") String coreUrl,
                           @Value("${app.security.api-key}") String apiKey) {
        this.restClient = builder.build();
        this.coreUrl = coreUrl;
        this.apiKey = apiKey;
    }

    @Override
    public void notificarActualizacion(Long polizaId) {
        CoreEventoRequest evento = new CoreEventoRequest(EVENTO_ACTUALIZACION, polizaId);
        try {
            restClient.post()
                    .uri(coreUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-api-key", apiKey)
                    .body(evento)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Evento {} enviado al CORE para la póliza {}", EVENTO_ACTUALIZACION, polizaId);
        } catch (RestClientException e) {
            log.error("No fue posible enviar el evento {} al CORE para la póliza {}: {}",
                    EVENTO_ACTUALIZACION, polizaId, e.getMessage());
        }
    }
}
