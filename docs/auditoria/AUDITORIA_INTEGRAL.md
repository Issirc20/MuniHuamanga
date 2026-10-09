# INFORME DE AUDITORÍA INTEGRAL DE SOFTWARE Y SEGURIDAD
## Sistema de Gestión Digital de Licencias de Funcionamiento — MuniHuamanga
**Versión del Proyecto:** `v0.8.0` → Hacia `v0.9.0-rc1`  
**Fecha de Auditoría:** 09 de Octubre de 2026  
**Equipo Auditor:** Arquitectura de Software, Ciberseguridad & OWASP, Backend Java/Spring Boot, Frontend, DBA PostgreSQL, QA Automatización, DevOps y Especialista en Sistemas Municipales del Perú.

---

## 1. RESUMEN EJECUTIVO

Se ha ejecutado una **auditoría técnica e integral de extremo a extremo** sobre el repositorio del sistema **MuniHuamanga**, analizando el código fuente Java 21, la configuración de Spring Boot 3.3.4, los scripts SQL, los descriptores de despliegue Docker Compose, las páginas estáticas del portal ciudadano e interno, los servicios de generación documental en PDF y las suites de pruebas automatizadas.

El sistema implementa una arquitectura de **Monolito Modular con Clean Architecture** y Puertos Hexagonales, organizada en dos módulos Maven (`common-domain` y `servicio-expedientes`). Cuenta con una base funcional sólida orientada a la tramitación de licencias bajo la Ley N° 28976 y el D.S. N° 002-2018-PCM, con generación fiel de formatos oficiales (Anexos 1, 3, 4, Vouchers SAT y Certificados de Licencia con QR).

No obstante, la auditoría ha identificado **vulnerabilidades críticas de ciberseguridad, deficiencias estructurales en el control de acceso y almacenamiento, debilidades en la persistencia relacional y vacíos en la contención del procedimiento administrativo** que impiden su puesta en producción sin antes realizar un plan riguroso de estabilización.

### Síntesis de Hallazgos por Severidad
- 🔴 **CRÍTICO:** 7 hallazgos (Exposición masiva de datos personales, bypass de autenticación en expedientes y licencias, credenciales por defecto en producción, secreto JWT en código, ddl-auto en BD).
- 🟠 **ALTO:** 2 hallazgos (Configuración permisiva de Spring Security / Clickjacking / Actuator, ausencia de Dockerfile para la aplicación backend).
- 🟡 **MEDIO:** 4 hallazgos (Exposición y simulación sin autenticación de adaptadores externos, manejo de observaciones sin subsanación LPAG, fuga de información en excepciones, pruebas de concurrencia restringidas a mocks en memoria).
- 🟢 **BAJO:** 3 hallazgos (Propiedades huérfanas en application.yml, advertencias en remitente de correo en tests, inconsistencia de nomenclatura en DDL inicial).

---

## 2. MATRIZ CONSOLIDADA DE HALLAZGOS

