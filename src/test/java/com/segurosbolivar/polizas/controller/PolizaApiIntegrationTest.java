package com.segurosbolivar.polizas.controller;

import com.segurosbolivar.polizas.domain.Poliza;
import com.segurosbolivar.polizas.domain.Riesgo;
import com.segurosbolivar.polizas.domain.TipoPoliza;
import com.segurosbolivar.polizas.integration.CoreGateway;
import com.segurosbolivar.polizas.repository.PolizaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Prueba los endpoints de punta a punta: filtro de seguridad, controller, servicio y base H2. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PolizaApiIntegrationTest {

    private static final String API_KEY = "123456";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PolizaRepository repo;

    @MockBean
    private CoreGateway coreGateway;

    private Long individualId;
    private Long colectivaId;
    private Long riesgoColectivaId;
    private Long riesgoIndividualId;

    @BeforeEach
    void preparar() {
        repo.deleteAll();
        Poliza individual = new Poliza(TipoPoliza.INDIVIDUAL, "Laura", "Carlos",
                LocalDate.of(2026, 1, 1), 12, new BigDecimal("1000000"));
        individual.agregarRiesgo(new Riesgo("Calle 1", "Laura"));
        Poliza colectiva = new Poliza(TipoPoliza.COLECTIVA, "Inmobiliaria", "Arrendadores",
                LocalDate.of(2026, 1, 1), 6, new BigDecimal("2000000"));
        colectiva.agregarRiesgo(new Riesgo("Calle 2", "Andrés"));

        individual = repo.save(individual);
        colectiva = repo.save(colectiva);
        individualId = individual.getId();
        colectivaId = colectiva.getId();
        riesgoIndividualId = individual.getRiesgos().get(0).getId();
        riesgoColectivaId = colectiva.getRiesgos().get(0).getId();
        reset(coreGateway);
    }

    @Test
    void sinApiKeyResponde401() throws Exception {
        mvc.perform(get("/polizas")).andExpect(status().isUnauthorized());
    }

    @Test
    void conApiKeyIncorrectaResponde401() throws Exception {
        mvc.perform(get("/polizas").header("x-api-key", "otra")).andExpect(status().isUnauthorized());
    }

    @Test
    void listaPolizasFiltrandoPorTipoYEstado() throws Exception {
        mvc.perform(get("/polizas").param("tipo", "colectiva").param("estado", "ACTIVA")
                        .header("x-api-key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tipo").value("COLECTIVA"));
    }

    @Test
    void filtroInvalidoResponde400() throws Exception {
        mvc.perform(get("/polizas").param("tipo", "OTRA").header("x-api-key", API_KEY))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listaRiesgosDeUnaPoliza() throws Exception {
        mvc.perform(get("/polizas/{id}/riesgos", colectivaId).header("x-api-key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].arrendatario").value("Andrés"));
    }

    @Test
    void polizaInexistenteResponde404() throws Exception {
        mvc.perform(get("/polizas/{id}/riesgos", 9999).header("x-api-key", API_KEY))
                .andExpect(status().isNotFound());
    }

    @Test
    void renovarAplicaIpcYNotificaAlCore() throws Exception {
        mvc.perform(post("/polizas/{id}/renovar", individualId).header("x-api-key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RENOVADA"))
                .andExpect(jsonPath("$.valorCanon").value(1051000.00))
                .andExpect(jsonPath("$.valorPrima").value(12612000.00));
        verify(coreGateway).notificarActualizacion(individualId);
    }

    @Test
    void noSeRenuevaUnaPolizaCancelada() throws Exception {
        mvc.perform(post("/polizas/{id}/cancelar", individualId).header("x-api-key", API_KEY))
                .andExpect(status().isOk());
        mvc.perform(post("/polizas/{id}/renovar", individualId).header("x-api-key", API_KEY))
                .andExpect(status().isConflict());
    }

    @Test
    void cancelarPolizaCancelaSusRiesgos() throws Exception {
        mvc.perform(post("/polizas/{id}/cancelar", colectivaId).header("x-api-key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.riesgosActivos").value(0));
        mvc.perform(get("/polizas/{id}/riesgos", colectivaId).header("x-api-key", API_KEY))
                .andExpect(jsonPath("$[0].estado").value("CANCELADO"));
    }

    @Test
    void agregarRiesgoAColectivaResponde201() throws Exception {
        mvc.perform(post("/polizas/{id}/riesgos", colectivaId).header("x-api-key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direccionInmueble\":\"Calle 3\",\"arrendatario\":\"María\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.polizaId").value(colectivaId));
        verify(coreGateway).notificarActualizacion(colectivaId);
    }

    @Test
    void agregarRiesgoAIndividualResponde409() throws Exception {
        mvc.perform(post("/polizas/{id}/riesgos", individualId).header("x-api-key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direccionInmueble\":\"Calle 3\",\"arrendatario\":\"María\"}"))
                .andExpect(status().isConflict());
        verify(coreGateway, never()).notificarActualizacion(anyLong());
    }

    @Test
    void agregarRiesgoSinDatosResponde400() throws Exception {
        mvc.perform(post("/polizas/{id}/riesgos", colectivaId).header("x-api-key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direccionInmueble\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelarRiesgoDeColectiva() throws Exception {
        mvc.perform(post("/riesgos/{id}/cancelar", riesgoColectivaId).header("x-api-key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"));
    }

    @Test
    void cancelarRiesgoDeIndividualResponde409() throws Exception {
        mvc.perform(post("/riesgos/{id}/cancelar", riesgoIndividualId).header("x-api-key", API_KEY))
                .andExpect(status().isConflict());
    }

    @Test
    void coreMockRegistraElEvento() throws Exception {
        mvc.perform(post("/core-mock/evento").header("x-api-key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evento\":\"ACTUALIZACION\",\"polizaId\":555}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.registrado").value(true));
    }
}
