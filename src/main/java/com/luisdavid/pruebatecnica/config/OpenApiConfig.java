package com.luisdavid.pruebatecnica.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadata y esquema de seguridad para Swagger UI / OpenAPI.
 *
 * <p>Define el esquema de API Key para que desde la propia UI se pueda
 * configurar el header {@code X-API-KEY} y probar los endpoints autenticados
 * con un clic ("Authorize").</p>
 */
@Configuration
public class OpenApiConfig {

    private static final String API_KEY_SCHEME = "ApiKeyAuth";
    private static final String API_KEY_HEADER = "X-API-KEY";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Alquiler de Bicicletas Urbanas")
                        .description("Prueba tecnica — Practicante Java. Gestion de alquileres, "
                                + "control de disponibilidad y calculo de costos y multas.")
                        .version("v1")
                        .contact(new Contact()
                                .name("Luis Miguel David")
                                .email("luis-miguel-david@hotmail.com")))
                .addSecurityItem(new SecurityRequirement().addList(API_KEY_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(API_KEY_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(API_KEY_HEADER)
                                .description("Clave de API requerida en todos los endpoints /api/**")));
    }
}
