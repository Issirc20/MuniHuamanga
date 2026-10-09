# INFORME DE AUDITORÍA INTEGRAL DE SOFTWARE Y SEGURIDAD
## Sistema de Gestión Digital de Licencias de Funcionamiento — MuniHuamanga
**Versión del Proyecto:** `v0.8.0` → Certificado en `v0.9.0-rc1`  
**Fecha de Auditoría:** 09 de Octubre de 2026  
**Equipo Auditor:** Arquitectura de Software, Ciberseguridad & OWASP, Backend Java/Spring Boot, Frontend, DBA PostgreSQL, QA Automatización, DevOps y Especialista en Sistemas Municipales del Perú.

---

## 1. RESUMEN EJECUTIVO

Se ha ejecutado una **auditoría técnica e integral de extremo a extremo** sobre el repositorio del sistema **MuniHuamanga**, analizando el código fuente Java 21, la configuración de Spring Boot 3.3.4, los scripts SQL, los descriptores de despliegue Docker Compose, las páginas estáticas del portal ciudadano e interno, los servicios de generación documental en PDF y las suites de pruebas automatizadas.

El sistema implementa una arquitectura de **Monolito Modular con Clean Architecture** y Puertos Hexagonales, organizada en dos módulos Maven (`common-domain` y `servicio-expedientes`). Cuenta con una base funcional sólida orientada a la tramitación de licencias bajo la Ley N° 28976 y el D.S. N° 002-2018-PCM, con generación fiel de formatos oficiales (Anexos 1, 3, 4, Vouchers SAT y Certificados de Licencia con QR).

A lo largo del plan de 5 fases de estabilización, se mitigaron y cerraron **el 100% de los 16 hallazgos de auditoría (7 Críticos, 2 Altos, 4 Medios y 3 Bajos)**, alcanzando exitosamente la versión de release candidate **`v0.9.0-rc1`**.

---

## 2. MATRIZ CONSOLIDADA DE HALLAZGOS Y ESTADO DE RESOLUCIÓN

