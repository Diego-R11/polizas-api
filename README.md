# API de Gestión de Pólizas de Arrendamiento

Prueba técnica – Desarrollador TI, Seguros Bolívar. Autor: Diego Rodríguez.

API REST en **Spring Boot 3 / Java 17** para gestionar pólizas de arrendamiento **individuales** y **colectivas**, sus riesgos, la renovación por IPC y la notificación al CORE transaccional.

## Requisitos

- Java 17 o superior
- Maven 3.9+ (o abrir el proyecto en IntelliJ IDEA, que trae Maven integrado)

## Cómo ejecutar

```bash
mvn spring-boot:run
```

La API queda en `http://localhost:8080`. Al arrancar se cargan 3 pólizas de ejemplo en una base H2 en memoria:

| id | tipo | estado | riesgos |
|----|------|--------|---------|
| 1 | INDIVIDUAL | ACTIVA | 1 |
| 2 | COLECTIVA | ACTIVA | 2 |
| 3 | INDIVIDUAL | CANCELADA | 1 (cancelado) |

## Cómo ejecutar los tests

```bash
mvn test
```

- `PolizaReglasNegocioTest`: pruebas unitarias de las reglas de negocio (sin Spring ni base de datos).
- `PolizaApiIntegrationTest`: pruebas de punta a punta de cada endpoint con MockMvc, incluyendo seguridad, validaciones y códigos de error.

El repositorio incluye un pipeline de **GitHub Actions** (`.github/workflows/build.yml`) que compila y ejecuta los tests en cada push.

## Seguridad

Todas las peticiones requieren el header:

```
x-api-key: 123456
```

Sin él, o con un valor distinto, la API responde `401 Unauthorized`. El valor se puede cambiar con la variable de entorno `API_KEY`.

## Endpoints

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/polizas?tipo=&estado=` | Lista pólizas. Filtros opcionales: `tipo` (INDIVIDUAL, COLECTIVA) y `estado` (ACTIVA, RENOVADA, CANCELADA). |
| GET | `/polizas/{id}` | Consulta una póliza. |
| GET | `/polizas/{id}/riesgos` | Lista los riesgos de una póliza. |
| POST | `/polizas/{id}/renovar` | Renueva: canon y prima + IPC, estado `RENOVADA`, nueva vigencia por el mismo periodo. |
| POST | `/polizas/{id}/cancelar` | Cancela la póliza y todos sus riesgos. |
| POST | `/polizas/{id}/riesgos` | Agrega un riesgo. Solo pólizas COLECTIVAS. |
| POST | `/riesgos/{id}/cancelar` | Cancela un riesgo de una póliza colectiva. |
| POST | `/core-mock/evento` | Mock del CORE: registra en logs el evento recibido. |

### Ejemplos con curl

```bash
# Listar pólizas colectivas activas
curl -H "x-api-key: 123456" "http://localhost:8080/polizas?tipo=COLECTIVA&estado=ACTIVA"

# Riesgos de la póliza 2
curl -H "x-api-key: 123456" http://localhost:8080/polizas/2/riesgos

# Renovar la póliza 1
curl -X POST -H "x-api-key: 123456" http://localhost:8080/polizas/1/renovar

# Agregar un riesgo a la póliza colectiva 2
curl -X POST -H "x-api-key: 123456" -H "Content-Type: application/json" \
  -d '{"direccionInmueble":"Calle 26 # 50-10 Apto 402, Bogotá","arrendatario":"Sofía León"}' \
  http://localhost:8080/polizas/2/riesgos

# Cancelar el riesgo 2
curl -X POST -H "x-api-key: 123456" http://localhost:8080/riesgos/2/cancelar

# Cancelar la póliza 2 (cancela todos sus riesgos)
curl -X POST -H "x-api-key: 123456" http://localhost:8080/polizas/2/cancelar

# Mock del CORE
curl -X POST -H "x-api-key: 123456" -H "Content-Type: application/json" \
  -d '{"evento":"ACTUALIZACION","polizaId":555}' http://localhost:8080/core-mock/evento
```

## Reglas de negocio implementadas

| Regla | Dónde vive | Respuesta si se viola |
|-------|-----------|----------------------|
| Una póliza individual solo puede tener 1 riesgo | `Poliza.agregarRiesgo` | 409 |
| No se puede renovar una póliza cancelada | `Poliza.renovar` | 409 |
| Cancelar una póliza cancela todos sus riesgos | `Poliza.cancelar` | — |
| Agregar riesgo exige que la póliza sea COLECTIVA (y no esté cancelada) | `PolizaService.agregarRiesgo` / `Poliza.agregarRiesgo` | 409 |
| Prima = canon mensual × meses de vigencia | `Poliza.calcularPrima` | — |
| Renovación: canon × (1 + IPC), mismo periodo de vigencia | `Poliza.renovar` | — |

Decisiones adicionales:

- **Cancelar un riesgo de póliza individual** responde 409 y pide cancelar la póliza: en una individual el único riesgo es la póliza misma.
- **El IPC es parametrizable** (`app.renovacion.ipc`, variable `IPC_RENOVACION`). Por defecto `0.0510` (5,10 %), valor de referencia que debe reemplazarse por el IPC oficial vigente.
- Los montos usan `BigDecimal` con 2 decimales para evitar errores de redondeo de `double`.

## Integración con el CORE

Toda acción que modifica estado (renovar, cancelar póliza, agregar o cancelar riesgo) llama a `CoreGateway.notificarActualizacion`, que envía `{"evento":"ACTUALIZACION","polizaId":N}` al endpoint `/core-mock/evento`. El mock lo registra en el log:

```
[CORE-MOCK] Evento recibido: evento=ACTUALIZACION, polizaId=1
```

`CoreGateway` es un **puerto** (arquitectura hexagonal): para conectar el servicio real de la capa media en WebLogic solo se cambia la URL (`CORE_URL`) o la implementación del adapter, sin tocar la lógica de negocio. Si el CORE falla, el error se registra en el log y la operación del usuario no se revierte; en producción esto se reforzaría con un patrón Outbox y reintentos (ver documento de diseño).

## Estructura del proyecto

```
src/main/java/com/segurosbolivar/polizas
├── controller/    Capa HTTP: PolizaController, RiesgoController, CoreMockController
├── service/       Casos de uso: PolizaService, RiesgoService
├── repository/    Acceso a datos con Spring Data JPA
├── domain/        Entidades Poliza y Riesgo, con sus reglas de negocio
├── dto/           Objetos de entrada y salida de la API
├── integration/   Puerto CoreGateway y adapter HTTP hacia el CORE
├── config/        Filtro de api key y datos iniciales
└── exception/     Excepciones de negocio y manejador global de errores
```

## Formato de errores

```json
{
  "status": 409,
  "error": "Conflict",
  "mensaje": "No se puede renovar una póliza cancelada",
  "path": "/polizas/3/renovar",
  "timestamp": "2026-10-15T10:30:00-05:00"
}
```

| Código | Cuándo |
|--------|--------|
| 400 | Datos inválidos o filtro con valor desconocido |
| 401 | Falta el header `x-api-key` o es incorrecto |
| 404 | La póliza o el riesgo no existe |
| 409 | Se viola una regla de negocio |
