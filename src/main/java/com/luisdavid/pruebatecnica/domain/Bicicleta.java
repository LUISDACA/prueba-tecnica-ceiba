package com.luisdavid.pruebatecnica.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Bicicleta del sistema de alquiler (RF-01).
 *
 * <p>Identidad: el {@code id} numerico es la PK tecnica; el {@code codigo} es el
 * identificador de negocio unico (ej. {@code BIC-001}).</p>
 *
 * <p>Concurrencia: {@link #version} habilita bloqueo optimista (JPA {@code @Version}).
 * Si dos transacciones intentan alquilar la misma bici simultaneamente, solo la primera
 * persistira; la segunda lanzara {@code ObjectOptimisticLockingFailureException},
 * que el manejador global traduce a HTTP 409.</p>
 *
 * <p>No uso {@code @Data} de Lombok porque genera {@code equals}/{@code hashCode}
 * sobre todos los campos, lo cual rompe el contrato cuando el id se asigna despues
 * de persistir. Defino igualdad por la PK (ver {@link #equals(Object)}).</p>
 */
@Entity
@Table(name = "bicicletas")
public class Bicicleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoBicicleta tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoBicicleta estado;

    @Version
    private Long version;

    protected Bicicleta() {
        // requerido por JPA
    }

    public Bicicleta(String codigo, TipoBicicleta tipo, EstadoBicicleta estado) {
        this.codigo = codigo;
        this.tipo = tipo;
        this.estado = estado;
    }

    // --- Comportamiento de dominio ---

    /**
     * Marca la bicicleta como alquilada. Valida la transicion (RN-04).
     *
     * @throws IllegalStateException si la bici no esta disponible.
     */
    public void marcarComoAlquilada() {
        if (estado != EstadoBicicleta.DISPONIBLE) {
            throw new IllegalStateException(
                    "La bicicleta " + codigo + " no esta disponible (estado actual: " + estado + ")");
        }
        this.estado = EstadoBicicleta.ALQUILADA;
    }

    /**
     * Devuelve la bicicleta al estado disponible al finalizar un alquiler.
     */
    public void marcarComoDisponible() {
        this.estado = EstadoBicicleta.DISPONIBLE;
    }

    public boolean estaDisponible() {
        return estado == EstadoBicicleta.DISPONIBLE;
    }

    // --- Getters ---

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public TipoBicicleta getTipo() { return tipo; }
    public EstadoBicicleta getEstado() { return estado; }
    public Long getVersion() { return version; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bicicleta otra)) return false;
        return id != null && id.equals(otra.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