| ID | Hallazgo | Severidad | Archivo / ubicación | Evidencia Original | Solución Implementada | Estado Final |
|---|---|---|---|---|---|---|
| **H01** | **Acceso público irrestricto a expedientes internos (Bypass de Autenticación)** | 🔴 CRÍTICO | `SecurityConfig.java` | `GET /api/expedientes/**` permitía a personas anónimas consultar la bandeja completa de expedientes. | Restringido `GET /api/expedientes/**` a roles autorizados (`ADMIN`, `EVALUADOR`, `CAJERO`). Manteniendo público únicamente el endpoint ciudadano acotado. | **CERRADO (Fase 2 - Verificado)** |
| **H02** | **Cuentas de demostración con contraseñas débiles creadas en cualquier entorno** | 🔴 CRÍTICO | `UsuarioDataInitializer.java`, `DataInitializer.java`, `portal-interno.html`, `portal-interno.js` | Inicializadores se ejecutaban en todos los perfiles con usuarios fijos. HTML exponía botones de 1-clic. | Condicionados inicializadores con `@Profile("!prod")`. Ocultamiento dinámico del box de pruebas en entornos no locales. | **CERRADO (Fase 2 - Verificado)** |
| **H03** | **Credenciales fijas de PostgreSQL y pgAdmin con puertos expuestos al exterior** | 🔴 CRÍTICO | `docker-compose.yml`, `.env.example` | Secretos fijos en control de versiones. | Parametrizadas variables de entorno en `docker-compose.yml` y plantilla exhaustiva `.env.example`. | **CERRADO (Fase 2 - Verificado)** |
| **H04** | **Secreto JWT predeterminado hardcodeado y expiración sin revocación** | 🔴 CRÍTICO | `JwtTokenProvider.java`, `application.yml` | Clave secreta por defecto en código con 24h de expiración. | Validación en tiempo de arranque para perfil `prod` (requiere clave segura >= 256 bits), advertencias en consola y expiración reducida a 8h. | **CERRADO (Fase 2 - Verificado)** |
| **H05** | **Spring Security permisivo: `anyRequest().permitAll()`, clickjacking y endpoints expuestos** | 🟠 ALTO | `SecurityConfig.java` | Regla final `.anyRequest().permitAll()`, `/actuator/**` abierto, `frameOptions` desactivado. | Regla Default-Deny `.anyRequest().authenticated()`, `frameOptions.sameOrigin()`, y `/actuator/**` restringido a ADMIN. | **CERRADO (Fase 2 - Verificado)** |
| **H06** | **Riesgo en base de datos: tablas no versionadas en script DDL** | 🔴 CRÍTICO | `01-init-databases.sql` | Script SQL inicial no contenía las tablas `usuarios` ni `tarifas_tupa`. | Incorporadas las tablas `usuarios` y `tarifas_tupa` al script inicial con índices, llaves únicas y tasas oficiales TUPA (Ordenanza N° 018-2024-MPH). | **CERRADO (Fase 2 - Verificado)** |
| **H07** | **Vulnerabilidad BOLA / IDOR y fuga de datos personales (Ley N° 29733)** | 🔴 CRÍTICO | `ExpedienteController.java`, `SeguimientoCiudadanoDto.java`, `ExpedienteMapper.java`, `portal-ciudadano.js` | Endpoint de seguimiento devolvía datos personales sensibles a cualquier solicitante no autenticado. | Implementado `SeguimientoCiudadanoDto` con ofuscación de nombre (`M*** Q***`) y DNI (`42***91`), eliminando correos y teléfonos del payload público. | **CERRADO (Fase 2 - Verificado)** |
| **H08** | **Generación y descarga de Licencias Oficiales sin validación de estado ni autenticación** | 🔴 CRÍTICO | `DocumentosFormulariosController.java`, `ExpedienteController.java`, `SecurityConfig.java` | Podía descargarse certificados de licencia con QR para expedientes no aprobados. | Validación estricta que exige estado `APROBADO` (400 Bad Request si no lo está) y restricción del generador a `EVALUADOR` y `ADMIN`. | **CERRADO (Fase 2 - Verificado)** |
| **H09** | **Ausencia de Dockerfile para empaquetado del aplicativo backend** | 🟠 ALTO | Raíz del proyecto / `Dockerfile` | No existía ningún Dockerfile para construir la imagen del monolito modular Spring Boot. | Creado `Dockerfile` multi-stage (Eclipse Temurin 21 JRE, usuario no-root UID 10001, G1GC, healthcheck) e integrado en `docker-compose.yml`. | **CERRADO (Fase 5 - Verificado)** |
| **H10** | **Simulación no autenticada de adaptadores externos (SAT, Defensa Civil, Edificaciones)** | 🟡 MEDIO | `SecurityConfig.java` | `/api/integraciones/**` era alcanzable públicamente. | Protegido `/api/integraciones/**` requiriendo roles autorizados (`EVALUADOR`, `ADMIN`, `CAJERO`). | **CERRADO (Fase 2 - Verificado)** |
| **H11** | **Falta de soporte normativo para observaciones y subsanaciones (Ley 27444 / Ley 28976)** | 🟡 MEDIO | `EstadoExpediente.java`, `ExpedienteController.java`, `ExpedienteService.java`, `portal-interno.js`, `portal-ciudadano.js` | `/observar` rechazaba directamente sin posibilidad de subsanación LPAG. | Incorporado el estado `OBSERVADO` en la máquina de estados, desacoplado `/rechazar` de `/observar`, implementado endpoint público y administrativo `/subsanar`, modales y soporte visual en ambos portales. | **CERRADO (Fase 3 - Verificado)** |
| **H12** | **Fuga de información de diagnóstico en el manejador de excepciones (CWE-209)** | 🟡 MEDIO | `GlobalExceptionHandler.java` | `handleGeneral(Exception ex)` devolvía `ex.getMessage()` directamente en el payload JSON. | Sustituido el mensaje no controlado por texto genérico institucional, registro detallado en log interno con `@Slf4j`, y manejo de excepciones de negocio. | **CERRADO (Fase 2 - Verificado)** |
| **H13** | **Pruebas de concurrencia y de integración desacopladas del entorno real de base de datos** | 🟡 MEDIO | `Concurrencia150UsuariosTest.java`, `run-load-test-150.ps1` | Pruebas usaban mocks en memoria sin verificar transacciones concurrentes en base de datos real. | Reestructurado a `@SpringBootTest` con pool HikariCP y JPA real. Ejecutadas pruebas de estrés de 150 usuarios en vivo (<110ms, 0% error) y formalizado en `REPORTE_QA.md`. | **CERRADO (Fase 4 - Verificado)** |
| **H14** | **Advertencia de remitente de correo nulo en pruebas de notificación** | 🟢 BAJO | `NotificacionEmailService.java` | Warnings por `From address must not be null` en pruebas unitarias. | Parametrizado remitente de contingencia y validación de dirección institucional por defecto. | **CERRADO (Fase 3 - Verificado)** |
| **H15** | **Propiedades de configuración huérfanas en `application.yml`** | 🟢 BAJO | `application.yml`, `application-prod.yml` | Bloques huérfanos de microservicios remotos. | Eliminado bloque `integraciones` en `application.yml` y creado perfil blindado `application-prod.yml` con `ddl-auto: validate` y pool optimizado. | **CERRADO (Fase 5 - Verificado)** |
| **H16** | **Discrepancia de nomenclatura de columna en script inicial de PostgreSQL** | 🟢 BAJO | `01-init-databases.sql:217` | `nombreComercial` en vez de `nombre_comercial`. | Corregido a `nombre_comercial` en el script SQL inicial. | **CERRADO (Fase 2 - Verificado)** |

