package com.predictivemaintenance.config;

import com.predictivemaintenance.dto.ErrorResponseDTO;
import com.predictivemaintenance.exception.InvalidSensorDataException;
import com.predictivemaintenance.exception.SensorNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;

/**
 * GlobalExceptionHandler - Manejador CENTRALIZADO de excepciones
 * 
 * ════════════════════════════════════════════════════════════════════════════════
 * ¿QUÉ ES @ControllerAdvice?
 * ════════════════════════════════════════════════════════════════════════════════
 * Es un componente de Spring que INTERCEPTA todas las excepciones lanzadas en
 * CUALQUIER @Controller o @RestController de la aplicación.
 * 
 * FLUJO SIN GlobalExceptionHandler:
 * ================================
 * Cliente → SensorController → SensorReadingService (lanza excepción)
 *                          ↓ try-catch
 *                    maneja excepción
 *                    retorna response
 *
 * FLUJO CON GlobalExceptionHandler:
 * ==================================
 * Cliente → SensorController → SensorReadingService (lanza excepción)
 *                          ↓ NO hay try-catch
 *                      excepción sube
 *                          ↓
 *          GlobalExceptionHandler la captura
 *                          ↓
 *          Retorna ErrorResponseDTO con status HTTP
 *
 * VENTAJAS:
 * ==========
 * 1. ✅ Controllers más LIMPIOS (sin try-catch)
 * 2. ✅ Manejo de errores CENTRALIZADO (un solo lugar)
 * 3. ✅ Formato de respuesta CONSISTENTE (ErrorResponseDTO)
 * 4. ✅ Fácil de EXTENDER (agregar nuevas excepciones)
 * 5. ✅ Separación de CONCERNS (Controllers vs error handling)
 * 6. ✅ Logging automático de errores
 * 
 * ════════════════════════════════════════════════════════════════════════════════
 * ARQUITECTURA DE SPRING EXCEPTION HANDLING
 * ════════════════════════════════════════════════════════════════════════════════
 * 
 * JERARQUÍA DE EXCEPTIONS EN JAVA:
 * 
 * Throwable
 *     ├── Exception
 *     │   ├── RuntimeException
 *     │   │   ├── SensorNotFoundException ← Nuestras excepciones custom
 *     │   │   ├── InvalidSensorDataException ← (heredan de RuntimeException)
 *     │   │   └── ...
 *     │   ├── IOException
 *     │   ├── SQLException
 *     │   └── ...
 *     └── Error
 *         ├── OutOfMemoryError
 *         ├── StackOverflowError
 *         └── ...
 * 
 * ORDEN DE PROCESAMIENTO DE @ExceptionHandler:
 * =============================================
 * Spring busca en este orden:
 * 1. Manejador para la EXCEPCIÓN ESPECÍFICA (ej: SensorNotFoundException)
 * 2. Manejador para la CLASE PADRE (ej: RuntimeException)
 * 3. Manejador para Exception genérica (catch-all)
 * 4. Si nada coincide → error 500 automático
 * 
 * EJEMPLO:
 * Lanzamos: throw new SensorNotFoundException("Motor-999 not found")
 * Spring busca:
 *   ✓ @ExceptionHandler(SensorNotFoundException.class) → ENCONTRADO, usa este
 *   ✗ @ExceptionHandler(RuntimeException.class)
 *   ✗ @ExceptionHandler(Exception.class)
 * 
 * ════════════════════════════════════════════════════════════════════════════════
 * PRINCIPIOS SOLID APLICADOS
 * ════════════════════════════════════════════════════════════════════════════════
 * 
 * S - Single Responsibility:
 *   Esta clase SOLO maneja excepciones, nada más.
 * 
 * O - Open/Closed:
 *   ABIERTO a extensión: agregar nuevo @ExceptionHandler para nuevas excepciones
 *   CERRADO a modificación: código existente no cambia
 * 
 * D - Dependency Inversion:
 *   Depende de abstracciones (Exception) no de concretos
 * 
 * @author David García
 * @version 1.0
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ════════════════════════════════════════════════════════════════════
    // 1. @ExceptionHandler para SensorNotFoundException (404 NOT FOUND)
    // ════════════════════════════════════════════════════════════════════

    /**
     * Maneja SensorNotFoundException
     * 
     * CUÁNDO SE EJECUTA:
     * - Cuando cualquier método lanza: throw new SensorNotFoundException(...)
     * 
     * EJEMPLO:
     * Cliente hace: GET /api/sensors/motor-999/history
     * Service lanza: throw new SensorNotFoundException("Sensor motor-999 not found")
     * GlobalExceptionHandler.handleSensorNotFound() se ejecuta
     * Retorna: HTTP 404 + ErrorResponseDTO
     * 
     * @param ex - La excepción SensorNotFoundException que fue lanzada
     * @return ResponseEntity con HTTP 404 y ErrorResponseDTO
     */
    @ExceptionHandler(SensorNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleSensorNotFound(SensorNotFoundException ex) {
        
        log.warn("⚠️  [404 NOT FOUND] Sensor no encontrado: {}", ex.getMessage());

        // Construir respuesta de error estándar
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .status(HttpStatus.NOT_FOUND.value())           // 404
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())   // "Not Found"
                .message(ex.getMessage())                        // Mensaje específico de excepción
                .timestamp(LocalDateTime.now())                  // Ahora mismo
                .build();

        // Retornar ResponseEntity con status 404 y body ErrorResponseDTO
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse);
    }

    // ════════════════════════════════════════════════════════════════════
    // 2. @ExceptionHandler para InvalidSensorDataException (400 BAD REQUEST)
    // ════════════════════════════════════════════════════════════════════

    /**
     * Maneja InvalidSensorDataException
     * 
     * CUÁNDO SE EJECUTA:
     * - Cuando cualquier método lanza: throw new InvalidSensorDataException(...)
     * 
     * EJEMPLO:
     * Cliente hace: POST /api/sensors/data
     *   Body: { "temperature": 200, ... } (temperatura > 150°C = INVÁLIDA)
     * Service.validateSensorData() lanza: throw new InvalidSensorDataException("Temperature out of range")
     * GlobalExceptionHandler.handleInvalidSensorData() se ejecuta
     * Retorna: HTTP 400 + ErrorResponseDTO
     * 
     * @param ex - La excepción InvalidSensorDataException que fue lanzada
     * @return ResponseEntity con HTTP 400 y ErrorResponseDTO
     */
    @ExceptionHandler(InvalidSensorDataException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidSensorData(InvalidSensorDataException ex) {
        
        log.warn("⚠️  [400 BAD REQUEST] Validación de sensor fallida: {}", ex.getMessage());

        // Construir respuesta de error estándar
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .status(HttpStatus.BAD_REQUEST.value())           // 400
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())   // "Bad Request"
                .message(ex.getMessage())                          // Mensaje específico de validación
                .timestamp(LocalDateTime.now())                    // Ahora mismo
                .build();

        // Retornar ResponseEntity con status 400 y body ErrorResponseDTO
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    // ════════════════════════════════════════════════════════════════════
    // 3. @ExceptionHandler para MethodArgumentNotValidException (400)
    // ════════════════════════════════════════════════════════════════════

    /**
     * Maneja fallos de validación (@Valid fallida)
     * 
     * CUÁNDO SE EJECUTA:
     * - Cuando @Valid falla en un @RequestBody
     * 
     * EJEMPLO:
     * Cliente hace: POST /api/sensors/data
     *   Body: { "temperature": null, ... }
     * Spring valida con @Valid
     * Validación falla (@NotNull en temperature)
     * Spring lanza: MethodArgumentNotValidException
     * GlobalExceptionHandler.handleValidationException() se ejecuta
     * Retorna: HTTP 400 + ErrorResponseDTO
     * 
     * @param ex - La excepción MethodArgumentNotValidException
     * @return ResponseEntity con HTTP 400 y ErrorResponseDTO
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationException(MethodArgumentNotValidException ex) {
        
        // Obtener el primer error de validación
        String fieldName = ex.getBindingResult().getFieldError().getField();
        String defaultMessage = ex.getBindingResult().getFieldError().getDefaultMessage();
        String errorMessage = String.format("Validation failed on field '%s': %s", fieldName, defaultMessage);
        
        log.warn("⚠️  [400 BAD REQUEST] Validación fallida: {}", errorMessage);

        // Construir respuesta de error estándar
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .status(HttpStatus.BAD_REQUEST.value())           // 400
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())   // "Bad Request"
                .message(errorMessage)                             // Detalle de qué validación falló
                .timestamp(LocalDateTime.now())                    // Ahora mismo
                .build();

        // Retornar ResponseEntity con status 400 y body ErrorResponseDTO
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    // ════════════════════════════════════════════════════════════════════
    // 4. @ExceptionHandler para Exception genérica (500 INTERNAL SERVER ERROR)
    // ════════════════════════════════════════════════════════════════════

    /**
     * Catch-all: Maneja CUALQUIER excepción no manejada
     * 
     * CUÁNDO SE EJECUTA:
     * - Cuando se lanza una excepción que NO tiene su propio @ExceptionHandler
     * - Es el "fallback" por si algo sale mal inesperadamente
     * 
     * EJEMPLO:
     * Cliente hace: GET /api/sensors/list
     * Service accede a PostgreSQL
     * PostgreSQL está caído → SQLException
     * No hay @ExceptionHandler(SQLException.class)
     * Sube hasta Exception genérica
     * GlobalExceptionHandler.handleGenericException() se ejecuta
     * Retorna: HTTP 500 + ErrorResponseDTO
     * 
     * ¿POR QUÉ GENÉRICA?
     * ==================
     * Mejor capturar TODAS las excepciones inesperadas con status 500,
     * que dejar que Spring las maneje automáticamente con HTML error pages.
     * 
     * @param ex - Cualquier excepción que no fue manejada específicamente
     * @return ResponseEntity con HTTP 500 y ErrorResponseDTO
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception ex) {
        
        log.error("💥 [500 INTERNAL SERVER ERROR] Error no controlado: {}", ex.getMessage(), ex);

        // Construir respuesta de error estándar
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())           // 500
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())   // "Internal Server Error"
                .message("An unexpected error occurred. Please contact support.")  // Mensaje genérico (sin detalles internos)
                .timestamp(LocalDateTime.now())                              // Ahora mismo
                .build();

        // Retornar ResponseEntity con status 500 y body ErrorResponseDTO
        // NOTA: El mensaje detallado SOLO aparece en logs, no en respuesta HTTP
        //       Esto es importante por seguridad (no exponer internals)
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }
}
