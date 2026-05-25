package com.luisdavid.pruebatecnica.service;

import com.luisdavid.pruebatecnica.domain.Alquiler;
import com.luisdavid.pruebatecnica.domain.Bicicleta;
import com.luisdavid.pruebatecnica.domain.EstadoBicicleta;
import com.luisdavid.pruebatecnica.domain.TipoBicicleta;
import com.luisdavid.pruebatecnica.exception.AlquilerNoEncontradoException;
import com.luisdavid.pruebatecnica.exception.AlquilerYaFinalizadoException;
import com.luisdavid.pruebatecnica.exception.BicicletaNoDisponibleException;
import com.luisdavid.pruebatecnica.exception.BicicletaNoEncontradaException;
import com.luisdavid.pruebatecnica.repository.AlquilerRepository;
import com.luisdavid.pruebatecnica.repository.BicicletaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios del {@link AlquilerService}.
 *
 * <p>Usa un {@link Clock#fixed} para controlar el tiempo y verificar calculos
 * deterministas. Los repositorios son mocks (Mockito).</p>
 */
@DisplayName("AlquilerService")
class AlquilerServiceTest {

    private AlquilerRepository alquilerRepository;
    private BicicletaRepository bicicletaRepository;
    private TarifaCalculator tarifaCalculator;
    private Clock clockInicial;
    private AlquilerService service;

    @BeforeEach
    void setUp() {
        alquilerRepository = mock(AlquilerRepository.class);
        bicicletaRepository = mock(BicicletaRepository.class);
        tarifaCalculator = new TarifaCalculator(); // calculador real, para integrar con la logica monetaria
        clockInicial = Clock.fixed(Instant.parse("2026-05-22T10:00:00Z"), ZoneId.of("UTC"));
        service = new AlquilerService(alquilerRepository, bicicletaRepository, tarifaCalculator, clockInicial);
    }

    @Test
    @DisplayName("RF-02: iniciar marca la bici como ALQUILADA y persiste el alquiler")
    void iniciarAlquiler() {
        Bicicleta bici = new Bicicleta("BIC-001", TipoBicicleta.URBANA, EstadoBicicleta.DISPONIBLE);
        when(bicicletaRepository.findByCodigo("BIC-001")).thenReturn(Optional.of(bici));
        when(alquilerRepository.save(any(Alquiler.class))).thenAnswer(inv -> inv.getArgument(0));

        Alquiler resultado = service.iniciar("BIC-001", "Juan Perez", 2);

        assertThat(bici.getEstado()).isEqualTo(EstadoBicicleta.ALQUILADA);
        assertThat(resultado.getNombreCliente()).isEqualTo("Juan Perez");
        assertThat(resultado.getDuracionEstimadaHoras()).isEqualTo(2);
        verify(alquilerRepository).save(any(Alquiler.class));
    }

    @Test
    @DisplayName("RN-04: iniciar con bici EN_MANTENIMIENTO lanza BicicletaNoDisponibleException")
    void iniciarConBiciNoDisponible() {
        Bicicleta bici = new Bicicleta("BIC-004", TipoBicicleta.MONTANA, EstadoBicicleta.EN_MANTENIMIENTO);
        when(bicicletaRepository.findByCodigo("BIC-004")).thenReturn(Optional.of(bici));

        assertThatThrownBy(() -> service.iniciar("BIC-004", "Cliente", 1))
                .isInstanceOf(BicicletaNoDisponibleException.class)
                .hasMessageContaining("BIC-004")
                .hasMessageContaining("EN_MANTENIMIENTO");

        verify(alquilerRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("Iniciar con codigo inexistente lanza BicicletaNoEncontradaException")
    void iniciarConBiciInexistente() {
        when(bicicletaRepository.findByCodigo("BIC-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.iniciar("BIC-999", "Cliente", 1))
                .isInstanceOf(BicicletaNoEncontradaException.class)
                .hasMessageContaining("BIC-999");
    }

    @Test
    @DisplayName("RF-03: finalizar calcula costo+multa y libera la bici")
    void finalizarAlquiler() {
        Bicicleta bici = new Bicicleta("BIC-002", TipoBicicleta.MONTANA, EstadoBicicleta.DISPONIBLE);
        when(bicicletaRepository.findByCodigo("BIC-002")).thenReturn(Optional.of(bici));
        when(alquilerRepository.save(any(Alquiler.class))).thenAnswer(inv -> inv.getArgument(0));

        // Iniciar a las 10:00, estimado 2h
        Alquiler alquiler = service.iniciar("BIC-002", "Ana", 2);
        when(alquilerRepository.findById(any())).thenReturn(Optional.of(alquiler));

        // Avanzo el reloj 3h 20min para reproducir el ejemplo del enunciado
        Clock relojFinal = Clock.fixed(Instant.parse("2026-05-22T13:20:00Z"), ZoneId.of("UTC"));
        service = new AlquilerService(alquilerRepository, bicicletaRepository, tarifaCalculator, relojFinal);

        Alquiler finalizado = service.finalizar(alquiler.getId());

        assertThat(finalizado.estaFinalizado()).isTrue();
        assertThat(finalizado.getCostoBase()).isEqualByComparingTo("20000.00");
        assertThat(finalizado.getMulta()).isEqualByComparingTo("5000.00");
        assertThat(finalizado.getCostoTotal()).isEqualByComparingTo("25000.00");
        assertThat(bici.getEstado()).isEqualTo(EstadoBicicleta.DISPONIBLE);
    }

    @Test
    @DisplayName("RN-05: finalizar alquiler inexistente lanza AlquilerNoEncontradoException")
    void finalizarInexistente() {
        when(alquilerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.finalizar(99L))
                .isInstanceOf(AlquilerNoEncontradoException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("RN-05: finalizar alquiler ya finalizado lanza AlquilerYaFinalizadoException")
    void finalizarYaFinalizado() {
        Bicicleta bici = new Bicicleta("BIC-001", TipoBicicleta.URBANA, EstadoBicicleta.DISPONIBLE);
        when(bicicletaRepository.findByCodigo("BIC-001")).thenReturn(Optional.of(bici));
        when(alquilerRepository.save(any(Alquiler.class))).thenAnswer(inv -> inv.getArgument(0));

        Alquiler alquiler = service.iniciar("BIC-001", "Cliente", 1);
        // simulo un id ya asignado
        when(alquilerRepository.findById(any())).thenReturn(Optional.of(alquiler));

        // Primera finalizacion: ok
        Clock relojFinal = Clock.fixed(Instant.parse("2026-05-22T11:00:00Z"), ZoneId.of("UTC"));
        service = new AlquilerService(alquilerRepository, bicicletaRepository, tarifaCalculator, relojFinal);
        service.finalizar(alquiler.getId());

        // Segunda finalizacion: debe fallar
        assertThatThrownBy(() -> service.finalizar(alquiler.getId()))
                .isInstanceOf(AlquilerYaFinalizadoException.class);
    }
}
