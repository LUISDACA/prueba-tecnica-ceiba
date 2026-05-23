package com.luisdavid.pruebatecnica.service;

import com.luisdavid.pruebatecnica.domain.Alquiler;
import com.luisdavid.pruebatecnica.domain.Bicicleta;
import com.luisdavid.pruebatecnica.exception.AlquilerNoEncontradoException;
import com.luisdavid.pruebatecnica.exception.AlquilerYaFinalizadoException;
import com.luisdavid.pruebatecnica.exception.BicicletaNoDisponibleException;
import com.luisdavid.pruebatecnica.exception.BicicletaNoEncontradaException;
import com.luisdavid.pruebatecnica.repository.AlquilerRepository;
import com.luisdavid.pruebatecnica.repository.BicicletaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Orquesta los flujos de alquiler (RF-02, RF-03, RF-05).
 *
 * <p>El {@link Clock} es inyectado para que los tests puedan fijar el tiempo
 * (Clock.fixed) y verificar costos/multas de forma determinista. En producción
 * se registra un Clock.systemDefaultZone() vía configuración.</p>
 */
@Service
@Transactional
public class AlquilerService {

    private final AlquilerRepository alquilerRepository;
    private final BicicletaRepository bicicletaRepository;
    private final TarifaCalculator tarifaCalculator;
    private final Clock clock;

    public AlquilerService(AlquilerRepository alquilerRepository,
                           BicicletaRepository bicicletaRepository,
                           TarifaCalculator tarifaCalculator,
                           Clock clock) {
        this.alquilerRepository = alquilerRepository;
        this.bicicletaRepository = bicicletaRepository;
        this.tarifaCalculator = tarifaCalculator;
        this.clock = clock;
    }

    /**
     * Inicia un alquiler (RF-02). La hora de inicio la fija el servidor
     * (supuesto documentado en README: evita manipulación por parte del cliente).
     *
     * @throws BicicletaNoEncontradaException si el código no existe.
     * @throws BicicletaNoDisponibleException si la bici no está DISPONIBLE (RN-04).
     */
    public Alquiler iniciar(String codigoBicicleta, String nombreCliente, int duracionEstimadaHoras) {
        Bicicleta bicicleta = bicicletaRepository.findByCodigo(codigoBicicleta)
                .orElseThrow(() -> new BicicletaNoEncontradaException(codigoBicicleta));

        if (!bicicleta.estaDisponible()) {
            throw new BicicletaNoDisponibleException(codigoBicicleta, bicicleta.getEstado());
        }

        bicicleta.marcarComoAlquilada();
        // La hora la fija el servidor para evitar inconsistencias con el reloj del cliente.
        LocalDateTime ahora = LocalDateTime.now(clock);
        Alquiler alquiler = new Alquiler(bicicleta, nombreCliente, ahora, duracionEstimadaHoras);
        return alquilerRepository.save(alquiler);
    }

    /**
     * Finaliza un alquiler (RF-03). Calcula costo y multa, libera la bici.
     *
     * @throws AlquilerNoEncontradoException si el alquiler no existe (RN-05).
     * @throws AlquilerYaFinalizadoException si ya tiene hora de fin (RN-05).
     */
    public Alquiler finalizar(Long idAlquiler) {
        Alquiler alquiler = alquilerRepository.findById(idAlquiler)
                .orElseThrow(() -> new AlquilerNoEncontradoException(idAlquiler));

        if (alquiler.estaFinalizado()) {
            throw new AlquilerYaFinalizadoException(idAlquiler);
        }

        LocalDateTime ahora = LocalDateTime.now(clock);
        CalculoTarifa calculo = tarifaCalculator.calcular(
                alquiler.getBicicleta().getTipo(),
                alquiler.getHoraInicio(),
                ahora,
                alquiler.getDuracionEstimadaHoras()
        );

        alquiler.finalizar(ahora, calculo.costoBase(), calculo.multa());
        alquiler.getBicicleta().marcarComoDisponible();
        return alquiler;
    }

    @Transactional(readOnly = true)
    public List<Alquiler> historialDeBicicleta(String codigoBicicleta) {
        if (!bicicletaRepository.existsByCodigo(codigoBicicleta)) {
            throw new BicicletaNoEncontradaException(codigoBicicleta);
        }
        return alquilerRepository.findByBicicletaCodigoOrderByHoraInicioDesc(codigoBicicleta);
    }

    @Transactional(readOnly = true)
    public Alquiler buscarPorId(Long id) {
        return alquilerRepository.findWithBicicletaById(id)
                .orElseThrow(() -> new AlquilerNoEncontradoException(id));
    }
}
