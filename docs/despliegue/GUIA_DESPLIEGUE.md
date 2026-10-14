# Manual Institucional de Despliegue y Operaciones — MuniHuamanga V2
## Sistema de Gestión Digital de Licencias de Funcionamiento

**Entidad:** Municipalidad Provincial de Huamanga (Ayacucho, Perú)
**Versión:** `v2.0.0-rc1` (Corte I10 Fase A)
**Fecha de Actualización:** 14 de octubre de 2026
**Audiencia:** Ingenieros DevOps, Administradores de Sistemas, DBAs y Soporte TI MPH
**Plataforma Tecnológica:** Java 21 LTS, Spring Boot 3.3.4, PostgreSQL 16, Docker Compose

---

## 1. Requisitos del Sistema e Infraestructura

### 1.1 Entorno de Servidor Recomendado (On-Premise / Servidor Municipal)
- **Sistema Operativo:** Ubuntu Server 22.04 LTS / Debian 12 / Rocky Linux 9 / RHEL 9.
- **CPU:** 4 vCPUs (Mínimo 2 vCPUs).
- **Memoria RAM:** 8 GB RAM (Mínimo 4 GB RAM con asignación de 1024 MB a 2048 MB para Heap JVM).
- **Almacenamiento:** 50 GB SSD NVMe (Espacio para PostgreSQL 16, volúmenes Docker y logs).
- **Red:** Conectividad interna municipal, puerto HTTPS (443) y servicio SMTP institucional.

### 1.2 Software Base Requerido
1. **Docker Engine:** Versión $\ge 24.0.0$
2. **Docker Compose Plugin:** Versión $\ge v2.20.0$
3. **Java Runtime (si se ejecuta sin Docker):** OpenJDK 21 LTS (Eclipse Temurin recomendado)
4. **Apache Maven (para compilación):** Versión $\ge 3.9.0$
5. **PostgreSQL (si se ejecuta sin Docker):** Versión **16.x**

---

## 2. Variables de Entorno y Configuración de Seguridad (`.env`)

Antes del primer arranque en producción, configure el archivo `.env` a partir de `.env.example`:

```bash
cd /opt/munihuamanga
cp .env.example .env
chmod 600 .env
```

Parámetros mandatorios de producción:

```ini
# ══════════════════════════════════════════════════════════════════════════════
# CONFIGURACIÓN DE PRODUCCIÓN - MUNIHUAMANGA V2
# ══════════════════════════════════════════════════════════════════════════════

# 1. Base de Datos Relacional PostgreSQL 16
POSTGRES_DB=muni_licencias_db
POSTGRES_USER=muni_user
POSTGRES_PASSWORD=<DEFINIR_PASSWORD_COMPLEJO_ALFANUMERICO_32_CHARS>

# 2. Seguridad y Tokens JWT (CWE-326: Clave criptográfica aleatoria de alta entropía)
# Generar mediante: openssl rand -base64 48
JWT_SECRET=<SECRETO_CRIPTO_ALEATORIO_GENERADO_DE_ALTA_ENTROPIA_2026!>
JWT_EXPIRATION_MS=28800000 # 8 Horas

# 3. Perfil de Ejecución Activo
SPRING_PROFILES_ACTIVE=prod

# 4. Notificaciones por Correo Electrónico Institucional
MAIL_ENABLED=true
MAIL_HOST=smtp.munihuamanga.gob.pe
MAIL_PORT=587
MAIL_USERNAME=notificaciones@munihuamanga.gob.pe
MAIL_PASSWORD=<PASSWORD_CUENTA_SERVICIOS_CORREO>

# 5. Dominios Públicos Institucionales
PORTAL_URL=https://licencias.munihuamanga.gob.pe
PORTAL_VERIFICACION_URL=https://licencias.munihuamanga.gob.pe/verificar-licencia.html?token=
```

---

## 3. Política Estricta de Inicialización y Migración de Base de Datos

