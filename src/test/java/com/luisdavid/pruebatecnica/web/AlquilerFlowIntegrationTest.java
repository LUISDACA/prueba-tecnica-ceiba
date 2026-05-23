package com.luisdavid.pruebatecnica.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luisdavid.pruebatecnica.domain.Bicicleta;
import com.luisdavid.pruebatecnica.domain.EstadoBicicleta;
import com.luisdavid.pruebatecnica.domain.TipoBicicleta;
import com.luisdavid.pruebatecnica.repository.AlquilerRepository;
import com.luisdavid.pruebatecnica.repository.BicicletaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración end-to-end de la API.
 *
 * <p>Levanta el contexto completo de Spring Boot con H2 en memoria. Cada test
 * inserta su propio estado en {@link BicicletaRepository} y verifica la respuesta
 * HTTP (status, headers, body) mediante {@link MockMvc}.</p>
 *
 * <p>Perfil {@code test}: deshabilita el DataSeeder de producción para que la BD
 * arranque vacía y cada test sea independiente.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.security.api-key=test-key",
        "app.security.api-key-header=X-API-KEY"
})
@DisplayName("Flujo de alquiler — integración")
class AlquilerFlowIntegrationTest {

    private static final String API_KEY = "test-key";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private BicicletaRepository bicicletaRepository;

    @Autowired
    private AlquilerRepository alquilerRepository;

    @Autowired
    private ObjectMapper mapper;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        // springSecurity() añade la cadena de filtros real para que el API Key se valide
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();
        // Orden: alquileres antes que bicicletas (FK).
        alquilerRepository.deleteAll();
        bicicletaRepository.deleteAll();
    }

    @Test
    @DisplayName("Sin API Key responde 401")
    void sinApiKey() throws Exception {
        mvc.perform(get("/api/v1/bicicletas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Crear bicicleta devuelve 201 + Location header")
    void crearBicicleta() throws Exception {
        String body = mapper.writeValueAsString(Map.of(
                "codigo", "BIC-T01",
                "tipo", "URBANA"
        ));

        mvc.perform(post("/api/v1/bicicletas")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.codigo").value("BIC-T01"))
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"))
                .andExpect(jsonPath("$.tarifaPorHora").value(3500));
    }

    @Test
    @DisplayName("Validación: codigo vacío devuelve 400 con errores por campo")
    void validacionCampos() throws Exception {
        String body = mapper.writeValueAsString(Map.of("codigo", "", "tipo", "URBANA"));

        mvc.perform(post("/api/v1/bicicletas")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Datos inválidos"))
                .andExpect(jsonPath("$.errores.codigo").exists());
    }

    @Test
    @DisplayName("RN-04: alquilar bicicleta EN_MANTENIMIENTO devuelve 409")
    void alquilarBicicletaEnMantenimiento() throws Exception {
        bicicletaRepository.save(new Bicicleta("BIC-MNT", TipoBicicleta.MONTAÑA, EstadoBicicleta.EN_MANTENIMIENTO));

        String body = mapper.writeValueAsString(Map.of(
                "codigoBicicleta", "BIC-MNT",
                "nombreCliente", "Test",
                "duracionEstimadaHoras", 1
        ));

        mvc.perform(post("/api/v1/alquileres")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Bicicleta no disponible"));
    }

    @Test
    @DisplayName("Bicicleta inexistente al iniciar alquiler devuelve 404")
    void alquilarBicicletaInexistente() throws Exception {
        String body = mapper.writeValueAsString(Map.of(
                "codigoBicicleta", "BIC-NO",
                "nombreCliente", "Test",
                "duracionEstimadaHoras", 1
        ));

        mvc.perform(post("/api/v1/alquileres")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Bicicleta no encontrada"));
    }

    @Test
    @DisplayName("Flujo completo: crear bici, iniciar alquiler, finalizar, ver historial")
    void flujoCompleto() throws Exception {
        // 1) Crear bici URBANA
        String biciBody = mapper.writeValueAsString(Map.of("codigo", "BIC-INT", "tipo", "URBANA"));
        mvc.perform(post("/api/v1/bicicletas")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(biciBody))
                .andExpect(status().isCreated());

        // 2) Iniciar alquiler
        String alqBody = mapper.writeValueAsString(Map.of(
                "codigoBicicleta", "BIC-INT",
                "nombreCliente", "Cliente Integ",
                "duracionEstimadaHoras", 2
        ));
        String createdJson = mvc.perform(post("/api/v1/alquileres")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alqBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.finalizado").value(false))
                .andReturn().getResponse().getContentAsString();
        Long alqId = ((Number) mapper.readValue(createdJson, Map.class).get("id")).longValue();

        // 3) La bici ahora está ALQUILADA
        mvc.perform(get("/api/v1/bicicletas/BIC-INT").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ALQUILADA"));

        // 4) Finalizar
        mvc.perform(patch("/api/v1/alquileres/{id}/finalizar", alqId)
                        .header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finalizado").value(true))
                .andExpect(jsonPath("$.costoTotal").isNumber());

        // 5) Bici vuelve a DISPONIBLE
        mvc.perform(get("/api/v1/bicicletas/BIC-INT").header("X-API-KEY", API_KEY))
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"));

        // 6) Historial muestra el alquiler
        mvc.perform(get("/api/v1/bicicletas/BIC-INT/historial").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombreCliente").value("Cliente Integ"))
                .andExpect(jsonPath("$[0].finalizado").value(true));

        // 7) Finalizar dos veces el mismo alquiler -> 409
        mvc.perform(patch("/api/v1/alquileres/{id}/finalizar", alqId)
                        .header("X-API-KEY", API_KEY))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /disponibles?tipo=URBANA filtra correctamente")
    void filtrarDisponiblesPorTipo() throws Exception {
        bicicletaRepository.save(new Bicicleta("BIC-U1", TipoBicicleta.URBANA, EstadoBicicleta.DISPONIBLE));
        bicicletaRepository.save(new Bicicleta("BIC-U2", TipoBicicleta.URBANA, EstadoBicicleta.ALQUILADA));
        bicicletaRepository.save(new Bicicleta("BIC-M1", TipoBicicleta.MONTAÑA, EstadoBicicleta.DISPONIBLE));

        mvc.perform(get("/api/v1/bicicletas/disponibles").param("tipo", "URBANA")
                        .header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].codigo").value("BIC-U1"));
    }
}
