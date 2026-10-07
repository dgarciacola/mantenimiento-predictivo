package com.predictivemaintenance.service;

import com.predictivemaintenance.dto.SensorReadingDTO;
import com.predictivemaintenance.entity.SensorReading;
import com.predictivemaintenance.exception.InvalidSensorDataException;
import com.predictivemaintenance.exception.SensorNotFoundException;
import com.predictivemaintenance.repository.SensorReadingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests UNITARIOS de SensorReadingService.
 *
 * ¿QUÉ ES UN TEST UNITARIO?
 * =========================
 * Prueba UNA clase aislada. Sus dependencias (PostgreSQL y Redis) se sustituyen por
 * "mocks": objetos falsos que nosotros controlamos. Así el test:
 *   - no necesita Docker, ni base de datos, ni Redis levantados
 *   - es rápido (milisegundos) y determinista
 *   - puede simular fallos difíciles de provocar (Redis caído)
 *
 * ANOTACIONES:
 *  @ExtendWith(MockitoExtension.class)  activa Mockito en JUnit 5
 *  @Mock        crea un objeto falso de esa clase/interfaz
 *  @InjectMocks crea el servicio REAL e inyecta los mocks en su constructor
 *  @Test        marca un método como test
 *  @ParameterizedTest + @CsvSource  repite el mismo test con varias filas de datos
 *
 * PATRÓN DE CADA TEST: Given (preparo) / When (ejecuto) / Then (compruebo)
 */
@ExtendWith(MockitoExtension.class)
class SensorReadingServiceTest {

    private static final String KEY = "sensor:motor-1:latest";

    @Mock private SensorReadingRepository repository;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ListOperations<String, String> listOps;

    @InjectMocks private SensorReadingService service;

    @BeforeEach
    void setUp() {
        // lenient(): este stub lo usan solo algunos tests; sin lenient(), Mockito en modo
        // estricto fallaría los tests que no lo necesitan ("UnnecessaryStubbingException")
        lenient().when(redisTemplate.opsForList()).thenReturn(listOps);
    }

    // ───────────────────────── helpers ─────────────────────────

    private SensorReadingDTO dto(double temp, double vib, double cur) {
        SensorReadingDTO d = new SensorReadingDTO();
        d.setSensorId("motor-1");
        d.setTemperature(temp);
        d.setVibration(vib);
        d.setCurrent(cur);
        return d;
    }

