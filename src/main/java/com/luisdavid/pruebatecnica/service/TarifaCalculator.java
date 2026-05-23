package com.luisdavid.pruebatecnica.service;

import com.luisdavid.pruebatecnica.domain.RedondeoHoras;
import com.luisdavid.pruebatecnica.domain.TipoBicicleta;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Calcula el costo total de un alquiler (RN-01, RN-02, RN-03).
 *
 * <p>Esta clase es <strong>pura</strong>: no accede a base de datos, no depende
 * del reloj, no muta estado externo. Recibe entradas explícitas y devuelve un
 * {@link CalculoTarifa} inmutable. Esto la convierte en la pieza más fácil de
 * testear y la más sensible: aquí está toda la lógica monetaria del sistema.</p>
 *
 * <p><strong>Reglas implementadas:</strong></p>
 * <ul>
 *   <li>RN-02: horas reales redondeadas al alza × tarifa del tipo.</li>
 *   <li>RN-03: si {@code duracionReal > duracionEstimada}, se cobra una multa
 *       igual a {@code 50% × tarifa × horasRetraso}. El retraso mínimo facturable
 *       es 1 hora — por debajo de eso no se cobra multa. Por encima, el retraso
 *       se redondea al alza.</li>
 * </ul>
 */
@Component
public class TarifaCalculator {

    /** Factor de multa según RN-03: 50% de la tarifa por hora. */
    private static final BigDecimal FACTOR_MULTA = new BigDecimal("0.5");

    /**
     * Calcula el cobro final de un alquiler.
     *
     * @param tipo                  tipo de bicicleta (define la tarifa por hora)
     * @param horaInicio            hora real de inicio del alquiler
     * @param horaFin               hora real de devolución
     * @param duracionEstimadaHoras duración estimada que el cliente declaró al iniciar
     * @return desglose del cobro (base + multa + total)
     * @throws IllegalArgumentException si {@code horaFin} no es posterior a {@code horaInicio}
     */
    public CalculoTarifa calcular(
            TipoBicicleta tipo,
            LocalDateTime horaInicio,
            LocalDateTime horaFin,
            int duracionEstimadaHoras
    ) {
        if (!horaFin.isAfter(horaInicio)) {
            throw new IllegalArgumentException("La hora de devolución debe ser posterior a la de inicio");
        }

        Duration usoReal = Duration.between(horaInicio, horaFin);
        long horasRealesCobradas = RedondeoHoras.alAlza(usoReal);

        BigDecimal tarifa = tipo.getTarifaPorHora();
        BigDecimal costoBase = tarifa.multiply(BigDecimal.valueOf(horasRealesCobradas));

        long horasRetraso = calcularHorasRetraso(usoReal, duracionEstimadaHoras);
        BigDecimal multa = horasRetraso == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : tarifa.multiply(FACTOR_MULTA).multiply(BigDecimal.valueOf(horasRetraso));

        BigDecimal total = costoBase.add(multa);

        return new CalculoTarifa(
                costoBase.setScale(2, RoundingMode.HALF_UP),
                multa.setScale(2, RoundingMode.HALF_UP),
                total.setScale(2, RoundingMode.HALF_UP),
                horasRealesCobradas,
                horasRetraso
        );
    }

    /**
     * Calcula las horas de retraso facturables (RN-03).
     *
     * <p>Devuelve 0 si el uso real no excedió la estimación. En caso de exceder,
     * el retraso se redondea al alza y se asegura un mínimo de 1 hora.</p>
     */
    private long calcularHorasRetraso(Duration usoReal, int duracionEstimadaHoras) {
        Duration estimada = Duration.ofHours(duracionEstimadaHoras);
        Duration retraso = usoReal.minus(estimada);
        if (retraso.isZero() || retraso.isNegative()) {
            return 0;
        }
        long horasRetraso = RedondeoHoras.alAlza(retraso);
        return Math.max(1L, horasRetraso);
    }
}
