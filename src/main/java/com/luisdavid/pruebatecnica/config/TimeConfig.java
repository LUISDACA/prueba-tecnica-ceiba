package com.luisdavid.pruebatecnica.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Expone el reloj del sistema como bean para permitir su inyeccion.
 *
 * <p>En tests se sobreescribe con {@code Clock.fixed(...)} para obtener
 * resultados deterministas en los calculos temporales.</p>
 */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
