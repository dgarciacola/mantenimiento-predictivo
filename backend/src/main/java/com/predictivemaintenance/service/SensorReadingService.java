package com.predictivemaintenance.service;

import com.predictivemaintenance.dto.SensorReadingDTO;
import com.predictivemaintenance.entity.SensorReading;
import com.predictivemaintenance.exception.InvalidSensorDataException;
import com.predictivemaintenance.exception.SensorNotFoundException;
import com.predictivemaintenance.repository.SensorReadingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SensorReadingService - Lógica de negocio de las lecturas IoT.
 *
 * - Persiste en PostgreSQL (fuente de verdad).
 * - Cachea en Redis las últimas 24 h por sensor (write-through, lista acotada).
 * - Lanza excepciones custom que intercepta exception.GlobalExceptionHandler:
 *     InvalidSensorDataException -> 400 | SensorNotFoundException -> 404
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SensorReadingService {

    public static final String STATUS_NORMAL = "NORMAL";
    public static final String STATUS_WARNING = "WARNING";
    public static final String STATUS_CRITICAL = "CRITICAL";

    /** Ventana de caché: 24 h a 1 lectura cada 10 s = 8640 lecturas por sensor. */
    public static final int CACHE_MAX_READINGS = 8640;
    private static final String CACHE_KEY_FORMAT = "sensor:%s:latest";

    private final SensorReadingRepository sensorReadingRepository;
    private final StringRedisTemplate redisTemplate;

    // JSON con snake_case para que FastAPI (Python) lea la misma caché
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    /** Valida, calcula el estado, persiste y cachea una lectura. */
    public SensorReading saveSensorReading(SensorReadingDTO dto) {
        validateRanges(dto);

        SensorReading reading = new SensorReading();
        reading.setSensorId(dto.getSensorId());
        reading.setTemperature(dto.getTemperature());
        reading.setVibration(dto.getVibration());
        reading.setCurrent(dto.getCurrent());
        reading.setStatus(computeStatus(dto));

        SensorReading saved = sensorReadingRepository.save(reading);
        cacheReading(saved); // write-through: PostgreSQL ya tiene el dato, ahora Redis
        log.info("✅ Lectura guardada: sensor={} status={}", saved.getSensorId(), saved.getStatus());
        return saved;
    }

    /**
     * Historial (más recientes primero, máx. 24 h = CACHE_MAX_READINGS).
     * Lee de Redis; si no hay datos o Redis falla, cae a PostgreSQL. 404 si no hay lecturas.
     */
    public List<SensorReading> getHistoryBySensorId(String sensorId) {
        List<SensorReading> readings = readFromCache(sensorId);
        if (!readings.isEmpty()) {
            return readings;
        }
        readings = sensorReadingRepository.findBySensorIdOrderByTimestampDesc(sensorId).stream()
                .limit(CACHE_MAX_READINGS)
                .toList();
        if (readings.isEmpty()) {
            throw new SensorNotFoundException("Sensor '" + sensorId + "' not found or has no readings");
        }
        return readings;
    }

    public List<SensorReading> getReadingsByTimeRange(String sensorId, LocalDateTime start, LocalDateTime end) {
        if (start.isAfter(end)) {
            throw new InvalidSensorDataException("startTime must be before endTime");
        }
        return sensorReadingRepository.findByTimeRange(sensorId, start, end);
    }

    public List<String> getAllSensorIds() {
        return sensorReadingRepository.findAllDistinctSensorIds();
    }

    public List<SensorReading> getCriticalAlerts() {
        return sensorReadingRepository.findByStatusOrderByTimestampDesc(STATUS_CRITICAL);
    }

    // ───────────── Caché Redis (write-through, lista acotada) ─────────────

    private String cacheKey(String sensorId) {
        return String.format(CACHE_KEY_FORMAT, sensorId);
    }

    /** LPUSH + LTRIM. Nunca debe romper la escritura principal. */
    private void cacheReading(SensorReading r) {
        try {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("sensor_id", r.getSensorId());
            m.put("temperature", r.getTemperature());
            m.put("vibration", r.getVibration());
            m.put("current", r.getCurrent());
            m.put("timestamp", r.getTimestamp() == null ? null : r.getTimestamp().toString());
            m.put("status", r.getStatus());

            String key = cacheKey(r.getSensorId());
            redisTemplate.opsForList().leftPush(key, jsonMapper.writeValueAsString(m));
            redisTemplate.opsForList().trim(key, 0, CACHE_MAX_READINGS - 1L);
        } catch (RuntimeException e) {
            log.warn("⚠️ Redis no disponible, lectura solo en PostgreSQL: {}", e.getMessage());
        }
    }

    /** Lista vacía si no hay caché o Redis falla: el llamador cae a PostgreSQL. */
    @SuppressWarnings("unchecked")
    private List<SensorReading> readFromCache(String sensorId) {
        try {
            List<String> raw = redisTemplate.opsForList().range(cacheKey(sensorId), 0, -1);
            if (raw == null || raw.isEmpty()) {
                return List.of();
            }
            return raw.stream().map(json -> {
                Map<String, Object> m = jsonMapper.readValue(json, Map.class);
                SensorReading r = new SensorReading();
                r.setId(m.get("id") == null ? null : ((Number) m.get("id")).longValue());
                r.setSensorId((String) m.get("sensor_id"));
                r.setTemperature(((Number) m.get("temperature")).doubleValue());
                r.setVibration(((Number) m.get("vibration")).doubleValue());
                r.setCurrent(((Number) m.get("current")).doubleValue());
                r.setTimestamp(m.get("timestamp") == null ? null : LocalDateTime.parse((String) m.get("timestamp")));
                r.setStatus((String) m.get("status"));
                return r;
            }).toList();
        } catch (RuntimeException e) {
            log.warn("⚠️ Fallo leyendo caché Redis, usando PostgreSQL: {}", e.getMessage());
            return List.of();
        }
    }

    // ───────────── Validación y estado ─────────────

    private void validateRanges(SensorReadingDTO dto) {
        if (dto.getTemperature() < 0 || dto.getTemperature() > 100
                || dto.getVibration() < 0 || dto.getVibration() > 10
                || dto.getCurrent() < 0 || dto.getCurrent() > 50) {
            throw new InvalidSensorDataException(
                    "Sensor data out of range. Temperature: 0-100°C, Vibration: 0-10 mm/s, Current: 0-50A");
        }
    }

    private String computeStatus(SensorReadingDTO dto) {
        if (dto.getTemperature() >= 80 || dto.getVibration() >= 7 || dto.getCurrent() >= 40) {
            return STATUS_CRITICAL;
        }
        if (dto.getTemperature() >= 60 || dto.getVibration() >= 4 || dto.getCurrent() >= 30) {
            return STATUS_WARNING;
        }
        return STATUS_NORMAL;
    }
}
