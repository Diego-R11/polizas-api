package com.segurosbolivar.polizas.domain;

import com.segurosbolivar.polizas.exception.ReglaNegocioException;
import jakarta.persistence.*;

/**
 * Un riesgo asegurado: el inmueble arrendado y el arrendatario que lo ocupa.
 */
@Entity
@Table(name = "riesgos")
public class Riesgo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poliza_id")
    private Poliza poliza;

    @Column(nullable = false)
    private String direccionInmueble;

    @Column(nullable = false)
    private String arrendatario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoRiesgo estado = EstadoRiesgo.ACTIVO;

    protected Riesgo() {
        // requerido por JPA
    }

    public Riesgo(String direccionInmueble, String arrendatario) {
        this.direccionInmueble = direccionInmueble;
        this.arrendatario = arrendatario;
    }

    public void cancelar() {
        if (estado == EstadoRiesgo.CANCELADO) {
            throw new ReglaNegocioException("El riesgo " + id + " ya está cancelado");
        }
        this.estado = EstadoRiesgo.CANCELADO;
    }

    /** Usado al cancelar la póliza completa: no falla si el riesgo ya estaba cancelado. */
    void cancelarPorPoliza() {
        this.estado = EstadoRiesgo.CANCELADO;
    }

    public boolean estaActivo() {
        return estado == EstadoRiesgo.ACTIVO;
    }

    void asignarPoliza(Poliza poliza) {
        this.poliza = poliza;
    }

    public Long getId() { return id; }
    public Poliza getPoliza() { return poliza; }
    public String getDireccionInmueble() { return direccionInmueble; }
    public String getArrendatario() { return arrendatario; }
    public EstadoRiesgo getEstado() { return estado; }
}
