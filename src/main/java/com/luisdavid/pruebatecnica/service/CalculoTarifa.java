package com.luisdavid.pruebatecnica.service;

import java.math.BigDecimal;

/**
 * Resultado inmutable del cálculo de tarifa para un alquiler finalizado.
 *
 * <p>Se modela como {@code record} para garantizar inmutabilidad y reducir boilerplate.
 * Separa los componentes (base, multa) además del total, de modo que el llamador
 * pueda mostrar el desglose al cliente y persistirlo en la entidad.</p>
 *
 * @param costoBase         costo por horas reales de uso (RN-02)
 * @param multa             penalización por devolución tardía (RN-03), cero si no aplica
 * @param costoTotal        suma de costoBase y multa
 * @param horasRealesCobradas horas reales redondeadas al alza usadas para el cobro
 * @param horasRetraso      horas de retraso facturadas (cero si no hubo retraso)
 */
public record CalculoTarifa(
        BigDecimal costoBase,
        BigDecimal multa,
        BigDecimal costoTotal,
        long horasRealesCobradas,
        long horasRetraso
) {}
