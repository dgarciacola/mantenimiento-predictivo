package com.predictivemaintenance.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║              SensorReading Entity - Modelo de Base de Datos             ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 * 
 * ¿Qué es una Entity?
 * 
 * Una Entity es la representación en Java de una TABLA en la base de datos.
 * Es el modelo persistente (los datos que se guardan para siempre en PostgreSQL).
 * 
 * Relación Entity ↔ BD:
 * 
 *   Java Entity                          SQL Table
 *   ─────────────────────────────────    ──────────────────────────────
 *   @Entity                              CREATE TABLE sensor_readings (
 *   public class SensorReading {           id BIGINT PRIMARY KEY,
 *     @Id                                  sensor_id VARCHAR(100) NOT NULL,
 *     private Long id;                     temperature DOUBLE PRECISION,
 *     
 *     private String sensorId;  ←────────→ sensor_id VARCHAR(100),
 *     private Double temperature; ←──────→ temperature DOUBLE PRECISION,
 *     private Double vibration;   ←──────→ vibration DOUBLE PRECISION,
 *     private Double current;     ←──────→ current DOUBLE PRECISION,
 *     private LocalDateTime timestamp; ←→ timestamp TIMESTAMP,
 *     private String status;      ←──────→ status VARCHAR(50)
 *   }                                    );
 * 
 * ¿Por qué Entity y no directamente SQL?
 * 
 * Ventajas de usar Entity (JPA/Hibernate):
 * 1. Código Java, no SQL → type-safe, refactoring fácil
 * 2. Independencia de BD → cambiar de PostgreSQL a MySQL sin código Java
 * 3. Automatic mapping → Hibernate traduce Java ↔ SQL automáticamente
 * 4. Lazy loading → cargar datos solo cuando se necesitan
 * 5. Query by Example → buscar sin escribir SQL crudo
 * 6. Transacciones automáticas → ACID garantizado
 * 
 * Desventajas (trade-offs):
 * - Overhead en queries complejas
 * - Curva de aprendizaje
 * - Performance puede ser peor si no se optimiza
 * 
 * Pero para 95% de casos, Entity es mejor que SQL crudo.
 */
@Entity
@Table(name = "sensor_readings", indexes = {
    @Index(name = "idx_sensor_timestamp", columnList = "sensor_id, timestamp DESC")
})
@Data                      // Lombok: genera @Getter, @Setter, @ToString, @EqualsAndHashCode
@NoArgsConstructor         // Lombok: genera constructor vacío
@AllArgsConstructor        // Lombok: genera constructor con todos los parámetros
public class SensorReading {