> [!WARNING]
> **ADVERTENCIA CRÍTICA SOBRE SCRIPTS SQL:**
> Los scripts ubicados en `docker/postgres/init/` (`01-init-databases.sql` a `08-v2-verificacion-qr-inspeccion.sql`) son **SCRIPTS DE INICIALIZACIÓN PARA ENTORNOS LIMPIOS**.
> **NO DEBEN CONSIDERARSE NI EJECUTARSE COMO UN MECANISMO AUTOMÁTICO DE MIGRACIÓN SOBRE BASES DE DATOS EN PRODUCCIÓN CON DATOS PREEXISTENTES.**
> En una base de datos productiva existente, cualquier cambio de esquema debe aplicarse mediante scripts DDL versionados incrementales controlados por el DBA municipal, previa copia de seguridad certificada.

---

## 4. Procedimiento de Construcción y Despliegue

### 4.1 Despliegue con Docker Compose (Recomendado)

1. **Construir el artefacto y la imagen multi-stage:**
   ```bash
   docker compose build --no-cache
   ```
2. **Iniciar los servicios:**
   ```bash
   docker compose up -d
   ```
3. **Verificar el estado de los contenedores:**
   ```bash
   docker compose ps
   ```
   *Debe confirmar estado `healthy` o `running`:*
   - `muni_licencias_postgres` (PostgreSQL 16)
   - `muni_licencias_backend` (Spring Boot 3.3.4 en Java 21)

4. **Monitorear logs de arranque:**
   ```bash
   docker compose logs -f app
   ```

### 4.2 Despliegue Autónomo con JAR Local

```bash
# Compilar y empaquetar con Maven
mvn clean package -DskipTests

# Ejecutar con perfil prod
java -jar servicio-expedientes/target/servicio-expedientes-1.0.0-SNAPSHOT.jar \
     --spring.profiles.active=prod
```

---

## 5. Pruebas de Humo (Smoke Tests) y Validación Post-Despliegue

1. **Salud del Sistema (Spring Actuator):**
   ```bash
   curl -i http://localhost:8081/actuator/health
   # Respuesta esperada: HTTP/1.1 200 OK -> {"status":"UP"}
   ```
2. **Verificación de Seguridad (Rechazo sin Autenticación):**
   ```bash
   curl -i http://localhost:8081/api/expedientes
   # Respuesta esperada: HTTP/1.1 401 Unauthorized
   ```
3. **Consulta Pública QR:**
   ```bash
   curl -i http://localhost:8081/api/public/licencias/{tokenQr}
   # Respuesta esperada: HTTP/1.1 200 OK (Minimización Ley 29733: sin DNI ni coordenadas)
   ```

---

## 6. Mantenimiento, Copias de Seguridad y Recuperación

### 6.1 Respaldo Diario Automatizado (`pg_dump`)
Ejecutar tarea programada diaria con retención de 30 días:
```bash
FECHA=$(date +%Y%m%d_%H%M%S)
docker exec muni_licencias_postgres pg_dump -U muni_user -d muni_licencias_db -F c -b -v -f /var/lib/postgresql/data/backup_muni_${FECHA}.dump
```

### 6.2 Procedimiento de Recuperación (Disaster Recovery)
```bash
docker exec -i muni_licencias_postgres pg_restore -U muni_user -d muni_licencias_db -v -c /var/lib/postgresql/data/backup_muni_${FECHA}.dump
```

---

## 7. Dependencias Institucionales y Condición para Pase a Producción

> [!CAUTION]
> **NO DECLARAR EL SISTEMA APTO PARA PRODUCCIÓN SIN RESOLVER:**
> 1. **Firma Digital Institucional (ADR-04, ADR-07):** El sistema aplica política *fail-closed*. Requiere la integración formal con un proveedor acreditado ante la Infraestructura Oficial de Firma Electrónica (IOFE - RENIEC / INDECOPI). Sin esta integración, la emisión oficial de licencias permanece bloqueada por diseño legal.
> 2. **Sustitución de Credenciales:** Obligatorio cambiar las contraseñas de las cuentas de demostración preconfiguradas (`admin123`, `eval123`, etc.) y definir un `JWT_SECRET` criptográfico de producción.
> 3. **Procesos Manuales SAT y Defensa Civil (ADR-06):** Operadores de caja y evaluadores deben estar debidamente capacitados en las ventanillas físicas institucionales.
