package com.predictivemaintenance.controller;

import com.predictivemaintenance.dto.SensorReadingDTO;
import com.predictivemaintenance.dto.SensorReadingResponseDTO;
import com.predictivemaintenance.entity.SensorReading;
import com.predictivemaintenance.service.SensorReadingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * SensorController - Capa de Presentación (REST API)
 *
 * ¿QUÉ ES UN CONTROLLER?
 * ======================
 * Es el PUNTO DE ENTRADA para las solicitudes HTTP.
 * Recibe peticiones del cliente, las delega al Service, y retorna respuestas HTTP.
 *
 * FLUJO HTTP:
 *
 * Cliente (navegador/app)
 *     ↓ HTTP POST
 * SensorController
 *     ↓ llama a
 * SensorReadingService
 *     ↓ llama a
 * SensorReadingRepository
 *     ↓ accede a
 * PostgreSQL Database
 *     ↑ retorna Entity
 * SensorReadingService
 *     ↑ retorna a
 * SensorController
 *     ↑ HTTP 201 CREATED
 * Cliente
 *
 * RESPONSABILIDADES DEL CONTROLLER:
 * ==================================
 * 1. ✅ Recibir peticiones HTTP (parámetros, body, headers)
 * 2. ✅ Validar entrada (@Valid annotations)
 * 3. ✅ Delegar lógica al Service
 * 4. ✅ Convertir Entity → DTO (proteger internals)
 * 5. ✅ Retornar HTTP status correcto (201, 200, 400, 500, etc)
 * 6. ✅ Manejo de errores y excepciones
 *
 * @RestController = @Controller + @ResponseBody
 * - @Controller: Marca como componente web
 * - @ResponseBody: Serializa retorno a JSON automáticamente
 *
 * @RequestMapping("/api/sensors")
 * - Prefijo de URL para todos los endpoints
 * - POST /api/sensors/data
 * - GET /api/sensors/{sensorId}/history
 * - etc
 */
@RestController
@RequestMapping("/api/sensors")
@Slf4j
@RequiredArgsConstructor
public class SensorController {

    // Inyección del Service (lógica de negocio)
    private final SensorReadingService sensorReadingService;

    // ═══════════════════════════════════════════════════════════════════
    // 1. POST /api/sensors/data - GUARDAR LECTURA
    // ═══════════════════════════════════════════════════════════════════

