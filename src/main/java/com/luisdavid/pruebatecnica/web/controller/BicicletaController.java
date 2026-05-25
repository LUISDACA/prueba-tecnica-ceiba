package com.luisdavid.pruebatecnica.web.controller;

import com.luisdavid.pruebatecnica.domain.Alquiler;
import com.luisdavid.pruebatecnica.domain.Bicicleta;
import com.luisdavid.pruebatecnica.domain.TipoBicicleta;
import com.luisdavid.pruebatecnica.service.AlquilerService;
import com.luisdavid.pruebatecnica.service.BicicletaService;
import com.luisdavid.pruebatecnica.web.dto.AlquilerResponse;
import com.luisdavid.pruebatecnica.web.dto.BicicletaResponse;
import com.luisdavid.pruebatecnica.web.dto.CrearBicicletaRequest;
import com.luisdavid.pruebatecnica.web.mapper.AlquilerMapper;
import com.luisdavid.pruebatecnica.web.mapper.BicicletaMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Endpoints REST para bicicletas (RF-01, RF-04, RF-05).
 */
@RestController
@RequestMapping("/api/v1/bicicletas")
public class BicicletaController {

    private final BicicletaService bicicletaService;
    private final AlquilerService alquilerService;

    public BicicletaController(BicicletaService bicicletaService, AlquilerService alquilerService) {
        this.bicicletaService = bicicletaService;
        this.alquilerService = alquilerService;
    }

    /** RF-01: registrar bicicleta. */
    @PostMapping
    public ResponseEntity<BicicletaResponse> crear(@Valid @RequestBody CrearBicicletaRequest request) {
        Bicicleta bici = bicicletaService.crear(request.codigo(), request.tipo(), request.estado());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{codigo}")
                .buildAndExpand(bici.getCodigo())
                .toUri();
        return ResponseEntity.created(location).body(BicicletaMapper.toResponse(bici));
    }

    @GetMapping
    public List<BicicletaResponse> listarTodas() {
        return bicicletaService.listarTodas().stream().map(BicicletaMapper::toResponse).toList();
    }

    /**
     * RF-04: consultar bicicletas disponibles, opcionalmente filtradas por tipo.
     * <p>Si {@code tipo} no se envia, devuelve todas las disponibles.</p>
     */
    @GetMapping("/disponibles")
    public List<BicicletaResponse> listarDisponibles(@RequestParam(required = false) TipoBicicleta tipo) {
        return bicicletaService.listarDisponibles(tipo).stream()
                .map(BicicletaMapper::toResponse)
                .toList();
    }

    @GetMapping("/{codigo}")
    public BicicletaResponse buscar(@PathVariable String codigo) {
        return BicicletaMapper.toResponse(bicicletaService.buscarPorCodigo(codigo));
    }

    /** RF-05: historial de alquileres de una bicicleta, mas reciente primero. */
    @GetMapping("/{codigo}/historial")
    public List<AlquilerResponse> historial(@PathVariable String codigo) {
        List<Alquiler> historial = alquilerService.historialDeBicicleta(codigo);
        return historial.stream().map(AlquilerMapper::toResponse).toList();
    }
}
