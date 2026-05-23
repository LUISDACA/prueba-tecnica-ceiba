package com.luisdavid.pruebatecnica.service;

import com.luisdavid.pruebatecnica.domain.TipoBicicleta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitarios del {@link TarifaCalculator}.
 *
 * <p>Cubre los casos del enunciado y casos límite del redondeo. Es el componente
 * más crítico del sistema (toda la lógica monetaria), por eso recibe atención
 * exhaustiva.</p>
 */
@DisplayName("TarifaCalculator")
class TarifaCalculatorTest {

    private final TarifaCalculator calculator = new TarifaCalculator();
    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 5, 22, 10, 0);

    @Nested
    @DisplayName("RN-02: redondeo del costo base")
    class RedondeoCostoBase {

        @Test
        @DisplayName("2 horas exactas: cobra 2 horas")
        void dosHorasExactas() {
            var resultado = calculator.calcular(TipoBicicleta.URBANA, INICIO, INICIO.plusHours(2), 2);
            assertThat(resultado.horasRealesCobradas()).isEqualTo(2);
            assertThat(resultado.costoBase()).isEqualByComparingTo("7000.00"); // 2 * 3500
        }

        @Test
        @DisplayName("1 hora 10 minutos: cobra 2 horas (redondea al alza)")
        void unaHoraDiezMinutos() {
            var resultado = calculator.calcular(TipoBicicleta.URBANA, INICIO,
                    INICIO.plusHours(1).plusMinutes(10), 2);
            assertThat(resultado.horasRealesCobradas()).isEqualTo(2);
            assertThat(resultado.costoBase()).isEqualByComparingTo("7000.00");
        }

        @Test
        @DisplayName("1 segundo: cobra 1 hora")
        void unSegundo() {
            var resultado = calculator.calcular(TipoBicicleta.URBANA, INICIO,
                    INICIO.plusSeconds(1), 1);
            assertThat(resultado.horasRealesCobradas()).isEqualTo(1);
            assertThat(resultado.costoBase()).isEqualByComparingTo("3500.00");
        }

        @Test
        @DisplayName("500 ms (fracción de segundo): cobra 1 hora")
        void fraccionDeSegundo() {
            var resultado = calculator.calcular(TipoBicicleta.URBANA, INICIO,
                    INICIO.plusNanos(500_000_000L), 1);
            assertThat(resultado.horasRealesCobradas()).isEqualTo(1);
            assertThat(resultado.costoBase()).isEqualByComparingTo("3500.00");
        }

        @Test
        @DisplayName("Multiplica por la tarifa del tipo correcto")
        void usaTarifaSegunTipo() {
            var urbana = calculator.calcular(TipoBicicleta.URBANA, INICIO, INICIO.plusHours(1), 1);
            var montana = calculator.calcular(TipoBicicleta.MONTAÑA, INICIO, INICIO.plusHours(1), 1);
            var electrica = calculator.calcular(TipoBicicleta.ELÉCTRICA, INICIO, INICIO.plusHours(1), 1);

            assertThat(urbana.costoBase()).isEqualByComparingTo("3500.00");
            assertThat(montana.costoBase()).isEqualByComparingTo("5000.00");
            assertThat(electrica.costoBase()).isEqualByComparingTo("7500.00");
        }
    }

    @Nested
    @DisplayName("RN-03: multa por devolución tardía")
    class MultaPorRetraso {

        @Test
        @DisplayName("Devolución a tiempo: sin multa")
        void devolucionATiempo() {
            var resultado = calculator.calcular(TipoBicicleta.URBANA, INICIO, INICIO.plusHours(2), 2);
            assertThat(resultado.horasRetraso()).isZero();
            assertThat(resultado.multa()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("Devolución antes de tiempo: sin multa, pero se cobra tiempo real")
        void devolucionAntesDeTiempo() {
            var resultado = calculator.calcular(TipoBicicleta.URBANA, INICIO, INICIO.plusHours(1), 3);
            assertThat(resultado.horasRetraso()).isZero();
            assertThat(resultado.multa()).isEqualByComparingTo("0.00");
            assertThat(resultado.costoBase()).isEqualByComparingTo("3500.00"); // 1h URBANA
        }

        @Test
        @DisplayName("Ejemplo del enunciado: MONTAÑA 2h estimadas, 3h20min reales -> total $25.000")
        void ejemploEnunciado() {
            var resultado = calculator.calcular(
                    TipoBicicleta.MONTAÑA,
                    INICIO,
                    INICIO.plusHours(3).plusMinutes(20),
                    2);

            assertThat(resultado.horasRealesCobradas()).isEqualTo(4);
            assertThat(resultado.horasRetraso()).isEqualTo(2);
            assertThat(resultado.costoBase()).isEqualByComparingTo("20000.00");
            assertThat(resultado.multa()).isEqualByComparingTo("5000.00");
            assertThat(resultado.costoTotal()).isEqualByComparingTo("25000.00");
        }

        @Test
        @DisplayName("Retraso de 1 segundo: cobra mínimo 1 hora de multa")
        void retrasoMinimoFacturable() {
            var resultado = calculator.calcular(
                    TipoBicicleta.URBANA,
                    INICIO,
                    INICIO.plusHours(2).plusSeconds(1),
                    2);

            assertThat(resultado.horasRetraso()).isEqualTo(1);
            // multa = 1 * 3500 * 0.5 = 1750
            assertThat(resultado.multa()).isEqualByComparingTo("1750.00");
        }

        @Test
        @DisplayName("Retraso de 1h 20min: cobra 2 horas de multa (redondeo al alza)")
        void retrasoRedondeadoAlAlza() {
            var resultado = calculator.calcular(
                    TipoBicicleta.URBANA,
                    INICIO,
                    INICIO.plusHours(2).plusMinutes(20).plusHours(1), // 3h 20min total
                    2);

            assertThat(resultado.horasRetraso()).isEqualTo(2);
            // multa = 2 * 3500 * 0.5 = 3500
            assertThat(resultado.multa()).isEqualByComparingTo("3500.00");
        }
    }

    @Nested
    @DisplayName("Validación de argumentos")
    class Argumentos {

        @Test
        @DisplayName("Hora fin igual a inicio lanza IllegalArgumentException")
        void horaFinIgualAInicio() {
            assertThatThrownBy(() ->
                    calculator.calcular(TipoBicicleta.URBANA, INICIO, INICIO, 1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Hora fin anterior a inicio lanza IllegalArgumentException")
        void horaFinAnteriorAInicio() {
            assertThatThrownBy(() ->
                    calculator.calcular(TipoBicicleta.URBANA, INICIO, INICIO.minusHours(1), 1))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Resultado consistente")
    class Consistencia {

        @Test
        @DisplayName("costoTotal siempre = costoBase + multa")
        void totalEsSumaDeComponentes() {
            var resultado = calculator.calcular(
                    TipoBicicleta.ELÉCTRICA,
                    INICIO,
                    INICIO.plusHours(5).plusMinutes(45),
                    3);

            BigDecimal esperado = resultado.costoBase().add(resultado.multa());
            assertThat(resultado.costoTotal()).isEqualByComparingTo(esperado);
        }
    }
}