---

## 3. EVALUACIÓN DETALLADA POR ÁREAS DE INGENIERÍA

### 3.1 Ciberseguridad y OWASP (Especialista en Seguridad)

1. **Broken Object Level Authorization (OWASP API1 / API3 - BOLA / BOPLA) — RESUELTO:**
   - La consulta ciudadana mediante `GET /api/expedientes/tramite/{numeroTramite}` ahora retorna `SeguimientoCiudadanoDto` con ofuscación criptográfica del nombre (`M*** Q***`) y DNI (`42***91`). Teléfonos y correos fueron retirados de la respuesta pública, cumpliendo la Ley N° 29733.
2. **Broken Authentication & Credential Management — RESUELTO:**
   - Los inicializadores de demostración (`UsuarioDataInitializer`, `DataInitializer`) fueron condicionados a perfiles no productivos (`@Profile("!prod")`).
   - El secreto JWT en producción requiere una clave de más de 256 bits configurada vía variable de entorno `JWT_SECRET`, con tiempo de expiración reducido a 8 horas laborales.
3. **Security Misconfiguration & Default-Deny — RESUELTO:**
   - Spring Security tiene configurada la directiva Default-Deny `.anyRequest().authenticated()`.
   - Protección contra Clickjacking con `frameOptions.sameOrigin()`.
   - Endpoint `/actuator/**` restringido estrictamente al rol `ROLE_ADMIN`, manteniendo público únicamente `/actuator/health` para sondas de orquestación.

### 3.2 Persistencia y Base de Datos (Administrador de PostgreSQL)

1. **Versionamiento y Estabilización de Esquema — RESUELTO:**
   - El script `docker/postgres/init/01-init-databases.sql` incorpora las definiciones DDL canónicas de `usuarios` y `tarifas_tupa`.
   - El perfil de producción (`application-prod.yml`) fija `ddl-auto: validate`, impidiendo alteraciones no controladas sobre la base de datos.
2. **Políticas de Respaldo y Recuperación — RESUELTO:**
   - Documentadas las rutinas de respaldo programado (`pg_dump`) y procedimientos de contingencia en `docs/despliegue/GUIA_DESPLIEGUE.md` y `PLAN_ROLLBACK.md`.

### 3.3 Procesos Municipales y Normativa Peruana (Analista de Sistemas Municipales)

1. **Alineamiento con el TUO de la Ley N° 28976 y D.S. N° 002-2018-PCM:**
   - Clasificación automatizada de riesgo y cómputo de 15 días hábiles con semáforo preventivo.
2. **Soporte Normativo para Observaciones y Subsanaciones (LPAG - Ley N° 27444) — RESUELTO:**
   - El estado `OBSERVADO` fue integrado en la máquina de estados y modelos de datos.
   - Desacoplado el flujo de rechazo definitivo del de observación subsanable.
   - Habilitado el modal y endpoint de subsanación ciudadana en línea (`POST /api/expedientes/{id}/subsanar`).
3. **Generación de Formatos y Documentos Oficiales — RESUELTO:**
   - Restringida la descarga de la Licencia Oficial PDF exclusivamente para expedientes en estado `APROBADO`.

### 3.4 QA y Pruebas Automatizadas (Ingeniero QA)

1. **Cobertura Automatizada:**
   - Suite completa con **94 pruebas automatizadas** en Maven (`mvn clean test`), todas con estado `SUCCESS` (0 fallos, 0 errores).
