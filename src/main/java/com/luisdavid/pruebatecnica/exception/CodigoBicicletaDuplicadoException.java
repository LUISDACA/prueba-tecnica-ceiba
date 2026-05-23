package com.luisdavid.pruebatecnica.exception;

/**
 * Lanzada al registrar una bicicleta con un código que ya existe.
 * El manejador global la traduce a HTTP 409 Conflict.
 */
public class CodigoBicicletaDuplicadoException extends RuntimeException {

    public CodigoBicicletaDuplicadoException(String codigo) {
        super("Ya existe una bicicleta con código '" + codigo + "'");
    }
}
