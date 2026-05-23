package com.luisdavid.pruebatecnica.exception;

/**
 * Lanzada al consultar/finalizar un alquiler inexistente (RN-05).
 * El manejador global la traduce a HTTP 404.
 */
public class AlquilerNoEncontradoException extends RuntimeException {

    public AlquilerNoEncontradoException(Long id) {
        super("No existe un alquiler con id " + id);
    }
}
