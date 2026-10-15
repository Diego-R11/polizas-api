package com.segurosbolivar.polizas.exception;

/** Se lanza cuando una operación viola una regla de negocio. Se responde con HTTP 409. */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