    /**
     * PRIMARY KEY - Identificador único de cada lectura
     * 
     * ¿Qué es @Id?
     * Anotación JPA que marca este campo como PRIMARY KEY (llave primaria).
     * Cada lectura tiene un ID único e irrepetible.
     * 
     * ¿Qué es @GeneratedValue(strategy = GenerationType.IDENTITY)?
     * 
     * Estrategia de generación de IDs:
     * 
     * 1. IDENTITY (aquí usamos esta)
     *    - La BD genera el ID automáticamente (AUTO INCREMENT en PostgreSQL)
     *    - Cuando haces INSERT, PostgreSQL asigna el siguiente ID
     *    - Ejemplo:
     *      INSERT INTO sensor_readings (sensor_id, temperature, ...) VALUES (...)
     *      → PostgreSQL asigna id=1
     *      INSERT ...
     *      → PostgreSQL asigna id=2
     * 
     * 2. SEQUENCE (alternativa mejor en PostgreSQL)
     *    @GeneratedValue(strategy = GenerationType.SEQUENCE, 
     *                    generator = "sensor_seq")
     *    @SequenceGenerator(name = "sensor_seq", sequenceName = "sensor_id_seq")
     *    - PostgreSQL crea una SEQUENCE (generador de números)
     *    - Más eficiente en batch inserts
     * 
     * 3. AUTO
     *    - Elige automáticamente la mejor estrategia
     *    - (No recomendado, mejor ser explícito)
     * 
     * 4. TABLE (legacy)
     *    - Genera IDs en una tabla separada (lento)
     * 
     * ¿Por qué IDENTITY es simple?
     * PostgreSQL lo maneja automáticamente.
     * No necesitas configurar nada extra.
     * Cons: En batch inserts es más lento que SEQUENCE.
     * 
     * Ejemplo en la BD:
     *   CREATE TABLE sensor_readings (
     *     id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
     *     ...
     *   );
     * 
     * El "GENERATED ALWAYS AS IDENTITY" significa que PostgreSQL
     * genera automáticamente el ID.
     * 
     * ¿Por qué Long (bigint) y no Integer (int)?
     * - Integer: 2^31 - 1 = 2.1 billones (se acaba rápido en IoT)
     * - Long: 2^63 - 1 = 9 trillones (suficiente para siempre)
     * 
     * Con 100 sensores × 1 lectura/segundo:
     * - 86,400 lecturas/día
     * - 31.5 millones/año
     * - Llegas a Long max en 285 años
     * - Integer max en 67 años
     * 
     * Así que Long es la elección segura.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * SENSOR ID - Identificador del sensor que envía la lectura
     * 
     * ¿Qué es @NotBlank?
     * Anotación de validación que garantiza:
     * - El valor no es null
     * - El valor no es una cadena vacía ""
     * - El valor no es solo espacios "   "
     * 
     * Válido:   "motor-line3-unit5", "sensor-1"
     * Inválido: null, "", "   "
     * 
     * ¿Por qué @NotBlank en lugar de @NotNull?
     * - @NotNull: permite "", "   " (solo rechaza null)
     * - @NotBlank: rechaza null, "", y espacios en blanco
     * 
     * En nuestro caso: sensor_id no puede estar vacío o ser solo espacios.
     * 
     * ¿Dónde se ejecuta la validación?
     * Dos lugares:
     * 1. API (Spring MVC) - si llega un @RequestBody con @Valid
     *    Retorna 400 BAD REQUEST con detalles
     * 2. Database constraints - si alguien intenta INSERT directamente en BD
     *    PostgreSQL rechaza: NOT NULL CONSTRAINT VIOLATED
     * 
     * Defensa en profundidad: validamos en ambos lados.
     * 
     * ¿Qué es @Column?
     * Mapeo explícito a columna en BD.
     * 
     * Atributos:
     * - name = "sensor_id" ← nombre en la BD (snake_case)
     * - nullable = false ← NOT NULL en la BD (rechaza nulls)
     * - length = 100 ← VARCHAR(100), máximo 100 caracteres
     * 
     * Sin @Column:
     * Hibernate asume:
     * - name = "sensorId" (usa el nombre del campo Java)
     * - nullable = true (permite nulls)
     * - length = 255 (por defecto)
     * 
     * Como queremos explicititud (una de las razones de usar Entity),
     * lo especificamos.
     * 
     * Ejemplos de sensorId:
     * - "motor-line3-unit5"
     * - "pump-area2-unit1"
     * - "conveyor-area1-unit3"
     * - "compressor-floor1-zone2"
     */
    @NotBlank(message = "Sensor ID cannot be blank")
    @Column(name = "sensor_id", nullable = false, length = 100)
    private String sensorId;

