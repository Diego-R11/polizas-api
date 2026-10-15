package com.segurosbolivar.polizas.service;

import com.segurosbolivar.polizas.domain.*;
import com.segurosbolivar.polizas.exception.ReglaNegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas unitarias de las reglas de negocio, sin Spring ni base de datos. */
class PolizaReglasNegocioTest {

    private Poliza nuevaPoliza(TipoPoliza tipo) {
        return new Poliza(tipo, "Tomador", "Beneficiario", LocalDate.of(2026, 1, 1), 12, new BigDecimal("1000000"));
    }

    @Test
    void laPrimaEsCanonPorMesesDeVigencia() {
        Poliza poliza = nuevaPoliza(TipoPoliza.INDIVIDUAL);
        assertEquals(new BigDecimal("12000000.00"), poliza.getValorPrima());
    }

    @Test
    void unaPolizaIndividualSoloPuedeTenerUnRiesgo() {
        Poliza poliza = nuevaPoliza(TipoPoliza.INDIVIDUAL);
        poliza.agregarRiesgo(new Riesgo("Dir 1", "Arrendatario 1"));

        assertThrows(ReglaNegocioException.class,
                () -> poliza.agregarRiesgo(new Riesgo("Dir 2", "Arrendatario 2")));
        assertEquals(1, poliza.getRiesgos().size());
    }

    @Test
    void unaPolizaColectivaPuedeTenerVariosRiesgos() {
        Poliza poliza = nuevaPoliza(TipoPoliza.COLECTIVA);
        poliza.agregarRiesgo(new Riesgo("Dir 1", "A1"));
        poliza.agregarRiesgo(new Riesgo("Dir 2", "A2"));
        poliza.agregarRiesgo(new Riesgo("Dir 3", "A3"));
        assertEquals(3, poliza.getRiesgos().size());
    }

    @Test
    void renovarIncrementaCanonYPrimaPorIpcYCambiaEstado() {
        Poliza poliza = nuevaPoliza(TipoPoliza.INDIVIDUAL);

        poliza.renovar(new BigDecimal("0.0510"));

        assertEquals(new BigDecimal("1051000.00"), poliza.getValorCanon());
        assertEquals(new BigDecimal("12612000.00"), poliza.getValorPrima());
        assertEquals(EstadoPoliza.RENOVADA, poliza.getEstado());
        assertEquals(LocalDate.of(2027, 1, 1), poliza.getFechaInicioVigencia());
        assertEquals(LocalDate.of(2028, 1, 1), poliza.getFechaFinVigencia());
    }

    @Test
    void noSePuedeRenovarUnaPolizaCancelada() {
        Poliza poliza = nuevaPoliza(TipoPoliza.INDIVIDUAL);
        poliza.cancelar();

        assertThrows(ReglaNegocioException.class, () -> poliza.renovar(new BigDecimal("0.05")));
        assertEquals(EstadoPoliza.CANCELADA, poliza.getEstado());
    }

    @Test
    void cancelarLaPolizaCancelaTodosSusRiesgos() {
        Poliza poliza = nuevaPoliza(TipoPoliza.COLECTIVA);
        poliza.agregarRiesgo(new Riesgo("Dir 1", "A1"));
        poliza.agregarRiesgo(new Riesgo("Dir 2", "A2"));

        poliza.cancelar();

        assertEquals(EstadoPoliza.CANCELADA, poliza.getEstado());
        assertTrue(poliza.getRiesgos().stream().allMatch(r -> r.getEstado() == EstadoRiesgo.CANCELADO));
    }

    @Test
    void noSeAgreganRiesgosAUnaPolizaCancelada() {
        Poliza poliza = nuevaPoliza(TipoPoliza.COLECTIVA);
        poliza.cancelar();

        assertThrows(ReglaNegocioException.class, () -> poliza.agregarRiesgo(new Riesgo("Dir", "A")));
    }

    @Test
    void noSeCreaUnaPolizaConCanonInvalido() {
        assertThrows(ReglaNegocioException.class, () -> new Poliza(TipoPoliza.INDIVIDUAL, "T", "B",
                LocalDate.now(), 12, BigDecimal.ZERO));
    }
}
