# API de Alquiler de Bicicletas Urbanas

Esta es mi solucion a la prueba tecnica para el rol de Practicante Java. Es
una API REST en Spring Boot que registra bicicletas, controla disponibilidad
y calcula cuanto cobrar por cada alquiler (incluyendo multa si el cliente
devuelve tarde).

---

## Probarla sin instalar nada

La app esta desplegada en Azure, asi que se puede probar directamente:

| | |
|---|---|
| **URL** | https://prueba-tecnica-ceiba.graydune-89367257.centralus.azurecontainerapps.io |
| **Swagger UI** | [/swagger-ui/index.html](https://prueba-tecnica-ceiba.graydune-89367257.centralus.azurecontainerapps.io/swagger-ui/index.html) |
| **API Key** | `jSVF2NqpgpKwrfAE5IDOmdKz3lJX` (se envia en el header `X-API-KEY`) |

Si entras a la URL base te redirige a Swagger UI. Ahi mismo hay un boton
"Authorize" donde pegas la API Key una vez y todo queda listo para probar.

> La app esta configurada para escalar a cero cuando nadie la usa (para no
> consumir el free tier). La primera peticion puede tardar 5-10 segundos
> mientras el contenedor arranca; las siguientes ya van rapido.

---

## Que hace

- Registra bicicletas con codigo unico, tipo y estado.
- Inicia alquileres de bicicletas disponibles.
- Finaliza alquileres y calcula el costo (mas la multa si hubo retraso).
- Lista bicicletas disponibles, con filtro opcional por tipo.
- Muestra el historial de alquileres de una bicicleta.

Cubri los cinco requerimientos funcionales (RF-01 a RF-05) y las cinco reglas
de negocio (RN-01 a RN-05) del enunciado.

---

## Como esta organizado

Lo arme en capas, que es como aprendi Spring Boot:

```
web         (controllers, DTOs, mappers, handler de errores)
service     (logica del negocio: calculo de tarifa, servicios)
repository  (Spring Data JPA)
domain      (entidades, enums, utilitarios de calculo)
```

La idea fue que cada capa solo conozca a la de abajo. Por ejemplo, el
controller no sabe de JPA, y el dominio no sabe de Spring. Esto me ayudo
sobre todo para los tests: el `TarifaCalculator` (donde esta toda la logica
del costo y la multa) no depende de nada, asi que pude testearlo con muchos
casos sin tener que levantar la app.

---

## Algunas cosas que decidi

Las pongo aqui porque seguramente se preguntan en la entrevista:

**Saque el calculo del costo y la multa a una clase aparte (`TarifaCalculator`).**
Es la parte mas importante del proyecto (toca la plata del cliente), asi que
quise tenerla aislada y bien probada. No depende de Spring ni de la base de
datos: recibe datos, devuelve datos. Eso hizo los tests rapidisimos.

**Inyecte un `Clock` en lugar de usar `LocalDateTime.now()` directo.**
Si usaba `now()` los tests dependian del reloj real y nunca iban a ser
deterministas. Con un `Clock` puedo congelar el tiempo en los tests y
verificar que los calculos dan exactamente lo que deben.

**Las tarifas las puse dentro del enum `TipoBicicleta`.**
Cada tipo "sabe" su tarifa, en lugar de tener un mapa o una tabla. El
enunciado las da fijas, asi que esto me parecio lo mas directo. Si mas
adelante cambian dinamicamente, las paso a una tabla.

**Para los DTOs use `record` de Java en vez de clases con Lombok.**
Los records vienen en el lenguaje, son inmutables y se escriben en una
linea. Para los datos que entran y salen por la API, no necesitaba nada
mas.

**Separe los DTOs de las entidades JPA.**
No queria exponer el `id` interno ni el campo `version` (que uso para
controlar concurrencia) en el JSON de respuesta. Tambien asi puedo cambiar
el modelo sin romper el contrato de la API.

**Las excepciones describen el negocio, no HTTP.**
Por ejemplo `BicicletaNoDisponibleException` no sabe nada de codigos HTTP.
Hay una clase aparte (`GlobalExceptionHandler`) que las traduce a la
respuesta HTTP correspondiente. Asi no tengo `try/catch` repartidos en cada
controller.

**`@Version` en la entidad `Bicicleta` para concurrencia.**
Pense en el caso de que dos personas intenten alquilar la misma bici al
mismo tiempo. Con `@Version`, JPA detecta el conflicto y solo una gana; la
otra recibe un 409.

**Use `BigDecimal` para todo lo monetario.**
Con `double` aparecen errores raros del tipo `0.1 + 0.2 = 0.30000000004`,
y eso no se puede permitir cuando hablamos de plata.

**Seguridad basica con un API Key en el header.**
El enunciado pide "seguridad basica", y para una API interna esto me parecio
suficiente. JWT u OAuth ya se sentia como demasiado para esta prueba. El
filtro de Spring Security valida el header `X-API-KEY`.

**Use `PATCH` para finalizar el alquiler, no `POST`.**
Como finalizar es modificar un alquiler existente (no crear uno nuevo),
`PATCH` me parecio mas correcto que `POST /alquileres/{id}/finalizar`.

**Versione la API en la URL (`/api/v1/...`).**
Por si en algun momento toca cambiar el contrato sin romper clientes
viejos.

---

## Stack

- **Java 21** (LTS).
- **Spring Boot 3.5.14**.
- **Maven** (inclui el wrapper, asi que no toca instalarlo).
- **H2** como base de datos en memoria — para que el revisor no tenga que
  instalar nada externo.
- **Spring Security** para el filtro de API Key.
- **Bean Validation** para validar los datos de entrada.
- **SpringDoc OpenAPI** para Swagger UI (lo agregue a mano al pom).
- **Lombok** para no escribir tantos getters/setters, pero no en las
  entidades JPA (ahi lo evite porque genera `equals`/`hashCode` que rompen
  el contrato cuando el id se asigna despues).
- **JUnit 5 + Mockito + AssertJ** para los tests.

Elegi Maven sobre Gradle porque el `pom.xml` me parecia mas facil de leer
para alguien revisando por primera vez. Y H2 sobre Postgres para que el
revisor pueda correr todo con un solo comando.

---

## Como correrlo en local

Lo unico que se necesita es **JDK 21**. Maven ya viene incluido con el
wrapper.

```bash
git clone https://github.com/LUISDACA/prueba-tecnica-ceiba.git
cd prueba-tecnica-ceiba

# Tests
./mvnw test                  # Linux/Mac
mvnw.cmd test                # Windows

# Arrancar la app
./mvnw spring-boot:run       # Linux/Mac
mvnw.cmd spring-boot:run     # Windows
```

Se levanta en http://localhost:8080 en unos 4 segundos.

Una vez arriba:

- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs
- **Consola H2**: http://localhost:8080/h2-console
  (JDBC URL: `jdbc:h2:mem:alquileresdb`, usuario `sa`, sin contrasena)

---

## Configuracion

Esta en `src/main/resources/application.properties`. Las cosas importantes
se pueden cambiar por variable de entorno:

| Propiedad | Default | Variable |
|---|---|---|
| `app.security.api-key` | `dev-api-key-cambiar-en-produccion` | `API_KEY` |
| `server.port` | 8080 | `PORT` |

En produccion la API key viene de la variable de entorno. La default solo
sirve para que el revisor pueda correr en local sin configurar nada.

---

## Probar la API en produccion

Estos curl van directo contra la app desplegada en Azure.

```bash
URL="https://prueba-tecnica-ceiba.graydune-89367257.centralus.azurecontainerapps.io"
KEY="jSVF2NqpgpKwrfAE5IDOmdKz3lJX"
```

**Listar las 5 bicicletas del seed**

```bash
curl -H "X-API-KEY: $KEY" $URL/api/v1/bicicletas
```

**Solo las URBANA disponibles**

```bash
curl -H "X-API-KEY: $KEY" "$URL/api/v1/bicicletas/disponibles?tipo=URBANA"
```

**Iniciar un alquiler**

```bash
curl -X POST -H "X-API-KEY: $KEY" -H "Content-Type: application/json" \
     -d '{"codigoBicicleta":"BIC-002","nombreCliente":"Evaluador","duracionEstimadaHoras":2}' \
     $URL/api/v1/alquileres
```

**Finalizarlo (reemplazar `{id}` por el id que devolvio el paso anterior)**

```bash
curl -X PATCH -H "X-API-KEY: $KEY" $URL/api/v1/alquileres/{id}/finalizar
```

**Ver el historial de una bicicleta**

```bash
curl -H "X-API-KEY: $KEY" $URL/api/v1/bicicletas/BIC-002/historial
```

**Sin la API key tiene que dar 401**

```bash
curl -i $URL/api/v1/bicicletas
```

---

## Endpoints

Todos los `/api/**` necesitan el header `X-API-KEY`.

### Bicicletas

#### `POST /api/v1/bicicletas` — crear

```bash
curl -X POST http://localhost:8080/api/v1/bicicletas \
  -H "X-API-KEY: dev-api-key-cambiar-en-produccion" \
  -H "Content-Type: application/json" \
  -d '{"codigo":"BIC-100","tipo":"ELECTRICA"}'
```

Respuesta `201`:

```json
{
  "codigo": "BIC-100",
  "tipo": "ELECTRICA",
  "estado": "DISPONIBLE",
  "tarifaPorHora": 7500
}
```

#### `GET /api/v1/bicicletas` — listar todas

#### `GET /api/v1/bicicletas/disponibles?tipo=URBANA` — disponibles, opcionalmente filtradas

#### `GET /api/v1/bicicletas/{codigo}` — una sola

#### `GET /api/v1/bicicletas/{codigo}/historial` — historial de alquileres

### Alquileres

#### `POST /api/v1/alquileres` — iniciar

```bash
curl -X POST http://localhost:8080/api/v1/alquileres \
  -H "X-API-KEY: dev-api-key-cambiar-en-produccion" \
  -H "Content-Type: application/json" \
  -d '{
    "codigoBicicleta": "BIC-002",
    "nombreCliente": "Juan Perez",
    "duracionEstimadaHoras": 2
  }'
```

#### `PATCH /api/v1/alquileres/{id}/finalizar` — finalizar y calcular costo

Reproduciendo el ejemplo del enunciado (MONTANA, 2h estimadas, 3h 20min reales):

```json
{
  "id": 1,
  "codigoBicicleta": "BIC-002",
  "tipoBicicleta": "MONTANA",
  "duracionEstimadaHoras": 2,
  "duracionRealHoras": 4,
  "costoBase": 20000.00,
  "multa": 5000.00,
  "costoTotal": 25000.00,
  "tuvoMulta": true,
  "finalizado": true
}
```

#### `GET /api/v1/alquileres/{id}` — detalle

### Datos cargados al arrancar (seed)

| Codigo | Tipo | Estado inicial |
|---|---|---|
| BIC-001 | URBANA | DISPONIBLE |
| BIC-002 | MONTANA | DISPONIBLE |
| BIC-003 | ELECTRICA | DISPONIBLE |
| BIC-004 | MONTANA | EN_MANTENIMIENTO |
| BIC-005 | URBANA | DISPONIBLE |

---

## Reglas de negocio

**RN-01 — Tarifas por tipo**

| Tipo | Tarifa/hora |
|---|---|
| URBANA | $3.500 |
| MONTANA | $5.000 |
| ELECTRICA | $7.500 |

**RN-02 — Costo base**

`costoBase = horasReales (redondeadas al alza) × tarifa`

- 1h 10min → 2h
- 2h exactas → 2h
- 500 ms → 1h (cualquier uso mayor a 0 cuenta como al menos 1 hora)

**RN-03 — Multa por devolucion tardia**

`multa = 50% × tarifa × horasDeRetraso (redondeadas al alza)`

- A tiempo o antes → multa = 0.
- Si hay retraso, minimo se cobra 1 hora (un retraso de 30 segundos cuenta
  como 1 hora).
- Verifique el ejemplo del enunciado (MONTANA, 2h estimadas, 3h 20min
  reales): base 4h × $5.000 = $20.000, multa 2h × $2.500 = $5.000, total
  $25.000.

**RN-04 — No alquilar bicicletas no disponibles**

Si la bici no esta en estado DISPONIBLE, no se puede alquilar (responde
con 409).

**RN-05 — No finalizar dos veces ni un alquiler que no existe**

- Alquiler no existente → 404.
- Alquiler ya finalizado → 409.

---

## Manejo de errores

Todas las respuestas de error usan el mismo formato (lo da `ProblemDetail`
de Spring, que sigue la RFC 7807):

```json
{
  "type": "about:blank",
  "title": "Bicicleta no disponible",
  "status": 409,
  "detail": "La bicicleta 'BIC-004' no esta disponible para alquiler (estado actual: EN_MANTENIMIENTO)",
  "timestamp": "2026-05-22T22:50:30-05:00"
}
```

| Caso | HTTP |
|---|---|
| Bicicleta o alquiler no encontrado | 404 |
| Ruta no existe | 404 |
| Bicicleta no disponible | 409 |
| Alquiler ya finalizado | 409 |
| Codigo de bicicleta duplicado | 409 |
| Datos del request invalidos | 400 |
| JSON malformado | 400 |
| Cualquier error inesperado | 500 (sin filtrar stack traces) |

---

## Tests

```bash
./mvnw test
```

Tengo **47 tests** entre unitarios y de integracion. La idea fue cubrir
bien lo mas sensible (el calculo) y al menos un test que recorra el flujo
completo:

- `TarifaCalculatorTest` — todos los casos del calculo del costo y la
  multa, incluyendo el ejemplo del enunciado.
- `RedondeoHorasTest` — tests parametrizados con valores limite.
- `AlquilerServiceTest` — flujo de iniciar/finalizar con el reloj
  congelado.
- `BicicletaServiceTest` — alta, busqueda, listados, duplicados.
- `AlquilerFlowIntegrationTest` — flujo entero con MockMvc levantando
  todo el contexto, incluida la seguridad real.

---

## Supuestos que tome

El enunciado dice que documente los supuestos cuando haya ambiguedad, asi
que estos son los mios:

1. **La hora de inicio y de fin la pone el servidor**, no el cliente. Asi
   nadie puede mandar una hora falsa para pagar menos. En los tests uso un
   `Clock` que se puede congelar.

2. **El tiempo se mide en milisegundos**, pero cualquier uso mayor a 0
   cuenta como al menos 1 hora. Me parecio mas razonable que un uso de "1
   segundo" cueste $3.500 a que cueste $0.

3. **No hay descuento por devolver antes**. Si el cliente dice 3h pero
   devuelve a la 1h, le cobro 1h. El enunciado habla de "tiempo real de
   uso", asi que entendi que solo se paga lo que se usa, pero tampoco hay
   bonus.

4. **Concurrencia**: use bloqueo optimista en la bici (`@Version`). Si dos
   personas la intentan alquilar al mismo tiempo, solo una gana — la otra
   recibe un 409.

5. **No hice un endpoint para pasar de EN_MANTENIMIENTO a DISPONIBLE**
   porque el enunciado no lo pide. Pero si permiti registrar bicicletas con
   estado distinto a DISPONIBLE, porque BIC-004 del seed nace en
   mantenimiento.

6. **Seguridad basica = API Key**. La validacion pasa por un filtro de
   Spring Security. Swagger UI y la consola H2 quedan abiertas para que el
   revisor pueda probar facil.

7. **El codigo de bicicleta solo admite mayusculas, digitos y guiones**
   (ej. `BIC-001`). Lo valide con regex (`^[A-Z0-9-]+$`).

---

## Despliegue en Azure

La app esta corriendo en **Azure Container Apps**, en la URL del inicio
del README.

### Flujo

```
git push main ──> GitHub Actions construye la imagen Docker y la sube a GHCR
                                          │
                                          ▼
                          .\deploy.ps1 desde mi maquina
                                          │
                                          ▼
                        Azure Container App toma la nueva imagen
```

### Por que hay un ultimo paso manual

Mi cuenta de Azure es Azure for Students dentro del tenant de la
universidad, y ahi no tengo permisos para registrar aplicaciones en
Entra ID. Sin esos permisos no puedo crear el Service Principal que se
necesita para que GitHub Actions se autentique contra Azure con OIDC.

Como solucion, deje un script `deploy.ps1` que ejecuta el ultimo paso
desde mi maquina con un solo comando. El flujo termina siendo: hago push
y, cuando el workflow termina de subir la imagen, ejecuto `.\deploy.ps1`.

Si en algun momento me dan los permisos, agregar el deploy automatico al
workflow son 5 lineas mas — lo deje explicado en `DEPLOYMENT.md`.

### Decisiones del despliegue

- **Container Apps** en vez de App Service porque tiene free tier real
  (180.000 vCPU-segundos al mes gratis) y porque escala a cero cuando no
  hay trafico (asi no consume nada estando inactivo).
- **GHCR** (GitHub Container Registry) en vez de Azure Container Registry
  porque es gratis y ya esta integrado con GitHub Actions.
- **Dockerfile multi-stage** con JRE 21 alpine y usuario no-root. La
  imagen final pesa ~80MB.

Mas detalles para reproducirlo en [`DEPLOYMENT.md`](DEPLOYMENT.md).

---

## Estructura del proyecto

```
prueba-tecnica-ceiba/
├── pom.xml
├── mvnw, mvnw.cmd                # Maven Wrapper
├── Dockerfile
├── .dockerignore
├── deploy.ps1                    # Script para actualizar Azure tras un push
├── DEPLOYMENT.md                 # Como esta montado Azure paso a paso
├── .github/workflows/
│   ├── ci.yml                    # Tests en cada push
│   └── azure-deploy.yml          # Build de imagen y push a GHCR
└── src/
    ├── main/
    │   ├── java/com/luisdavid/pruebatecnica/
    │   │   ├── PruebaTecnicaJavaApplication.java
    │   │   ├── config/
    │   │   │   ├── DataSeeder.java
    │   │   │   ├── OpenApiConfig.java
    │   │   │   ├── TimeConfig.java
    │   │   │   └── security/
    │   │   │       ├── ApiKeyAuthenticationFilter.java
    │   │   │       └── SecurityConfig.java
    │   │   ├── domain/
    │   │   │   ├── Bicicleta.java
    │   │   │   ├── Alquiler.java
    │   │   │   ├── TipoBicicleta.java
    │   │   │   ├── EstadoBicicleta.java
    │   │   │   └── RedondeoHoras.java
    │   │   ├── repository/
    │   │   ├── service/
    │   │   │   ├── BicicletaService.java
    │   │   │   ├── AlquilerService.java
    │   │   │   ├── TarifaCalculator.java
    │   │   │   └── CalculoTarifa.java
    │   │   ├── exception/        # Excepciones de dominio + handler global
    │   │   └── web/
    │   │       ├── controller/
    │   │       ├── dto/
    │   │       └── mapper/
    │   └── resources/
    │       └── application.properties
    └── test/
        └── java/.../
            ├── domain/RedondeoHorasTest.java
            ├── service/
            │   ├── TarifaCalculatorTest.java
            │   ├── AlquilerServiceTest.java
            │   └── BicicletaServiceTest.java
            └── web/AlquilerFlowIntegrationTest.java
```

---

**Luis Miguel David Campo**
[migueldavidcampo@gmail.com](mailto:migueldavidcampo@gmail.com) · [github.com/LUISDACA](https://github.com/LUISDACA)