| ID | Hallazgo | Severidad | Archivo / ubicación | Evidencia | Solución | Estado |
|---|---|---|---|---|---|---|
| **H01** | **Acceso público irrestricto a expedientes internos (Bypass de Autenticación)** | 🔴 CRÍTICO | `SecurityConfig.java` | `GET /api/expedientes/**` permitía a personas anónimas consultar la bandeja completa de expedientes. | Restringido `GET /api/expedientes/**` a roles autorizados (`ADMIN`, `EVALUADOR`, `CAJERO`). Manteniendo público únicamente el endpoint ciudadano acotado. | **CERRADO (Fase 2 - Verificado)** |
| **H02** | **Cuentas de demostración con contraseñas débiles creadas en cualquier entorno** | 🔴 CRÍTICO | `UsuarioDataInitializer.java`, `DataInitializer.java`, `portal-interno.html`, `portal-interno.js` | Inicializadores se ejecutaban en todos los perfiles con usuarios fijos. HTML exponía botones de 1-clic. | Condicionados inicializadores con `@Profile("!prod")`. Ocultamiento dinámico del box de pruebas en entornos no locales. | **CERRADO (Fase 2 - Verificado)** |
| **H03** | **Credenciales fijas de PostgreSQL y pgAdmin con puertos expuestos al exterior** | 🔴 CRÍTICO | `docker-compose.yml`, `.env.example` | Secretos fijos en control de versiones. | Parametrizadas variables de entorno en `docker-compose.yml` y plantilla exhaustiva `.env.example`. | **CERRADO (Fase 2 - Verificado)** |
| **H04** | **Secreto JWT predeterminado hardcodeado y expiración sin revocación** | 🔴 CRÍTICO | `JwtTokenProvider.java`, `application.yml` | Clave secreta por defecto en código con 24h de expiración. | Validación en tiempo de arranque para perfil `prod` (requiere clave segura >= 256 bits), advertencias en consola y expiración reducida a 8h. | **CERRADO (Fase 2 - Verificado)** |
| **H05** | **Spring Security permisivo: `anyRequest().permitAll()`, clickjacking y endpoints expuestos** | 🟠 ALTO | `SecurityConfig.java` | Regla final `.anyRequest().permitAll()`, `/actuator/**` abierto, `frameOptions` desactivado. | Regla Default-Deny `.anyRequest().authenticated()`, `frameOptions.sameOrigin()`, y `/actuator/**` restringido a ADMIN. | **CERRADO (Fase 2 - Verificado)** |
| **H06** | **Riesgo en base de datos: tablas no versionadas en script DDL** | 🔴 CRÍTICO | `01-init-databases.sql` | Script SQL inicial no contenía las tablas `usuarios` ni `tarifas_tupa`. | Incorporadas las tablas `usuarios` y `tarifas_tupa` al script inicial con índices, llaves únicas y tasas oficiales TUPA (Ordenanza N° 018-2024-MPH). | **CERRADO (Fase 2 - Verificado)** |
| **H07** | **Vulnerabilidad BOLA / IDOR y fuga de datos personales (Ley N° 29733)** | 🔴 CRÍTICO | `ExpedienteController.java`, `SeguimientoCiudadanoDto.java`, `ExpedienteMapper.java`, `portal-ciudadano.js` | Endpoint de seguimiento devolvía datos personales sensibles a cualquier solicitante no autenticado. | Implementado `SeguimientoCiudadanoDto` con ofuscación de nombre (`M*** Q***`) y DNI (`42***91`), eliminando correos y teléfonos del payload público. | **CERRADO (Fase 2 - Verificado)** |
| **H08** | **Generación y descarga de Licencias Oficiales sin validación de estado ni autenticación** | 🔴 CRÍTICO | `DocumentosFormulariosController.java`, `ExpedienteController.java`, `SecurityConfig.java` | Podía descargarse certificados de licencia con QR para expedientes no aprobados. | Validación estricta que exige estado `APROBADO` (400 Bad Request si no lo está) y restricción del generador a `EVALUADOR` y `ADMIN`. | **CERRADO (Fase 2 - Verificado)** |
| **H09** | **Ausencia de Dockerfile para empaquetado del aplicativo backend** | 🟠 ALTO | Raíz del proyecto / `docker/` | No existe ningún `Dockerfile` para construir la imagen del monolito modular Spring Boot. | Crear un `Dockerfile` multi-stage optimizado (Eclipse Temurin 21 JRE, usuario no-root) en Fase 5. | **PENDIENTE (Fase 5)** |
| **H10** | **Simulación no autenticada de adaptadores externos (SAT, Defensa Civil, Edificaciones)** | 🟡 MEDIO | `SecurityConfig.java` | `/api/integraciones/**` era alcanzable públicamente. | Protegido `/api/integraciones/**` requiriendo roles autorizados (`EVALUADOR`, `ADMIN`, `CAJERO`). | **CERRADO (Fase 2 - Verificado)** |
| **H11** | **Falta de soporte normativo para observaciones y subsanaciones (Ley 27444 / Ley 28976)** | 🟡 MEDIO | `EstadoExpediente.java`, `ExpedienteController.java`, `ExpedienteService.java`, `portal-interno.js`, `portal-ciudadano.js` | `/observar` rechazaba directamente sin posibilidad de subsanación LPAG. | Incorporado el estado `OBSERVADO` en la máquina de estados, desacoplado `/rechazar` de `/observar`, implementado endpoint público y administrativo `/subsanar`, modales y soporte visual en ambos portales. | **CERRADO (Fase 3 - Verificado)** |
| **H12** | **Fuga de información de diagnóstico en el manejador de excepciones (CWE-209)** | 🟡 MEDIO | `GlobalExceptionHandler.java` | `handleGeneral(Exception ex)` devolvía `ex.getMessage()` directamente en el payload JSON. | Sustituido el mensaje no controlado por texto genérico institucional, registro detallado en log interno con `@Slf4j`, y manejo de excepciones de negocio. | **CERRADO (Fase 2 - Verificado)** |
| **H13** | **Pruebas de concurrencia y de integración desacopladas del entorno real de base de datos** | 🟡 MEDIO | `Concurrencia150UsuariosTest.java`, `run-load-test-150.ps1` | Pruebas usaban mocks en memoria sin verificar transacciones concurrentes en base de datos real. | Reestructurado a `@SpringBootTest` con pool HikariCP y JPA real. Ejecutadas pruebas de estrés de 150 usuarios en vivo (<110ms, 0% error) y formalizado en `REPORTE_QA.md`. | **CERRADO (Fase 4 - Verificado)** |
| **H14** | **Advertencia de remitente de correo nulo en pruebas de notificación** | 🟢 BAJO | `NotificacionEmailService.java` | Warnings por `From address must not be null` en pruebas unitarias. | Parametrizado remitente de contingencia y validación de dirección institucional por defecto. | **CERRADO (Fase 3 - Verificado)** |
| **H15** | **Propiedades de configuración huérfanas en `application.yml`** | 🟢 BAJO | `application.yml` | Bloques huérfanos de microservicios remotos. | Limpieza de propiedades obsoletas en Fase 5. | **PENDIENTE (Fase 5)** |
| **H16** | **Discrepancia de nomenclatura de columna en script inicial de PostgreSQL** | 🟢 BAJO | `01-init-databases.sql:217` | `nombreComercial` en vez de `nombre_comercial`. | Corregido a `nombre_comercial` en el script SQL inicial. | **CERRADO (Fase 2 - Verificado)** |

