package com.luisdavid.pruebatecnica.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Configuración de Spring Security.
 *
 * <p>La API es <strong>stateless</strong> (sin sesiones) y autentica las
 * peticiones a {@code /api/**} con un filtro de API Key (ver
 * {@link ApiKeyAuthenticationFilter}).</p>
 *
 * <p>Rutas abiertas:</p>
 * <ul>
 *   <li>{@code /swagger-ui/**}, {@code /v3/api-docs/**} — documentación de la API.</li>
 *   <li>{@code /h2-console/**} — consola H2 en desarrollo.</li>
 *   <li>{@code /actuator/health} — health check.</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    @Bean
    public ApiKeyAuthenticationFilter apiKeyAuthenticationFilter(
            @Value("${app.security.api-key}") String apiKey,
            @Value("${app.security.api-key-header}") String headerName
    ) {
        return new ApiKeyAuthenticationFilter(apiKey, headerName);
    }

    /**
     * Evita que Spring Boot registre el filtro en la cadena de servlets global.
     * Solo debe correr dentro de la cadena de Spring Security (limitado a /api/**).
     */
    @Bean
    public FilterRegistrationBean<ApiKeyAuthenticationFilter> disableApiKeyFilterAutoRegistration(
            ApiKeyAuthenticationFilter filter) {
        FilterRegistrationBean<ApiKeyAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ApiKeyAuthenticationFilter apiKeyFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // API REST stateless — no usa cookies/sesiones
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        // Permite mostrar la consola H2 en un iframe del mismo origen
                        .frameOptions(frame -> frame.sameOrigin()))
                .securityMatcher(new AntPathRequestMatcher("/api/**"))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
