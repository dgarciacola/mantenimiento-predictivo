package com.predictivemaintenance.controller;

import com.predictivemaintenance.entity.SensorReading;
import com.predictivemaintenance.exception.GlobalExceptionHandler;
import com.predictivemaintenance.exception.InvalidSensorDataException;
import com.predictivemaintenance.exception.SensorNotFoundException;
import com.predictivemaintenance.dto.SensorReadingDTO;
import com.predictivemaintenance.service.SensorReadingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de la CAPA WEB: SensorController + GlobalExceptionHandler.
 *
 * ¿QUÉ ES MockMvc?
 * ================
 * Simula peticiones HTTP contra el controlador SIN levantar un servidor real ni Spring
 * completo. Se construye en modo "standalone": solo registramos el controlador y el
 * @ControllerAdvice que queremos probar. El servicio es un mock (no hay BD ni Redis).
 *
 * QUÉ DEMUESTRAN ESTOS TESTS
 * ==========================
 * Que cada excepción del servicio acaba convertida por GlobalExceptionHandler en el
 * código HTTP y el formato JSON (ErrorResponseDTO) correctos, sin try-catch en el controlador.
 */
@ExtendWith(MockitoExtension.class)
class SensorControllerTest {

    @Mock private SensorReadingService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SensorController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static final String VALID_BODY =
            "{\"sensor_id\":\"motor-1\",\"temperature\":45.3,\"vibration\":2.1,\"current\":15.2}";

    private SensorReading reading() {
        SensorReading r = new SensorReading();
        r.setId(1L);
        r.setSensorId("motor-1");
        r.setTemperature(45.3);
        r.setVibration(2.1);
        r.setCurrent(15.2);
        r.setTimestamp(LocalDateTime.of(2026, 10, 7, 4, 0));
        r.setStatus("NORMAL");
        return r;
    }

    // ───────────────────────── POST /api/sensors/data ─────────────────────────

    @Test
    void post_validReading_returns201() throws Exception {
        when(service.saveSensorReading(any(SensorReadingDTO.class))).thenReturn(reading());

        mockMvc.perform(post("/api/sensors/data").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.sensorId").value("motor-1"))
                .andExpect(jsonPath("$.dataStatus").value("NORMAL"));
    }

    @Test
    void post_missingField_returns400ValidationFailed_andNeverCallsService() throws Exception {
        String missingTemperature = "{\"sensor_id\":\"motor-1\",\"vibration\":2.1,\"current\":15.2}";

        mockMvc.perform(post("/api/sensors/data").contentType(MediaType.APPLICATION_JSON).content(missingTemperature))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(service);
    }

    @Test
    void post_serviceRejectsData_returns400InvalidSensorData() throws Exception {
        when(service.saveSensorReading(any(SensorReadingDTO.class)))
                .thenThrow(new InvalidSensorDataException("Sensor data out of range"));

        mockMvc.perform(post("/api/sensors/data").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Sensor Data"))
                .andExpect(jsonPath("$.message").value("Sensor data out of range"));
    }

    // ───────────────────────── GET history ─────────────────────────

    @Test
    void getHistory_existingSensor_returns200() throws Exception {
        when(service.getHistoryBySensorId("motor-1")).thenReturn(List.of(reading()));

        mockMvc.perform(get("/api/sensors/motor-1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sensorId").value("motor-1"))
                .andExpect(jsonPath("$.totalReadings").value(1));
    }

    @Test
    void getHistory_unknownSensor_returns404() throws Exception {
        when(service.getHistoryBySensorId("motor-999"))
                .thenThrow(new SensorNotFoundException("Sensor 'motor-999' not found or has no readings"));

        mockMvc.perform(get("/api/sensors/motor-999/history"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Sensor 'motor-999' not found or has no readings"));
    }

    // ───────────────────────── resto de endpoints ─────────────────────────

    @Test
    void getList_returnsDistinctSensors() throws Exception {
        when(service.getAllSensorIds()).thenReturn(List.of("motor-1", "motor-2"));

        mockMvc.perform(get("/api/sensors/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSensors").value(2));
    }

    @Test
    void getCritical_withoutAlerts_returnsEmptyList() throws Exception {
        when(service.getCriticalAlerts()).thenReturn(List.of());

        mockMvc.perform(get("/api/sensors/critical"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void health_returnsUp() throws Exception {
        mockMvc.perform(get("/api/sensors/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void unexpectedError_returns500WithoutLeakingInternals() throws Exception {
        when(service.getAllSensorIds()).thenThrow(new RuntimeException("password=postgres123 connection refused"));

        mockMvc.perform(get("/api/sensors/list"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                // El mensaje interno NO debe llegar al cliente (seguridad)
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("postgres123"))));
    }
}
