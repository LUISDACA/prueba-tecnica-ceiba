package com.luisdavid.pruebatecnica.domain;

/**
 * Estados posibles de una bicicleta (RF-01).
 *
 * <p>Solo {@link #DISPONIBLE} permite iniciar un alquiler (RN-04).
 * {@link #ALQUILADA} es un estado transitorio mientras dura el alquiler.
 * {@link #EN_MANTENIMIENTO} bloquea la bicicleta para uso operativo.</p>
 */
public enum EstadoBicicleta {
    DISPONIBLE,
    ALQUILADA,
    EN_MANTENIMIENTO
}
