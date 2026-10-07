package com.predictivemaintenance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SensorReadingResponseDTO - DTO para Respuestas HTTP
 *
 * ¿DIFERENCIA CON SensorReadingDTO?
 * ==================================
 *
 * SensorReadingDTO (INPUT - lo que recibe el cliente):
 * - sensorId
 * - temperature
 * - vibration
 * - current
 * - NO incluye: id, timestamp, status
 *
 * SensorReadingResponseDTO (OUTPUT - lo que retorna el servidor):
 * - id (generado en BD)
 * - sensorId
 * - temperature
 * - vibration
 * - current
 * - timestamp (generado por servidor)
 * - status (calculado por Service)
 *
 * FLUJO:
 * ======
 * Cliente envía:
 * {
 *   "sensorId": "motor-1",
 *   "temperature": 45.3,
 *   "vibration": 2.1,
 *   "current": 15.2
 * }
 * ↓ Controller recibe en SensorReadingDTO
 * ↓ Service convierte a Entity, calcula status
 * ↓ Repository guarda en BD (genera id y timestamp)
 * ↓ Controller convierte Entity → SensorReadingResponseDTO
 * ↓ Cliente recibe:
 * {
 *   "id": 42,
 *   "sensor_id": "motor-1",
 *   "temperature": 45.3,
 *   "vibration": 2.1,
 *   "current": 15.2,
 *   "timestamp": "2026-10-06T14:33:00",
 *   "status": "NORMAL"
 * }
 *
 * ¿POR QUÉ DOS DTOs?
 * ==================
 * SEGURIDAD:
 * - Input DTO: El cliente SOLO puede enviar datos de entrada
 * - Output DTO: El servidor SOLO retorna lo que decide
 * - Imposible que cliente manipule id o timestamp
 *
 * FLEXIBILIDAD:
 * - Input: Datos mínimos del cliente
 * - Output: Datos enriquecidos del servidor
 * - Pueden cambiar sin quebrar una a la otra
 *
 * VALIDACIÓN:
 * - Input DTO: Valida datos de entrada
 * - Output DTO: No necesita validación (ya validados)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorReadingResponseDTO {

    /**
     * id - Identificador único en BD
     *
     * GENERADO POR:
     * - BD (PostgreSQL con IDENTITY strategy)
     * - Nunca enviado por cliente
     *
     * USADO EN:
     * - Identificar lectura específica
     * - Update/Delete después
     * - Relaciones con otras tablas
     *
     * EJEMPLO: 42
     */
    private Long id;

    /**
     * sensorId - Identificador del sensor
     *
     * ENVIADO POR: Cliente
     * FORMATO: String (ej: "motor-line3-unit5")
     *
     * USADO PARA:
     * - Filtrar lecturas de un sensor
     * - Agrupar historial
     * - Buscar en dashboards
     *
     * JSON MAPPING:
     * - Java: sensorId
     * - JSON: sensor_id (snake_case)
     *
     * @JsonProperty mapea Java camelCase ↔ JSON snake_case
     * Ejemplo:
     * Java: sensorId = "motor-1"
     * JSON: "sensor_id": "motor-1"
     */
    @JsonProperty("sensor_id")
    private String sensorId;

    /**
     * temperature - Temperatura en °C
     *
     * RANGO: 0-100
     * UNIDAD: Celsius (°C)
     * PRECISIÓN: 1 decimal
     *
     * EJEMPLO: 45.3
     */
    private Double temperature;

    /**
     * vibration - Vibración en mm/s
     *
     * RANGO: 0-10
     * UNIDAD: milímetros por segundo (mm/s)
     * PRECISIÓN: 2 decimales
     *
     * UMBRALES:
     * - < 5.0: NORMAL
     * - 5.0-8.0: WARNING
     * - > 8.0: CRITICAL
     *
     * EJEMPLO: 2.1
     */
    private Double vibration;

    /**
     * current - Corriente eléctrica en Amperios
     *
     * RANGO: 0-50
     * UNIDAD: Amperios (A)
     * PRECISIÓN: 1 decimal
     *
     * INDICADORES:
     * - Corriente normal: 15-20A
     * - Corriente alta: > 25A (esfuerzo anormal)
     *
     * EJEMPLO: 15.2
     */
    private Double current;

    /**
     * timestamp - Timestamp de la lectura
     *
     * GENERADO POR: Servidor (BD con @CreationTimestamp)
     * FORMATO: ISO 8601 (2026-10-06T14:33:00)
     * PRECISIÓN: Segundos
     * TIMEZONE: UTC (siempre)
     *
     * ¿POR QUÉ EL SERVIDOR?
     * =====================
     * - El cliente puede tener reloj incorrecto
     * - Evita timestamps falsos o manipulados
     * - Garantiza ordenamiento consistente
     * - Auditoría confiable
     *
     * EJEMPLO: "2026-10-06T14:33:00"
     */
    private LocalDateTime timestamp;

    /**
     * status - Estado de la lectura
     *
     * VALORES: "NORMAL", "WARNING", "CRITICAL"
     *
     * CALCULADO POR: SensorReadingService.calculateStatus()
     *
     * LÓGICA:
     * - CRITICAL: Vibración ≥ 8.0 mm/s O Temperatura ≥ 90°C
     * - WARNING: Vibración ≥ 5.0 mm/s O Temperatura ≥ 75°C
     * - NORMAL: Ninguno de los anteriores
     *
     * USADO PARA:
     * - Dashboard (colores: verde/amarillo/rojo)
     * - Alertas automáticas
     * - Filtrar por status en queries
     * - ML training
     *
     * EJEMPLO: "NORMAL"
     */
    private String status;

    /**
     * CONCEPTOS: DTO BUILDER
     * =======================
     *
     * @Builder genera un constructor fluido:
     *
     * SensorReadingResponseDTO dto = SensorReadingResponseDTO.builder()
     *     .id(42L)
     *     .sensorId("motor-1")
     *     .temperature(45.3)
     *     .vibration(2.1)
     *     .current(15.2)
     *     .timestamp(LocalDateTime.now())
     *     .status("NORMAL")
     *     .build();
     *
     * Alternativa sin @Builder (constructores):
     * SensorReadingResponseDTO dto = new SensorReadingResponseDTO(
     *     42L, "motor-1", 45.3, 2.1, 15.2, LocalDateTime.now(), "NORMAL"
     * );
     *
     * Builder es mejor porque:
     * - No requiere orden específico
     * - Código más legible
     * - Fácil omitir campos opcionales
     *
     * ═════════════════════════════════════════════════════════════════
     *
     * JACKSON - JSON SERIALIZATION
     * =============================
     *
     * DTO → JSON:
     * Spring convierte el DTO a JSON automáticamente
     *
     * Sin @JsonProperty:
     * {
     *   "id": 42,
     *   "sensorId": "motor-1",
     *   "temperature": 45.3
     * }
     *
     * Con @JsonProperty("sensor_id"):
     * {
     *   "id": 42,
     *   "sensor_id": "motor-1",
     *   "temperature": 45.3
     * }
     *
     * ¿POR QUÉ?
     * - Cliente espera JSON snake_case (sensor_id)
     * - Java usa camelCase (sensorId)
     * - @JsonProperty mapea entre ambos
     *
     * ═════════════════════════════════════════════════════════════════
     *
     * LOMBOK ANNOTATIONS
     * ===================
     *
     * @Data:
     * - Genera getters y setters
     * - Genera equals() y hashCode()
     * - Genera toString()
     *
     * @NoArgsConstructor:
     * - Genera constructor sin parámetros
     * - Necesario para Jackson (JSON → DTO)
     *
     * @AllArgsConstructor:
     * - Genera constructor con todos los campos
     * - Alternativa al Builder
     *
     * @Builder:
     * - Genera patrón Builder
     * - Constructor fluido: .builder().id(42).build()
     *
     * Equivalente SIN LOMBOK:
     * public class SensorReadingResponseDTO {
     *     private Long id;
     *     private String sensorId;
     *     // ... otros campos
     *
     *     // Constructor sin args
     *     public SensorReadingResponseDTO() {}
     *
     *     // Constructor con todos
     *     public SensorReadingResponseDTO(Long id, String sensorId, ...) {
     *         this.id = id;
     *         this.sensorId = sensorId;
     *     }
     *
     *     // Getters y setters
     *     public Long getId() { return id; }
     *     public void setId(Long id) { this.id = id; }
     *
     *     // equals, hashCode, toString...
     *
     *     // Builder
     *     public static SensorReadingResponseDTOBuilder builder() {
     *         return new SensorReadingResponseDTOBuilder();
     *     }
     * }
     *
     * Así que @Data + @NoArgsConstructor + @AllArgsConstructor + @Builder
     * = ~150 líneas de código generadas automáticamente!
     *
     * ═════════════════════════════════════════════════════════════════
     */
}