    /**
     * TEMPERATURE - Temperatura del equipo en grados Celsius
     * 
     * ¿Qué es @DecimalMin y @DecimalMax?
     * Validaciones numéricas para rangos.
     * 
     * @DecimalMin(value = "0")
     *   ↓
     * Rechaza: valores < 0
     * Acepta: 0, 0.1, 45.3, 100
     * Rechaza: -1, -0.5
     * 
     * @DecimalMax(value = "100")
     *   ↓
     * Rechaza: valores > 100
     * Acepta: 0, 45.3, 100
     * Rechaza: 100.1, 150
     * 
     * ¿Por qué "Decimal" en lugar de "Min"?
     * - @Min/@Max: solo para Integer/Long (números enteros)
     * - @DecimalMin/@DecimalMax: para Double/BigDecimal (decimales)
     * 
     * Como temperature es Double (45.3°C, no 45°C), usamos Decimal*.
     * 
     * ¿Por qué String "0" en lugar de número 0?
     * Razón histórica. La anotación espera String por compatibilidad
     * con arbitrarias expresiones. Pero usa value = "0", no value = 0.
     * 
     * ¿Qué es @NotNull?
     * Rechaza null, pero permite 0 y valores negativos.
     * Combinamos con @DecimalMin para rechazar valores fuera de rango.
     * 
     * Rango válido: 0°C a 100°C
     * Razón: equipos industriales típicamente operan en este rango.
     * Valores fuera indican:
     * - Sensor malfunction (< 0)
     * - Equipment failure (> 100)
     * 
     * @Column(name = "temperature")
     *   Mapea a columna "temperature" en BD
     *   Sin especificar length porque es DOUBLE PRECISION (variable)
     * 
     * ¿Por qué Double y no Float?
     * - Float: 7 dígitos significativos (45.30000 ≈ 8 bytes)
     * - Double: 15 dígitos significativos (45.3000000000000 ≈ 8 bytes)
     * 
     * A igualdad de memoria, Double es más preciso.
     * Industrial IoT requiere precisión.
     */
    @NotNull(message = "Temperature cannot be null")
    @DecimalMin(value = "0", message = "Temperature cannot be negative")
    @DecimalMax(value = "100", message = "Temperature cannot exceed 100°C")
    @Column(name = "temperature")
    private Double temperature;

    /**
     * VIBRATION - Nivel de vibración en mm/s (milímetros por segundo)
     * 
     * ¿Qué indica la vibración?
     * - 0-1 mm/s: Normal, sin problemas
     * - 1-2 mm/s: Ligeramente elevado, monitorear
     * - 2-5 mm/s: Alto, alerta temprana
     * - > 5 mm/s: Crítico, riesgo de fallo
     * - > 10 mm/s: Falla inminente, apagar equipo
     * 
     * Rango válido: 0-10 mm/s
     * Razón: Sensores industriales típicos miden hasta 10 mm/s
     * 
     * Si el sensor reporta > 10 mm/s: error del sensor, rechazar.
     * 
     * Unidad: mm/s (milímetros por segundo)
     * ISO 20816-3 es el estándar industrial para vibración.
     */
    @NotNull(message = "Vibration cannot be null")
    @DecimalMin(value = "0", message = "Vibration cannot be negative")
    @DecimalMax(value = "10", message = "Vibration cannot exceed 10 mm/s")
    @Column(name = "vibration")
    private Double vibration;

    /**
     * CURRENT - Corriente eléctrica en amperios (A)
     * 
     * ¿Qué indica la corriente?
     * - Baja: Equipo subutilizado o posible fallo mecánico
     * - Normal: Equipo operando en parámetros
     * - Alta: Equipo sometido a carga, posible sobrecalentamiento
     * - Muy alta: Fallo inminente, riesgo de quema
     * 
     * Rango válido: 0-50 A (amperios)
     * Razón: Equipos industriales típicos usan 0-50 A
     * 
     * Si la corriente es 0 A: equipo apagado o fallo total
     * Si la corriente es > 50 A: sobrecarga o error del sensor
     */
    @NotNull(message = "Current cannot be null")
    @DecimalMin(value = "0", message = "Current cannot be negative")
    @DecimalMax(value = "50", message = "Current cannot exceed 50A")
    @Column(name = "current")
    private Double current;

