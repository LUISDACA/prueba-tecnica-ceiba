package com.luisdavid.pruebatecnica.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Alquiler de una bicicleta (RF-02, RF-03).
 *
 * <p>Ciclo de vida:</p>
 * <ol>
 *   <li>Se crea con {@link #horaInicio} y {@link #duracionEstimadaHoras};
 *       los campos de cierre quedan nulos.</li>
 *   <li>Al finalizar, se completan {@link #horaFin}, {@link #costoBase},
 *       {@link #multa} y {@link #costoTotal}.</li>
 * </ol>
 *
 * <p>Decisión: en lugar de un {@code enum EstadoAlquiler}, se infiere por
 * {@code horaFin != null}. Simplifica el modelo y evita estados inconsistentes
 * (una hora fin sin estado FINALIZADO o viceversa).</p>
 *
 * <p>Los importes ({@code costoBase}, {@code multa}, {@code costoTotal}) usan
 * {@link BigDecimal} para evitar errores de redondeo binario propios de {@code double}.</p>
 */
@Entity
@Table(name = "alquileres")
public class Alquiler {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bicicleta_id", nullable = false)
    private Bicicleta bicicleta;

    @Column(nullable = false, length = 100)
    private String nombreCliente;

    @Column(nullable = false)
    private LocalDateTime horaInicio;

    @Column(nullable = false)
    private Integer duracionEstimadaHoras;

    @Column
    private LocalDateTime horaFin;

    @Column(precision = 12, scale = 2)
    private BigDecimal costoBase;

    @Column(precision = 12, scale = 2)
    private BigDecimal multa;

    @Column(precision = 12, scale = 2)
    private BigDecimal costoTotal;

    protected Alquiler() {
        // requerido por JPA
    }

    public Alquiler(Bicicleta bicicleta, String nombreCliente,
                    LocalDateTime horaInicio, Integer duracionEstimadaHoras) {
        this.bicicleta = bicicleta;
        this.nombreCliente = nombreCliente;
        this.horaInicio = horaInicio;
        this.duracionEstimadaHoras = duracionEstimadaHoras;
    }

    // --- Comportamiento de dominio ---

    /**
     * Cierra el alquiler con los valores calculados por el servicio.
     *
     * @throws IllegalStateException si el alquiler ya estaba finalizado (RN-05).
     */
    public void finalizar(LocalDateTime horaFin, BigDecimal costoBase, BigDecimal multa) {
        if (this.horaFin != null) {
            throw new IllegalStateException("El alquiler " + id + " ya fue finalizado previamente");
        }
        this.horaFin = horaFin;
        this.costoBase = costoBase;
        this.multa = multa;
        this.costoTotal = costoBase.add(multa);
    }

    public boolean estaFinalizado() {
        return horaFin != null;
    }

    public boolean tuvoMulta() {
        return multa != null && multa.signum() > 0;
    }

    // --- Getters ---

    public Long getId() { return id; }
    public Bicicleta getBicicleta() { return bicicleta; }
    public String getNombreCliente() { return nombreCliente; }
    public LocalDateTime getHoraInicio() { return horaInicio; }
    public Integer getDuracionEstimadaHoras() { return duracionEstimadaHoras; }
    public LocalDateTime getHoraFin() { return horaFin; }
    public BigDecimal getCostoBase() { return costoBase; }
    public BigDecimal getMulta() { return multa; }
    public BigDecimal getCostoTotal() { return costoTotal; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Alquiler otro)) return false;
        return id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
