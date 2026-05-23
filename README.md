# API REST — Alquiler de Bicicletas Urbanas

Esta es mi solución a la prueba técnica para el rol de Practicante Java. Es una
API REST en Spring Boot que permite registrar bicicletas, controlar su
disponibilidad y calcular automáticamente el costo de cada alquiler junto con
la multa por devolución tardía.

Intenté que el README sea suficiente para correr el proyecto, entender por qué
está organizado como está y revisar cualquier decisión que tomé.

---

## Tabla de contenido

1. [Qué hace](#qué-hace)
2. [Cómo lo organicé](#cómo-lo-organicé)
3. [Decisiones que tomé y por qué](#decisiones-que-tomé-y-por-qué)
4. [Stack](#stack)
5. [Cómo correr el proyecto](#cómo-correr-el-proyecto)
6. [Configuración](#configuración)
7. [Endpoints con ejemplos](#endpoints-con-ejemplos)
8. [Reglas de negocio](#reglas-de-negocio)
9. [Manejo de errores](#manejo-de-errores)
10. [Pruebas](#pruebas)
11. [Supuestos que tomé](#supuestos-que-tomé)
12. [Despliegue en Azure](#despliegue-en-azure)
13. [Estructura del proyecto](#estructura-del-proyecto)

---

## Qué hace

La API permite:

- Registrar bicicletas con su código único, tipo y estado.
- Iniciar alquileres de bicicletas disponibles.
- Finalizar alquileres calculando el costo base y, si aplica, la multa por
  devolución tardía.
- Consultar la disponibilidad de bicicletas (con filtro opcional por tipo).
- Ver el historial de alquileres de cada bicicleta.

Cubrí todos los requerimientos funcionales (RF-01 a RF-05) y las cinco reglas
de negocio (RN-01 a RN-05) del enunciado.

---

## Cómo lo organicé

Opté por una arquitectura por capas, que es la que mejor conozco para Spring
Boot y la que separa más claramente las responsabilidades:

```
┌──────────────────────────────────────────────────────────────┐
│  web (controllers, DTOs, mappers, ExceptionHandler global)   │  ← HTTP
├──────────────────────────────────────────────────────────────┤
│  service (BicicletaService, AlquilerService, TarifaCalc.)    │  ← Negocio
├──────────────────────────────────────────────────────────────┤
│  repository (Spring Data JPA)                                │  ← Persistencia
├──────────────────────────────────────────────────────────────┤
│  domain (entidades, enums, RedondeoHoras)                    │  ← Modelo
└──────────────────────────────────────────────────────────────┘
```

La idea es que cada capa solo conozca a la que tiene justo debajo. Los
controllers no saben de JPA; los repositorios no saben de HTTP; el dominio no
sabe de Spring. Esto me ayudó a poder testear el `TarifaCalculator` (lo más
sensible del proyecto) sin levantar el contexto de Spring.

---

## Decisiones que tomé y por qué

| Lo que hice | Por qué |
|---|---|
| Saqué la lógica del cálculo del costo y la multa a una clase aparte (`TarifaCalculator`), sin estado y sin dependencias de Spring. | Es la parte más sensible del sistema (toda la lógica monetaria). Al ser una clase pura, pude testearla con un montón de casos en milisegundos. Cumple SRP. |
| Inyecté un `Clock` en `AlquilerService` en vez de usar `LocalDateTime.now()` directo. | Los tests necesitan ser deterministas. Con un `Clock.fixed(...)` puedo congelar el tiempo y verificar costos exactos. Es Inversión de Dependencias en acción. |
| Puse las tarifas adentro del enum `TipoBicicleta`. | El tipo "sabe" su tarifa, en lugar de tener un `Map<TipoBicicleta, BigDecimal>` por ahí. Si en algún momento las tarifas se vuelven dinámicas, las muevo a una tabla — pero el enunciado las da fijas, así que esto era lo más simple y cohesivo. |
| Usé `record` de Java para los DTOs en vez de clases con Lombok. | Son inmutables sin esfuerzo, no necesitan getters/setters, y Bean Validation funciona perfecto con ellos. Cero dependencias adicionales. |
| Separé los DTOs de las entidades JPA. | Para no exponer detalles internos (como el `id` numérico o el campo `version` de optimistic locking) y para poder versionar la API sin tocar el modelo. |
| Las excepciones describen el negocio (`BicicletaNoDisponibleException`), no HTTP. Un único `@RestControllerAdvice` las mapea a `ProblemDetail` (RFC 7807). | Así no tengo que repetir try/catch en cada controller (DRY) y los servicios no necesitan saber de códigos HTTP. |
| Agregué `@Version` a `Bicicleta` para bloqueo optimista. | Si dos peticiones intentan alquilar la misma bici al mismo tiempo, JPA detecta el conflicto. El que llega segundo recibe un 409, no una bici "doblemente alquilada". |
| Usé `@EntityGraph` en consultas que retornan listas de alquileres. | Para evitar el error clásico de *lazy loading* fuera de transacción cuando el mapper accede a `alquiler.getBicicleta()`. |
| `BigDecimal` para todo lo monetario. | Con `double` cosas como `0.1 + 0.2 != 0.3` rompen los cálculos. Para dinero el estándar es `BigDecimal` con `RoundingMode` explícito. |
| API Key en header (`X-API-KEY`) sobre Spring Security. | El enunciado pide "seguridad básica". JWT/OAuth me parecía sobreingeniería para esto. Un filtro `OncePerRequestFilter` que valida la clave es simple y suficiente. |
| No usé Lombok en las entidades JPA. | `@Data` genera `equals`/`hashCode` con todos los campos, y eso rompe el contrato cuando el id se asigna después de persistir. Las entidades las hice "a mano" definiendo igualdad por la PK. |
| Usé `PATCH` para finalizar el alquiler, no `POST`. | Finalizar es una actualización parcial del recurso, no la creación de uno nuevo. Quería evitar el anti-patrón RPC-sobre-REST (`POST /alquileres/{id}/finalizar`). |
| Versioné la API con `/api/v1/...`. | Para que si más adelante hay que cambiar algo en el contrato, pueda existir un `/api/v2/...` sin romper a los clientes viejos. |
| Desactivé `spring.jpa.open-in-view`. | Es un anti-patrón. Las transacciones se cierran al salir del servicio, no se extienden al controller. |

---

## Stack

| Componente | Versión | Para qué |
|---|---|---|
| Java | 21 LTS | Lenguaje (uso records y otras features modernas) |
| Spring Boot | 3.5.14 | Framework |
| Maven | wrapper incluido | Build |
| Spring Web | (con Spring Boot) | REST controllers |
| Spring Data JPA + Hibernate | (con Spring Boot) | Persistencia |
| H2 Database | (con Spring Boot) | BD en memoria, sin instalar nada |
| Spring Security | (con Spring Boot) | Filtro de API Key |
| Bean Validation | (con Spring Boot) | Validación de DTOs |
| SpringDoc OpenAPI | 2.8.0 | Swagger UI (la agregué a mano al pom) |
| Lombok | (con Spring Boot) | Reducir boilerplate, con moderación |
| Spring Boot DevTools | (con Spring Boot) | Hot reload mientras desarrollaba |
| JUnit 5 + Mockito + AssertJ | (con Spring Boot Test) | Tests |

Elegí Maven sobre Gradle porque me pareció más legible para alguien que no
conoce el proyecto. Elegí H2 sobre Postgres para que no haya que instalar nada
externo — la idea es que el revisor pueda correrlo con un solo comando.

---

## Cómo correr el proyecto

### Requisitos

- **JDK 21** instalado (probado con OpenJDK 21.0.10).
- **No necesitas instalar Maven** — el proyecto incluye el wrapper (`mvnw` /
  `mvnw.cmd`).
- **No necesitas base de datos externa** — H2 corre en memoria.

### Pasos

```bash
# 1. Clonar el repositorio
git clone <URL-del-repo>
cd prueba-tecnica-java

# 2. Compilar y correr los tests
./mvnw test                  # Linux/Mac
mvnw.cmd test                # Windows

# 3. Arrancar la app
./mvnw spring-boot:run       # Linux/Mac
mvnw.cmd spring-boot:run     # Windows
```

La aplicación arranca en **http://localhost:8080** en unos 4 segundos.

### Recursos útiles una vez arrancada

| Recurso | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Consola H2 | http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:alquileresdb`, user: `sa`, sin password) |

> 💡 La forma más cómoda de probar la API es desde Swagger UI: hay un botón
> *Authorize* donde se pega la API Key una sola vez y todos los endpoints
> quedan listos para probar.

---

## Configuración

Toda la configuración está en `src/main/resources/application.properties`. Las
propiedades importantes se pueden sobreescribir con variables de entorno:

| Propiedad | Default | Variable de entorno |
|---|---|---|
| `app.security.api-key` | `dev-api-key-cambiar-en-produccion` | `API_KEY` |
| `app.security.api-key-header` | `X-API-KEY` | — |
| `server.port` | 8080 | — |

En producción la idea es que la API Key venga de fuera (variable de entorno o
secret manager), no del archivo de propiedades. Para esta prueba la dejé con
un default para que el revisor pueda correr todo sin configurar nada.

```bash
API_KEY=clave-secreta-real ./mvnw spring-boot:run
```

---

## Endpoints con ejemplos

Todas las rutas `/api/**` requieren el header `X-API-KEY`. Los ejemplos usan
la API Key por defecto.

### Bicicletas

#### Crear bicicleta — `POST /api/v1/bicicletas`

```bash
curl -X POST http://localhost:8080/api/v1/bicicletas \
  -H "X-API-KEY: dev-api-key-cambiar-en-produccion" \
  -H "Content-Type: application/json" \
  -d '{"codigo":"BIC-100","tipo":"ELÉCTRICA"}'
```

Respuesta `201 Created`:

```json
{
  "codigo": "BIC-100",
  "tipo": "ELÉCTRICA",
  "estado": "DISPONIBLE",
  "tarifaPorHora": 7500
}
```

#### Listar todas — `GET /api/v1/bicicletas`

```bash
curl http://localhost:8080/api/v1/bicicletas \
  -H "X-API-KEY: dev-api-key-cambiar-en-produccion"
```

#### Listar disponibles — `GET /api/v1/bicicletas/disponibles`

```bash
# Todas las disponibles
curl "http://localhost:8080/api/v1/bicicletas/disponibles" \
  -H "X-API-KEY: dev-api-key-cambiar-en-produccion"

# Filtrar por tipo
curl "http://localhost:8080/api/v1/bicicletas/disponibles?tipo=URBANA" \
  -H "X-API-KEY: dev-api-key-cambiar-en-produccion"
```

#### Buscar por código — `GET /api/v1/bicicletas/{codigo}`

```bash
curl http://localhost:8080/api/v1/bicicletas/BIC-001 \
  -H "X-API-KEY: dev-api-key-cambiar-en-produccion"
```

#### Historial de alquileres — `GET /api/v1/bicicletas/{codigo}/historial`

```bash
curl http://localhost:8080/api/v1/bicicletas/BIC-001/historial \
  -H "X-API-KEY: dev-api-key-cambiar-en-produccion"
```

### Alquileres

#### Iniciar alquiler — `POST /api/v1/alquileres`

```bash
curl -X POST http://localhost:8080/api/v1/alquileres \
  -H "X-API-KEY: dev-api-key-cambiar-en-produccion" \
  -H "Content-Type: application/json" \
  -d '{
    "codigoBicicleta": "BIC-002",
    "nombreCliente": "Juan Pérez",
    "duracionEstimadaHoras": 2
  }'
```

Respuesta `201 Created`:

```json
{
  "id": 1,
  "codigoBicicleta": "BIC-002",
  "tipoBicicleta": "MONTAÑA",
  "nombreCliente": "Juan Pérez",
  "horaInicio": "2026-05-22T22:50:29.16",
  "horaFin": null,
  "duracionEstimadaHoras": 2,
  "duracionRealHoras": null,
  "costoBase": null,
  "multa": null,
  "costoTotal": null,
  "tuvoMulta": false,
  "finalizado": false
}
```

#### Finalizar alquiler — `PATCH /api/v1/alquileres/{id}/finalizar`

```bash
curl -X PATCH http://localhost:8080/api/v1/alquileres/1/finalizar \
  -H "X-API-KEY: dev-api-key-cambiar-en-produccion"
```

Respuesta `200 OK` reproduciendo el ejemplo del enunciado (MONTAÑA, 2h
estimadas, 3h 20min reales):

```json
{
  "id": 1,
  "codigoBicicleta": "BIC-002",
  "tipoBicicleta": "MONTAÑA",
  "nombreCliente": "Juan Pérez",
  "horaInicio": "...",
  "horaFin": "...",
  "duracionEstimadaHoras": 2,
  "duracionRealHoras": 4,
  "costoBase": 20000.00,
  "multa": 5000.00,
  "costoTotal": 25000.00,
  "tuvoMulta": true,
  "finalizado": true
}
```

#### Detalle de alquiler — `GET /api/v1/alquileres/{id}`

```bash
curl http://localhost:8080/api/v1/alquileres/1 \
  -H "X-API-KEY: dev-api-key-cambiar-en-produccion"
```

### Datos que se cargan al arrancar (seed)

Al iniciar la app un `CommandLineRunner` carga las cinco bicicletas del
enunciado:

| Código | Tipo | Estado inicial |
|---|---|---|
| BIC-001 | URBANA | DISPONIBLE |
| BIC-002 | MONTAÑA | DISPONIBLE |
| BIC-003 | ELÉCTRICA | DISPONIBLE |
| BIC-004 | MONTAÑA | EN_MANTENIMIENTO |
| BIC-005 | URBANA | DISPONIBLE |

---

## Reglas de negocio

### RN-01 — Tarifas por tipo

Las definí dentro del enum `TipoBicicleta`:

| Tipo | Tarifa/hora |
|---|---|
| URBANA | $3.500 |
| MONTAÑA | $5.000 |
| ELÉCTRICA | $7.500 |

### RN-02 — Cálculo del costo base

`costoBase = horasRealesRedondeadasAlAlza × tarifaPorHora`

El redondeo lo encapsulé en `RedondeoHoras.alAlza(Duration)`:

- 1h 10min → 2h
- 2h exactas → 2h
- 500 ms → 1h (cualquier uso > 0 implica al menos 1 hora facturable)
- 0 ms → 0h

### RN-03 — Multa por devolución tardía

`multa = 50% × tarifaPorHora × horasRetrasoRedondeadasAlAlza`

- Si la devolución es a tiempo o antes → multa = 0.
- Mínimo facturable de retraso: 1 hora (un retraso de 30 segundos cuenta como
  1h).
- Verifiqué el ejemplo del enunciado: MONTAÑA, 2h estimadas, 3h 20min reales:
  - Costo base: 4h × $5.000 = $20.000
  - Multa: 2h × ($5.000 × 0.5) = $5.000
  - **Total: $25.000** ✓

### RN-04 — No se puede alquilar una bicicleta no disponible

`AlquilerService.iniciar()` verifica el estado antes de aceptar el alquiler. Si
la bici no está DISPONIBLE, lanzo `BicicletaNoDisponibleException` → HTTP 409.

### RN-05 — No se puede finalizar un alquiler que no existe o ya terminó

- Si el id no existe → `AlquilerNoEncontradoException` → HTTP 404.
- Si ya tiene `horaFin` → `AlquilerYaFinalizadoException` → HTTP 409.

---

## Manejo de errores

Todas las excepciones se traducen a `ProblemDetail` (RFC 7807) en
`GlobalExceptionHandler`. Quería que el formato del error fuera siempre el
mismo, sin importar de dónde venga:

```json
{
  "type": "about:blank",
  "title": "Bicicleta no disponible",
  "status": 409,
  "detail": "La bicicleta 'BIC-004' no está disponible para alquiler (estado actual: EN_MANTENIMIENTO)",
  "timestamp": "2026-05-22T22:50:30-05:00"
}
```

### Mapeo de excepciones a HTTP

| Excepción | HTTP |
|---|---|
| `BicicletaNoEncontradaException` | 404 |
| `AlquilerNoEncontradoException` | 404 |
| `BicicletaNoDisponibleException` | 409 |
| `AlquilerYaFinalizadoException` | 409 |
| `CodigoBicicletaDuplicadoException` | 409 |
| `OptimisticLockingFailureException` | 409 |
| `MethodArgumentNotValidException` (Bean Validation) | 400 + lista de errores por campo |
| `HttpMessageNotReadableException` (JSON malformado) | 400 |
| `MethodArgumentTypeMismatchException` (enum inválido en query) | 400 |
| Cualquier otra `Exception` | 500 (sin filtrar stack traces) |

---

## Pruebas

```bash
./mvnw test
```

Escribí **47 tests automatizados**. La idea fue cubrir bien lo más sensible (el
cálculo del costo y la multa) y dejar al menos un test de integración que
recorra el flujo completo:

| Suite | Qué cubre |
|---|---|
| `TarifaCalculatorTest` | RN-02 (redondeo del costo base), RN-03 (multa), validación de argumentos, consistencia interna |
| `RedondeoHorasTest` | Tests parametrizados con `@CsvSource` para valores límite |
| `AlquilerServiceTest` | Iniciar/finalizar con `Clock` fijo, RN-04, RN-05, errores |
| `BicicletaServiceTest` | Crear/buscar/listar, código duplicado, filtros |
| `AlquilerFlowIntegrationTest` | Integración end-to-end con `MockMvc` levantando todo el contexto de Spring (incluyendo la seguridad real). Flujo completo: crear bici → alquilar → finalizar → ver historial → intentar finalizar dos veces |

---

## Supuestos que tomé

El enunciado pide documentar los supuestos cuando haya ambigüedad, así que
estos son los míos:

1. **La hora de inicio y de devolución la asigna el servidor**, no el cliente.
   Lo hice así para evitar que un cliente manipule las horas y obtenga
   descuentos. Uso `LocalDateTime.now(clock)`, donde `clock` es inyectable
   (así puedo congelarlo en los tests).

2. **El tiempo real lo mido en milisegundos**: cualquier duración mayor que
   cero implica cobrar al menos 1 hora. El enunciado no especifica precisión,
   y me pareció más razonable que un uso de "1 segundo" cueste 1 hora a que
   cueste $0.

3. **No hay descuento por devolución anticipada**: si el cliente declara 3h
   pero devuelve a la 1h, solo le cobro 1h. El enunciado dice "costo base
   sobre el tiempo real de uso", así que entendí que solo se paga lo que se
   usa, sin penalizar tampoco por devolver antes.

4. **Concurrencia con bloqueo optimista**: agregué `@Version` a `Bicicleta`.
   Si dos transacciones intentan alquilar la misma bici simultáneamente, solo
   una gana — la otra recibe HTTP 409.

5. **EN_MANTENIMIENTO → DISPONIBLE manualmente**: el enunciado no pide un
   endpoint para esta transición, así que no lo creé. Pero sí permití
   registrar bicicletas con estado explícito distinto a DISPONIBLE, porque
   BIC-004 del seed nace en mantenimiento.

6. **Seguridad básica = API Key**: implementada con un `OncePerRequestFilter`
   sobre Spring Security. Cubre todo `/api/**`. Dejé abierto Swagger UI y la
   consola H2 para que el revisor pueda probar fácil. En producción la
   API Key debería venir de variable de entorno (`API_KEY`).

7. **Códigos de bicicleta validados con regex `^[A-Z0-9-]+$`**: solo
   mayúsculas, dígitos y guiones (ej. `BIC-001`). Evita inconsistencias por
   capitalización.

---

## Despliegue en Azure

La app está desplegada en **Azure Container Apps**:

> **https://prueba-tecnica-ceiba.graydune-89367257.centralus.azurecontainerapps.io**
>
> Swagger UI: `/swagger-ui/index.html`. Los endpoints `/api/**` requieren el
> header `X-API-KEY` (la clave es un secret del Container App).

### Archivos relacionados

| Archivo | Para qué |
|---|---|
| `Dockerfile` | Imagen multi-stage con JRE 21 alpine, usuario no-root, ~80MB |
| `.dockerignore` | Excluye `target/`, `.git/`, etc., del contexto de build |
| `.github/workflows/ci.yml` | Corre tests en cada push y PR |
| `.github/workflows/azure-deploy.yml` | Build de imagen Docker + push a GHCR |
| `deploy.ps1` | Script local que actualiza el Container App con la imagen recién publicada |
| `DEPLOYMENT.md` | Guía completa paso a paso para reproducir el setup |

### Flujo

```
git push main ──> GitHub Actions (build imagen + push a GHCR)
                                          │
                                          ▼
                            .\deploy.ps1 desde mi máquina
                                          │
                                          ▼
                       Azure Container App pull + nueva revisión
```

### Correr el contenedor en local

```bash
docker build -t prueba-tecnica:local .
docker run -p 8080:8080 -e API_KEY=mi-clave-segura prueba-tecnica:local
```

La app queda en http://localhost:8080.

### Decisiones de despliegue

- **Azure Container Apps** en lugar de App Service: free tier real (180.000
  vCPU-segundos/mes), escalado a cero (no consume si nadie usa), más moderno.
- **GHCR (GitHub Container Registry)** en lugar de Azure Container Registry:
  gratis incluso para repos públicos, sin el costo extra de $5/mes de ACR.
- **Dockerfile multi-stage**: la imagen final usa solo JRE (no JDK) sobre
  alpine, resultando en ~80MB vs ~400MB de imágenes JDK estándar.
- **Último paso manual con `deploy.ps1`**: mi cuenta de Azure for Students
  está en el tenant universitario, donde no tengo permisos para registrar
  apps en Entra ID. Sin esos permisos no puedo crear Service Principals
  para autenticar GitHub Actions contra Azure (OIDC). El workflow construye
  y publica la imagen automáticamente; yo solo ejecuto `.\deploy.ps1` para
  que Azure tome la nueva versión. En un entorno con esos permisos
  configurados, sustituir el script por dos pasos más en el workflow es
  trivial — está documentado en `DEPLOYMENT.md`.

Para reproducir el setup paso a paso, ver [`DEPLOYMENT.md`](DEPLOYMENT.md).

---

## Estructura del proyecto

```
prueba-tecnica-java/
├── pom.xml
├── mvnw, mvnw.cmd                            # Maven Wrapper (sin instalar Maven)
├── Dockerfile                                # Build multi-stage para producción
├── .dockerignore
├── DEPLOYMENT.md                             # Guía paso a paso de despliegue
├── .github/
│   └── workflows/
│       ├── ci.yml                            # CI: build + tests en cada push
│       └── azure-deploy.yml                  # CD: deploy a Azure Container Apps
├── src/
│   ├── main/
│   │   ├── java/com/luisdavid/pruebatecnica/
│   │   │   ├── PruebaTecnicaJavaApplication.java
│   │   │   ├── config/
│   │   │   │   ├── DataSeeder.java           # Seed BIC-001..BIC-005
│   │   │   │   ├── OpenApiConfig.java        # Metadata Swagger
│   │   │   │   ├── TimeConfig.java           # Bean Clock
│   │   │   │   └── security/
│   │   │   │       ├── ApiKeyAuthenticationFilter.java
│   │   │   │       └── SecurityConfig.java
│   │   │   ├── domain/                       # Modelo de dominio
│   │   │   │   ├── Bicicleta.java
│   │   │   │   ├── Alquiler.java
│   │   │   │   ├── TipoBicicleta.java        # enum con tarifa por hora
│   │   │   │   ├── EstadoBicicleta.java
│   │   │   │   └── RedondeoHoras.java        # utility de redondeo (DRY)
│   │   │   ├── repository/                   # Spring Data JPA
│   │   │   │   ├── BicicletaRepository.java
│   │   │   │   └── AlquilerRepository.java
│   │   │   ├── service/                      # Lógica de negocio
│   │   │   │   ├── BicicletaService.java
│   │   │   │   ├── AlquilerService.java
│   │   │   │   ├── TarifaCalculator.java     # núcleo del cálculo monetario
│   │   │   │   └── CalculoTarifa.java        # record de resultado
│   │   │   ├── exception/                    # Excepciones de dominio + handler
│   │   │   │   ├── BicicletaNoEncontradaException.java
│   │   │   │   ├── BicicletaNoDisponibleException.java
│   │   │   │   ├── AlquilerNoEncontradoException.java
│   │   │   │   ├── AlquilerYaFinalizadoException.java
│   │   │   │   ├── CodigoBicicletaDuplicadoException.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   └── web/                          # Capa HTTP
│   │   │       ├── controller/
│   │   │       │   ├── BicicletaController.java
│   │   │       │   └── AlquilerController.java
│   │   │       ├── dto/                      # Records de petición y respuesta
│   │   │       └── mapper/                   # Entidad ↔ DTO
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/luisdavid/pruebatecnica/
│           ├── domain/RedondeoHorasTest.java
│           ├── service/
│           │   ├── TarifaCalculatorTest.java
│           │   ├── AlquilerServiceTest.java
│           │   └── BicicletaServiceTest.java
│           └── web/AlquilerFlowIntegrationTest.java
└── README.md
```

---

**Luis Miguel David Campo** — migueldavidcampo@gmail.com