    /**
     * TIMESTAMP - Fecha y hora de la lectura
     * 
     * ¿Qué es LocalDateTime?
     * Clase Java para fechas + horas sin zona horaria.
     * Formato: 2026-10-07T01:23:45 (ISO 8601)
     * 
     * Alternativas:
     * - LocalDate: solo fecha (2026-10-07)
     * - LocalTime: solo hora (01:23:45)
     * - ZonedDateTime: con zona horaria (2026-10-07T01:23:45+02:00)
     * 
     * Elegimos LocalDateTime porque:
     * 1. Suficiente para IoT (zona horaria va en metadata, no en dato)
     * 2. Simple y performante
     * 3. Estándar en Java moderno
     * 
     * ¿Qué es @CreationTimestamp?
     * Anotación Hibernate que:
     * 1. Genera automáticamente la fecha/hora actual al insertar
     * 2. NO actualiza si editas el registro (immutable)
     * 3. Te ahorra: timestamp = LocalDateTime.now()
     * 
     * Flujo:
     *   sensorService.save(new SensorReading(...))
     *        ↓
     *   Hibernate ve @CreationTimestamp
     *        ↓
     *   Asigna LocalDateTime.now() automáticamente
     *        ↓
     *   Guarda en BD
     * 
     * En BD (PostgreSQL):
     *   Mapea a TIMESTAMP type
     * 
     * Ventaja de usar @CreationTimestamp:
     * - Timestamp confiable (genera el servidor, no el cliente)
     * - Cliente malicioso no puede falsificar hora
     * - Si cliente envía timestamp falso, se ignora
     * 
     * Ejemplo:
     *   POST /api/sensors/data
     *   {
     *     "sensorId": "motor-1",
     *     "temperature": 45.3,
     *     "timestamp": "2020-01-01T00:00:00"  ← ignorado
     *   }
     *   
     *   BD recibe:
     *   {
     *     "timestamp": "2026-10-07T01:23:45"  ← timestamp actual del servidor
     *   }
     * 
     * Esto es importante en auditoría y predicción.
     */
    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    /**
     * STATUS - Estado de la lectura (NORMAL, WARNING, CRITICAL)
     * 
     * ¿Qué es el status?
     * Clasificación calculada por la lógica de negocio:
     * 
     * NORMAL:
     * - Temperature: 0-100°C ✓
     * - Vibration: 0-2 mm/s ✓
     * - Current: 0-50 A ✓
     * Conclusión: Equipo opera correctamente
     * 
     * WARNING:
     * - Vibration: 2-5 mm/s ⚠️
     * - Temperature: cercano a 100°C (95-100) ⚠️
     * - Current: cercano a 50 A (40-50) ⚠️
     * Conclusión: Monitorear, posible fallo en 7-14 días
     * 
     * CRITICAL:
     * - Vibration: > 5 mm/s 🔴
     * - Temperature: > 100°C 🔴 (nunca debería pasar por validación)
     * - Current: > 50 A 🔴 (nunca debería pasar por validación)
     * Conclusión: Fallo inminente, apagar equipo ya
     * 
     * ¿Quién calcula el status?
     * El SensorReadingService.
     * 
     * El SensorController recibe los datos crudos.
     * El Service aplica lógica y calcula status.
     * 
     * Ejemplo:
     *   POST /api/sensors/data
     *   {
     *     "sensorId": "motor-1",
     *     "temperature": 45.3,
     *     "vibration": 3.2,  ← entre 2-5 mm/s
     *     "current": 15.2
     *   }
     *   
     *   Servidor:
     *   - Valida: ✓ todos en rango
     *   - Calcula: vibration=3.2 → WARNING
     *   - Guarda en BD:
     *     {
     *       "temperature": 45.3,
     *       "vibration": 3.2,
     *       "current": 15.2,
     *       "status": "WARNING"  ← calculado
     *     }
     * 
     * ¿Por qué guardarlo en BD?
     * Para auditoría y análisis histórico.
     * Puedes consultar: "¿cuántos WARNING hubo en octubre?"
     * 
     * ¿Por qué VARCHAR(50)?
     * Los nombres más largos son "CRITICAL" (8 caracteres).
     * Dejamos 50 para posibles futuros estados.
     */
    @Column(name = "status", length = 50)
    private String status;

