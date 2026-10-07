package com.predictivemaintenance.controller;

import com.predictivemaintenance.dto.SensorReadingDTO;
import com.predictivemaintenance.entity.SensorReading;
import com.predictivemaintenance.service.SensorReadingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SensorReadingController - REST API para lecturas de sensores
 * 
 * ARQUITECTURA DE EXCEPTION HANDLING:
 * ====================================
 * ✅ TODOS los métodos están LIMPIOS (sin try-catch)
 * ✅ Las excepciones se lanzan y se dejan subir
 * ✅ GlobalExceptionHandler las captura automáticamente
 * ✅ Se retornan respuestas standardizadas con ErrorResponseDTO
 * 
 * FLUJO EJEMPLO:
 * 1. Cliente: GET /api/sensors/motor-999/history
 * 2. getSensorHistory() llama a sensorReadingService.getLatestReadings()
 * 3. Service lanza: SensorNotFoundException (sensor no existe)
 * 4. Exception SUBE (no hay try-catch aquí)
 * 5. GlobalExceptionHandler la captura
 * 6. Retorna: HTTP 404 + ErrorResponseDTO
 */
@RestController
@RequestMapping("/api/sensors")
@Tag(name = "Sensor Data", description = "IoT Sensor data ingestion and retrieval")
@Slf4j
public class SensorReadingController {

    @Autowired
    private SensorReadingService sensorReadingService;

    /**
     * POST /api/sensors/data - Ingest sensor data
     * Expected JSON:
     * {
     *   "sensorId": "motor-line3-unit5",
     *   "temperature": 45.3,
     *   "vibration": 2.1,
     *   "current": 15.2,
     *   "timestamp": "2026-10-06T14:33:00"
     * }
     */
    @PostMapping("/data")
    @Operation(summary = "Ingest sensor reading",
               description = "Receive IoT sensor data, validate, and store in database and cache")
    public ResponseEntity<?> ingestSensorData(@Valid @RequestBody SensorReadingDTO sensorDTO) {

        // ✅ LIMPIO: Sin try-catch (GlobalExceptionHandler maneja excepciones)
        log.info("📥 Received sensor data from: {}", sensorDTO.getSensorId());

        long startTime = System.currentTimeMillis();

        // Delegar al Service (puede lanzar InvalidSensorDataException si datos inválidos)
        // GlobalExceptionHandler lo capturará automáticamente
        SensorReading saved = sensorReadingService.saveSensorReading(sensorDTO);

        long processingTime = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Sensor data received and validated");
        response.put("sensorId", saved.getSensorId());
        response.put("timestamp", saved.getTimestamp());
        response.put("dataProcessingTime", processingTime + "ms");

        log.info("✅ Sensor data saved successfully in {}ms", processingTime);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/sensors/{sensorId}/history - Get latest readings from database
     */
    @GetMapping("/{sensorId}/history")
    @Operation(summary = "Get sensor history", description = "Retrieve latest 100 readings for a sensor")
    public ResponseEntity<?> getSensorHistory(@PathVariable String sensorId) {

        // ✅ LIMPIO: Sin try-catch (GlobalExceptionHandler maneja excepciones)
        log.info("📖 Retrieving history for sensor: {}", sensorId);

        // Puede lanzar SensorNotFoundException si no existe
        // GlobalExceptionHandler lo capturará automáticamente
        List<SensorReading> readings = sensorReadingService.getLatestReadings(sensorId);

        Map<String, Object> response = new HashMap<>();
        response.put("sensorId", sensorId);
        response.put("totalReadings", readings.size());
        response.put("readings", readings);

        log.info("✅ Retrieved {} readings for sensor: {}", readings.size(), sensorId);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/sensors/{sensorId}/history/time-range
     * Query params: startTime, endTime (ISO-8601 format)
     */
    @GetMapping("/{sensorId}/history/time-range")
    @Operation(summary = "Get sensor readings by time range")
    public ResponseEntity<?> getSensorHistoryByTimeRange(
            @PathVariable String sensorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        // ✅ LIMPIO: Sin try-catch (GlobalExceptionHandler maneja excepciones)
        log.info("📅 Retrieving readings between {} and {}", startTime, endTime);

        // Puede lanzar excepciones (GlobalExceptionHandler las captura)
        List<SensorReading> readings = sensorReadingService.getReadingsByTimeRange(sensorId, startTime, endTime);

        Map<String, Object> response = new HashMap<>();
        response.put("sensorId", sensorId);
        response.put("startTime", startTime);
        response.put("endTime", endTime);
        response.put("totalReadings", readings.size());
        response.put("readings", readings);

        log.info("✅ Retrieved {} readings in time range", readings.size());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/sensors/list - Get all sensor IDs
     */
    @GetMapping("/list")
    @Operation(summary = "Get all sensor IDs", description = "List all distinct sensor IDs in system")
    public ResponseEntity<?> getAllSensors() {

        // ✅ LIMPIO: Sin try-catch (GlobalExceptionHandler maneja excepciones)
        log.info("📊 Retrieving all distinct sensor IDs");

        // Puede lanzar excepciones (GlobalExceptionHandler las captura)
        List<String> sensorIds = sensorReadingService.getAllSensorIds();

        Map<String, Object> response = new HashMap<>();
        response.put("totalSensors", sensorIds.size());
        response.put("sensorIds", sensorIds);

        log.info("✅ Found {} distinct sensors", sensorIds.size());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/sensors/health - Health check
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if sensor service is running")
    public ResponseEntity<?> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Sensor Reading Service");
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }
}