    /** El repositorio "guarda" devolviendo la misma entidad que recibe. */
    private void repositorySavesSameEntity() {
        when(repository.save(any(SensorReading.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ───────────────────────── saveSensorReading ─────────────────────────

    @Test
    void save_validReading_persistsAndCachesInRedis() {
        repositorySavesSameEntity();

        SensorReading saved = service.saveSensorReading(dto(45.3, 2.1, 15.2));

        assertEquals("motor-1", saved.getSensorId());
        verify(repository).save(any(SensorReading.class));
        // write-through: LPUSH en la lista del sensor + LTRIM a 8640 elementos (índices 0..8639)
        verify(listOps).leftPush(eq(KEY), anyString());
        verify(listOps).trim(KEY, 0L, 8639L);
    }

    @ParameterizedTest(name = "temp={0} vib={1} cur={2} -> {3}")
    @CsvSource({
            "45, 2, 15, NORMAL",
            "65, 2, 15, WARNING",   // temperatura >= 60
            "45, 5, 15, WARNING",   // vibración >= 4
            "45, 2, 35, WARNING",   // corriente >= 30
            "85, 2, 15, CRITICAL",  // temperatura >= 80
            "45, 8, 15, CRITICAL",  // vibración >= 7
            "45, 2, 45, CRITICAL"   // corriente >= 40
    })
    void save_computesStatusFromThresholds(double temp, double vib, double cur, String expectedStatus) {
        repositorySavesSameEntity();

        service.saveSensorReading(dto(temp, vib, cur));

        // ArgumentCaptor "captura" la entidad que el servicio pasó al repositorio
        ArgumentCaptor<SensorReading> captor = ArgumentCaptor.forClass(SensorReading.class);
        verify(repository).save(captor.capture());
        assertEquals(expectedStatus, captor.getValue().getStatus());
    }

    @ParameterizedTest(name = "temp={0} vib={1} cur={2} -> rechazada")
    @CsvSource({
            "150, 2, 15",   // temperatura > 100
            "-5, 2, 15",    // temperatura < 0
            "45, 11, 15",   // vibración > 10
            "45, 2, 60"     // corriente > 50
    })
    void save_outOfRange_throwsAndNeverTouchesDatabaseOrCache(double temp, double vib, double cur) {
        assertThrows(InvalidSensorDataException.class,
                () -> service.saveSensorReading(dto(temp, vib, cur)));

        // Un dato inválido NO debe llegar ni a PostgreSQL ni a Redis
        verifyNoInteractions(repository);
        verifyNoInteractions(listOps);
    }

    @Test
    void save_whenRedisFails_stillSavesInDatabase() {
        repositorySavesSameEntity();
        when(listOps.leftPush(anyString(), anyString())).thenThrow(new RuntimeException("Redis down"));

        // Redis es una optimización: su caída NO debe romper la ingesta
        SensorReading saved = assertDoesNotThrow(() -> service.saveSensorReading(dto(45, 2, 15)));

        assertNotNull(saved);
        verify(repository).save(any(SensorReading.class));
    }

    // ───────────────────────── getHistoryBySensorId ─────────────────────────

    @Test
    void history_cacheHit_returnsFromRedisWithoutQueryingDatabase() {
        String json = "{\"id\":1,\"sensor_id\":\"motor-1\",\"temperature\":45.3,\"vibration\":2.1,"
                + "\"current\":15.2,\"timestamp\":\"2026-10-07T04:00:00\",\"status\":\"NORMAL\"}";
        when(listOps.range(KEY, 0L, -1L)).thenReturn(List.of(json));

        List<SensorReading> result = service.getHistoryBySensorId("motor-1");

        assertEquals(1, result.size());
        assertEquals(45.3, result.get(0).getTemperature());
        assertEquals(LocalDateTime.of(2026, 10, 7, 4, 0, 0), result.get(0).getTimestamp());
        verifyNoInteractions(repository);
    }

    @Test
    void history_cacheMiss_fallsBackToDatabase() {
        when(listOps.range(KEY, 0L, -1L)).thenReturn(List.of());
        SensorReading r = new SensorReading();
        r.setSensorId("motor-1");
        when(repository.findBySensorIdOrderByTimestampDesc("motor-1")).thenReturn(List.of(r));

        List<SensorReading> result = service.getHistoryBySensorId("motor-1");

        assertEquals(1, result.size());
        verify(repository).findBySensorIdOrderByTimestampDesc("motor-1");
    }

    @Test
    void history_whenRedisFails_fallsBackToDatabase() {
        when(listOps.range(KEY, 0L, -1L)).thenThrow(new RuntimeException("Redis down"));
        SensorReading r = new SensorReading();
        r.setSensorId("motor-1");
        when(repository.findBySensorIdOrderByTimestampDesc("motor-1")).thenReturn(List.of(r));

        assertEquals(1, service.getHistoryBySensorId("motor-1").size());
    }

    @Test
    void history_noDataAnywhere_throwsSensorNotFound() {
        when(listOps.range(KEY, 0L, -1L)).thenReturn(List.of());
        when(repository.findBySensorIdOrderByTimestampDesc("motor-1")).thenReturn(List.of());

        SensorNotFoundException ex = assertThrows(SensorNotFoundException.class,
                () -> service.getHistoryBySensorId("motor-1"));
        assertTrue(ex.getMessage().contains("motor-1"));
    }

    // ───────────────────────── resto de consultas ─────────────────────────

    @Test
    void timeRange_startAfterEnd_throwsInvalidData() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 2, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 1, 0, 0);

        assertThrows(InvalidSensorDataException.class,
                () -> service.getReadingsByTimeRange("motor-1", start, end));
        verifyNoInteractions(repository);
    }

    @Test
    void timeRange_validRange_delegatesToRepository() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 2, 0, 0);
        when(repository.findByTimeRange("motor-1", start, end)).thenReturn(List.of(new SensorReading()));

        assertEquals(1, service.getReadingsByTimeRange("motor-1", start, end).size());
    }

    @Test
    void criticalAlerts_queriesCriticalStatus() {
        when(repository.findByStatusOrderByTimestampDesc("CRITICAL")).thenReturn(List.of(new SensorReading()));

        assertEquals(1, service.getCriticalAlerts().size());
    }
}
