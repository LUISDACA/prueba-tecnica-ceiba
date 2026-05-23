package com.luisdavid.pruebatecnica.domain;

import java.math.BigDecimal;

/**
 * Tipos de bicicleta soportados por el sistema, con su tarifa por hora asociada (RN-01).
 *
 * <p>La tarifa se modela como parte intrínseca del tipo (no como tabla externa) porque
 * el enunciado las define como constantes de negocio. Esto favorece cohesión y simplicidad.
 * Si en el futuro las tarifas debieran variar dinámicamente, se migrarían a una tabla
 * de configuración sin cambiar la API pública.</p>
 *
 * <p>Los nombres conservan tildes y la "ñ" tal cual el enunciado para que el contrato
 * JSON los refleje literalmente: {@code "tipo": "MONTAÑA"}.</p>
 */
public enum TipoBicicleta {

    URBANA(new BigDecimal("3500")),
    MONTAÑA(new BigDecimal("5000")),
    ELÉCTRICA(new BigDecimal("7500"));

    private final BigDecimal tarifaPorHora;

    TipoBicicleta(BigDecimal tarifaPorHora) {
        this.tarifaPorHora = tarifaPorHora;
    }

    public BigDecimal getTarifaPorHora() {
        return tarifaPorHora;
    }
}