    /**
     * saveSensorData() - Endpoint para ingerir datos de sensores
     *
     * HTTP METHOD: POST
     * URL: /api/sensors/data
     * CONTENT-TYPE: application/json
     *
     * EJEMPLO DE PETICIÓN:
     * =====================
     * POST http://localhost:8080/api/sensors/data
     * Content-Type: application/json
     *
     * {
     *   "sensorId": "motor-line3-unit5",
     *   "temperature": 45.3,
     *   "vibration": 2.1,
     *   "current": 15.2
     * }
     *
     * EJEMPLO DE RESPUESTA (201 CREATED):
     * ====================================
     * {
     *   "status": "success",
     *   "message": "Sensor data received and validated",
     *   "sensorId": "motor-line3-unit5",
     *   "timestamp": "2026-10-06T14:33:00",
     *   "dataStatus": "NORMAL",
     *   "dataProcessingTime": "45ms"
     * }
     *
     * VALIDACIÓN:
     * ===========
     * @Valid en DTO dispara validaciones:
     * - @NotNull en temperature, vibration, current
     * - @DecimalMin("0") y @DecimalMax validaciones de rango
     * - Si falla → 400 BAD REQUEST (automático)
     *
     * HTTP STATUS:
     * =============
     * - 201 CREATED: Guardado exitoso
     * - 400 BAD REQUEST: Datos inválidos
     * - 500 INTERNAL SERVER ERROR: Error en servidor
     *
     * @param dto - Datos del sensor (temperature, vibration, current)
     * @return ResponseEntity con status 201 y datos guardados
     */
    @PostMapping("/data")
    public ResponseEntity<Map<String, Object>> saveSensorData(
            @Valid @RequestBody SensorReadingDTO dto) {

        log.info("📥 [POST /api/sensors/data] Recibiendo datos del sensor: {}", dto.getSensorId());

        long startTime = System.currentTimeMillis();

        // Delegar al Service (lógica de negocio)
        SensorReading saved = sensorReadingService.saveSensorReading(dto);

        // Calcular tiempo de procesamiento
        long processingTime = System.currentTimeMillis() - startTime;

        // Construir respuesta exitosa
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Sensor data received and validated");
        response.put("sensorId", saved.getSensorId());
        response.put("timestamp", saved.getTimestamp());
        response.put("dataStatus", saved.getStatus());
        response.put("dataProcessingTime", processingTime + "ms");

        log.info("✅ Lectura guardada exitosamente. Tiempo: {}ms", processingTime);

        // Retornar 201 CREATED (indicamos que se creó un recurso)
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ═══════════════════════════════════════════════════════════════════
    // 2. GET /api/sensors/{sensorId}/history - HISTORIAL
    // ═══════════════════════════════════════════════════════════════════

    /**
     * getSensorHistory() - Obtener historial de un sensor
     *
     * HTTP METHOD: GET
     * URL: /api/sensors/{sensorId}/history
     *
     * EJEMPLO:
     * ========
     * GET http://localhost:8080/api/sensors/motor-line3-unit5/history
     *
     * RESPUESTA (200 OK):
     * ===================
     * {
     *   "sensorId": "motor-line3-unit5",
     *   "totalReadings": 42,
     *   "readings": [
     *     {
     *       "id": 42,
     *       "sensorId": "motor-line3-unit5",
     *       "temperature": 45.3,
     *       "vibration": 2.1,
     *       "current": 15.2,
     *       "timestamp": "2026-10-06T14:33:00",
     *       "status": "NORMAL"
     *     },
     *     ...
     *   ]
     * }
     *
     * @PathVariable:
     * ===============
     * {sensorId} → Se extrae de la URL
     * Ejemplo: /api/sensors/motor-1/history
     *          sensorId = "motor-1"
     *
     * @param sensorId - ID del sensor desde la URL
     * @return ResponseEntity con historial completo
     */
    @GetMapping("/{sensorId}/history")
    public ResponseEntity<Map<String, Object>> getSensorHistory(
            @PathVariable String sensorId) {

        log.info("📖 [GET /api/sensors/{}/history] Obteniendo historial", sensorId);

        // Obtener historial del Service
        List<SensorReading> readings = sensorReadingService.getHistoryBySensorId(sensorId);

        // Convertir Entity → DTO (ocultamos internals)
        List<SensorReadingResponseDTO> dtos = readings.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());

        // Construir respuesta
        Map<String, Object> response = new HashMap<>();
        response.put("sensorId", sensorId);
        response.put("totalReadings", dtos.size());
        response.put("readings", dtos);

        log.info("✅ Historial obtenido: {} lecturas", dtos.size());

        return ResponseEntity.ok(response);
    }

    // ═══════════════════════════════════════════════════════════════════
    // 3. GET /api/sensors/{sensorId}/history/time-range - RANGO TEMPORAL
    // ═══════════════════════════════════════════════════════════════════

    /**
     * getReadingsByTimeRange() - Lecturas en rango temporal
     *
     * HTTP METHOD: GET
     * URL: /api/sensors/{sensorId}/history/time-range
     * QUERY PARAMS: startTime, endTime
     *
     * EJEMPLO:
     * ========
     * GET http://localhost:8080/api/sensors/motor-1/history/time-range?startTime=2026-10-06T00:00:00&endTime=2026-10-06T23:59:59
     *
     * @RequestParam:
     * ===============
     * ?startTime=2026-10-06T00:00:00
     * startTime = "2026-10-06T00:00:00"
     *
     * @DateTimeFormat:
     * =================
     * Convierte String → LocalDateTime automáticamente
     * Sin esto habría error 400
     *
     * @param sensorId - ID del sensor
     * @param startTime - Inicio del rango
     * @param endTime - Fin del rango
     * @return Lecturas en el rango
     */
    @GetMapping("/{sensorId}/history/time-range")
    public ResponseEntity<Map<String, Object>> getReadingsByTimeRange(
            @PathVariable String sensorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        log.info("📅 [GET] Lecturas entre {} y {}", startTime, endTime);

        List<SensorReading> readings = sensorReadingService.getReadingsByTimeRange(
                sensorId, startTime, endTime);

        List<SensorReadingResponseDTO> dtos = readings.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("sensorId", sensorId);
        response.put("startTime", startTime);
        response.put("endTime", endTime);
        response.put("totalReadings", dtos.size());
        response.put("readings", dtos);

        return ResponseEntity.ok(response);
    }

