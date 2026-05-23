package com.luisdavid.pruebatecnica.web.dto;

import com.luisdavid.pruebatecnica.domain.TipoBicicleta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representación pública de un alquiler.
 *
 * <p>Mientras está activo, los campos {@code horaFin}, {@code costoBase},
 * {@code multa} y {@code costoTotal} aparecen como {@code null} en el JSON.
 * Al finalizar, todos los campos se completan.</p>
 *
 * <p>{@code duracionRealHoras} solo se incluye cuando el alquiler ya finalizó —
 * son las horas reales redondeadas al alza usadas para el cobro (RN-02).</p>
 */
public record AlquilerResponse(
        Long id,
        String codigoBicicleta,
        TipoBicicleta tipoBicicleta,
        String nombreCliente,
        LocalDateTime horaInicio,
        LocalDateTime horaFin,
        Integer duracionEstimadaHoras,
        Long duracionRealHoras,
        BigDecimal costoBase,
        BigDecimal multa,
        BigDecimal costoTotal,
        boolean tuvoMulta,
        boolean finalizado
) {}
