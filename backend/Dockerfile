# ════════════════════════════════════════════════════════════════════════
# Dockerfile del backend (Spring Boot 4 / Java 21) - MULTI-STAGE BUILD
# ════════════════════════════════════════════════════════════════════════
# Dos etapas en un solo archivo:
#   1) "build":   imagen con Maven + JDK para COMPILAR (pesada, ~500 MB)
#   2) "runtime": imagen mínima con solo el JRE y el .jar resultante (ligera)
# La imagen final no contiene Maven, ni el código fuente, ni el JDK completo:
# es más pequeña, más rápida de desplegar y expone menos superficie de ataque.

# ───────────── Etapa 1: compilar ─────────────
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copiamos SOLO el pom.xml primero y descargamos dependencias.
# Docker cachea cada instrucción como una "capa": mientras el pom.xml no cambie,
# esta capa (lenta) se reutiliza y no se vuelven a descargar las dependencias.
COPY pom.xml .
RUN mvn -q dependency:go-offline

# Ahora sí copiamos el código y empaquetamos el .jar.
# -DskipTests: los tests se ejecutan aparte (mvn test / CI), no al construir la imagen.
COPY src ./src
RUN mvn -q clean package -DskipTests

# ───────────── Etapa 2: ejecutar ─────────────
FROM eclipse-temurin:21-jre
WORKDIR /app

# Usuario SIN privilegios: si alguien explota la app, no es root dentro del contenedor.
RUN useradd --system --create-home appuser
USER appuser

# Copiamos únicamente el .jar generado en la etapa anterior.
COPY --from=build /app/target/*.jar app.jar

# Documenta el puerto en el que escucha la app (el mapeo real lo hace docker-compose).
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
