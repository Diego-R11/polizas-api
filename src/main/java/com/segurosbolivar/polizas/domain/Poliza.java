package com.segurosbolivar.polizas.domain;

import com.segurosbolivar.polizas.exception.ReglaNegocioException;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Póliza de arrendamiento. Las reglas de negocio viven aquí (modelo de dominio rico),
 * de modo que no dependen del controller ni de la base de datos.
 */
@Entity
@Table(name = "polizas")
public class Poliza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPoliza tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPoliza estado = EstadoPoliza.ACTIVA;

    /** Inmobiliaria/copropiedad (colectiva) o arrendatario (individual). */
    @Column(nullable = false)
    private String tomador;

    /** Arrendador: quien recibe la indemnización en caso de siniestro. */
    @Column(nullable = false)
    private String beneficiario;

    @Column(nullable = false)
    private LocalDate fechaInicioVigencia;

    @Column(nullable = false)
    private int mesesVigencia;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valorCanon;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valorPrima;

    @OneToMany(mappedBy = "poliza", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Riesgo> riesgos = new ArrayList<>();

    protected Poliza() {
        // requerido por JPA
    }

    public Poliza(TipoPoliza tipo, String tomador, String beneficiario,
                  LocalDate fechaInicioVigencia, int mesesVigencia, BigDecimal valorCanon) {
        if (mesesVigencia <= 0) {
            throw new ReglaNegocioException("La vigencia debe ser de al menos 1 mes");
        }
        if (valorCanon == null || valorCanon.signum() <= 0) {
            throw new ReglaNegocioException("El canon debe ser mayor a cero");
        }
        this.tipo = tipo;
        this.tomador = tomador;
        this.beneficiario = beneficiario;
        this.fechaInicioVigencia = fechaInicioVigencia;
        this.mesesVigencia = mesesVigencia;
        this.valorCanon = valorCanon.setScale(2, RoundingMode.HALF_UP);
        this.valorPrima = calcularPrima(this.valorCanon, mesesVigencia);
    }

    /** Prima = canon mensual x número de meses de vigencia. */
    static BigDecimal calcularPrima(BigDecimal canon, int meses) {
        return canon.multiply(BigDecimal.valueOf(meses)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Regla: agregar riesgo exige validar el tipo de póliza.
     * - No se agregan riesgos a pólizas canceladas.
     * - Una póliza individual solo puede tener 1 riesgo.
     */
    public void agregarRiesgo(Riesgo riesgo) {
        if (estado == EstadoPoliza.CANCELADA) {
            throw new ReglaNegocioException("No se pueden agregar riesgos a una póliza cancelada");
        }
        if (tipo == TipoPoliza.INDIVIDUAL && !riesgos.isEmpty()) {
            throw new ReglaNegocioException("Una póliza individual solo puede tener 1 riesgo");
        }
        riesgo.asignarPoliza(this);
        riesgos.add(riesgo);
    }

    /**
     * Regla: no se puede renovar una póliza cancelada.
     * Se renueva por el mismo periodo inicial y el canon se ajusta por IPC.
     *
     * @param ipc incremento en formato decimal (ej. 0.0510 = 5,10 %)
     */
    public void renovar(BigDecimal ipc) {
        if (estado == EstadoPoliza.CANCELADA) {
            throw new ReglaNegocioException("No se puede renovar una póliza cancelada");
        }
        if (ipc == null || ipc.signum() < 0) {
            throw new ReglaNegocioException("El IPC debe ser un valor mayor o igual a cero");
        }
        this.valorCanon = valorCanon.multiply(BigDecimal.ONE.add(ipc)).setScale(2, RoundingMode.HALF_UP);
        this.valorPrima = calcularPrima(valorCanon, mesesVigencia);
        this.fechaInicioVigencia = getFechaFinVigencia();
        this.estado = EstadoPoliza.RENOVADA;
    }

    /** Regla: la cancelación de una póliza cancela todos sus riesgos. */
    public void cancelar() {
        if (estado == EstadoPoliza.CANCELADA) {
            throw new ReglaNegocioException("La póliza " + id + " ya está cancelada");
        }
        riesgos.forEach(Riesgo::cancelarPorPoliza);
        this.estado = EstadoPoliza.CANCELADA;
    }

    public LocalDate getFechaFinVigencia() {
        return fechaInicioVigencia.plusMonths(mesesVigencia);
    }

    public Long getId() { return id; }
    public TipoPoliza getTipo() { return tipo; }
    public EstadoPoliza getEstado() { return estado; }
    public String getTomador() { return tomador; }
    public String getBeneficiario() { return beneficiario; }
    public LocalDate getFechaInicioVigencia() { return fechaInicioVigencia; }
    public int getMesesVigencia() { return mesesVigencia; }
    public BigDecimal getValorCanon() { return valorCanon; }
    public BigDecimal getValorPrima() { return valorPrima; }
    public List<Riesgo> getRiesgos() { return Collections.unmodifiableList(riesgos); }
}
