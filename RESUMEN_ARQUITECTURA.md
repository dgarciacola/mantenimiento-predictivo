# 📚 Resumen Arquitectura - Predictive Maintenance Backend

**Fecha:** 2026-10-07  
**Estado:** MVP Week 1 - Backend Development  
**Stack:** Spring Boot 4.1.1 + Java 21 + PostgreSQL + Redis

---

## 🗂️ Estructura de Carpetas

```
predictive-maintenance/
│
├── docker-compose.yml          ← 🐳 Infraestructura (PostgreSQL + Redis + pgAdmin)
│
└── backend/                     ← 🔧 Código Java/Spring Boot
    │
    ├── pom.xml                 ← 📦 Dependencias Maven
    │                              (Spring Boot, PostgreSQL, Redis, Swagger, Lombok)
    │
    ├── src/
    │   ├── main/
    │   │   ├── java/
    │   │   │   └── com/predictivemaintenance/
    │   │   │       │
    │   │   │       ├── PredictiveMaintenanceApplication.java
    │   │   │       │   └── Entry point (@SpringBootApplication)
    │   │   │       │
    │   │   │       ├── entity/
    │   │   │       │   └── SensorReading.java
    │   │   │       │       ├── @Entity → Mapea a tabla sensor_readings
    │   │   │       │       ├── Campos: id, sensorId, temperature, vibration, current, timestamp, status
    │   │   │       │       ├── Validaciones: @NotNull, @DecimalMin, @DecimalMax
    │   │   │       │       └── @Index en (sensor_id, timestamp DESC) para performance
    │   │   │       │
    │   │   │       ├── dto/
    │   │   │       │   ├── SensorReadingDTO.java
    │   │   │       │   │   ├── INPUT: Lo que recibe del cliente
    │   │   │       │   │   ├── Campos: sensorId, temperature, vibration, current
    │   │   │       │   │   ├── NO incluye: id, timestamp, status (server-generated)
    │   │   │       │   │   └── Validaciones en frontera API
    │   │   │       │   │
    │   │   │       │   └── SensorReadingResponseDTO.java
    │   │   │       │       ├── OUTPUT: Lo que retorna al cliente
    │   │   │       │       ├── Campos: id, sensorId, temp, vibration, current, timestamp, status
    │   │   │       │       ├── @JsonProperty mapea camelCase → snake_case
    │   │   │       │       └── @Builder para construcción fluida
    │   │   │       │
    │   │   │       ├── repository/
    │   │   │       │   └── SensorReadingRepository.java
    │   │   │       │       ├── @Repository interface extends JpaRepository<SensorReading, Long>
    │   │   │       │       ├── Métodos automáticos: save(), findAll(), delete(), etc
    │   │   │       │       ├── Derived queries:
    │   │   │       │       │   ├── findBySensorIdOrderByTimestampDesc(sensorId)
    │   │   │       │       │   ├── findLatestBySensorId(sensorId) → Optional
    │   │   │       │       │   ├── findByStatusOrderByTimestampDesc(status)
    │   │   │       │       │   └── countBySensorId(sensorId)
    │   │   │       │       └── @Query JPQL:
    │   │   │       │           └── findByTimeRange(sensorId, startTime, endTime)
    │   │   │       │
    │   │   │       ├── service/
    │   │   │       │   └── SensorReadingService.java
    │   │   │       │       ├── @Service → Lógica de negocio
    │   │   │       │       ├── @Transactional → ACID compliance
    │   │   │       │       ├── Métodos:
    │   │   │       │       │   ├── saveSensorReading(DTO)
    │   │   │       │       │   │   ├── Convierte DTO → Entity
    │   │   │       │       │   │   ├── Calcula status (NORMAL/WARNING/CRITICAL)
    │   │   │       │       │   │   └── Guarda en BD
    │   │   │       │       │   ├── getHistoryBySensorId(sensorId)
    │   │   │       │       │   ├── getReadingsByTimeRange(sensorId, start, end)
    │   │   │       │       │   ├── getCriticalAlerts()
    │   │   │       │       │   └── getLatestReading(sensorId) → Optional
    │   │   │       │       └── calculateStatus() - Lógica de umbrales
    │   │   │       │           ├── Vibración ≥ 8.0 → CRITICAL
    │   │   │       │           ├── Temperatura ≥ 90°C → CRITICAL
    │   │   │       │           ├── Vibración ≥ 5.0 → WARNING
    │   │   │       │           ├── Temperatura ≥ 75°C → WARNING
    │   │   │       │           └── Resto → NORMAL
    │   │   │       │
    │   │   │       └── controller/
    │   │   │           └── SensorController.java
    │   │   │               ├── @RestController → REST API endpoints
    │   │   │               ├── @RequestMapping("/api/sensors")
    │   │   │               └── Endpoints (6):
    │   │   │                   ├── POST /api/sensors/data
    │   │   │                   │   └── Guardar lectura → 201 CREATED
    │   │   │                   ├── GET /api/sensors/{sensorId}/history
    │   │   │                   │   └── Historial completo
    │   │   │                   ├── GET /api/sensors/{sensorId}/history/time-range?start=...&end=...
    │   │   │                   │   └── Rango temporal
    │   │   │                   ├── GET /api/sensors/list
    │   │   │                   │   └── Listar sensores únicos
    │   │   │                   ├── GET /api/sensors/health
    │   │   │                   │   └── Health check
    │   │   │                   └── GET /api/sensors/critical
    │   │   │                       └── Alertas críticas
    │   │   │
    │   │   └── resources/
    │   │       └── application.yml ⚙️ Configuración Spring Boot
    │   │           ├── Server: puerto 8080, Tomcat config
    │   │           ├── DataSource: PostgreSQL localhost:5432
    │   │           │   └── HikariCP pool: 20 conexiones
    │   │           ├── JPA: Hibernate, ddl-auto: update (dev)
    │   │           ├── Redis: localhost:6379
    │   │           ├── Logging: DEBUG para app, INFO para Spring
    │   │           └── Swagger: /swagger-ui.html, /v3/api-docs
    │   │
    │   └── test/
    │       └── (Tests aquí - próxima fase)
    │
    └── Dockerfile               ← Multi-stage Docker image
        ├── Stage 1: maven:3.9 → Compila con mvn clean package
        └── Stage 2: eclipse-temurin:17-jre-alpine → Corre JAR

```

