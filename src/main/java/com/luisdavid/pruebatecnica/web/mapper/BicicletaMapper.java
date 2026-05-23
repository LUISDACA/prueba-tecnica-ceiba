package com.luisdavid.pruebatecnica.web.mapper;

import com.luisdavid.pruebatecnica.domain.Bicicleta;
import com.luisdavid.pruebatecnica.web.dto.BicicletaResponse;

/**
 * Convierte entidades {@link Bicicleta} a su DTO público.
 *
 * <p>Mapeo manual y estático — para este tamaño no compensa traer MapStruct.</p>
 */
public final class BicicletaMapper {

    private BicicletaMapper() {
        // utility class
    }

    public static BicicletaResponse toResponse(Bicicleta b) {
        return new BicicletaResponse(
                b.getCodigo(),
                b.getTipo(),
                b.getEstado(),
                b.getTipo().getTarifaPorHora()
        );
    }
}
