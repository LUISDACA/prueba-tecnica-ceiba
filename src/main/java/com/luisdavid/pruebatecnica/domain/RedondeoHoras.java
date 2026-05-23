package com.luisdavid.pruebatecnica.domain;

import java.time.Duration;

/**
 * Redondeo al alza de duraciones a horas completas (RN-02).
 *
 * <p>Vive en el paquete de dominio porque es lógica de negocio pura
 * (no depende de Spring ni de la BD). Se reutiliza en
 * {@code TarifaCalculator} y en los mappers de salida — DRY.</p>
 *
 * <p>Ejemplos:</p>
 * <ul>
 *   <li>{@code Duration.ofMinutes(70)} → 2h</li>
 *   <li>{@code Duration.ofHours(2)} → 2h</li>
 *   <li>{@code Duration.ofMillis(500)} → 1h (cualquier uso &gt; 0 implica ≥ 1 hora facturable)</li>
 *   <li>{@code Duration.ZERO} → 0h</li>
 * </ul>
 *
 * <p>Trabaja sobre milisegundos para no perder precisión con fracciones de segundo.</p>
 */
public final class RedondeoHoras {

    private static final long MILLIS_POR_HORA = 3600L * 1000L;

    private RedondeoHoras() {
        // utility class
    }

    public static long alAlza(Duration duracion) {
        if (duracion.isZero() || duracion.isNegative()) {
            return 0L;
        }
        long millis = duracion.toMillis();
        // (a + b - 1) / b en aritmética entera = ceil(a/b) cuando a > 0
        return (millis + MILLIS_POR_HORA - 1) / MILLIS_POR_HORA;
    }
}