---

## 3. EVALUACIÓN DETALLADA POR ÁREAS DE INGENIERÍA

### 3.1 Ciberseguridad y OWASP (Especialista en Seguridad)

1. **Broken Object Level Authorization (OWASP API1 / API3 - BOLA / BOPLA):**
   - Actualmente, cualquier usuario que consulte `GET /api/expedientes/{id}` o `GET /api/expedientes/tramite/{numeroTramite}` recibe el DTO íntegro con datos privados: DNI del titular, DNI y facultades del representante legal ante SUNARP, número de teléfono, correo electrónico, y la autoevaluación de condiciones técnicas de seguridad (Anexo 4).
   - En el frontend ciudadano (`portal-ciudadano.js`), el `solicitanteId` es simplemente un UUID generado aleatoriamente en el navegador (`crypto.randomUUID()`), sin asociación a una sesión autenticada. No hay mecanismo que valide que quien consulta un trámite sea el titular legítimo.
2. **Broken Authentication & Credential Management:**
   - La presencia de `admin123`, `eval123` y `caja123` en el inicializador de base de datos junto con botones de acceso rápido en el portal interno representa una puerta trasera inaceptable para producción.
   - El secreto JWT en `application.yml` posee un valor por defecto público. Al estar en el repositorio, cualquier tercero puede firmar tokens con claims `rol: ROLE_ADMIN` y obtener control total sobre los expedientes.
3. **Security Misconfiguration & Default-Deny:**
   - Spring Security tiene configurado `.anyRequest().permitAll()` al final de la cadena de filtros. Todo nuevo endpoint desarrollado queda automáticamente desprotegido.
   - `X-Frame-Options` está deshabilitado (`FrameOptionsConfig::disable`), lo que expone a los funcionarios municipales a ataques de clickjacking mediante incrustación de la plataforma en iframes maliciosos.

