package com.predictivemaintenance.exception;

import com.predictivemaintenance.dto.ErrorResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * GlobalExceptionHandler - Manejo centralizado de excepciones
 *
 * ¿QUÉ ES @ControllerAdvice?
 * ==========================
 * Una anotación Spring que marca esta clase como INTERCEPTOR GLOBAL de excepciones.
 * Cualquier excepción lanzada en un @Controller es interceptada aquí.
 *
 * FLUJO
 * =====
 * 1. Se lanza una excepción en SensorController
 * 2. Spring NO la retorna al cliente automáticamente
 * 3. Spring busca en @ControllerAdvice si hay un @ExceptionHandler para esa excepción
 * 4. ¡Lo encuentra! Ejecuta ese método
 * 5. El método retorna un ResponseEntity con ErrorResponseDTO
 * 6. Spring retorna ese JSON + HTTP status al cliente
 *
 * VENTAJAS
 * ========
 * ✅ Controllers quedan LIMPIOS (sin try-catch)
 * ✅ Respuestas de error CONSISTENTES
 * ✅ Una sola fuente de verdad para errores
 * ✅ Fácil agregar nuevas excepciones
 * ✅ SOLID: Separación de responsabilidades
 *
 * ¿CÓMO AGREGAR UNA NUEVA EXCEPCIÓN?
 * ===================================
 * 1. Crear la excepción custom (extends RuntimeException)
 * 2. Agregar @ExceptionHandler aquí
 * 3. ¡Listo! Todos los Controllers la pueden lanzar
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ═══════════════════════════════════════════════════════════════════
    // EXCEPCIONES CUSTOM (Lógica de Negocio)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Maneja: SensorNotFoundException
     * Cuándo: GET /api/sensors/{sensorId}/history con ID que no existe
     * HTTP Status: 404 NOT FOUND
     *
     * FLUJO:
     * ------
     * 1. Service busca el sensor en BD
     * 2. No lo encuentra → throw new SensorNotFoundException("Sensor motor-999 not found")
     * 3. GlobalExceptionHandler.handleSensorNotFound() lo intercepta
     * 4. Construye ErrorResponseDTO
     * 5. Retorna HTTP 404 con el DTO
     */
    @ExceptionHandler(SensorNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleSensorNotFound(
            SensorNotFoundException ex,
            WebRequest request) {

        log.warn("❌ Sensor no encontrado: {}", ex.getMessage());

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .status(404)
                .error("Not Found")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * Maneja: InvalidSensorDataException
     * Cuándo: POST /api/sensors/data con datos fuera de rango
     * HTTP Status: 400 BAD REQUEST
     *
     * FLUJO:
     * ------
     * 1. Service valida datos
     * 2. Temperature es 999 (fuera de rango) → throw new InvalidSensorDataException("Temperature must be 0-150")
     * 3. GlobalExceptionHandler.handleInvalidSensorData() lo intercepta
     * 4. Construye ErrorResponseDTO
     * 5. Retorna HTTP 400 con el DTO
     */
    @ExceptionHandler(InvalidSensorDataException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidSensorData(
            InvalidSensorDataException ex,
            WebRequest request) {

        log.warn("❌ Datos de sensor inválidos: {}", ex.getMessage());

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .status(400)
                .error("Invalid Sensor Data")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // ═══════════════════════════════════════════════════════════════════
    // EXCEPCIONES DE VALIDACIÓN (Spring Validation)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Maneja: MethodArgumentNotValidException
     * Cuándo: @Valid en DTO falla (ej: @NotBlank, @DecimalMin, etc)
     * HTTP Status: 400 BAD REQUEST
     *
     * FLUJO:
     * ------
     * 1. Cliente envía POST /api/sensors/data con sensorId=null
     * 2. Spring valida el DTO (ve @NotBlank en sensorId)
     * 3. Validación falla → Spring lanza MethodArgumentNotValidException
     * 4. GlobalExceptionHandler.handleValidationException() lo intercepta
     * 5. Extrae TODOS los errores de validación
     * 6. Retorna HTTP 400 con mensaje de todos los errores
     *
     * EJEMPLO:
     * --------
     * Cliente envía: {"temperature": -50, "vibration": 999}
     * Errores:
     *   - sensorId: cannot be null
     *   - temperature: must be > 0
     *   - vibration: must be < 10
     * Respuesta: "sensorId: cannot be null, temperature: must be > 0, vibration: must be < 10"
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        // Extraer TODOS los errores de validación
        String message = ex.getBindingResult()
                .getFieldErrors()                    // Lista de errores por campo
                .stream()                            // Convertir a Stream
                .map(error -> 
                    error.getField() + ": " + error.getDefaultMessage()
                )                                    // Formatear: "sensorId: cannot be null"
                .collect(Collectors.joining(", ")); // Unir con coma: "sensorId: ..., temperature: ..."

        log.warn("❌ Validación fallida: {}", message);

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .status(400)
                .error("Validation Failed")
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // ═══════════════════════════════════════════════════════════════════
    // EXCEPCIONES GENÉRICAS (Catch-all)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Maneja: CUALQUIER otra excepción no capturada
     * HTTP Status: 500 INTERNAL SERVER ERROR
     *
     * FLUJO:
     * ------
     * 1. Se lanza una excepción que NO tiene @ExceptionHandler específico
     * 2. Spring busca @ExceptionHandler(Exception.class) ← Este (padre de todas)
     * 3. Lo encuentra → ejecuta este método
     * 4. Retorna error genérico 500 sin exponer detalles internos
     *
     * ¿POR QUÉ NO exponer detalles?
     * ==============================
     * Si retornamos la excepción completa con stack trace:
     * - Exponemos internals del servidor (rutas, packages, etc)
     * - El cliente puede explotarlos
     * - Se vuelve confuso (cliente no necesita toda esa info)
     *
     * MEJOR: Mensaje genérico + log del error en servidor para debugging
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGlobalException(
            Exception ex,
            WebRequest request) {

        // Log COMPLETO en servidor (para debugging)
        log.error("💥 Error interno no controlado: ", ex);

        // Respuesta GENÉRICA al cliente (sin exponer detalles)
        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .status(500)
                .error("Internal Server Error")
                .message("An unexpected error occurred. Please try again later.")
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ═══════════════════════════════════════════════════════════════════
    // NOTAS DE ARQUITECTURA
    // ═══════════════════════════════════════════════════════════════════

    /**
     * ORDEN DE MATCHING
     * =================
     * Si se lanza SensorNotFoundException:
     *   1. Spring busca @ExceptionHandler(SensorNotFoundException.class) ← ENCONTRADA
     *   2. (Nunca llega al padre RuntimeException ni Exception)
     *
     * Si se lanza NullPointerException (extends RuntimeException):
     *   1. Spring busca @ExceptionHandler(NullPointerException.class) ← NO EXISTE
     *   2. Spring busca @ExceptionHandler(RuntimeException.class) ← NO EXISTE
     *   3. Spring busca @ExceptionHandler(Exception.class) ← ENCONTRADA ✅
     *
     * VENTAJA: Excepciones específicas primero, catch-all al final
     *
     * ═════════════════════════════════════════════════════════════════
     *
     * CÓMO EXTENDER
     * ==============
     * ¿Quieres agregar manejo para AuthenticationException?
     *
     * 1. Crear @ExceptionHandler(AuthenticationException.class)
     * 2. Retornar ErrorResponseDTO con 401 UNAUTHORIZED
     * 3. Controllers NO necesitan cambiar
     * 4. SOLID: Open for extension, closed for modification
     *
     * ═════════════════════════════════════════════════════════════════
     *
     * @WebRequest
     * ===========
     * Contiene info de la request:
     *   - getHeader("User-Agent")
     *   - getDescription(false) → ruta (ej: "/api/sensors/data")
     *   - getParameter("name")
     * Útil para logging detallado
     */
}
