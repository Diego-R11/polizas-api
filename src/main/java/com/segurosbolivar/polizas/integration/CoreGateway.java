package com.segurosbolivar.polizas.integration;

/**
 * Puerto hacia el CORE transaccional (arquitectura hexagonal).
 * Los servicios dependen de esta interfaz, no de cómo se llama al CORE.
 * Hoy la implementación apunta al mock; mañana puede apuntar al servicio
 * agnóstico de edición en WebLogic sin tocar la lógica de negocio.
 */
public interface CoreGateway {

    void notificarActualizacion(Long polizaId);
}
