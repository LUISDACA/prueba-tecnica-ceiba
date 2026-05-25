package com.luisdavid.pruebatecnica.exception;

import com.luisdavid.pruebatecnica.domain.EstadoBicicleta;

/**
 * Lanzada al intentar alquilar una bicicleta cuyo estado no es DISPONIBLE (RN-04).
 * El manejador global la traduce a HTTP 409 Conflict.
 */
public class BicicletaNoDisponibleException extends RuntimeException {

    public BicicletaNoDisponibleException(String codigo, EstadoBicicleta estadoActual) {
        super("La bicicleta '" + codigo + "' no esta disponible para alquiler (estado actual: "
                + estadoActual + ")");
    }
}