---

## 🏗️ Flujo de Arquitectura - Capas

```
┌─────────────────────────────────────────────────────────────┐
│                    CLIENTE (IoT Sensor)                      │
│              POST /api/sensors/data (JSON)                   │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTP POST
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                   CONTROLLER LAYER                           │
│              SensorController.java                           │
│  ✓ @Valid SensorReadingDTO                                  │
│  ✓ HTTP status (201, 400, 500)                              │
│  ✓ Manejo de errores                                        │
│  ✓ Conversión Entity → ResponseDTO                          │
└────────────────────────┬────────────────────────────────────┘
                         │ saveSensorReading(DTO)
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    SERVICE LAYER                             │
│             SensorReadingService.java                        │
│  ✓ Lógica de negocio                                        │
│  ✓ Conversión DTO → Entity                                  │
│  ✓ Cálculo de status (NORMAL/WARNING/CRITICAL)             │
│  ✓ Transacciones (@Transactional)                           │
│  ✓ Validaciones adicionales                                 │
└────────────────────────┬────────────────────────────────────┘
                         │ repository.save(Entity)
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                  REPOSITORY LAYER                            │
│          SensorReadingRepository.java                        │
│  ✓ Spring Data JPA                                          │
│  ✓ Derived queries + @Query JPQL                            │
│  ✓ Generación automática de SQL                             │
│  ✓ Parámetros nombrados (@Param)                            │
└────────────────────────┬────────────────────────────────────┘
                         │ INSERT + SELECT
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                   DATABASE LAYER                             │
│              PostgreSQL (5432)                               │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ TABLE: sensor_readings                              │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │ id (BIGINT, PK, IDENTITY)                           │   │
│  │ sensor_id (VARCHAR 100)          ← @Index           │   │
│  │ temperature (DOUBLE, 0-100°C)    ← Constraint       │   │
│  │ vibration (DOUBLE, 0-10 mm/s)    ← Constraint       │   │
│  │ current (DOUBLE, 0-50 A)         ← Constraint       │   │
│  │ timestamp (TIMESTAMP, CreationTS)                   │   │
│  │ status (VARCHAR 50: NORMAL/WARNING/CRITICAL)        │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │ INDEX: idx_sensor_timestamp (sensor_id, timestamp)  │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                              │
│  CACHE:                                                     │
│  ├─ Redis (6379) → Últimas 100 lecturas                    │
│  └─ TTL configurable (evita stale data)                     │
└─────────────────────────────────────────────────────────────┘
```

---

## 📡 Endpoints API - Resumen