    // ═══════════════════════════════════════════════════════════════════
    // 4. GET /api/sensors/list - LISTAR SENSORES
    // ═══════════════════════════════════════════════════════════════════

    /**
     * listAllSensors() - Obtener IDs de todos los sensores
     *
     * HTTP METHOD: GET
     * URL: /api/sensors/list
     *
     * EJEMPLO:
     * ========
     * GET http://localhost:8080/api/sensors/list
     *
     * RESPUESTA (200 OK):
     * ===================
     * {
     *   "totalSensors": 3,
     *   "sensorIds": [
     *     "motor-line3-unit5",
     *     "pump-area2-unit1",
     *     "conveyor-area1-unit3"
     *   ]
     * }
     *
     * ¿CÓMO OBTENEMOS SENSORES ÚNICOS?
     * ==================================
     * Alternativa 1: Query especial en Repository
     * @Query("SELECT DISTINCT s.sensorId FROM SensorReading s")
     * List<String> findDistinctSensorIds();
     *
     * Alternativa 2: Obtener y filtrar en memoria (aquí)
     * - Para este MVP es suficiente
     * - En producción con millones de filas, usar DB query
     *
     * @return Lista de sensores únicos
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listAllSensors() {

        log.info("📊 [GET /api/sensors/list] Listando sensores únicos");

        // Obtener todos los sensores del historial (con duplicados)
        // En una solución real, habría una query específica en Repository
        List<String> uniqueSensorIds = sensorReadingService.getAllSensorIds();

        // Filtrar sensores únicos

        Map<String, Object> response = new HashMap<>();
        response.put("totalSensors", uniqueSensorIds.size());
        response.put("sensorIds", uniqueSensorIds);

        log.info("✅ {} sensores encontrados", uniqueSensorIds.size());

        return ResponseEntity.ok(response);
    }

    // ═══════════════════════════════════════════════════════════════════
    // 5. GET /api/sensors/health - HEALTH CHECK
    // ═══════════════════════════════════════════════════════════════════

    /**
     * healthCheck() - Verifica si el API está UP
     *
     * HTTP METHOD: GET
     * URL: /api/sensors/health
     *
     * EJEMPLO:
     * ========
     * GET http://localhost:8080/api/sensors/health
     *
     * RESPUESTA (200 OK):
     * ===================
     * {
     *   "status": "UP",
     *   "timestamp": "2026-10-06T14:33:00",
     *   "message": "API is running"
     * }
     *
     * ¿POR QUÉ?
     * ==========
     * - Load balancers usan health checks
     * - Kubernetes verifica si pod está vivo
     * - Monitoreo detecta caídas automáticamente
     *
     * @return Estado del API
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {

        log.debug("❤️  [GET /api/sensors/health] Health check");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("message", "API is running");

        return ResponseEntity.ok(response);
    }

    // ═══════════════════════════════════════════════════════════════════
    // 6. GET /api/sensors/critical - ALERTAS CRÍTICAS
    // ═══════════════════════════════════════════════════════════════════

    /**
     * getCriticalAlerts() - Obtener sensores en estado CRÍTICO
     *
     * HTTP METHOD: GET
     * URL: /api/sensors/critical
     *
     * EJEMPLO:
     * ========
     * GET http://localhost:8080/api/sensors/critical
     *
     * RESPUESTA (200 OK):
     * ===================
     * {
     *   "status": "critical-alerts",
     *   "count": 2,
     *   "alerts": [
     *     {
     *       "id": 100,
     *       "sensorId": "motor-line3-unit5",
     *       "temperature": 92.5,
     *       "vibration": 8.5,
     *       "current": 28.3,
     *       "timestamp": "2026-10-06T14:33:00",
     *       "status": "CRITICAL"
     *     },
     *     ...
     *   ]
     * }
     *
     * USO EN DASHBOARD:
     * ==================
     * - Mostrar sensores críticos en ROJO
     * - Enviar notificaciones a operrios
     * - Trigger automático para apagar máquina
     *
     * @return Todas las lecturas críticas
     */
    @GetMapping("/critical")
    public ResponseEntity<Map<String, Object>> getCriticalAlerts() {

        log.info("🚨 [GET /api/sensors/critical] Obteniendo alertas críticas");

        List<SensorReading> criticals = sensorReadingService.getCriticalAlerts();

        List<SensorReadingResponseDTO> dtos = criticals.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "critical-alerts");
        response.put("count", dtos.size());
        response.put("alerts", dtos);

