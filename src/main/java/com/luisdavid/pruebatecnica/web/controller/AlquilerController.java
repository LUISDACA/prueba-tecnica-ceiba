package com.luisdavid.pruebatecnica.web.controller;

import com.luisdavid.pruebatecnica.domain.Alquiler;
import com.luisdavid.pruebatecnica.service.AlquilerService;
import com.luisdavid.pruebatecnica.web.dto.AlquilerResponse;
import com.luisdavid.pruebatecnica.web.dto.IniciarAlquilerRequest;
import com.luisdavid.pruebatecnica.web.mapper.AlquilerMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Endpoints REST para alquileres (RF-02, RF-03).
 *
 * <p>Se usa {@code PATCH} para finalizar porque es una actualización parcial del
 * recurso existente, no la creación de uno nuevo. Evita el anti-patrón
 * RPC-sobre-REST que sería {@code POST /alquileres/{id}/finalizar}.</p>
 */
@RestController
@RequestMapping("/api/v1/alquileres")
public class AlquilerController {

    private final AlquilerService alquilerService;

    public AlquilerController(AlquilerService alquilerService) {
        this.alquilerService = alquilerService;
    }

    /** RF-02: iniciar un alquiler. */
    @PostMapping
    public ResponseEntity<AlquilerResponse> iniciar(@Valid @RequestBody IniciarAlquilerRequest request) {
        Alquiler alquiler = alquilerService.iniciar(
                request.codigoBicicleta(),
                request.nombreCliente(),
                request.duracionEstimadaHoras()
        );
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(alquiler.getId())
                .toUri();
        return ResponseEntity.created(location).body(AlquilerMapper.toResponse(alquiler));
    }

    /** RF-03: finalizar un alquiler y calcular costo + multa. */
    @PatchMapping("/{id}/finalizar")
    public AlquilerResponse finalizar(@PathVariable Long id) {
        return AlquilerMapper.toResponse(alquilerService.finalizar(id));
    }

    @GetMapping("/{id}")
    public AlquilerResponse buscar(@PathVariable Long id) {
        return AlquilerMapper.toResponse(alquilerService.buscarPorId(id));
    }
}
