package com.predictivemaintenance.exception;

/**
 * SensorNotFoundException - Excepción Custom para cuando un sensor NO existe
 * 
 * ¿CUÁNDO SE LANZA?
 * =================
 * Cuando intentamos obtener el historial de un sensor que no ha registrado datos.
 * 
 * EJEMPLO DE FLUJO:
 * 1. Cliente hace: GET /api/sensors/motor-999/history
 * 2. SensorReadingService.getHistoryBySensorId("motor-999") → Lista vacía
 * 3. Verificamos si existe → NO EXISTE
 * 4. Lanzamos: throw new SensorNotFoundException("Sensor motor-999 not found")
 * 5. GlobalExceptionHandler lo captura
 * 6. Retorna HTTP 404 NOT FOUND con ErrorResponseDTO
 * 
 * ARQUITECTURA:
 * =============
 * Extends RuntimeException porque:
 * - No es obligatorio capturar en compile-time
 * - Spring puede interceptarla automáticamente
 * - Es la práctica estándar en frameworks modernos
 * 
 * EJEMPLO DE RESPUESTA HTTP:
 * ==========================
 * Status: 404 NOT FOUND
 * Body:
 * {
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Sensor motor-999 not found",
 *   "timestamp": "2026-10-07T04:00:15.234"
 * }
 * 
 * @author David García (usando GlobalExceptionHandler)
 * @version 1.0
 */
public class SensorNotFoundException extends RuntimeException {

    // ═══════════════════════════════════════════════════════════════════
    // CONSTRUCTORES
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Constructor 1: Solo mensaje
     * Uso: throw new SensorNotFoundException("Sensor motor-999 not found");
     * 
     * @param message - Descripción del error
     */
    public SensorNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructor 2: Mensaje + Causa
     * Uso: throw new SensorNotFoundException("Sensor not found", originalException);
     * Útil para encadenar excepciones (exception chaining)
     * 
     * @param message - Descripción del error
     * @param cause   - Excepción original que causó esto
     */
    public SensorNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
