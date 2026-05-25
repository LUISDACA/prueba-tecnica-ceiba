package com.luisdavid.pruebatecnica.web.dto;

import com.luisdavid.pruebatecnica.domain.EstadoBicicleta;
import com.luisdavid.pruebatecnica.domain.TipoBicicleta;

import java.math.BigDecimal;

/**
 * Representacion publica de una bicicleta.
 *
 * <p>Expone tambien la tarifa derivada del tipo como cortesia al cliente
 * — evita que el consumidor de la API tenga que conocer las tarifas.</p>
 */
public record BicicletaResponse(
        String codigo,
        TipoBicicleta tipo,
        EstadoBicicleta estado,
        BigDecimal tarifaPorHora
) {}
