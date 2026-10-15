package com.segurosbolivar.polizas.dto;

import java.time.OffsetDateTime;

/** Formato único de error para todos los endpoints. */
public record ErrorResponse(
        int status,
        String error,
        String mensaje,
        String path,
        OffsetDateTime timestamp
) {
    public static ErrorResponse de(int status, String error, String mensaje, String path) {
        return new ErrorResponse(status, error, mensaje, path, OffsetDateTime.now());
    }
}
