package com.luisdavid.pruebatecnica.repository;

import com.luisdavid.pruebatecnica.domain.Alquiler;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de alquileres.
 *
 * <p>El historial (RF-05) se ordena por hora de inicio descendente — el alquiler
 * mas reciente aparece primero, que es lo que un operador esperaria al consultar.</p>
 *
 * <p>{@code @EntityGraph(attributePaths = "bicicleta")} en consultas que
 * retornan listas evita el problema clasico de <em>lazy loading</em> fuera de
 * transaccion: la bicicleta se carga con un JOIN en lugar de un proxy lazy,
 * y el mapeo a DTO en el controller no falla al cerrarse la sesion Hibernate.</p>
 */
@Repository
public interface AlquilerRepository extends JpaRepository<Alquiler, Long> {

    /** Historial de alquileres de una bicicleta por su codigo de negocio (RF-05). */
    @EntityGraph(attributePaths = "bicicleta")
    List<Alquiler> findByBicicletaCodigoOrderByHoraInicioDesc(String codigoBicicleta);

    /** Carga un alquiler con su bicicleta en una sola consulta. */
    @EntityGraph(attributePaths = "bicicleta")
    Optional<Alquiler> findWithBicicletaById(Long id);
}
