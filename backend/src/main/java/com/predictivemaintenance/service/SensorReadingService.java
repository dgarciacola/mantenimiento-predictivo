package com.predictivemaintenance.service;

import com.predictivemaintenance.dto.SensorReadingDTO;
import com.predictivemaintenance.entity.SensorReading;
import com.predictivemaintenance.exception.InvalidSensorDataException;
import com.predictivemaintenance.exception.SensorNotFoundException;
import com.predictivemaintenance.repository.SensorReadingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SensorReadingService - Lógica de negocio de las lecturas IoT.
 *
 * Lanza excepciones custom que NO se capturan en el controller;
 * las intercepta exception.GlobalExceptionHandler:
 *  - InvalidSensorDataException -> HTTP 400 (valores fuera de rango)
 *  - SensorNotFoundException    -> HTTP 404 (sensor sin lecturas)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SensorReadingService {

    public static final String STATUS_NORMAL = "NORMAL";
    public static final String STATUS_WARNING = "WARNING";
    public static final String STATUS_CRITICAL = "CRITICAL";

    private final SensorReadingRepository sensorReadingRepository;

    /** Valida, calcula el estado y persiste una lectura. */
    public SensorReading saveSensorReading(SensorReadingDTO dto) {
        validateRanges(dto);

        SensorReading reading = new SensorReading();
        reading.setSensorId(dto.getSensorId());
        reading.setTemperature(dto.getTemperature());
        reading.setVibration(dto.getVibration());
        reading.setCurrent(dto.getCurrent());
        // timestamp lo rellena @CreationTimestamp en la entidad
        reading.setStatus(computeStatus(dto));

        SensorReading saved = sensorReadingRepository.save(reading);
        log.info("✅ Lectura guardada: sensor={} status={}", saved.getSensorId(), saved.getStatus());
        return saved;
    }

    /** Historial de un sensor (más recientes primero). 404 si no tiene lecturas. */
    public List<SensorReading> getHistoryBySensorId(String sensorId) {
        List<SensorReading> readings = sensorReadingRepository.findBySensorIdOrderByTimestampDesc(sensorId);
        if (readings.isEmpty()) {
            throw new SensorNotFoundException("Sensor '" + sensorId + "' not found or has no readings");
        }
        return readings;
    }

    /** Lecturas de un sensor en un rango temporal. */
    public List<SensorReading> getReadingsByTimeRange(String sensorId, LocalDateTime start, LocalDateTime end) {
        if (start.isAfter(end)) {
            throw new InvalidSensorDataException("startTime must be before endTime");
        }
        return sensorReadingRepository.findByTimeRange(sensorId, start, end);
    }

    /** IDs de sensores distintos. */
    public List<String> getAllSensorIds() {
        return sensorReadingRepository.findAllDistinctSensorIds();
    }

    /** Lecturas en estado CRÍTICO. */
    public List<SensorReading> getCriticalAlerts() {
        return sensorReadingRepository.findByStatusOrderByTimestampDesc(STATUS_CRITICAL);
    }

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
