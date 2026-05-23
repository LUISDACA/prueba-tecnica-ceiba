package com.luisdavid.pruebatecnica.config;

import com.luisdavid.pruebatecnica.domain.EstadoBicicleta;
import com.luisdavid.pruebatecnica.domain.TipoBicicleta;
import com.luisdavid.pruebatecnica.service.BicicletaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Carga los datos de referencia del enunciado (BIC-001 a BIC-005) al arrancar.
 *
 * <p>Excluido en perfil {@code test} para que los tests partan de una BD vacía
 * y puedan controlar su propio estado inicial.</p>
 *
 * <p>Idempotente: cada inserción se intenta y se ignora silenciosamente si la
 * bicicleta ya existe — útil ante reinicios en caliente con DevTools.</p>
 */
@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final BicicletaService bicicletaService;

    public DataSeeder(BicicletaService bicicletaService) {
        this.bicicletaService = bicicletaService;
    }

    @Override
    public void run(String... args) {
        log.info("Cargando datos de referencia...");
        seed("BIC-001", TipoBicicleta.URBANA, EstadoBicicleta.DISPONIBLE);
        seed("BIC-002", TipoBicicleta.MONTAÑA, EstadoBicicleta.DISPONIBLE);
        seed("BIC-003", TipoBicicleta.ELÉCTRICA, EstadoBicicleta.DISPONIBLE);
        seed("BIC-004", TipoBicicleta.MONTAÑA, EstadoBicicleta.EN_MANTENIMIENTO);
        seed("BIC-005", TipoBicicleta.URBANA, EstadoBicicleta.DISPONIBLE);
        log.info("Datos de referencia cargados.");
    }

    private void seed(String codigo, TipoBicicleta tipo, EstadoBicicleta estado) {
        try {
            bicicletaService.crear(codigo, tipo, estado);
        } catch (RuntimeException ex) {
            log.debug("Seed: {} ya existía o no se pudo crear: {}", codigo, ex.getMessage());
        }
    }
}
