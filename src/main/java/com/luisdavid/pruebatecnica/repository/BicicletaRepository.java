package com.luisdavid.pruebatecnica.repository;

import com.luisdavid.pruebatecnica.domain.Bicicleta;
import com.luisdavid.pruebatecnica.domain.EstadoBicicleta;
import com.luisdavid.pruebatecnica.domain.TipoBicicleta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de bicicletas. Las consultas se derivan del nombre del metodo
 * (Spring Data) — sin SQL manual mientras la logica sea trivial.
 */
@Repository
public interface BicicletaRepository extends JpaRepository<Bicicleta, Long> {

    /** Busqueda por identificador de negocio. Usado por casi todos los flujos. */
    Optional<Bicicleta> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    /** Bicicletas disponibles, sin filtro de tipo (RF-04). */
    List<Bicicleta> findByEstado(EstadoBicicleta estado);

    /** Bicicletas disponibles filtradas por tipo (RF-04 variante). */
    List<Bicicleta> findByEstadoAndTipo(EstadoBicicleta estado, TipoBicicleta tipo);
}
