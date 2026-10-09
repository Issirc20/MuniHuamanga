# ==============================================================================
# Dockerfile Multi-Stage — Sistema de Gestión Digital de Licencias MuniHuamanga
# Versión: v0.9.0-rc1
# Arquitectura: Multi-module Maven (common-domain + servicio-expedientes)
# Seguridad: Eclipse Temurin 21 JRE, Usuario no-root (UID 10001), Healthcheck
# ==============================================================================

# ------------------------------------------------------------------------------
# ETAPA 1: Construcción y Empaquetado (Build Stage)
# ------------------------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copiar descriptores de dependencias para aprovechar la caché de capas de Docker
COPY pom.xml .
COPY common-domain/pom.xml common-domain/
COPY servicio-expedientes/pom.xml servicio-expedientes/

# Descargar dependencias en capa cacheable
RUN mvn dependency:go-offline -B || true

# Copiar el código fuente completo de ambos módulos
COPY common-domain/src common-domain/src
COPY servicio-expedientes/src servicio-expedientes/src

# Compilar y empaquetar omitiendo pruebas (las pruebas se ejecutan en CI)
RUN mvn clean package -DskipTests -B

# ------------------------------------------------------------------------------
# ETAPA 2: Imagen de Ejecución Ligera y Segura (Runtime Stage)
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runner

LABEL maintainer="Equipo de Arquitectura MuniHuamanga <sistemas@munihuamanga.gob.pe>"
LABEL version="0.9.0-rc1"
LABEL description="Monolito Modular de Gestión de Licencias de Funcionamiento - Municipalidad de Huamanga"

# Instalar utilitarios mínimos necesarios (wget/curl para healthcheck, tzdata para zona horaria)
RUN apk add --no-cache tzdata wget ca-certificates \
    && cp /usr/share/zoneinfo/America/Lima /etc/localtime \
    && echo "America/Lima" > /etc/timezone

# Crear usuario y grupo de sistema sin privilegios de root (CWE-250)
RUN addgroup -g 10001 -S appgroup && \
    adduser -u 10001 -S appuser -G appgroup

WORKDIR /app

# Copiar el artefacto compilado desde la etapa de construcción
COPY --from=builder --chown=appuser:appgroup /build/servicio-expedientes/target/servicio-expedientes-*.jar app.jar

# Asignar permisos seguros
RUN chown -R appuser:appgroup /app && \
    chmod 500 app.jar

# Cambiar al usuario no-root
USER appuser:appgroup

# Puerto expuesto por el aplicativo Spring Boot
EXPOSE 8081

# Parámetros JVM optimizados para contenedores (G1GC, MaxRAMPercentage, cgroups v2)
ENV JAVA_OPTS="-XX:+UseG1GC \
               -XX:MaxRAMPercentage=75.0 \
               -XX:InitialRAMPercentage=50.0 \
               -XX:+ExitOnOutOfMemoryError \
               -Djava.security.egd=file:/dev/./urandom \
               -Duser.timezone=America/Lima \
               -Dfile.encoding=UTF-8"

# Healthcheck de contenedor contra el endpoint Actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Punto de entrada
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
