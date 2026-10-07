package com.predictivemaintenance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ErrorResponseDTO - Formato estándar para TODAS las respuestas de error
 * 
 * OBJETIVO PRINCIPAL
 * ==================
 * Garantizar que TODOS los errores en el API se retornen con el MISMO formato.
 * Así el cliente sabe exactamente qué esperar.
 * 
 * ¿POR QUÉ UN DTO PARA ERRORES?
 * =============================
 * Sin DTO (MALO):
 * - Endpoint 1 retorna: { "error": "Not found" }
 * - Endpoint 2 retorna: { "message": "Sensor not found", "code": 404 }
 * - Endpoint 3 retorna: { "status": "FAIL", "detail": "..." }
 * → Cliente confundido, parsing difícil, inconsistente
 * 
 * Con DTO (BUENO):
 * - TODOS los endpoints retornan:
 *   {
 *     "status": 404,
 *     "error": "Not Found",
 *     "message": "Sensor motor-999 not found",
 *     "timestamp": "2026-10-07T04:00:15.234"
 *   }
 * → Cliente sabe exactamente qué esperar
 * 
 * CAMPOS:
 * =======
 * status: HTTP status code (200, 400, 404, 500, etc)
 * error: Nombre del error en formato HTTP (Bad Request, Not Found, Internal Server Error)
 * message: Mensaje descriptivo específico del error
 * timestamp: Cuándo ocurrió el error (útil para logs y debugging)
 * 
 * ANOTACIONES LOMBOK:
 * ===================
 * @Data: Genera getter/setter/equals/hashCode/toString automáticamente
 * @NoArgsConstructor: Genera constructor vacío (para JSON deserialization)
 * @AllArgsConstructor: Genera constructor con todos los parámetros
 * @Builder: Patrón builder para crear objetos fácilmente
 * 
 * EJEMPLO DE USO:
 * ===============
 * ErrorResponseDTO error = ErrorResponseDTO.builder()
 *     .status(404)
 *     .error("Not Found")
 *     .message("Sensor motor-999 not found")
 *     .timestamp(LocalDateTime.now())
 *     .build();
 * 
 * return ResponseEntity.status(404).body(error);
 * 
 * @JsonProperty
 * ==============
 * Sin @JsonProperty: JSON tendría "timestamp" (camelCase)
 * Con @JsonProperty("timestamp"): JSON sigue siendo "timestamp" (snake_case)
 * Esto es importante para consistency en la API
 * 
 * @author David García (usando GlobalExceptionHandler)
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponseDTO {

    /**
     * HTTP Status Code del error
     * Ejemplos: 400 (Bad Request), 404 (Not Found), 500 (Internal Server Error)
     */
    private int status;

    /**
     * Nombre del error en formato HTTP
     * Ejemplos: "Bad Request", "Not Found", "Internal Server Error"
     */
    private String error;

    /**
     * Mensaje descriptivo específico del error
     * Ayuda al cliente a entender qué pasó y cómo solucionarlo
     * Ejemplos:
     * - "Sensor motor-999 not found"
     * - "Temperature out of range: 200. Valid range: 0-150"
     * - "Invalid JSON in request body"
     */
    private String message;

    /**
     * Timestamp de cuándo ocurrió el error
     * Formato ISO-8601: 2026-10-07T04:00:15.234
     * Útil para:
     * - Sincronización cliente-servidor
     * - Logs y auditoría
     * - Debugging de timings
     */
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
}
