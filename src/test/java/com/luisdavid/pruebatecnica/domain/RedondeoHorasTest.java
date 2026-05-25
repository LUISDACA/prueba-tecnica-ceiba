package com.luisdavid.pruebatecnica.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedondeoHoras.alAlza")
class RedondeoHorasTest {

    @ParameterizedTest(name = "{0}s -> {1}h")
    @CsvSource({
            "0,        0",
            "1,        1",
            "3599,     1",
            "3600,     1",   // 1h exacta
            "3601,     2",   // 1h y 1 seg
            "7200,     2",   // 2h exactas
            "7201,     3",
            "12000,    4"    // ejemplo del enunciado: 3h 20min
    })
    @DisplayName("redondeo correcto para distintos valores")
    void redondeo(long segundos, long horasEsperadas) {
        assertThat(RedondeoHoras.alAlza(Duration.ofSeconds(segundos))).isEqualTo(horasEsperadas);
    }

    @ParameterizedTest(name = "{0}ms -> {1}h")
    @CsvSource({
            "1,           1",     // fraccion de segundo cuenta como 1h
            "500,         1",
            "3599999,     1",     // ~1h menos 1ms
            "3600000,     1",     // 1h exacta
            "3600001,     2"
    })
    @DisplayName("fracciones de segundo se redondean a la proxima hora")
    void fraccionesDeSegundo(long millis, long horasEsperadas) {
        assertThat(RedondeoHoras.alAlza(Duration.ofMillis(millis))).isEqualTo(horasEsperadas);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("duracion negativa devuelve 0")
    void negativa() {
        assertThat(RedondeoHoras.alAlza(Duration.ofSeconds(-5))).isZero();
    }
}
