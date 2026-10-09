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
| **H01** | **Acceso público irrestricto a expedientes internos (Bypass de Autenticación)** | 🔴 CRÍTICO | `SecurityConfig.java:82` | `requestMatchers(HttpMethod.GET, "/api/expedientes/**").permitAll()` permite a cualquier persona anónima consultar la bandeja completa de expedientes, métricas internas y detalles de cualquier administrado. | Restringir `GET /api/expedientes/**` a roles autorizados (`ADMIN`, `EVALUADOR`, `CAJERO`). Mantener únicamente como público el endpoint ciudadano acotado de seguimiento. | **ABIERTO (Confirmado)** |
| **H02** | **Cuentas de demostración con contraseñas débiles creadas en cualquier entorno** | 🔴 CRÍTICO | `UsuarioDataInitializer.java:28-51`, `portal-interno.html:333-340` | `UsuarioDataInitializer` se ejecuta sin restricción de perfil `@Profile("!prod")` y crea usuarios fijos `admin/admin123`, `evaluador/eval123`, `cajero/caja123`. El HTML expone botones de 1-clic con dichas credenciales. | Condicionar el inicializador a perfiles no productivos (`dev`, `local`, `test`). Eliminar botones de credenciales del HTML de producción e implementar aprovisionamiento seguro de usuarios vía variables de entorno o migración controlada. | **ABIERTO (Confirmado)** |
| **H03** | **Credenciales fijas de PostgreSQL y pgAdmin con puertos expuestos al exterior** | 🔴 CRÍTICO | `docker-compose.yml:8-12, 29-33`, `application.yml:8-10` | Secretos fijos (`POSTGRES_PASSWORD: muni_pass123`, `PGADMIN_DEFAULT_PASSWORD: admin`) en control de versiones. Puertos `5432` y `5050` mapeados directamente a la interfaz pública del host. | Externalizar contraseñas a archivo `.env` fuera del repositorio. Aislar PostgreSQL en la red interna de Docker (`muni_network`) sin exponer el puerto 5432 al host en producción. Deshabilitar pgAdmin en entornos productivos. | **ABIERTO (Confirmado)** |
| **H04** | **Secreto JWT predeterminado hardcodeado y expiración sin revocación** | 🔴 CRÍTICO | `application.yml:94-95`, `JwtTokenProvider.java:33` | Clave secreta por defecto en código: `MunicipalidadProvincialDeHuamangaGestionLicenciasSecretKeySegura2026!`. Si no se define variable, cualquier atacante puede firmar tokens como ADMIN. Expiración de 24h sin blacklist ni refresh token. | Exigir `JWT_SECRET` como variable obligatoria en producción (lanzar excepción en inicio si está vacía o usa el valor por defecto). Reducir tiempo de vida del access token (ej. 1 a 2 horas) e implementar revocación. | **ABIERTO (Confirmado)** |
| **H05** | **Spring Security permisivo: `anyRequest().permitAll()`, clickjacking y endpoints expuestos** | 🟠 ALTO | `SecurityConfig.java:52, 78, 79, 99` | Regla final `.anyRequest().permitAll()` incumple el principio de mínimo privilegio. `/actuator/**` y `/h2-console/**` son públicos. `frameOptions` está deshabilitado globalmente (riesgo de Clickjacking). | Cambiar regla final a `.anyRequest().authenticated()`. Proteger `/actuator/**` para acceso exclusivo de ADMIN/monitoreo o red interna. Habilitar `frameOptions.sameOrigin()`. | **ABIERTO (Confirmado)** |
| **H06** | **Riesgo en base de datos: `ddl-auto: update` en producción y tablas no versionadas** | 🔴 CRÍTICO | `application.yml:14`, `01-init-databases.sql` | `ddl-auto: update` activo en configuración base. El script SQL inicial no contiene las tablas `usuarios` ni `tarifas_tupa` (fueron creadas al vuelo por Hibernate). Falta herramienta de migración formal (Flyway/Liquibase). | Configurar `ddl-auto: validate` o `none` para producción. Incorporar Flyway con scripts SQL versionados (`V1__baseline.sql`, `V2__usuarios.sql`, `V3__tarifas_tupa.sql`) reproducibles y auditables. | **ABIERTO (Confirmado)** |
| **H07** | **Vulnerabilidad BOLA / IDOR y fuga de datos personales (Ley N° 29733)** | 🔴 CRÍTICO | `ExpedienteController.java:61-73`, `ExpedienteResponseDto.java`, `portal-ciudadano.js` | `GET /api/expedientes/{id}` y `GET /api/expedientes/tramite/{numeroTramite}` devuelven la totalidad de datos sensibles (DNI, teléfono, correo, representante SUNARP, Anexo 4) sin verificar si el solicitante es el titular del trámite. | Crear un DTO público de seguimiento (`SeguimientoCiudadanoDto`) que exponga únicamente estado, fecha y plazos, ofuscando datos personales. Proteger la consulta completa por ID exigiendo token o validación de identidad. | **ABIERTO (Confirmado)** |
| **H08** | **Generación y descarga de Licencias Oficiales sin validación de estado ni autenticación** | 🔴 CRÍTICO | `DocumentosFormulariosController.java:146-156`, `SecurityConfig.java:74` | El endpoint `GET /api/formularios/licencia/{id}/pdf` no verifica que el expediente esté `APROBADO`, permitiendo descargar un certificado de licencia con QR para cualquier expediente rechazado o en trámite. Además `/api/formularios/**` es público. | Validar rigurosamente que el expediente esté en estado `APROBADO` antes de emitir o renderizar el PDF de la licencia. Restringir la descarga de licencias oficiales a funcionarios o al titular autenticado/validado. | **ABIERTO (Confirmado)** |
| **H09** | **Ausencia de Dockerfile para empaquetado del aplicativo backend** | 🟠 ALTO | Raíz del proyecto / `docker/` | No existe ningún `Dockerfile` para construir la imagen del monolito modular Spring Boot. `docker-compose.yml` solo levanta Postgres y pgAdmin, impidiendo el despliegue estandarizado de la aplicación. | Crear un `Dockerfile` multi-stage optimizado (Eclipse Temurin 21 JRE, usuario no-root, optimización de capas) e integrarlo en `docker-compose.yml` como servicio `app`. | **ABIERTO** |
| **H10** | **Simulación no autenticada de adaptadores externos (SAT, Defensa Civil, Edificaciones)** | 🟡 MEDIO | `AdaptadorController.java`, `SatAdapterService.java`, etc. | `/api/integraciones/**` es alcanzable públicamente por `.anyRequest().permitAll()`. Los adaptadores son stubs en memoria con datos aleatorios que no reflejan la operativa manual documentada de la municipalidad. | Proteger `/api/integraciones/**` con roles de funcionario (`ADMIN`, `EVALUADOR`). Documentar formalmente el alcance de stubs vs. procedimiento manual documentado según directiva municipal. | **ABIERTO** |
| **H11** | **Falta de soporte normativo para observaciones y subsanaciones (Ley 27444 / Ley 28976)** | 🟡 MEDIO | `EstadoExpediente.java`, `ExpedienteController.java:240-248` | El endpoint `/api/expedientes/{id}/observar` invoca directamente `rechazar()`, terminando el procedimiento de forma irreversible y vulnerando el derecho de subsanación del administrado regulado por la LPAG. | Incorporar el estado `OBSERVADO` en la máquina de estados con plazo de subsanación y transición hacia reevaluación antes de proceder con el rechazo definitivo. | **ABIERTO** |
| **H12** | **Fuga de información de diagnóstico en el manejador de excepciones (CWE-209)** | 🟡 MEDIO | `GlobalExceptionHandler.java:74-82` | `handleGeneral(Exception ex)` devuelve `ex.getMessage()` directamente en el payload JSON ante cualquier fallo imprevisto, exponiendo posibles trazas internas o nombres de tablas de la base de datos. | Sustituir el mensaje en excepciones no controladas por un texto genérico institucional ("Ocurrió un error interno al procesar su solicitud") y registrar el detalle exclusivamente en logs protegidos. | **ABIERTO** |
| **H13** | **Pruebas de concurrencia y de integración desacopladas del entorno real de base de datos** | 🟡 MEDIO | `Concurrencia150UsuariosTest.java`, `application-test.yml` | La prueba de concurrencia de 150 usuarios (`Concurrencia150UsuariosTest`) utiliza mocks de Mockito en memoria y la suite de tests corre sobre H2 en memoria (`create-drop`), sin verificar bloqueos reales en PostgreSQL. | Diseñar pruebas de integración que se ejecuten contra PostgreSQL (o Testcontainers) y formalizar la ejecución de la prueba k6 (`tests/load-test/k6-load-test.js`) en la fase de QA. | **ABIERTO** |
| **H14** | **Advertencia de remitente de correo nulo en pruebas de notificación** | 🟢 BAJO | `NotificacionEmailService.java:231`, `NotificacionEmailServiceTest.java` | Durante la ejecución de `mvn test`, `NotificacionEmailServiceTest` emite warnings por `From address must not be null`, debido a que las propiedades `@Value` no son inyectadas en pruebas con `@InjectMocks`. | Inicializar las variables con valores de contingencia o parametrizar el constructor del servicio para una inyección limpia en tests unitarios. | **ABIERTO** |
| **H15** | **Propiedades de configuración huérfanas en `application.yml`** | 🟢 BAJO | `application.yml:48-55` | Existen bloques de configuración para URLs de microservicios (`URL_SERVICIO_FORMULARIOS`, `URL_SERVICIO_VERIFICACION`, `URL_ADAPTADOR_INTEGRACION`) no utilizados tras la consolidación al Monolito Modular. | Limpiar las propiedades obsoletas para evitar confusiones operativas en los entornos de despliegue. | **ABIERTO** |
| **H16** | **Discrepancia de nomenclatura de columna en script inicial de PostgreSQL** | 🟢 BAJO | `01-init-databases.sql:217` | En la sentencia de inserción semilla se utiliza `nombreComercial` (camelCase) en lugar de `nombre_comercial` (snake_case), lo que genera incompatibilidades con DDL estricto. | Corregir la referencia de columna en el script SQL para garantizar total coherencia relacional. | **ABIERTO** |

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
│ [FASE 4] Automatización y Ejecución de Pruebas (QA)                    │
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