        if (!dtos.isEmpty()) {
            log.warn("🚨 {} sensores en estado CRÍTICO", dtos.size());
        }

        return ResponseEntity.ok(response);
    }

    // ═══════════════════════════════════════════════════════════════════
    // MÉTODOS AUXILIARES
    // ═══════════════════════════════════════════════════════════════════

    /**
     * toResponseDTO() - Convertir Entity → DTO
     *
     * ¿POR QUÉ CONVERTIR?
     * ====================
     * Entity contiene:
     * - Relaciones con otros entities
     * - Configuración JPA interna
     * - Campos que no queremos exponer
     *
     * DTO contiene SOLO:
     * - Lo que el cliente necesita
     * - Protege la estructura interna
     *
     * Ejemplo:
     * Entity tiene: @OneToMany alerts, @ManyToOne device, @JoinColumn...
     * DTO tiene SOLO: id, sensorId, temperature, vibration, current, timestamp, status
     *
     * @param reading - Entity de BD
     * @return DTO para JSON response
     */
    private SensorReadingResponseDTO toResponseDTO(SensorReading reading) {
        return SensorReadingResponseDTO.builder()
                .id(reading.getId())
                .sensorId(reading.getSensorId())
                .temperature(reading.getTemperature())
                .vibration(reading.getVibration())
                .current(reading.getCurrent())
                .timestamp(reading.getTimestamp())
                .status(reading.getStatus())
                .build();
    }

    /**
     * CONCEPTO: HTTP STATUS CODES
     * ============================
     *
     * 2xx - SUCCESS
     * - 200 OK: La petición fue exitosa
     * - 201 CREATED: Se creó un recurso nuevo
     * - 204 NO CONTENT: Éxito pero sin body
     *
     * 4xx - CLIENT ERROR
     * - 400 BAD REQUEST: Datos inválidos
     * - 401 UNAUTHORIZED: Sin autenticación
     * - 404 NOT FOUND: Recurso no existe
     * - 409 CONFLICT: Conflicto (ej: ID duplicado)
     *
     * 5xx - SERVER ERROR
     * - 500 INTERNAL SERVER ERROR: Error no controlado
     * - 503 SERVICE UNAVAILABLE: Servidor saturado
     *
     * RETORNAR EL STATUS CORRECTO:
     * - POST exitoso → 201 (no 200)
     * - GET exitoso → 200
     * - Validación fallida → 400 (no 500)
     * - Recurso no encontrado → 404 (no 500)
     *
     * ═════════════════════════════════════════════════════════════════
     *
     * @RestController vs @Controller
     * ===============================
     *
     * @Controller:
     * public String hello() { return "hello.html"; }  // Retorna template
     *
     * @RestController (@Controller + @ResponseBody):
     * public String hello() { return "hello"; }  // Retorna JSON/text
     *
     * @RestController es para APIs JSON. Es lo que usamos.
     *
     * ═════════════════════════════════════════════════════════════════
     *
     * VALIDACIÓN AUTOMÁTICA (@Valid)
     * ===============================
     *
     * Sin @Valid:
     * @PostMapping("/data")
     * public void save(@RequestBody SensorReadingDTO dto) {
     *     // dto puede tener vibration = 999 (INVÁLIDO)
     * }
     *
     * Con @Valid:
     * @PostMapping("/data")
     * public void save(@Valid @RequestBody SensorReadingDTO dto) {
     *     // Spring valida ANTES de llamar al método
     *     // Si falla → 400 BAD REQUEST automático
     * }
     *
     * Las validaciones vienen del DTO:
     * @NotNull, @DecimalMin, @DecimalMax, etc
     */
}
