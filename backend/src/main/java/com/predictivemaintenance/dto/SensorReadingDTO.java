package com.predictivemaintenance.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║          SensorReadingDTO - Data Transfer Object (API Layer)            ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 * 
 * ¿Qué es un DTO (Data Transfer Object)?
 * 
 * Es un objeto que REPRESENTA LOS DATOS EN TRÁNSITO entre cliente y servidor.
 * Es lo que el cliente ENVÍA en una petición POST/PUT y lo que el servidor DEVUELVE.
 * 
 * Diferencia clave: Entity vs DTO
 * 
 *   ENTITY (SensorReading)         DTO (SensorReadingDTO)
 *   ───────────────────────────    ──────────────────────────────
 *   @Entity                        NO @Entity (solo POJO)
 *   - id                           - NO id (cliente no lo envía)
 *   - sensorId                     - sensorId
 *   - temperature                  - temperature
 *   - vibration                    - vibration
 *   - current                      - current
 *   - timestamp (servidor)         - NO timestamp (servidor lo genera)
 *   - status (calculado)           - NO status (servidor lo calcula)
 *   
 *   Guardado en BD                 Transferido en HTTP
 *   Persistencia                   API Contract
 *   Contiene TODO                  Solo lo necesario para el cliente
 * 
 * ¿Por qué separar Entity y DTO?
 * 
 * 1. SEGURIDAD
 *    Nunca expongas todos los campos internos.
 *    Ejemplo: Si Entity tiene @Column(sensitive = true), no lo incluyas en DTO.
 * 
 * 2. VALIDACIÓN EN BOUNDARY
 *    DTO valida INPUT del cliente (lo que recibe la API).
 *    Entity valida OUTPUT a BD (lo que guarda la BD).
 *    
 *    Ejemplo:
 *      POST /api/sensors/data
 *      { "temperature": -50 }  ← DTO rechaza con 400 BAD REQUEST
 *      
 *      vs
 *      
 *      Entity también rechazaría, pero DTO lo detecta PRIMERO.
 *      Es más eficiente: no gasta CPU en BD si el dato es inválido.
 * 
 * 3. FLEXIBILIDAD
 *    Puedes cambiar Entity sin romper API.
 *    Agregar campo a Entity no significa agregarlo a DTO.
 *    
 *    Ejemplo: Agregas @Column(internal = true) a Entity
 *    DTO sigue igual → Cliente no se entera
 *    API mantiene contrato.
 * 
 * 4. SIMPLIFICACIÓN
 *    Entity puede tener relaciones complejas (OneToMany, ManyToMany).
 *    DTO es plano: solo los datos que necesita el cliente.
 * 
 * Analógía:
 *   Entity = Tu receta completa con notas privadas, medidas exactas, variaciones
 *   DTO = Tu receta publicada para amigos (sin secretos, sin detalles internos)
 * 
 * FLUJO DE DATOS:
 * 
 *   Cliente                 API (Spring)           Base de Datos
 *   ──────                  ────────               ──────────────
 *   
 *   JSON                   Deserialize
 *   {                      con Jackson          Entity
 *     "sensorId": "...",   ↓                    SensorReading
 *     "temperature": 45.3  ↓
 *   }         ────→     SensorReadingDTO   ─→  .save()
 *             HTTP        ↓
 *             POST      Validar                 Guardar en
 *             /api      @Valid                  PostgreSQL
 *             /sensors  ↓
 *             /data     Convertir DTO
 *                       a Entity
 *                       ↓
 *                       Guardar Entity
 *                       ↓
 *                       Responder Entity
 *                       (o DTO de response)
 * 
 * ¿Qué campos tiene SensorReadingDTO?
 * 
 * SensorReadingDTO (entrada del cliente):
 * - sensorId ✓
 * - temperature ✓
 * - vibration ✓
 * - current ✓
 * 
 * SensorReadingDTO NO tiene:
 * - id ✗ (servidor lo genera)
 * - timestamp ✗ (servidor lo genera)
 * - status ✗ (servidor lo calcula)
 * 
 * El cliente NO NECESITA enviar esos campos.
 * Si intenta enviarlos, Jackson los ignora (a menos que uses @JsonAnySetter).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensorReadingDTO {

    /**
     * SENSOR ID - Identificador del sensor
     * 
     * ¿Qué es @JsonProperty?
     * 
     * Anotación Jackson que especifica el nombre del campo en JSON.
     * 
     * Sin @JsonProperty:
     *   JSON recibido:     {"sensorId": "motor-1"}
     *   Jackson mapea a:   sensorReadingDTO.sensorId
     *   (Jackson convierte camelCase automáticamente)
     * 
     * Con @JsonProperty("sensor_id"):
     *   JSON recibido:     {"sensor_id": "motor-1"}
     *   Jackson mapea a:   sensorReadingDTO.sensorId
     *   (Jackson usa exactamente el nombre especificado)
     * 
     * ¿Cuándo usarlo?
     * 
     * Cuando el JSON tiene snake_case y Java usa camelCase.
     * 
     * Ejemplo - API desde sensores IoT (lenguaje C/Python):
     *   Sensor envía: {"sensor_id": "motor-1", "temperature": 45.3}
     *                    ↑ snake_case
     * 
     *   Java espera: {"sensorId": "motor-1", "temperature": 45.3}
     *                    ↑ camelCase
     * 
     * Soluciones:
     * 
     * 1. Sin @JsonProperty (confía en Jackson)
     *    Jackson automáticamente mapea sensor_id → sensorId
     *    (Funciona por defecto en Spring)
     * 
     * 2. Con @JsonProperty (explícito)
     *    @JsonProperty("sensor_id")
     *    private String sensorId;
     *    (Deja claro qué nombre JSON esperas)
     * 
     * En nuestro caso: Sensores IoT envían snake_case.
     * Usamos @JsonProperty para ser EXPLÍCITO sobre qué espera la API.
     * 
     * ¿Por qué ser explícito?
     * 
     * - Claridad: Otros developers ven qué JSON recibe la API
     * - Robustez: Si cambias nombre Java, JSON sigue igual
     * - Documentación: El código auto-documente
     * 
     * Validación: @NotBlank
     * Igual que en Entity.
     * El cliente DEBE enviar sensor_id con un valor no vacío.
     */
    @NotBlank(message = "sensorId is required and cannot be blank")
    @JsonProperty("sensor_id")
    private String sensorId;

    /**
     * TEMPERATURE - Temperatura en grados Celsius
     * 
     * Validaciones: @NotNull, @DecimalMin, @DecimalMax
     * Igual que en Entity.
     * 
     * ¿Por qué repetir validaciones?
     * 
     * Validación en MÚLTIPLES CAPAS (Defense in Depth):
     * 
     * 1. API (DTO) ← Primera línea
     *    Cliente envía: {"temperature": -50}
     *    DTO valida: RECHAZA inmediatamente con 400 BAD REQUEST
     *    Nunca llega al Service
     *    Muy eficiente: rechaza rápido
     * 
     * 2. Service (lógica de negocio)
     *    Si por alguna razón llega un valor inválido al Service
     *    (ej: acceso directo a BD, error en conversión DTO→Entity)
     *    El Service lo valida también
     * 
     * 3. Entity (BD)
     *    PostgreSQL CHECK CONSTRAINT hace tercera validación
     *    Si alguien inserta directamente en BD (CLI, otro app):
     *    BD rechaza
     * 
     * Tres capas = máxima seguridad.
     * 
     * Cost: validaciones duplicadas.
     * Benefit: integridad garantizada.
     * 
     * En la práctica:
     * - Casi siempre el DTO rechaza primero
     * - Entity/BD validaciones son "red de seguridad"
     */
    @NotNull(message = "temperature is required")
    @DecimalMin(value = "0", message = "temperature must be >= 0")
    @DecimalMax(value = "100", message = "temperature must be <= 100")
    private Double temperature;

    /**
     * VIBRATION - Nivel de vibración en mm/s
     * 
     * Validaciones: @NotNull, @DecimalMin, @DecimalMax
     * 
     * Rango: 0-10 mm/s
     * Interpretación:
     * - 0-2: Normal
     * - 2-5: Warning (posible fallo en próximas semanas)
     * - 5-10: Critical (fallo probable en horas)
     * - > 10: Error del sensor (rechazar)
     */
    @NotNull(message = "vibration is required")
    @DecimalMin(value = "0", message = "vibration must be >= 0")
    @DecimalMax(value = "10", message = "vibration must be <= 10 mm/s")
    private Double vibration;

    /**
     * CURRENT - Corriente eléctrica en amperios
     * 
     * Validaciones: @NotNull, @DecimalMin, @DecimalMax
     * 
     * Rango: 0-50 A
     * Interpretación:
     * - 0: Equipo apagado
     * - 1-40: Normal
     * - 40-50: Carga alta (monitorear)
     * - > 50: Sobrecarga (rechazar como error del sensor)
     */
    @NotNull(message = "current is required")
    @DecimalMin(value = "0", message = "current must be >= 0")
    @DecimalMax(value = "50", message = "current must be <= 50A")
    private Double current;

    /*
     * ════════════════════════════════════════════════════════════════════════
     * DISEÑO DE DTOs: ENTRADA vs SALIDA
     * ════════════════════════════════════════════════════════════════════════
     * 
     * En proyectos grandes se usan DTOs DIFERENTES para:
     * - Entrada (CreateSensorReadingDTO): lo que el cliente ENVÍA
     * - Salida (SensorReadingResponseDTO): lo que el servidor DEVUELVE
     * 
     * En nuestro caso: mismo DTO para entrada.
     * (Pero usaremos Entity como response)
     * 
     * Entrada (POST /api/sensors/data):
     *   Cliente envía:
     *   {
     *     "sensor_id": "motor-1",
     *     "temperature": 45.3,
     *     "vibration": 2.1,
     *     "current": 15.2
     *   }
     *   
     *   Jackson deserializa a:
     *   SensorReadingDTO(
     *     sensorId="motor-1",
     *     temperature=45.3,
     *     vibration=2.1,
     *     current=15.2
     *   )
     * 
     * Salida (Respuesta 201):
     *   Servidor responde con:
     *   {
     *     "id": 1,
     *     "sensorId": "motor-1",
     *     "temperature": 45.3,
     *     "vibration": 2.1,
     *     "current": 15.2,
     *     "timestamp": "2026-10-07T01:27:00",
     *     "status": "NORMAL"
     *   }
     *   
     *   (Jackson serializa Entity a JSON automáticamente)
     *   (El cliente ve más info en la respuesta que lo que envió)
     * 
     * ════════════════════════════════════════════════════════════════════════
     * CONVERSIÓN: DTO → Entity
     * ════════════════════════════════════════════════════════════════════════
     * 
     * ¿Cómo convertir SensorReadingDTO a SensorReading (Entity)?
     * 
     * En el Service:
     * 
     *   @Service
     *   public class SensorReadingService {
     *     
     *     public SensorReading createReading(SensorReadingDTO dto) {
     *       // Convertir DTO → Entity
     *       SensorReading entity = new SensorReading();
     *       entity.setSensorId(dto.getSensorId());
     *       entity.setTemperature(dto.getTemperature());
     *       entity.setVibration(dto.getVibration());
     *       entity.setCurrent(dto.getCurrent());
     *       // id y timestamp se asignan automáticamente
     *       
     *       // Calcular status
     *       String status = calculateStatus(dto);
     *       entity.setStatus(status);
     *       
     *       // Guardar en BD
     *       return repository.save(entity);
     *     }
     *     
     *     private String calculateStatus(SensorReadingDTO dto) {
     *       if (dto.getVibration() > 5) return "CRITICAL";
     *       if (dto.getVibration() > 2) return "WARNING";
     *       return "NORMAL";
     *     }
     *   }
     * 
     * Libraries para conversión automática:
     * - MapStruct (recomendado, rápido, type-safe)
     * - ModelMapper (más flexible, más lento)
     * - Manual (explícito, control total)
     * 
     * Por ahora: conversión manual (explícita y educativa).
     * 
     * ════════════════════════════════════════════════════════════════════════
     * VALIDACIÓN: @Valid en Controller
     * ════════════════════════════════════════════════════════════════════════
     * 
     * En el Controller:
     * 
     *   @RestController
     *   @PostMapping("/sensors/data")
     *   public ResponseEntity<SensorReading> createReading(
     *     @Valid @RequestBody SensorReadingDTO dto
     *   ) {
     *     // Spring automáticamente:
     *     // 1. Deserializa JSON a SensorReadingDTO
     *     // 2. Ejecuta @Valid → valida anotaciones (@NotNull, @DecimalMin, etc.)
     *     // 3. Si falla validación: retorna 400 BAD REQUEST
     *     // 4. Si pasa: llama al método
     *     
     *     SensorReading saved = sensorReadingService.createReading(dto);
     *     return ResponseEntity.created(URI).body(saved);
     *   }
     * 
     * Respuesta si validación falla:
     * 
     *   400 BAD REQUEST
     *   {
     *     "timestamp": "2026-10-07T01:27:00",
     *     "status": 400,
     *     "error": "Bad Request",
     *     "message": "Validation failed",
     *     "errors": {
     *       "temperature": "temperature must be >= 0",
     *       "vibration": "vibration is required"
     *     },
     *     "path": "/api/sensors/data"
     *   }
     * 
     * Cliente ve inmediatamente qué campos son inválidos.
     * Importante para debugging en IoT (sensores enviando datos malformados).
     */

}
