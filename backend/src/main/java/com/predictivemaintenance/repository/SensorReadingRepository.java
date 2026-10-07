package com.predictivemaintenance.repository;

import com.predictivemaintenance.entity.SensorReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║              SensorReadingRepository - Data Access Layer                ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 * 
 * ¿Qué es un Repository?
 * 
 * Es la INTERFAZ entre tu lógica de negocio (Service) y la base de datos.
 * Abstrae las queries SQL y proporciona métodos Java limpios.
 * 
 * Patrón Repository:
 *   Service → Repository → Database
 *            (interfaz)
 * 
 * Ventaja: Service no sabe SQL, no sabe qué BD se usa.
 * Solo conoce la interfaz Repository.
 * 
 * ¿Cómo funciona Spring Data JPA?
 * 
 * 1. Defines una interfaz que extiende JpaRepository
 * 2. Spring Data crea automáticamente la implementación en tiempo de ejecución
 * 3. Los métodos se traduce a SQL automáticamente
 * 
 * Ejemplo:
 *   public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {
 *     List<SensorReading> findBySensorId(String sensorId);
 *   }
 * 
 * Spring Data genera automáticamente:
 *   class SensorReadingRepositoryImpl implements SensorReadingRepository {
 *     public List<SensorReading> findBySensorId(String sensorId) {
 *       return em.createQuery(
 *         "SELECT sr FROM SensorReading sr WHERE sr.sensorId = ?1"
 *       ).getResultList();
 *     }
 *   }
 * 
 * "Magia" de Spring Data: el nombre del método se traduce a SQL.
 * 
 * Naming conventions:
 *   findBySensorId(String sensorId)
 *   └─ SELECT * FROM sensor_readings WHERE sensor_id = ?
 * 
 *   findBySensorIdOrderByTimestampDesc(String sensorId)
 *   └─ SELECT * FROM sensor_readings WHERE sensor_id = ? ORDER BY timestamp DESC
 * 
 *   findByTemperatureGreaterThan(Double temp)
 *   └─ SELECT * FROM sensor_readings WHERE temperature > ?
 * 
 * Esto es "Derived Query" - queries derivadas del nombre del método.
 * 
 * ════════════════════════════════════════════════════════════════════════
 * JpaRepository - Herencia automática
 * ════════════════════════════════════════════════════════════════════════
 * 
 * Al extender JpaRepository<SensorReading, Long>, heredas estos métodos:
 * 
 * Lectura:
 * - findAll() → SELECT * FROM sensor_readings
 * - findById(Long id) → SELECT * FROM sensor_readings WHERE id = ?
 * - findAll(Pageable) → SELECT * con paginación y sorting
 * - existsById(Long id) → SELECT COUNT(*) > 0
 * - count() → SELECT COUNT(*)
 * 
 * Escritura:
 * - save(SensorReading) → INSERT or UPDATE
 * - saveAll(List<SensorReading>) → INSERT/UPDATE múltiples
 * - delete(SensorReading) → DELETE (marca para eliminar)
 * - deleteById(Long id) → DELETE WHERE id = ?
 * - deleteAll() → DELETE * (¡cuidado!)
 * 
 * ¿Qué significa save()?
 * 
 * INSERT si:
 *   - Entity.id = null
 *   - Entity es TRANSIENT (no está en BD)
 * 
 * UPDATE si:
 *   - Entity.id != null
 *   - Entity está MANAGED (ya existe en BD)
 * 
 * Ejemplo:
 *   // INSERT
 *   SensorReading sr = new SensorReading();
 *   sr.setSensorId("motor-1");
 *   sr.setTemperature(45.3);
 *   repository.save(sr);  // INSERT porque id = null
 *   // Después de save(): sr.id = 1 (asignado por BD)
 *   
 *   // UPDATE
 *   sr.setTemperature(50.0);
 *   repository.save(sr);  // UPDATE porque id != null
 * 
 * ════════════════════════════════════════════════════════════════════════
 * Parámetros genéricos
 * ════════════════════════════════════════════════════════════════════════
 * 
 * JpaRepository<SensorReading, Long>
 *                 ↑              ↑
 *                 Entity         Primary Key Type
 * 
 * SensorReading: la Entity con la que trabajas
 * Long: tipo de @Id (en nuestro caso BIGINT en BD)
 * 
 * Si @Id fuera Integer:
 *   JpaRepository<SensorReading, Integer>
 * 
 * Si @Id fuera String (UUID):
 *   JpaRepository<SensorReading, String>
 */
