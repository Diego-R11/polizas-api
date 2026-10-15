package com.segurosbolivar.polizas.exception;

/** Se lanza cuando una póliza o riesgo no existe. Se responde con HTTP 404. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
