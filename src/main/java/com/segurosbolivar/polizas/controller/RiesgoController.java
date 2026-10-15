package com.segurosbolivar.polizas.controller;

import com.segurosbolivar.polizas.dto.RiesgoResponse;
import com.segurosbolivar.polizas.service.RiesgoService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/riesgos")
public class RiesgoController {

    private final RiesgoService riesgoService;

    public RiesgoController(RiesgoService riesgoService) {
        this.riesgoService = riesgoService;
    }

    @PostMapping("/{id}/cancelar")
    public RiesgoResponse cancelar(@PathVariable Long id) {
        return riesgoService.cancelar(id);
    }
}