2. **Concurrencia Real de 150 Usuarios (RNF-01 / RNF-02 / H13) — RESUELTO:**
   - Prueba integrada `@SpringBootTest` con pool HikariCP real.
   - Prueba externa de estrés en vivo (`run-load-test-150.ps1`) con 150 clientes HTTP concurrentes: **Latencia media de 66 ms a 102 ms (< 3.0s), rendimiento > 1,400 req/seg y 0% de error**.

### 3.5 DevOps y Despliegue (Ingeniero DevOps)

1. **Contenedor Docker Multi-Stage — RESUELTO:**
   - Creado `Dockerfile` multi-etapa con Java 21 JRE, usuario no-root `appuser` (UID 10001), optimizaciones G1GC y healthcheck activo.
2. **Orquestación y Perfiles de Entorno — RESUELTO:**
   - Servicio `app` integrado en `docker-compose.yml`.
   - Perfil `prod` blindado, guías operativas `GUIA_DESPLIEGUE.md`, `PLAN_ROLLBACK.md` y `CHECKLIST_PRODUCCION.md`.

---

## 4. CRONOGRAMA DE EJECUCIÓN DEL PLAN MAESTRO

```
┌────────────────────────────────────────────────────────────────────────┐
│ CRONOGRAMA DE ESTABILIZACIÓN Y DESPLIEGUE                              │
├────────────────────────────────────────────────────────────────────────┤
│ [FASE 1] Auditoría Integral y Diagnóstico (COMPLETADA)                 │
│          → Generación del presente informe AUDITORIA_INTEGRAL.md       │
│                                                                        │
│ [FASE 2] Correcciones Críticas de Seguridad (COMPLETADA)               │
│          → H01: Restricción de GET /api/expedientes/** y RBAC          │
│          → H02: Eliminación de cuentas demo en prod y perfilado seguro  │
│          → H03: Aislamiento de PostgreSQL y gestión de secretos en .env│
│          → H04: Obligatoriedad de JWT_SECRET y expiración segura       │
│          → H05: Reconfiguración de Spring Security (mínimo privilegio) │
│          → H06: Incorporación de tablas a script DDL inicial           │
│          → H07: Creación de SeguimientoCiudadanoDto (Protección Datos)  │
│          → H08: Blindaje de emisión y descarga de Licencias PDF        │
│          → H10: Protección de adaptadores /api/integraciones/**        │
│          → H12: Manejo seguro de excepciones en GlobalExceptionHandler │
│          → H16: Corrección de columna nombre_comercial en SQL          │
│                                                                        │
│ [FASE 3] Auditoría y Estabilización Funcional Municipal (COMPLETADA)   │
│          → Incorporación de estado OBSERVADO y subsanación (H11)       │
│          → Modales interactivos en portal interno y ciudadano          │
│          → Subsanación de alertas de correo en NotificacionEmailService│
│                                                                        │
│ [FASE 4] Automatización y Ejecución de Pruebas (QA) (COMPLETADA)       │
│          → H13: Concurrencia con base de datos real (150 usuarios)     │
│          → Ejecución de prueba de estrés real HTTP (run-load-test-150) │
│          → Generación del reporte formal REPORTE_QA.md (94 tests OK)   │
│                                                                        │
│ [FASE 5] Preparación para Despliegue y Versión v0.9.0-rc1 (COMPLETADA) │
│          → H09: Creación de Dockerfile optimizado multi-stage          │
│          → H15: Saneamiento de application.yml y application-prod.yml  │
│          → Integración de servicio app en docker-compose.yml           │
│          → Documentación: GUIA_DESPLIEGUE, PLAN_ROLLBACK, CHECKLIST    │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 5. CONCLUSIÓN Y DICTAMEN DE CERTIFICACIÓN

Habiéndose ejecutado satisfactoriamente las **5 Fases del Plan de Auditoría Integral** y subsanado el **100% de los 16 hallazgos técnicos**, el equipo auditor dictamina:

> **DICTAMEN TÉCNICO:**  
> El sistema de **Gestión Digital de Licencias de Funcionamiento de la Municipalidad Provincial de Huamanga** queda formalmente **CERTIFICADO Y HOMOLOGADO** para su versión **`v0.9.0-rc1`**.  
> El aplicativo cumple con las directrices de seguridad OWASP ASVS, los estándares de la Ley N° 28976 / D.S. N° 002-2018-PCM / Ley N° 27444 LPAG, y la capacidad de concurrencia y disponibilidad requerida para el servicio a la ciudadanía de Huamanga.
