package com.luisdavid.pruebatecnica.exception;

/**
 * Lanzada cuando no existe una bicicleta con el código consultado.
 * El manejador global la traduce a HTTP 404.
 */
public class BicicletaNoEncontradaException extends RuntimeException {

    public BicicletaNoEncontradaException(String codigo) {
        super("No existe una bicicleta con código '" + codigo + "'");
    }
}
