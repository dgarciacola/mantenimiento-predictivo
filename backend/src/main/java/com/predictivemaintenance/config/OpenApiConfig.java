package com.predictivemaintenance.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenApiConfig - Cabecera de la documentación Swagger / OpenAPI.
 *
 * ¿QUÉ ES OpenAPI?
 * ================
 * Un estándar para describir una API REST en JSON: endpoints, parámetros,
 * respuestas y códigos HTTP. springdoc lo genera AUTOMÁTICAMENTE leyendo
 * los controladores, DTOs y anotaciones de validación.
 *
 * ¿DÓNDE LO VEO?
 * ==============
 *   Swagger UI (interfaz para probar endpoints): http://localhost:8080/swagger-ui.html
 *   JSON OpenAPI (crudo):                        http://localhost:8080/v3/api-docs
 *   (las rutas se definen en application.yml)
 *
 * ANOTACIONES USADAS
 * ==================
 * @Configuration: indica a Spring que esta clase define beans (objetos gestionados por Spring).
 * @Bean:          registra el objeto que devuelve el método. springdoc busca un bean
 *                 OpenAPI y, si existe, lo usa como cabecera de la documentación.
 *
 * Solo afecta a la DOCUMENTACIÓN: no cambia el comportamiento de la API.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI predictiveMaintenanceOpenAPI() {
        // Estilo "fluido": cada método devuelve el mismo objeto para encadenar llamadas
        return new OpenAPI().info(new Info()
                .title("Predictive Maintenance API")
                .version("0.1.0")
                .description("Ingesta de datos IoT, histórico de sensores y alertas críticas "
                        + "para mantenimiento predictivo industrial"));
    }
}
