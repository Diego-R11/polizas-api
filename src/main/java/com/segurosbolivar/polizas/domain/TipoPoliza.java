package com.segurosbolivar.polizas.domain;

public enum TipoPoliza {
    /** Tomador y asegurado: el arrendatario. Beneficiario: el arrendador. Solo 1 riesgo. */
    INDIVIDUAL,
    /** Tomador: inmobiliaria o administración de copropiedad. Uno o muchos riesgos. */
    COLECTIVA
}
