package com.luisdavid.pruebatecnica.domain;

import java.math.BigDecimal;

/**
 * Tipos de bicicleta soportados por el sistema, con su tarifa por hora asociada (RN-01).
 *
 * <p>La tarifa se modela como parte intrinseca del tipo (no como tabla externa) porque
 * el enunciado las define como constantes de negocio. Esto favorece cohesion y simplicidad.
 * Si en el futuro las tarifas debieran variar dinamicamente, se migrarian a una tabla
 * de configuracion sin cambiar la API publica.</p>
 *
 * <p>Los nombres conservan tildes y la "n" tal cual el enunciado para que el contrato
 * JSON los refleje literalmente: {@code "tipo": "MONTANA"}.</p>
 */
public enum TipoBicicleta {

    URBANA(new BigDecimal("3500")),
    MONTANA(new BigDecimal("5000")),
    ELECTRICA(new BigDecimal("7500"));

    private final BigDecimal tarifaPorHora;

    TipoBicicleta(BigDecimal tarifaPorHora) {
        this.tarifaPorHora = tarifaPorHora;
    }

    public BigDecimal getTarifaPorHora() {
        return tarifaPorHora;
    }
}
