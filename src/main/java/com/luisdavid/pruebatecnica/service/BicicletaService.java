package com.luisdavid.pruebatecnica.service;

import com.luisdavid.pruebatecnica.domain.Bicicleta;
import com.luisdavid.pruebatecnica.domain.EstadoBicicleta;
import com.luisdavid.pruebatecnica.domain.TipoBicicleta;
import com.luisdavid.pruebatecnica.exception.BicicletaNoEncontradaException;
import com.luisdavid.pruebatecnica.exception.CodigoBicicletaDuplicadoException;
import com.luisdavid.pruebatecnica.repository.BicicletaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Operaciones sobre bicicletas (RF-01, RF-04).
 *
 * <p>Las lecturas se anotan con {@code @Transactional(readOnly = true)} para
 * permitir a Hibernate optimizar (sin dirty checking, modo solo lectura).</p>
 */
@Service
@Transactional
public class BicicletaService {

    private final BicicletaRepository bicicletaRepository;

    public BicicletaService(BicicletaRepository bicicletaRepository) {
        this.bicicletaRepository = bicicletaRepository;
    }

    /**
     * Registra una nueva bicicleta. Por defecto queda en estado DISPONIBLE
     * salvo que el llamador indique uno distinto (necesario para el seed,
     * donde BIC-004 nace en mantenimiento).
     */
    public Bicicleta crear(String codigo, TipoBicicleta tipo, EstadoBicicleta estado) {
        if (bicicletaRepository.existsByCodigo(codigo)) {
            throw new CodigoBicicletaDuplicadoException(codigo);
        }
        EstadoBicicleta estadoInicial = (estado != null) ? estado : EstadoBicicleta.DISPONIBLE;
        Bicicleta nueva = new Bicicleta(codigo, tipo, estadoInicial);
        return bicicletaRepository.save(nueva);
    }

    @Transactional(readOnly = true)
    public List<Bicicleta> listarTodas() {
        return bicicletaRepository.findAll();
    }

    /**
     * Bicicletas disponibles (RF-04). Si {@code tipo} es {@code null} no se filtra.
     */
    @Transactional(readOnly = true)
    public List<Bicicleta> listarDisponibles(TipoBicicleta tipo) {
        if (tipo == null) {
            return bicicletaRepository.findByEstado(EstadoBicicleta.DISPONIBLE);
        }
        return bicicletaRepository.findByEstadoAndTipo(EstadoBicicleta.DISPONIBLE, tipo);
    }

    @Transactional(readOnly = true)
    public Bicicleta buscarPorCodigo(String codigo) {
        return bicicletaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new BicicletaNoEncontradaException(codigo));
    }
}
