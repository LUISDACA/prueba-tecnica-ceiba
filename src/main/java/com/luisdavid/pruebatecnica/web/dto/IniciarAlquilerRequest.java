package com.luisdavid.pruebatecnica.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Peticion para iniciar un alquiler (RF-02).
 *
 * <p>La hora de inicio NO se recibe del cliente — la asigna el servidor para
 * evitar manipulacion. Este supuesto esta documentado en el README.</p>
 */
public record IniciarAlquilerRequest(

        @NotBlank(message = "El codigo de la bicicleta es obligatorio")
        String codigoBicicleta,

        @NotBlank(message = "El nombre del cliente es obligatorio")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        String nombreCliente,

        @NotNull(message = "La duracion estimada es obligatoria")
        @Min(value = 1, message = "La duracion estimada debe ser al menos 1 hora")
        @Max(value = 24, message = "La duracion estimada no puede exceder 24 horas")
        Integer duracionEstimadaHoras
) {}
