package com.predictivemaintenance.exception;

/**
 * InvalidSensorDataException - Excepción Custom para validación de datos del sensor
 * 
 * ¿CUÁNDO SE LANZA?
 * =================
 * Cuando los datos del sensor están fuera de los rangos válidos:
 * - Temperatura: debe estar entre 0-150°C
 * - Vibración: debe estar entre 0-10 mm/s
 * - Corriente: debe estar entre 0-50A
 * 
 * EJEMPLO DE FLUJO:
 * 1. Cliente hace: POST /api/sensors/data
 *    Body: { "sensorId": "motor-1", "temperature": 200, "vibration": 2.1, "current": 15.2 }
 * 2. SensorReadingService.saveSensorReading() valida datos
 * 3. validateSensorData() comprueba temperatura > 150
 * 4. Condición fallida → throw new InvalidSensorDataException("Temperature out of range: 200")
 * 5. GlobalExceptionHandler lo captura
 * 6. Retorna HTTP 400 BAD REQUEST con ErrorResponseDTO
 * 
 * VENTAJAS SOBRE IllegalArgumentException:
 * ===========================================
 * - Nombre descriptivo: "InvalidSensorDataException" vs "IllegalArgumentException"
 * - Podemos tener manejadores específicos en GlobalExceptionHandler
 * - Código más legible y mantenible
 * - Seguimos SOLID: cada excepción tiene UN propósito
 * 
 * EJEMPLO DE RESPUESTA HTTP:
 * ==========================
 * Status: 400 BAD REQUEST
 * Body:
 * {
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "Invalid sensor data: Temperature out of range: 200. Valid range: 0-150",
 *   "timestamp": "2026-10-07T04:00:15.234"
 * }
 * 
 * @author David García (usando GlobalExceptionHandler)
 * @version 1.0
 */
public class InvalidSensorDataException extends RuntimeException {

    // ═══════════════════════════════════════════════════════════════════
    // CONSTRUCTORES
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Constructor 1: Solo mensaje
     * Uso: throw new InvalidSensorDataException("Temperature out of range");
     * 
     * @param message - Descripción detallada de la validación fallida
     */
    public InvalidSensorDataException(String message) {
        super(message);
    }

    /**
     * Constructor 2: Mensaje + Causa
     * Uso: throw new InvalidSensorDataException("Temperature validation failed", originalException);
     * 
     * @param message - Descripción del error
     * @param cause   - Excepción original que causó esto
     */
    public InvalidSensorDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