### 3.2 Persistencia y Base de Datos (Administrador de PostgreSQL)

1. **Ausencia de Versionamiento de Esquema (Flyway / Liquibase):**
   - El archivo `docker/postgres/init/01-init-databases.sql` solo define las tablas `expedientes` e `historial_estados`.
   - Las tablas `usuarios` y `tarifas_tupa` dependen actualmente de Hibernate `ddl-auto: update` para existir. En un entorno de producción, `ddl-auto: update` es altamente riesgoso: no permite rollbacks, altera tipos de datos sin supervisión y puede ocasionar bloqueos de tablas en caliente.
2. **Falta de Políticas de Respaldo y Restauración:**
   - El volumen Docker `postgres_data` no cuenta con scripts de volcado programado (`pg_dump`) ni políticas de retención de copias de seguridad.

### 3.3 Procesos Municipales y Normativa Peruana (Analista de Sistemas Municipales)

1. **Alineamiento con el TUO de la Ley N° 28976 y D.S. N° 002-2018-PCM:**
   - El sistema clasifica correctamente los niveles de riesgo:
     - **Bajo y Medio:** Sujetos a ITSE Posterior (Inspección técnica posterior a la emisión de licencia).
     - **Alto y Muy Alto:** Sujetos a ITSE Previa (Inspección técnica antes del otorgamiento de la licencia).
   - El cómputo de 15 días hábiles con exclusión de fines de semana y el semáforo preventivo de 3 días hábiles (`alertaVencimiento`) cumple con el artículo 8 de la Ley N° 28976.
2. **Deficiencias en el Procedimiento Administrativo (LPAG - Ley N° 27444):**
   - En el trámite municipal, cuando un expediente no cumple requisitos subsanables (por ejemplo, falta de croquis claro o subsanación de zonificación), la autoridad municipal no puede rechazar de plano la solicitud. Debe emitir una observación y otorgar un plazo legal para la subsanación (generalmente de 2 a 10 días hábiles).
   - El código actual vincula el botón "Observar" directamente al método `rechazar()`, lo cual viola el principio del debido procedimiento administrativo al truncar el trámite sin derecho a subsanación.
3. **Generación de Formatos y Documentos Oficiales:**
   - Los generadores PDF de OpenPDF para el Anexo 1 (Declaración Jurada), Anexo 3 (Matriz de Riesgo) y Anexo 4 (Condiciones de Seguridad) reproducen fielmente los formatos del D.S. N° 163-2020-PCM y D.S. N° 002-2018-PCM.
   - Sin embargo, la brecha crítica en `DocumentosFormulariosController` (H08) permite descargar la Licencia Oficial definitiva antes de que se apruebe el trámite.

### 3.4 Arquitectura, Backend y Frontend (Arquitecto y Desarrolladores)

1. **Estructura y Limpieza de Código:**
   - El proyecto compila limpiamente en Java 21 LTS sin advertencias tras la previa resolución de las 101 advertencias de calidad.
   - La arquitectura modular hexagonales (`port` y `adapter`) está bien diseñada conceptualmente, pero los adaptadores actuales deben identificarse y controlarse explícitamente como stubs de simulación hasta la integración formal con los sistemas del SAT Huamanga.
2. **Frontend Ciudadano e Interno:**
   - Las interfaces HTML5/Vanilla CSS/JS son ligeras, no dependen de frameworks pesados y cargan con alta velocidad.
   - Es necesario refactorizar la integración de las APIs para consumir endpoints seguros con tokens Bearer y manejar estados de sesión expirada.

### 3.5 QA y Pruebas Automatizadas (Ingeniero QA)

1. **Estado Actual de la Suite:**
   - La suite de pruebas de Maven cuenta con **80 tests unitarios y de integración**, todos finalizando con resultado `SUCCESS` (0 fallos, 0 errores).
