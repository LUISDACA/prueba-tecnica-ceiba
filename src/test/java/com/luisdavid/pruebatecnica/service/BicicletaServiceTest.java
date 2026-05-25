package com.luisdavid.pruebatecnica.service;

import com.luisdavid.pruebatecnica.domain.Bicicleta;
import com.luisdavid.pruebatecnica.domain.EstadoBicicleta;
import com.luisdavid.pruebatecnica.domain.TipoBicicleta;
import com.luisdavid.pruebatecnica.exception.BicicletaNoEncontradaException;
import com.luisdavid.pruebatecnica.exception.CodigoBicicletaDuplicadoException;
import com.luisdavid.pruebatecnica.repository.BicicletaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("BicicletaService")
class BicicletaServiceTest {

    private BicicletaRepository repository;
    private BicicletaService service;

    @BeforeEach
    void setUp() {
        repository = mock(BicicletaRepository.class);
        service = new BicicletaService(repository);
    }

    @Test
    @DisplayName("crear con estado nulo asigna DISPONIBLE por defecto")
    void crearConEstadoNulo() {
        when(repository.existsByCodigo("BIC-010")).thenReturn(false);
        when(repository.save(any(Bicicleta.class))).thenAnswer(inv -> inv.getArgument(0));

        Bicicleta b = service.crear("BIC-010", TipoBicicleta.URBANA, null);

        assertThat(b.getEstado()).isEqualTo(EstadoBicicleta.DISPONIBLE);
    }

    @Test
    @DisplayName("crear con estado explicito respeta el estado provisto (para seed)")
    void crearConEstadoExplicito() {
        when(repository.existsByCodigo("BIC-004")).thenReturn(false);
        when(repository.save(any(Bicicleta.class))).thenAnswer(inv -> inv.getArgument(0));

        Bicicleta b = service.crear("BIC-004", TipoBicicleta.MONTANA, EstadoBicicleta.EN_MANTENIMIENTO);

        assertThat(b.getEstado()).isEqualTo(EstadoBicicleta.EN_MANTENIMIENTO);
    }

    @Test
    @DisplayName("crear con codigo duplicado lanza CodigoBicicletaDuplicadoException")
    void crearConCodigoDuplicado() {
        when(repository.existsByCodigo("BIC-001")).thenReturn(true);

        assertThatThrownBy(() -> service.crear("BIC-001", TipoBicicleta.URBANA, null))
                .isInstanceOf(CodigoBicicletaDuplicadoException.class);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("buscarPorCodigo lanza BicicletaNoEncontradaException si no existe")
    void buscarInexistente() {
        when(repository.findByCodigo("BIC-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorCodigo("BIC-999"))
                .isInstanceOf(BicicletaNoEncontradaException.class)
                .hasMessageContaining("BIC-999");
    }

    @Test
    @DisplayName("listarDisponibles sin tipo no aplica filtro de tipo")
    void listarDisponiblesSinTipo() {
        Bicicleta b = new Bicicleta("BIC-001", TipoBicicleta.URBANA, EstadoBicicleta.DISPONIBLE);
        when(repository.findByEstado(EstadoBicicleta.DISPONIBLE)).thenReturn(List.of(b));

        List<Bicicleta> result = service.listarDisponibles(null);

        assertThat(result).hasSize(1);
        verify(repository).findByEstado(EstadoBicicleta.DISPONIBLE);
        verify(repository, never()).findByEstadoAndTipo(any(), any());
    }

    @Test
    @DisplayName("listarDisponibles con tipo filtra por tipo y estado")
    void listarDisponiblesConTipo() {
        Bicicleta b = new Bicicleta("BIC-001", TipoBicicleta.URBANA, EstadoBicicleta.DISPONIBLE);
        when(repository.findByEstadoAndTipo(EstadoBicicleta.DISPONIBLE, TipoBicicleta.URBANA))
                .thenReturn(List.of(b));

        List<Bicicleta> result = service.listarDisponibles(TipoBicicleta.URBANA);

        assertThat(result).hasSize(1);
        verify(repository).findByEstadoAndTipo(EstadoBicicleta.DISPONIBLE, TipoBicicleta.URBANA);
    }
}
