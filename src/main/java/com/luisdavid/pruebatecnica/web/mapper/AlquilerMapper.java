package com.luisdavid.pruebatecnica.web.mapper;

import com.luisdavid.pruebatecnica.domain.Alquiler;
import com.luisdavid.pruebatecnica.domain.RedondeoHoras;
import com.luisdavid.pruebatecnica.web.dto.AlquilerResponse;

import java.time.Duration;

/**
 * Convierte entidades {@link Alquiler} a su DTO público.
 *
 * <p>Calcula la duración real (horas redondeadas al alza) solo cuando el
 * alquiler ya finalizó — antes de eso, los campos de cierre quedan nulos.</p>
 */
public final class AlquilerMapper {

    private AlquilerMapper() {
        // utility class
    }

    public static AlquilerResponse toResponse(Alquiler a) {
        Long duracionRealHoras = null;
        if (a.estaFinalizado()) {
            Duration usoReal = Duration.between(a.getHoraInicio(), a.getHoraFin());
            duracionRealHoras = RedondeoHoras.alAlza(usoReal);
        }

        return new AlquilerResponse(
                a.getId(),
                a.getBicicleta().getCodigo(),
                a.getBicicleta().getTipo(),
                a.getNombreCliente(),
                a.getHoraInicio(),
                a.getHoraFin(),
                a.getDuracionEstimadaHoras(),
                duracionRealHoras,
                a.getCostoBase(),
                a.getMulta(),
                a.getCostoTotal(),
                a.tuvoMulta(),
                a.estaFinalizado()
        );
    }
}
