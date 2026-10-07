package com.predictivemaintenance.service;

import com.predictivemaintenance.dto.SensorReadingDTO;
import com.predictivemaintenance.entity.SensorReading;
import com.predictivemaintenance.exception.InvalidSensorDataException;
import com.predictivemaintenance.exception.SensorNotFoundException;
import com.predictivemaintenance.repository.SensorReadingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * SensorReadingService - Lógica de negocio para lecturas de sensores IoT
 *
 * RESPONSABILIDADES:
 * ==================
 * 1. ✅ Validar datos del sensor (rangos, tipos)
 * 2. ✅ Guardar lecturas en PostgreSQL
 * 3. ✅ Cachear últimas lecturas en Redis
 * 4. ✅ Recuperar lecturas (por ID, rango temporal, etc)
 * 5. ✅ Lanzar excepciones CUSTOM cuando algo falla
 *
 * EXCEPCIONES CUSTOM LANZADAS:
 * ============================
 * InvalidSensorDataException:
 *   - Cuando datos fuera de rango (temperatura, vibración, corriente)
 *   - HTTP 400 BAD REQUEST
 *
 * SensorNotFoundException:
 *   - Cuando sensor no tiene datos registrados
 *   - HTTP 404 NOT FOUND
 *
 * FLUJO DE ERROR:
 * ===============
 * Service lanza excepción
 *     ↓
 * Controller NO la captura (sin try-catch)
 *     ↓
 * GlobalExceptionHandler la intercepta
 *     ↓
 * Retorna ErrorResponseDTO con HTTP status correcto
 *
 * VENTAJAS DE ESTE ENFOQUE:
 * ==========================
 * - Controllers LIMPIOS (sin try-catch)
 * - Excepciones CENTRALIZADAS en GlobalExceptionHandler
 * - Respuestas CONSISTENTES (siempre ErrorResponseDTO)
 * - FÁCIL de EXTENDER (agregar nuevas excepciones)
 *
 * @author David García
 * @version 2.0 (con GlobalExceptionHandler)
 */
@Service
@Slf4j
public class SensorReadingService {

    @Autowired
    private SensorReadingRepository sensorReadingRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String REDIS_KEY_PREFIX = "sensor:";
    private static final int REDIS_RETENTION = 100; // Last 100 readings per sensor

    /**
     * Process and store sensor reading with validation
     *
     * EXCEPCIÓN LANZADA:
     * - InvalidSensorDataException si datos fuera de rango
     *
     * FLUJO:
     * 1. Valida datos con isValidSensorReading()
     * 2. Si falla → lanza InvalidSensorDataException
     * 3. GlobalExceptionHandler lo captura → HTTP 400
     */
    public SensorReading saveSensorReading(SensorReadingDTO dto) {
        log.info("📥 Processing sensor reading from: {}", dto.getSensorId());

        // Validate input data
        if (!isValidSensorReading(dto)) {
            log.error("❌ Invalid sensor data rejected: {}", dto);
            throw new InvalidSensorDataException("Sensor data validation failed: values out of range. Temperature: 0-100°C, Vibration: 0-10 mm/s, Current: 0-50A");
        }

        // Set timestamp if not provided
        if (dto.getTimestamp() == null) {
            dto.setTimestamp(LocalDateTime.now());
        }

        // Create entity
        SensorReading reading = SensorReading.builder()
                .sensorId(dto.getSensorId())
                .temperature(dto.getTemperature())
                .vibration(dto.getVibration())
                .current(dto.getCurrent())
                .timestamp(dto.getTimestamp())
                .status("NORMAL")
                .build();

        // Save to database
        SensorReading saved = sensorReadingRepository.save(reading);
        log.info("✅ Sensor reading saved: {} at {}", dto.getSensorId(), saved.getTimestamp());

        // Cache in Redis
        cacheLatestReadings(dto.getSensorId(), saved);

        return saved;
    }

    /**
     * Validate sensor reading values
     */
    private boolean isValidSensorReading(SensorReadingDTO dto) {
        // Check ranges
        if (dto.getTemperature() < 0 || dto.getTemperature() > 100) {
            log.warn("Temperature out of range: {}", dto.getTemperature());
            return false;
        }
        if (dto.getVibration() < 0 || dto.getVibration() > 10) {
            log.warn("Vibration out of range: {}", dto.getVibration());
            return false;
        }
        if (dto.getCurrent() < 0 || dto.getCurrent() > 50) {
            log.warn("Current out of range: {}", dto.getCurrent());
            return false;
        }
        return true;
    }

    /**
     * Cache latest readings in Redis for fast access
     */
    private void cacheLatestReadings(String sensorId, SensorReading reading) {
        try {
            String key = REDIS_KEY_PREFIX + sensorId;

            // Get current list size
            Long size = redisTemplate.opsForList().size(key);

            // Keep only last N readings
            if (size != null && size >= REDIS_RETENTION) {
                redisTemplate.opsForList().rightPop(key);
            }

            // Add new reading
            redisTemplate.opsForList().leftPush(key, reading);

            log.debug("Cached reading for sensor: {}", sensorId);
        } catch (Exception e) {
            log.error("Redis caching failed for sensor {}: {}", sensorId, e.getMessage());
            // Continue anyway - Redis is not critical for MVP
        }
    }

    /**
     * Get latest readings from cache (fast path)
     */
    public List<Object> getLatestReadingsFromCache(String sensorId) {
        try {
            String key = REDIS_KEY_PREFIX + sensorId;
            return redisTemplate.opsForList().range(key, 0, 99);
        } catch (Exception e) {
            log.error("❌ Redis retrieval failed: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Get latest readings from database (slow but reliable)
     *
     * EXCEPCIÓN LANZADA:
     * - SensorNotFoundException si sensor no tiene datos registrados
     *
     * FLUJO:
     * 1. Busca últimas 100 lecturas en base datos
     * 2. Si lista vacía → sensor NO tiene datos
     * 3. Lanza SensorNotFoundException
     * 4. GlobalExceptionHandler lo captura → HTTP 404
     */
    public List<SensorReading> getLatestReadings(String sensorId) {
        log.info("🔍 Searching readings for sensor: {}", sensorId);

        List<SensorReading> readings = sensorReadingRepository.findTop100BySensorIdOrderByTimestampDesc(sensorId);

        if (readings.isEmpty()) {
            log.warn("⚠️  Sensor not found or has no data: {}", sensorId);
            throw new SensorNotFoundException("Sensor '" + sensorId + "' not found or has no readings");
        }

        log.info("✅ Found {} readings for sensor: {}", readings.size(), sensorId);
        return readings;
    }

    /**
     * Get readings in time range
     */
    public List<SensorReading> getReadingsByTimeRange(String sensorId,
                                                      LocalDateTime startTime,
                                                      LocalDateTime endTime) {
        return sensorReadingRepository.findBySensorIdAndTimestampBetween(sensorId, startTime, endTime);
    }

    /**
     * Get all distinct sensor IDs
     */
    public List<String> getAllSensorIds() {
        return sensorReadingRepository.findAllDistinctSensorIds();
    }

    /**
     * Get paginated readings
     */
    public Page<SensorReading> getReadingsPaginated(String sensorId, Pageable pageable) {
        return sensorReadingRepository.findBySensorIdOrderByTimestampDesc(sensorId, pageable);
    }
}
