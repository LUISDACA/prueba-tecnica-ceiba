package com.luisdavid.pruebatecnica.web.dto;

import com.luisdavid.pruebatecnica.domain.EstadoBicicleta;
import com.luisdavid.pruebatecnica.domain.TipoBicicleta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Peticion para registrar una bicicleta (RF-01).
 *
 * <p>El campo {@code estado} es opcional — si no se envia, la bici se crea
 * en {@link EstadoBicicleta#DISPONIBLE}. Esto permite el seed de BIC-004
 * EN_MANTENIMIENTO sin obligar a especificarlo en cada alta normal.</p>
 */
public record CrearBicicletaRequest(

        @NotBlank(message = "El codigo es obligatorio")
        @Size(max = 20, message = "El codigo no puede exceder 20 caracteres")
        @Pattern(regexp = "^[A-Z0-9-]+$",
                message = "El codigo solo admite letras mayusculas, digitos y guiones")
        String codigo,

        @NotNull(message = "El tipo es obligatorio")
        TipoBicicleta tipo,

        EstadoBicicleta estado
) {}