| Método | Endpoint | Input | Output | Status |
|--------|----------|-------|--------|--------|
| **POST** | `/api/sensors/data` | SensorReadingDTO | ResponseDTO | 201 |
| **GET** | `/api/sensors/{sensorId}/history` | - | List[ResponseDTO] | 200 |
| **GET** | `/api/sensors/{sensorId}/history/time-range` | ?start &end | List[ResponseDTO] | 200 |
| **GET** | `/api/sensors/list` | - | { totalSensors, sensorIds[] } | 200 |
| **GET** | `/api/sensors/health` | - | { status: "UP" } | 200 |
| **GET** | `/api/sensors/critical` | - | { alerts[] } | 200 |

---

## 🐳 Docker - Infraestructura

```yaml
docker-compose.yml contiene:

┌─────────────────────────────────────────────────────────────┐
│ Service: postgres                                           │
│ Image: postgres:15-alpine                                  │
│ Port: 5432                                                 │
│ Credentials:                                               │
│   - User: postgres                                         │
│   - Pass: postgres123                                      │
│   - DB: predictive_maintenance_db                          │
│ Volume: postgres_data (persistente)                        │
│ Healthcheck: pg_isready                                    │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ Service: redis                                              │
│ Image: redis:7-alpine                                      │
│ Port: 6379                                                 │
│ Config:                                                    │
│   - maxmemory: 256MB                                       │
│   - maxmemory-policy: allkeys-lru                          │
│   - appendonly: yes (persistencia)                         │
│ Volume: redis_data (AOF file)                              │
│ Healthcheck: redis-cli PING                                │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ Service: pgadmin                                            │
│ Image: dpage/pgadmin4                                      │
│ Port: 5050 (web UI)                                        │
│ URL: http://localhost:5050                                 │
│ Credentials:                                               │
│   - User: admin@predictive.local                           │
│   - Pass: admin123                                         │
│ Depends on: PostgreSQL (healthcheck)                       │
└─────────────────────────────────────────────────────────────┘

Network: predictive-network (bridge)
  └─ Conecta postgres ↔ redis ↔ pgadmin
```

---

## 🚀 Flujo Completo - De Sensor a BD

```
1. SENSOR ENVÍA DATO
   ├─ HTTP POST http://localhost:8080/api/sensors/data
   ├─ JSON body: { sensorId: "motor-1", temperature: 45.3, vibration: 2.1, current: 15.2 }
   └─ Content-Type: application/json

2. CONTROLLER RECIBE
   ├─ @PostMapping("/data")
   ├─ Valida @Valid SensorReadingDTO
   │  └─ Si falla → 400 BAD REQUEST (automático)
   ├─ Si OK → delega a Service
   └─ readyTime: ~1ms

3. SERVICE PROCESA
   ├─ @Transactional comienza
   ├─ Convierte DTO → Entity
   ├─ Calcula status
   │  └─ vibration=2.1, temp=45.3 → NORMAL
   ├─ Inyecta logger y timestamp
   └─ Llama repository.save(entity)

4. REPOSITORY GUARDA
   ├─ JpaRepository.save() intercepta
   ├─ EntityManager genera INSERT SQL
   ├─ Parámetros inyectados (SQL injection prevention)
   ├─ Envía a PostgreSQL
   └─ ProcessTime: ~45ms

5. DATABASE INSERTA
   ├─ INSERT INTO sensor_readings (sensor_id, temperature, vibration, current, timestamp, status)
   ├─ Valida constraints (CHECK temperature 0-100, etc)
   ├─ Genera id (IDENTITY strategy)
   ├─ Genera timestamp (CreationTimestamp)
   ├─ Indexa en (sensor_id, timestamp DESC)
   └─ Retorna Entity con id y timestamp

6. REDIS CACHEA (opcional)
   ├─ Service puede cachear últimas 100 lecturas
   ├─ Key: sensor:motor-1
   ├─ TTL: configurable (ej: 1 hora)
   └─ Próximas lecturas: O(1) en lugar de O(log n) en BD

7. SERVICE RETORNA
   ├─ @Transactional commit
   ├─ Retorna Entity completo (con id y timestamp)
   └─ ProcessTime: ~60ms total

8. CONTROLLER RETORNA HTTP
   ├─ Convierte Entity → ResponseDTO
   ├─ JSON: { id: 42, sensor_id: "motor-1", temperature: 45.3, vibration: 2.1, current: 15.2, timestamp: "2026-10-06T14:33:00", status: "NORMAL" }
   ├─ HTTP 201 CREATED
   └─ ReturnTime: ~65ms

9. SENSOR RECIBE RESPUESTA
   └─ ID 42 guardado en BD ✓
```

---

## 📊 Validación en Capas (Defense in Depth)

