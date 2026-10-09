# MANUAL INSTITUCIONAL DE DESPLIEGUE Y OPERACIONES
## Sistema de Gestión Digital de Licencias de Funcionamiento — MuniHuamanga
**Versión de Entrega:** `v0.9.0-rc1`  
**Fecha:** 09 de Octubre de 2026  
**Audiencia:** Administradores de Sistemas, Ingenieros DevOps, DBAs y Soporte Técnico MPH  
**Arquitectura:** Monolito Modular Spring Boot 3.3.4 (Java 21 LTS) + PostgreSQL 15

---

## 1. REQUISITOS DEL SISTEMA Y PRERREQUISITOS

### 1.1 Entorno de Producción Recomendado (Servidor On-Premise / Cloud)
- **Sistema Operativo:** Ubuntu Server 22.04 LTS / Debian 12 / Rocky Linux 9 / Red Hat Enterprise Linux 9.
- **CPU:** 4 vCPUs (Mínimo 2 vCPUs).
- **Memoria RAM:** 8 GB RAM (Mínimo 4 GB RAM con asignación de 2.5 GB para JVM heap).
- **Almacenamiento:** 50 GB SSD NVMe (Espacio para base de datos y logs rotativos).
- **Red:** Conectividad a Internet para sincronización NTP y servicio SMTP institucional.

### 1.2 Software Base Requerido
1. **Docker Engine:** Versión >= `24.0.0`
2. **Docker Compose Plugin:** Versión >= `v2.20.0`
3. **Git:** Versión >= `2.40.0`
4. **Puertos de Red Requeridos:**
   - `8081` (o mapeado a `80`/`443` vía Nginx/Caddy Reverse Proxy): Portal Web y API REST
   - `5432`: PostgreSQL (Aislado a red interna de Docker en producción)
   - `5050`: pgAdmin 4 (Opcional, únicamente para administración de base de datos)

---

## 2. CONFIGURACIÓN DEL ENTORNO (.env)

Antes del primer arranque, clone la plantilla de variables de entorno y configure las credenciales reales:

```bash
cd /opt/munihuamanga  # o el directorio donde se encuentre el repositorio
cp .env.example .env
chmod 600 .env
```

Edite el archivo `.env` con un editor de texto seguro (`nano .env`):

```ini
# ══════════════════════════════════════════════════════════════════════════════
# CONFIGURACIÓN DE PRODUCCIÓN - MUNICIPALIDAD DE HUAMANGA
# ══════════════════════════════════════════════════════════════════════════════

# 1. Base de Datos PostgreSQL
POSTGRES_DB=muni_licencias_db
POSTGRES_USER=muni_user
POSTGRES_PASSWORD=<DEFINIR_PASSWORD_COMPLEJO_ALFANUMERICO_32_CHARS>

# 2. Seguridad y Tokens JWT (CWE-326: Mínimo 256 bits / 32 caracteres)
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
PORTAL_VERIFICACION_URL=https://licencias.munihuamanga.gob.pe/verificar-licencia.html?codigo=
```

---

## 3. PROCEDIMIENTO DE CONSTRUCCIÓN Y DESPLIEGUE

### 3.1 Despliegue Automatizado con Docker Compose (Recomendado)

1. **Construir la imagen de contenedor multi-stage:**
   ```bash
   docker compose build --no-cache
   ```
2. **Iniciar los servicios en segundo plano:**
   ```bash
   docker compose up -d
   ```
3. **Verificar el estado de salud de los contenedores:**
   ```bash
   docker compose ps
   ```
   *Debe mostrar los 3 contenedores con estado `healthy` o `running`:*
   - `muni_licencias_postgres` (healthy)
   - `muni_licencias_backend` (healthy)
   - `muni_licencias_pgadmin` (running)

4. **Monitorear los registros de arranque del backend:**
   ```bash
   docker compose logs -f app
   ```
   *Debe confirmar:*
   ```text
   Started ExpedientesApplication in X.XXX seconds
   ```

### 3.2 Despliegue Híbrido Local / Desarrollo (Sin Docker para Backend)

Si la base de datos se ejecuta en Docker o PostgreSQL local pero desea ejecutar el jar directamente:
```bash
# Empaquetar
mvn clean package -DskipTests

# Ejecutar con perfil prod
java -jar servicio-expedientes/target/servicio-expedientes-1.0.0-SNAPSHOT.jar \
     --spring.profiles.active=prod
```

---

## 4. VERIFICACIÓN Y PRUEBAS DE HUMO (SMOKE TESTS)

Una vez iniciado el contenedor, ejecute las siguientes validaciones:

1. **Verificar estado de salud del backend (Actuator):**
   ```bash
   curl -i http://localhost:8081/actuator/health
   # Respuesta esperada: HTTP/1.1 200 OK -> {"status":"UP"}
   ```

2. **Verificar consulta pública de licencia por QR (Solo lectura):**
   ```bash
   curl -i http://localhost:8081/api/public/licencias/LIC-2026-00000002
   # Respuesta esperada: HTTP/1.1 200 OK -> {"valida":true, "estado":"VIGENTE / AUTORIZADA"}
   ```

3. **Verificar rechazo de acceso no autenticado a expedientes internos (H01/H05):**
   ```bash
   curl -i http://localhost:8081/api/expedientes
   # Respuesta esperada: HTTP/1.1 401 Unauthorized
   ```

4. **Acceso a los Portales Web:**
   - **Portal Ciudadano:** `http://localhost:8081/portal-ciudadano.html`
   - **Portal Interno Municipal:** `http://localhost:8081/portal-interno.html`
   - **Verificación de Licencia por QR:** `http://localhost:8081/verificar-licencia.html?codigo=LIC-2026-00000002`

---

## 5. MANTENIMIENTO, LOGS Y COPIAS DE SEGURIDAD

### 5.1 Rotación y Consulta de Logs
```bash
# Ver las últimas 100 líneas con seguimiento en vivo:
docker compose logs -f --tail=100 app

# Inspeccionar logs de base de datos:
docker compose logs -f postgres
```

### 5.2 Respaldo Automatizado de Base de Datos (pg_dump)
Cree una tarea programada cron en el host para generar backups diarios:

```bash
# Backup manual inmediato:
FECHA=$(date +%Y%m%d_%H%M%S)
docker exec muni_licencias_postgres pg_dump -U muni_user -d muni_licencias_db -F c -b -v -f /var/lib/postgresql/data/backup_muni_${FECHA}.dump

echo "Copia de respaldo generada exitosamente en el volumen de datos."
```
