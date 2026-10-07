package com.predictivemaintenance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║          Predictive Maintenance - Backend Application Entry Point       ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 * 
 * Esta es la clase PRINCIPAL de tu aplicación Spring Boot.
 * 
 * ¿Qué pasa cuando ejecutas "java -jar backend-0.1.0-SNAPSHOT.jar"?
 * 
 * 1. La JVM (Java Virtual Machine) busca el Main-Class en el MANIFEST.MF
 * 2. El MANIFEST apunta a JarLauncher (del Spring Boot plugin)
 * 3. JarLauncher busca Main-Class en tus clases
 * 4. Encuentra esta clase: PredictiveMaintenanceApplication
 * 5. Ejecuta el método main()
 * 6. main() llama a SpringApplication.run(...)
 * 7. Spring Boot inicializa todo: Tomcat, BD, Redis, inyección de dependencias, etc.
 * 
 * ¿Por qué se llama "Entry Point"?
 * Es el punto de ENTRADA de tu aplicación. Como el puerta de un edificio:
 * - Sin puerta: nadie puede entrar
 * - Sin Entry Point: la JVM no sabe dónde empezar
 */
@SpringBootApplication
public class PredictiveMaintenanceApplication {

    /**
     * Método main() - El punto de entrada de TODA aplicación Java
     * 
     * Firma: public static void main(String[] args)
     * 
     * ¿Por qué "public"?
     * La JVM necesita acceder desde FUERA de la clase
     * 
     * ¿Por qué "static"?
     * El método debe existir SIN crear una instancia de la clase.
     * La JVM no puede hacer "new PredictiveMaintenanceApplication()"
     * porque podría fallar el constructor. Así que usa static.
     * 
     * ¿Por qué "void"?
     * No devuelve nada. Exit code 0 = éxito, cualquier excepción = fallo.
     * 
     * ¿Por qué "String[] args"?
     * Argumentos pasados desde la línea de comandos.
     * Ejemplo:
     *   java -jar backend.jar --server.port=9090
     *   Los argumentos llegan en: args = ["--server.port=9090"]
     * 
     * @param args Argumentos de línea de comandos
     *             Ejemplos:
     *             - --server.port=9090 (cambiar puerto)
     *             - --spring.profiles.active=production (activar profile)
     *             - --logging.level.root=DEBUG (cambiar log level)
     */
    public static void main(String[] args) {
        /*
         * SpringApplication.run() es la MAGIA.
         * 
         * ¿Qué hace?
         * 
         * 1. CREA una instancia de Spring Context
         *    (contenedor donde viven todos tus beans)
         * 
         * 2. ESCANEA el classpath buscando:
         *    - @Component, @Service, @Repository, @Controller
         *    - @Configuration, @Bean
         *    - Cualquier clase anotada que Spring deba conocer
         * 
         * 3. EJECUTA la auto-configuración (Auto-Configuration)
         *    Spring Boot mira tus dependencias y configura automáticamente:
         *    - DataSource (conexión a PostgreSQL)
         *    - JpaRepository (Hibernate)
         *    - RedisTemplate (cache Redis)
         *    - DispatcherServlet (Spring MVC)
         *    - Tomcat (servidor embebido)
         *    - Etc.
         * 
         * 4. INYECTA dependencias
         *    Si una clase necesita otra, Spring la busca y la pasa.
         *    Ejemplo:
         *      @Service
         *      class SensorReadingService {
         *        @Autowired
         *        private SensorReadingRepository repo;
         *        // Spring automáticamente busca SensorReadingRepository
         *        // la crea si no existe
         *        // la pasa aquí
         *      }
         * 
         * 5. INICIA Tomcat en el puerto 8080
         *    Ahora tu API está viva: http://localhost:8080
         * 
         * 6. REGISTRA un shutdown hook
         *    Si presionas Ctrl+C, Spring cierra gracefully:
         *    - Termina conexiones activas
         *    - Cierra base de datos
         *    - Guarda cache
         *    - Sale limpiamente (sin corrupt data)
         * 
         * Todo esto en UNA línea de código.
         * 
         * Parámetros:
         * - PredictiveMaintenanceApplication.class
         *   La clase donde se ejecuta SpringApplication.run()
         *   Spring usa esto para saber dónde buscar @ComponentScan
         * 
         * - args
         *   Argumentos de línea de comandos que pasas a Spring
         *   Spring los convierte en propiedades:
         *   --server.port=9090 → spring.boot.server.port = 9090
         */
        SpringApplication.run(PredictiveMaintenanceApplication.class, args);
    }

    /*
     * ════════════════════════════════════════════════════════════════════════
     * ANOTACIÓN: @SpringBootApplication
     * ════════════════════════════════════════════════════════════════════════
     * 
     * ¿Qué es @SpringBootApplication?
     * 
     * Es una MEGA-ANOTACIÓN que combina tres anotaciones:
     * 
     * 1. @SpringBootConfiguration
     *    Dice: "Esta clase es una configuración Spring Boot"
     *    (Puede usar @Bean, @PropertySource, etc.)
     * 
     * 2. @EnableAutoConfiguration
     *    Dice: "Haz la auto-configuración mágica"
     *    (Spring Boot mira dependencias y configura automáticamente)
     * 
     * 3. @ComponentScan
     *    Dice: "Busca @Component, @Service, @Repository en este paquete
     *    y sus sub-paquetes"
     * 
     * Equivalente sin @SpringBootApplication:
     * 
     *   @SpringBootConfiguration
     *   @EnableAutoConfiguration
     *   @ComponentScan(basePackages = "com.predictivemaintenance")
     *   public class PredictiveMaintenanceApplication { ... }
     * 
     * Pero @SpringBootApplication es más limpio y es el estándar.
     * 
     * ¿Qué significa el paquete "com.predictivemaintenance"?
     * 
     * @ComponentScan busca en:
     * - com.predictivemaintenance
     * - com.predictivemaintenance.controller
     * - com.predictivemaintenance.service
     * - com.predictivemaintenance.repository
     * - ... todos los sub-paquetes
     * 
     * Pero NO busca en:
     * - org.springframework... (librerías de Spring)
     * - com.postgresql...    (librerías de PostgreSQL)
     * - ... otras librerías
     * 
     * Razón: Escanear TODO el classpath sería lento.
     * Spring asume que tu código está bajo com.predictivemaintenance.
     * 
     * Estructura esperada:
     * backend/src/main/java/
     *   └─ com/predictivemaintenance/
     *      ├─ PredictiveMaintenanceApplication.java  ← Estás aquí
     *      ├─ controller/
     *      │  └─ SensorController.java
     *      ├─ service/
     *      │  └─ SensorReadingService.java
     *      ├─ repository/
     *      │  └─ SensorReadingRepository.java
     *      ├─ entity/
     *      │  └─ SensorReading.java
     *      ├─ dto/
     *      │  └─ SensorReadingDTO.java
     *      └─ config/
     *         └─ RedisConfig.java
     * 
     * Spring escanea TODO esto y crea beans automáticamente.
     */

}