@Repository
public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {

    /**
     * Encuentra todas las lecturas de un sensor específico, ordenadas por timestamp descendente
     * 
     * Nombre del método → SQL generada automáticamente:
     * findBySensorIdOrderByTimestampDesc
     * └─ find: SELECT
     * └─ BySensorId: WHERE sensor_id = ?
     * └─ OrderByTimestampDesc: ORDER BY timestamp DESC
     * 
     * SQL equivalente:
     *   SELECT * FROM sensor_readings 
     *   WHERE sensor_id = ?
     *   ORDER BY timestamp DESC
     * 
     * ¿Por qué ORDER BY DESC?
     * Porque queremos las ÚLTIMAS lecturas primero (más recientes).
     * Útil para dashboards que muestran "últimas 10 lecturas".
     * 
     * Uso en Service:
     *   List<SensorReading> latestReadings = repository.findBySensorIdOrderByTimestampDesc("motor-1");
     *   // latestReadings[0] = la más reciente
     *   // latestReadings[1] = la anterior
     * 
     * Performance:
     * Recuerda que en pom.xml definimos un INDEX:
     *   @Index(name = "idx_sensor_timestamp", columnList = "sensor_id, timestamp DESC")
     * 
     * Este índice hace que esta query sea rápida incluso con millones de lecturas.
     * Sin índice: 1000 ms (full table scan)
     * Con índice: 5 ms (index lookup)
     * 
     * @param sensorId El ID del sensor
     * @return Lista de lecturas del sensor, ordenadas descendente por timestamp
     */
    List<SensorReading> findBySensorIdOrderByTimestampDesc(String sensorId);

    /**
     * Encuentra lecturas de un sensor en un rango de tiempo específico
     * 
     * ¿Qué es @Query?
     * 
     * Anotación que permite escribir una query JPQL (Java Persistence Query Language)
     * manualmente en lugar de derivarla del nombre del método.
     * 
     * JPQL es similar a SQL pero usa Entity names, no table names:
     *   SQL:   SELECT * FROM sensor_readings WHERE sensor_id = ?
     *   JPQL:  SELECT sr FROM SensorReading sr WHERE sr.sensorId = ?
     * 
     * ¿Por qué @Query en lugar de Derived Query?
     * 
     * Porque el nombre del método sería muy largo:
     *   findBySensorIdAndTimestampBetweenOrderByTimestampDesc
     *                     ↑ "Between" no existe en SQL, hay que usar >= y <=
     * 
     * Con @Query:
     *   @Query("SELECT sr FROM SensorReading sr WHERE sr.sensorId = :sensorId AND sr.timestamp BETWEEN :start AND :end ORDER BY sr.timestamp DESC")
     *   List<SensorReading> findByTimeRange(@Param("sensorId") String sensorId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
     * 
     * Es más legible y claro.
     * 
     * ¿Qué es @Param?
     * 
     * Anotación que mapea parámetros Java a placeholders en JPQL.
     * 
     * Sin @Param:
     *   @Query("SELECT sr FROM SensorReading sr WHERE sr.sensorId = ? AND sr.timestamp BETWEEN ? AND ?")
     *   // No sabes cuál ? es cuál (confuso)
     * 
     * Con @Param:
     *   @Query("SELECT sr FROM SensorReading sr WHERE sr.sensorId = :sensorId AND sr.timestamp BETWEEN :start AND :end")
     *   List<SensorReading> findByTimeRange(
     *     @Param("sensorId") String sensorId,
     *     @Param("start") LocalDateTime start,
     *     @Param("end") LocalDateTime end
     *   )
     *   // Claro qué parámetro mapea a qué placeholder
     * 
     * Ejemplo de uso en API:
     *   GET /api/sensors/motor-1/history/time-range?startTime=2026-10-06T00:00:00&endTime=2026-10-06T23:59:59
     * 
     * El Controller extrae estos parámetros:
     *   LocalDateTime start = LocalDateTime.parse(startTime);
     *   LocalDateTime end = LocalDateTime.parse(endTime);
     *   List<SensorReading> results = repository.findByTimeRange("motor-1", start, end);
     * 
     * Casos de uso:
     * - "Dame todas las lecturas de hoy"
     * - "Dame lecturas de la última hora"
     * - "Dame lecturas del mes pasado para análisis"
     * 
     * @param sensorId Identificador del sensor
     * @param startTime Fecha/hora de inicio (inclusive)
     * @param endTime Fecha/hora de fin (inclusive)
     * @return Lecturas en el rango especificado, más recientes primero
     */
    @Query("SELECT sr FROM SensorReading sr WHERE sr.sensorId = :sensorId AND sr.timestamp BETWEEN :startTime AND :endTime ORDER BY sr.timestamp DESC")
    List<SensorReading> findByTimeRange(
        @Param("sensorId") String sensorId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * Encuentra lecturas con estado crítico
     * 
     * Derived Query que busca un campo de status específico.
     * 
     * Nombres de derivadas:
     * - findByStatus("CRITICAL")
     * - findByStatusAndSensorId("CRITICAL", "motor-1")
     * - findByStatusOrderByTimestampDesc("CRITICAL")
     * 
     * SQL generada:
     *   SELECT * FROM sensor_readings WHERE status = 'CRITICAL' ORDER BY timestamp DESC
     * 
     * Caso de uso:
     * - Dashboard que muestra "Equipos en estado crítico"
     * - Alertas automáticas
     * - Notificaciones a operadores
     * 
     * Ejemplo en Service:
     *   List<SensorReading> criticalReadings = repository.findByStatusOrderByTimestampDesc("CRITICAL");
     *   if (!criticalReadings.isEmpty()) {
     *     sendAlert("CRÍTICO: " + criticalReadings.size() + " equipos en fallo");
     *   }
     * 
     * @param status El estado a buscar (típicamente "CRITICAL")
     * @return Lecturas con ese status, ordenadas por timestamp descendente
     */
    List<SensorReading> findByStatusOrderByTimestampDesc(String status);

    /**
     * Encuentra la lectura más reciente de un sensor
     * 
     * ¿Qué es Optional?
     * 
     * Clase Java que representa "un valor que puede o no existir".
     * 
     * Problema sin Optional:
     *   public SensorReading findLatestBySensorId(String sensorId) { ... }
     *   
     *   SensorReading sr = repository.findLatestBySensorId("inexistent-sensor");
     *   if (sr == null) {  // ← NullPointerException risk
     *     // manejar no encontrado
     *   }
     * 
     * Solución con Optional:
     *   public Optional<SensorReading> findLatestBySensorId(String sensorId) { ... }
     *   
     *   Optional<SensorReading> sr = repository.findLatestBySensorId("inexistent-sensor");
     *   if (sr.isPresent()) {
     *     // usar sr.get()
     *   } else {
     *     // manejar no encontrado
     *   }
     *   
     *   O con Java 8+:
     *   repository.findLatestBySensorId("motor-1")
     *     .ifPresentOrElse(
     *       sr -> logger.info("Última lectura: " + sr.getTemperature()),
     *       () -> logger.warn("Sensor sin lecturas")
     *     );
     * 
     * Ventaja: Fuerza al programador a manejar el caso de "no encontrado".
     * Es más seguro que nullable.
     * 
     * JPQL con LIMIT 1:
     * "SELECT sr FROM SensorReading sr WHERE sr.sensorId = :sensorId ORDER BY sr.timestamp DESC"
     * 
     * Con nativeQuery = false (JPQL, recomendado):
     * - Portable (funciona en PostgreSQL, MySQL, Oracle, etc.)
     * - Type-safe (Hibernate verifica sintaxis)
     * 
     * Con nativeQuery = true (SQL puro):
     * - Más control pero menos portabilidad
     * - No recomendado a menos que necesites features específicas de BD
     * 
     * ¿Por qué LIMIT 1?
     * Optimización: Postgres se detiene después de encontrar la primera.
     * Sin LIMIT: traería TODAS las lecturas y Java elegiría la primera (lento).
     * 
     * Pero JPQL no tiene LIMIT estándar.
     * Hibernate lo traduce automáticamente:
     *   JPQL: ORDER BY sr.timestamp DESC (sin LIMIT)
     *   SQL: ORDER BY timestamp DESC LIMIT 1
     * 
     * En realidad, Spring Data en el repository se encarga de esto internamente.
     * 
     * Caso de uso:
     * - "¿Cuál es la temperatura actual del motor?"
     * - "¿Cuál fue la última lectura de este sensor?"
     * - Dashboard que muestra "Estado actual"
     * 
     * @param sensorId Identificador del sensor
     * @return Optional<SensorReading> (presente si hay lectura, vacío si no hay)
     */
    @Query(value = "SELECT sr FROM SensorReading sr WHERE sr.sensorId = :sensorId ORDER BY sr.timestamp DESC LIMIT 1", nativeQuery = false)
    Optional<SensorReading> findLatestBySensorId(@Param("sensorId") String sensorId);

    /**
     * Cuenta cuántas lecturas hay de un sensor específico
     * 
     * Derived Query: countBySensorId
     * └─ count: SELECT COUNT(*)
     * └─ BySensorId: WHERE sensor_id = ?
     * 
     * SQL equivalente:
     *   SELECT COUNT(*) FROM sensor_readings WHERE sensor_id = ?
     * 
     * Retorna un Long (número de filas).
     * 
     * Caso de uso:
     * - "¿Cuántas lecturas tenemos del sensor motor-1?" → 15,342
     * - "¿Este sensor está activo?" → if (count > 0) YES else NO
     * - Auditoría: "¿Cuántas lecturas recibimos hoy?" → count
     * 
     * Ejemplo en Service:
     *   Long totalReadings = repository.countBySensorId("motor-1");
     *   logger.info("Total lecturas de motor-1: " + totalReadings);
     * 
     * @param sensorId Identificador del sensor
     * @return Número de lecturas de ese sensor
     */
    Long countBySensorId(String sensorId);

    /*
     * ════════════════════════════════════════════════════════════════════════
     * PATRONES COMUNES EN REPOSITORIES
     * ════════════════════════════════════════════════════════════════════════
     * 
     * 1. BUSCAR UNO
     *    Optional<Entity> findById(ID id)  // Heredado
     *    Optional<Entity> findByEmail(String email)  // Derivada
     * 
     * 2. BUSCAR MUCHOS
     *    List<Entity> findAll()  // Heredado
     *    List<Entity> findBySensorId(String sensorId)  // Derivada
     * 
     * 3. BUSCAR CON CONDICIONES
     *    List<Entity> findByStatusAndSensorId(String status, String sensorId)
     *    List<Entity> findByTemperatureGreaterThan(Double temp)
     *    List<Entity> findByTimestampBetween(LocalDateTime start, LocalDateTime end)
     * 
     * 4. CONTAR
     *    Long count()  // Heredado
     *    Long countByStatus(String status)  // Derivada
     * 
     * 5. EXISTENCIA
     *    boolean existsById(ID id)  // Heredado
     *    boolean existsBySensorId(String sensorId)  // Derivada
     * 
     * 6. ELIMINAR
     *    void delete(Entity)  // Heredado
     *    void deleteById(ID id)  // Heredado
     *    Long deleteBySensorId(String sensorId)  // Derivada (elimina todos)
     * 
     * 7. ACTUALIZAR CON QUERY
     *    @Modifying  // Indica que modifica datos
     *    @Query("UPDATE SensorReading sr SET sr.status = 'ARCHIVED' WHERE sr.sensorId = :sensorId")
     *    int updateStatusBySensorId(@Param("sensorId") String sensorId);
     * 
     * ════════════════════════════════════════════════════════════════════════
     * KEYWORDS EN DERIVED QUERIES
     * ════════════════════════════════════════════════════════════════════════
     * 
     * And          findByTemperatureAndVibration(...)
     * Or           findByStatusOrSensorId(...)
     * Between      findByTimestampBetween(...)
     * LessThan     findByTemperatureLessThan(...)
     * GreaterThan  findByTemperatureGreaterThan(...)
     * Like         findBySensorIdLike(...)
     * OrderBy      findByStatusOrderByTimestampDesc(...)
     * Not          findByStatusNot(...)
     * In           findByStatusIn(List<String> statuses)
     * IsNull       findByTimestampIsNull()
     * 
     * ════════════════════════════════════════════════════════════════════════
     * PERFORMANCE - CONSULTAS COSTOSAS
     * ════════════════════════════════════════════════════════════════════════
     * 
     * Consulta lenta (sin índice):
     *   SELECT * FROM sensor_readings WHERE status = 'CRITICAL'
     *   → Full table scan si 100M de filas
     * 
     * Solución:
     *   CREATE INDEX idx_status ON sensor_readings(status);
     *   
     * En Entity, se define en @Table:
     *   @Table(indexes = {
     *     @Index(name = "idx_status", columnList = "status"),
     *     @Index(name = "idx_sensor_timestamp", columnList = "sensor_id, timestamp DESC")
     *   })
     * 
     * Hibernate crear estos índices automáticamente si usas:
     *   spring.jpa.hibernate.ddl-auto=create-drop
     *   (solo para desarrollo, no producción)
     */


    /** IDs de sensores distintos (para /api/sensors/list). */
    @Query("SELECT DISTINCT sr.sensorId FROM SensorReading sr ORDER BY sr.sensorId")
    List<String> findAllDistinctSensorIds();
}