    /*
     * ════════════════════════════════════════════════════════════════════════
     * ANOTACIONES LOMBOK (en la clase, arriba)
     * ════════════════════════════════════════════════════════════════════════
     * 
     * @Data
     *   ↓
     *   Genera automáticamente:
     *   - Getters: getTemperature(), getSensorId(), etc.
     *   - Setters: setTemperature(), setSensorId(), etc.
     *   - toString(): "SensorReading(id=1, sensorId=sensor-1, ...)"
     *   - equals(Object o): compara todos los campos
     *   - hashCode(): para usar en HashMap/HashSet
     * 
     * @NoArgsConstructor
     *   ↓
     *   Genera constructor vacío:
     *   public SensorReading() {}
     *   
     *   ¿Por qué lo necesita JPA/Hibernate?
     *   Cuando Hibernate recupera datos de BD, crea objetos así:
     *   1. new SensorReading()  ← constructor vacío
     *   2. setId(1L)
     *   3. setSensorId("motor-1")
     *   4. setTemperature(45.3)
     *   ... etc
     *   
     *   Si no hay constructor vacío, Hibernate falla.
     * 
     * @AllArgsConstructor
     *   ↓
     *   Genera constructor con todos los campos:
     *   public SensorReading(Long id, String sensorId, Double temperature,
     *                        Double vibration, Double current,
     *                        LocalDateTime timestamp, String status) {
     *     this.id = id;
     *     this.sensorId = sensorId;
     *     ...
     *   }
     *   
     *   ¿Para qué sirve?
     *   Para tests y creación manual de objetos:
     *   
     *   SensorReading sr = new SensorReading(
     *     null,  // id (null = auto-generate)
     *     "sensor-1",
     *     45.3,
     *     2.1,
     *     15.2,
     *     LocalDateTime.now(),
     *     "NORMAL"
     *   );
     * 
     * ════════════════════════════════════════════════════════════════════════
     * @Entity y @Table
     * ════════════════════════════════════════════════════════════════════════
     * 
     * @Entity
     *   Indica: "Esta clase es una Entity de JPA"
     *   Hibernate la registra y crea mappings automáticos.
     * 
     * @Table(name = "sensor_readings", indexes = {...})
     *   name: Nombre de la tabla en PostgreSQL
     *   indexes: Índices para optimizar queries
     *   
     *   Index: @Index(name = "idx_sensor_timestamp", columnList = "sensor_id, timestamp DESC")
     *   
     *   ¿Qué es un índice?
     *   Estructura de datos que acelera búsquedas.
     *   
     *   Sin índice:
     *   SELECT * FROM sensor_readings WHERE sensor_id = 'motor-1' ORDER BY timestamp DESC
     *   → PostgreSQL hace full table scan (100 ms, lento)
     *   
     *   Con índice:
     *   SELECT * FROM sensor_readings WHERE sensor_id = 'motor-1' ORDER BY timestamp DESC
     *   → PostgreSQL usa índice (5 ms, 20x más rápido)
     *   
     *   Trade-off:
     *   - Ventaja: queries más rápidas
     *   - Desventaja: inserts son más lentos (actualizar índice)
     *   
     *   Por qué (sensor_id, timestamp DESC)?
     *   Porque es la query más común: "dame las últimas lecturas de este sensor"
     *   
     *   SELECT * FROM sensor_readings
     *   WHERE sensor_id = ?
     *   ORDER BY timestamp DESC
     *   LIMIT 100
     * 
     * ════════════════════════════════════════════════════════════════════════
     * CICLO DE VIDA DE UNA ENTITY EN JPA
     * ════════════════════════════════════════════════════════════════════════
     * 
     * Estados de una Entity:
     * 
     * 1. TRANSIENT (nuevo, no persistido)
     *    SensorReading sr = new SensorReading();
     *    // sr no existe en BD, Hibernate no lo conoce
     * 
     * 2. MANAGED (persistido, asociado con sesión)
     *    repository.save(sr);
     *    // sr ahora existe en BD, Hibernate lo trackea
     *    sr.setTemperature(50.0);  ← cambio
     *    // Hibernate ve el cambio automáticamente
     * 
     * 3. DETACHED (fue persistido, pero sesión cerrada)
     *    // Sesión cierra (transacción termina)
     *    // sr sigue en memoria pero desconectado de BD
     *    sr.setTemperature(60.0);  ← cambio ignorado
     *    // Hibernate NO ve este cambio (no está en sesión)
     * 
     * 4. REMOVED (marcado para eliminar)
     *    repository.delete(sr);
     *    // sr será eliminado de BD en próximo flush
     * 
     * Automáticamente, Hibernate sincroniza:
     *   @Transactional  ← anotación Spring
     *   void updateTemperature() {
     *     SensorReading sr = repository.findById(1L).get();  // MANAGED
     *     sr.setTemperature(55.0);
     *     // NO llamas a save() explícitamente
     *     // al salir del método, Spring hace flush()
     *     // y la BD se actualiza automáticamente
     *   }
     * 
     * Esta es la "magia" de JPA.
     */

}
