package com.luisdavid.pruebatecnica.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro que autentica peticiones mediante una API Key enviada en un header HTTP.
 *
 * <p>Si la ruta requiere proteccion y el header esta ausente o no coincide con la
 * clave configurada, devuelve HTTP 401 con un cuerpo JSON descriptivo.</p>
 *
 * <p>La clave se lee de {@code app.security.api-key} (sobreescribible con la
 * variable de entorno {@code API_KEY}). El nombre del header es configurable
 * mediante {@code app.security.api-key-header}.</p>
 *
 * <p>No se anota con {@code @Component} — se instancia explicitamente desde
 * {@link SecurityConfig} para evitar que Spring Boot lo registre tambien
 * como filtro servlet (duplicacion) y para mantener su scope dentro de la
 * cadena de Spring Security.</p>
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final String apiKey;
    private final String headerName;

    public ApiKeyAuthenticationFilter(String apiKey, String headerName) {
        this.apiKey = apiKey;
        this.headerName = headerName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String provided = request.getHeader(headerName);

        if (provided == null || !provided.equals(apiKey)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write("""
                {
                  "type": "about:blank",
                  "title": "API Key invalida o ausente",
                  "status": 401,
                  "detail": "Debe enviar un header '%s' con la API Key configurada."
                }
                """.formatted(headerName));
            return;
        }

        // Autenticacion minima: la API Key reemplaza a un usuario/rol.
        var auth = new ApiKeyAuthenticationToken(apiKey, List.of());
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);
        chain.doFilter(request, response);
    }

    /** Token simple sin authorities para representar la autenticacion por API Key. */
    private static class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {
        private final String apiKey;

        ApiKeyAuthenticationToken(String apiKey, List<?> authorities) {
            super(List.of());
            this.apiKey = apiKey;
        }

        @Override public Object getCredentials() { return apiKey; }
        @Override public Object getPrincipal() { return "api-key-client"; }
    }
}
