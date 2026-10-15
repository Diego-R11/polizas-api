package com.segurosbolivar.polizas.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Seguridad mínima: toda petición debe traer el header x-api-key con el valor configurado.
 * Si falta o es incorrecto, se responde 401 antes de llegar a los controllers.
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyFilter.class);
    public static final String HEADER = "x-api-key";

    private final byte[] apiKeyEsperada;

    public ApiKeyFilter(@Value("${app.security.api-key}") String apiKey) {
        this.apiKeyEsperada = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String recibida = request.getHeader(HEADER);
        // MessageDigest.isEqual compara en tiempo constante (evita ataques por tiempo de respuesta)
        boolean valida = recibida != null
                && MessageDigest.isEqual(apiKeyEsperada, recibida.getBytes(StandardCharsets.UTF_8));

        if (!valida) {
            log.warn("Petición rechazada sin api key válida: {} {}", request.getMethod(), request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(
                    "{\"status\":401,\"error\":\"Unauthorized\",\"mensaje\":\"Header x-api-key ausente o inválido\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
