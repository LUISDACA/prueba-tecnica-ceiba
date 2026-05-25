package com.luisdavid.pruebatecnica.exception;

/**
 * Lanzada al intentar finalizar un alquiler que ya tiene hora de devolucion (RN-05).
 * El manejador global la traduce a HTTP 409 Conflict.
 */
public class AlquilerYaFinalizadoException extends RuntimeException {

    public AlquilerYaFinalizadoException(Long id) {
        super("El alquiler con id " + id + " ya fue finalizado previamente");
    }
}