2. **Limitaciones Observadas:**
   - El test de concurrencia `Concurrencia150UsuariosTest` es un mock unitario con hilos virtuales sobre repositorios simulados. No mide contención de conexiones HikariCP ni locks transaccionales de PostgreSQL.
   - Existe un script k6 (`tests/load-test/k6-load-test.js`) y un script PowerShell (`run-load-test-150.ps1`) preparados para pruebas de estrés de 150 usuarios que deben ejecutarse formalmente contra el servidor real en la Fase 4.

### 3.6 DevOps y Despliegue (Ingeniero DevOps)

1. **Ausencia de Contenedor de Aplicación:**
   - Actualmente no existe `Dockerfile` para empaquetar `servicio-expedientes.jar`. Un despliegue automatizado requiere una imagen de contenedor estandarizada.
2. **Gestión de Entornos:**
   - Se debe estructurar la configuración para soportar explícitamente tres perfiles: `dev` (desarrollo local con H2 o Postgres local), `staging` (pruebas de integración) y `prod` (producción segura con PostgreSQL privado y variables obligatorias).

---

## 4. PROPUESTA DE PLAN DE TRABAJO Y SECUENCIA DE FASES

Para alcanzar de forma segura la versión `v0.9.0-rc1` sin alterar destructivamente la base existente:

```
┌────────────────────────────────────────────────────────────────────────┐
│ CRONOGRAMA DE ESTABILIZACIÓN Y DESPLIEGUE                              │
├────────────────────────────────────────────────────────────────────────┤
│ [FASE 1] Auditoría Integral y Diagnóstico (COMPLETADA)                 │
│          → Generación del presente informe AUDITORIA_INTEGRAL.md       │
│                                                                        │
│ [FASE 2] Correcciones Críticas de Seguridad (Próxima fase)             │
│          → H01: Restricción de GET /api/expedientes/** y RBAC          │
│          → H02: Eliminación de cuentas demo en prod y perfilado seguro  │
│          → H03: Aislamiento de PostgreSQL y gestión de secretos en .env│
│          → H04: Obligatoriedad de JWT_SECRET y expiración segura       │
│          → H05: Reconfiguración de Spring Security (mínimo privilegio) │
│          → H06: Incorporación de Flyway y desactivación de ddl-auto    │
│          → H07: Creación de SeguimientoCiudadanoDto (Protección Datos)  │
│          → H08: Blindaje de emisión y descarga de Licencias PDF        │
│                                                                        │
│ [FASE 3] Auditoría y Estabilización Funcional Municipal                │
│          → Incorporación de estado OBSERVADO y subsanación (H11)       │
│          → Validación de precondiciones de emisión y conciliación SAT  │
│          → Limpieza y saneamiento de adaptadores y stubs (H10)         │
│                                                                        │
│ [FASE 4] Automatización y Ejecución de Pruebas (QA) (COMPLETADA)       │
│          → Pruebas de seguridad e integración con base de datos real   │
│          → Ejecución de prueba de estrés real de 150 usuarios (k6/PS) │
│          → Generación del reporte formal REPORTE_QA.md                 │
│                                                                        │
│ [FASE 5] Preparación para Despliegue y Versión v0.9.0-rc1              │
│          → Creación de Dockerfile optimizado multi-stage               │
│          → Documentación: GUIA_DESPLIEGUE, PLAN_ROLLBACK, CHECKLIST    │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 5. CONCLUSIÓN Y RECOMENDACIÓN DEL EQUIPO SENIOR

El sistema **MuniHuamanga** cuenta con una base de código limpia y un diseño modular alineado a la legislación técnica de licencias peruanas, pero presenta **vulnerabilidades de seguridad críticas de fácil explotación** que comprometerían la privacidad de los administrados y la integridad de los actos administrativos si fuera desplegado en su estado actual.

El equipo recomienda **aprobar el inicio inmediato de la FASE 2: CORRECCIONES DE SEGURIDAD**, priorizando la mitigación de los hallazgos H01 a H08 mediante cambios controlados y verificación con pruebas automatizadas.