```
CAPA 1: DTO Validation
├─ @NotNull en SensorReadingDTO
├─ @DecimalMin("0"), @DecimalMax("100") en temperature
└─ Spring valida ANTES de entrar al Controller

CAPA 2: Service Validation
├─ SensorReadingService.isValidSensorReading()
├─ Lógica de negocio adicional
└─ Calcula status basado en umbrales

CAPA 3: Entity Constraints
├─ @Column constraints en SensorReading
├─ CHECK constraints en BD
└─ La BD rechaza si no cumple

CAPA 4: Database Constraints
├─ CHECK temperature >= 0 AND temperature <= 100
├─ UNIQUE indexes
├─ Foreign keys (si hay relaciones)
└─ ACID transactions
```

---

## 🎯 Resumen de Tecnologías

| Componente | Tecnología | Versión | Puerto | Propósito |
|-----------|-----------|---------|--------|-----------|
| **Backend** | Spring Boot | 4.1.1 | 8080 | REST API |
| **JDK** | Eclipse Temurin | 21 LTS | - | Runtime Java |
| **ORM** | Hibernate/JPA | Spring Data | - | Mapeo Entity→BD |
| **BD** | PostgreSQL | 15-alpine | 5432 | Datos persistentes |
| **Cache** | Redis | 7-alpine | 6379 | Cache en memoria |
| **Pool** | HikariCP | Included | - | Conexiones BD |
| **Logging** | SLF4J/Logback | Included | - | Auditoría |
| **Docs** | Swagger/OpenAPI | springdoc-openapi | 8080/swagger-ui.html | Documentación |
| **Build** | Maven | 3.9 | - | Compilación |
| **Container** | Docker | Latest | - | Aislamiento |

---

## ⚙️ Comandos Esenciales

```bash
# SETUP INICIAL
cd ~/Desktop/predictive-maintenance
docker-compose up -d
mvn clean install
mvn spring-boot:run

# VERIFICACIÓN
curl http://localhost:8080/api/sensors/health
open http://localhost:8080/swagger-ui.html

# TESTING
curl -X POST http://localhost:8080/api/sensors/data \
  -H "Content-Type: application/json" \
  -d '{"sensorId":"motor-1","temperature":45.3,"vibration":2.1,"current":15.2}'

# LOGS
docker-compose logs -f postgres
docker-compose logs -f redis

# CLEANUP
docker-compose down -v
```

---

## ✅ Checklist - Lo que Hemos Creado

```
FASE 1: ENTITY + REPOSITORY + DTO ✓
├─ SensorReading.java (Entity con JPA mapping)
├─ SensorReadingDTO.java (Input DTO)
├─ SensorReadingResponseDTO.java (Output DTO)
└─ SensorReadingRepository.java (Data access)

FASE 2: SERVICE LAYER ✓
└─ SensorReadingService.java (Business logic, transactions)

FASE 3: CONTROLLER LAYER ✓
└─ SensorController.java (6 REST endpoints)

FASE 4: CONFIGURACIÓN ✓
├─ application.yml (BD, Redis, logging, swagger)
├─ pom.xml (dependencias Maven)
└─ docker-compose.yml (infra: PostgreSQL, Redis, pgAdmin)

FASE 5: ENTRY POINT ✓
└─ PredictiveMaintenanceApplication.java (@SpringBootApplication)

➡️ PRÓXIMO:
├─ GlobalExceptionHandler (@ControllerAdvice)
├─ Tests (JUnit 5, Mockito)
├─ ML Integration (Week 3-4)
└─ Frontend (Week 5)
```

---

## 🔗 Relaciones Entre Clases

```
SensorReadingDTO (input)
        │
        ▼
   SensorController
        │
        ▼
   SensorReadingService
        │
        ├─→ calcula status
        ├─→ convierte a Entity
        └─→ SensorReadingRepository
                │
                ▼
            SensorReading (Entity)
                │
                ▼
            PostgreSQL (BD)
                │
                ▼
            Redis (cache)
        
        ↓ (respuesta)
        
   SensorReadingResponseDTO (output)
        │
        ▼
   JSON → Cliente
```

---

## 📚 Referencias Rápidas

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **pgAdmin:** http://localhost:5050 (admin@predictive.local / admin123)
- **Redis CLI:** `docker-compose exec redis redis-cli`
- **PostgreSQL:** `docker-compose exec postgres psql -U postgres`
- **Logs:** `docker-compose logs -f [servicio]`

---

**Creado:** 2026-10-07 03:42 UTC+02:00  
**Autor:** Claude Code Session  
**Proyecto:** Predictive Maintenance - MVP Week 1
