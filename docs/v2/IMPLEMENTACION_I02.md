# MUNIHUAMANGA V2 — INFORME DE IMPLEMENTACIÓN INCREMENTO I02
## Seguridad, Autorización a Nivel de Objeto y Protección de Datos Personales

- **Fecha de Ejecución:** 14 de Octubre de 2026
- **Rama Git:** `develop/munihuamanga-v2`
- **Normativa y Estándares de Referencia:**
  - Ley N° 29733 (Protección de Datos Personales del Perú)
  - Ley N° 28976 / TUO D.S. N° 046-2017-PCM (Ley Marco de Licencias de Funcionamiento)
  - Ley N° 27444 (Ley del Procedimiento Administrativo General)
  - ERS-MH-V2-001 v1.0 / DAS-MH-V2-001 v1.0
  - OWASP Top 10 API Security 2023 (API1: BOLA / Broken Object Level Authorization, API3: Broken Object Property Level Authorization, API5: Broken Function Level Authorization)

---

### 1. RESUMEN EJECUTIVO DEL INCREMENTO I02

En el Incremento I02 se corrigieron las vulnerabilidades críticas de acceso no autorizado y fuga de datos personales identificadas durante la auditoría inicial de MuniHuamanga V2. Se eliminó la posibilidad de que usuarios anónimos o terceros no autorizados consulten expedientes completos, descarguen declaraciones juradas o subsanen trámites ajenos mediante enumeración de identificadores (BOLA/IDOR).

Asimismo, se implementó el componente de autorización contextual `ExpedienteSecurity`, se añadieron los roles institucionales `ROLE_CIUDADANO` y `ROLE_INSPECTOR`, se aplicó minimización estricta de datos en la consulta pública de licencias vía QR (ofuscando DNI/RUC y nombres), se habilitó un endpoint ampliado exclusivo para fiscalizadores en campo, se auditaron y blindaron las credenciales de producción, y se eliminaron los stubs de simulación ficticia incompatibles con V2.

La suite de pruebas automatizadas aumentó de **104 a 123 pruebas unitarias y de integración**, logrando una tasa de éxito del **100% con cero regresiones** (`mvn clean verify`).

---

### 2. VULNERABILIDADES CORREGIDAS

| Vulnerabilidad | Código CWE / OWASP | Severidad Previa | Diagnóstico en V1 | Solución Implementada en I02 | Estado |
|---|---|---|---|---|---|
| **BOLA / IDOR en Expedientes** | CWE-639 / API1:2023 | 🔴 CRÍTICA | `/api/expedientes/{id}` y `/detalle/{numeroTramite}` permitían lectura completa de expedientes sin verificar titularidad ni rol. | Implementación de `@PreAuthorize("@expedienteSecurity.puedeVerExpediente(...)")` y `@expedienteSecurity.puedeVerExpedientePorTramite(...)`. | **RESUELTO** |
| **Exposición Anónima de Documentos** | CWE-284 / API5:2023 | 🔴 CRÍTICA | Endpoints `/documentos/**` no requerían autenticación en `SecurityConfig.java`, permitiendo descarga de Anexo 1, Anexo 3, Anexo 4 y licencias. | Blindaje en `SecurityConfig` exigiendo `authenticated()` y evaluación `@expedienteSecurity.puedeDescargarDocumento(...)`. | **RESUELTO** |
| **Subsanación por Terceros** | CWE-862 / API5:2023 | 🔴 CRÍTICA | `POST /api/expedientes/{id}/subsanar` no validaba que el usuario fuese el titular ni que el expediente estuviera en estado `OBSERVADO`. | Validación estricta en `@expedienteSecurity.puedeSubsanar(...)`: exige coincidencia de titular o rol administrativo, y estado `OBSERVADO`. | **RESUELTO** |
| **Fuga de Datos Personales en QR** | CWE-359 / API3:2023 | 🟠 ALTA | Endpoint `/api/public/licencias/{cod}` devolvía DNI completo del administrado a cualquier persona que escaneara el QR (violación Ley N° 29733). | Función de ofuscación de identidad (`ofuscarDocumento`), conservando solo primeros 2 y últimos 2 dígitos (`42***91`). Creación de endpoint ampliado autenticado para inspectores. | **RESUELTO** |
| **Credenciales Inseguras por Defecto en Prod** | CWE-798 / API8:2023 | 🟠 ALTA | `application-prod.yml` heredaba valores predeterminados de base de datos y `JWT_SECRET` estático documentado en guías. | `application-prod.yml` exige ahora variables de entorno obligatorias `${JWT_SECRET}` y `${SPRING_DATASOURCE_PASSWORD}` sin fallbacks inseguros. `.env.example` actualizado con advertencia de rotación. | **RESUELTO** |
| **Stubs de Simulación Ficticia Incompatibles** | Violación ERS-1.3 | 🟡 MEDIA | Clases `DefensaCivilAdapterService` y `FiscalizacionAdapterService` generaban informes técnicos y actas de fiscalización con números aleatorios. | Eliminación segura de las 4 clases de stubs obsoletas tras comprobar que no tenían dependencias activas en el núcleo de expedientes. | **RESUELTO** |

---

### 3. ARQUITECTURA DE AUTORIZACIÓN: `ExpedienteSecurity`

Se implementó el componente Spring `@Component("expedienteSecurity")` en `pe.gob.munihuamanga.licencias.expedientes.security.ExpedienteSecurity`. Este servicio evalúa dinámicamente cuatro dimensiones de seguridad en tiempo de ejecución:

```
                  ┌────────────────────────────────────────────────────────┐
                  │                 Petición HTTP Entrante                 │
                  └───────────────────────────┬────────────────────────────┘
                                              │
                                              ▼
                             ┌─────────────────────────────────┐
                             │       SecurityFilterChain       │
                             │  (Validación de Token JWT)      │
                             └────────────────┬────────────────┘
                                              │
                        ┌─────────────────────┴─────────────────────┐
                        ▼                                           ▼
             [Rutas Públicas Mínimas]                   [Operaciones Protegidas]
             - /api/expedientes/tramite/{num}          - /api/expedientes/{id}
             - /api/public/licencias/{cod}             - /api/expedientes/detalle/{num}
             - /api/auth/login                         - /api/expedientes/{id}/documentos/**
             - Portales Web HTML / CSS                 - /api/expedientes/{id}/subsanar
                        │                                           │
                        ▼                                           ▼
             Respuesta Ofuscada (Ley 29733)              @PreAuthorize("@expedienteSecurity...")
                                                                    │
                                       ┌────────────────────────────┴────────────────────────────┐
                                       ▼                                                         ▼
                         [Funcionario Municipal]                                       [Ciudadano Administrado]
                         - ROLE_ADMIN / EVALUADOR:                                     - Verifica coincidencia:
                           Acceso a gestión y dictámenes                                 * solicitanteId == user.id, O
                         - ROLE_CAJERO:                                                  * documentoIdentidad == username
                           Acceso restringido a tasas y pagos                          - Bloqueo de expedientes ajenos (403)
                         - ROLE_INSPECTOR:                                             - Subsanación condicionada a
                           Lectura ampliada de QR; bloqueo                               estado == OBSERVADO
                           de emisión, actas y pagos
```

#### Métodos de `ExpedienteSecurity`:
1. `puedeVerExpediente(Authentication auth, UUID expedienteId)`: Permite acceso a administradores, evaluadores, cajeros o al administrado titular del expediente.
2. `puedeVerExpedientePorTramite(Authentication auth, String numeroTramite)`: Permite consulta del detalle completo oficial únicamente a personal autorizado o al ciudadano titular.
3. `puedeDescargarDocumento(Authentication auth, UUID expedienteId)`: Protege las declaraciones juradas y licencias oficiales frente a descargas no autorizadas.
4. `puedeSubsanar(Authentication auth, UUID expedienteId)`: Exige doble condición: legitimidad del solicitante (o funcionario) y que el expediente se encuentre efectivamente en estado `OBSERVADO`.

---

### 4. MATRIZ DE ENDPOINTS: PÚBLICOS VS. PROTEGIDOS

#### 4.1 Endpoints Públicos (Acceso Anónimo Permitido con Datos Protegidos)
| Método | Endpoint | Propósito | Medidas de Protección Aplicadas |
|---|---|---|---|
| `POST` | `/api/auth/login` | Autenticación institucional | Rate limiting base, contraseñas con BCrypt, emisión de JWT firmado HMAC-SHA512. |
| `POST` | `/api/expedientes` | Mesa de Partes Virtual (Registro Anexo 1) | Validación de esquema Jakarta Validation, asignación de UUID y número correlativo. |
| `GET` | `/api/expedientes/tramite/{numeroTramite}` | Seguimiento Ciudadano de Trámite | Retorna `SeguimientoCiudadanoDto` con datos de contacto excluidos y documento/titular ofuscados (`42***91`). |
| `GET` | `/api/public/licencias/{codigoLicencia}` | Verificación QR de Licencias | Retorna `VerificacionLicenciaDto` con documento de identidad ofuscado según Ley N° 29733. |
| `GET` | `/api/public/licencias/{codigoLicencia}/qr` | Imagen PNG del Código QR | Generación dinámica de matriz gráfica sin datos confidenciales incrustados. |
| `GET` | `/actuator/health` | Sonda de salud de infraestructura | Solo expone estado UP/DOWN sin detalles de configuración interna. |
| `GET` | `/*.html`, `/css/**`, `/js/**`, `/img/**` | Portales web estáticos de la plataforma | Solo lectura de archivos estáticos públicos. |

#### 4.2 Endpoints Protegidos (Autenticación y Autorización Obligatoria)
| Método | Endpoint | Rol Requerido / Regla SpEL | Acción Autorizada |
|---|---|---|---|
| `GET` | `/api/expedientes` | `ROLE_ADMIN`, `ROLE_EVALUADOR`, `ROLE_CAJERO` | Bandeja general de expedientes para funcionarios. |
| `GET` | `/api/expedientes/{id}` | `@expedienteSecurity.puedeVerExpediente(auth, #id)` | Consulta de expediente por ID (funcionarios o administrado titular). |
| `GET` | `/api/expedientes/detalle/{numeroTramite}` | `@expedienteSecurity.puedeVerExpedientePorTramite(auth, #numeroTramite)` | Detalle completo de expediente para edición o emisión formal. |
| `GET` | `/api/expedientes/{id}/historial` | `@expedienteSecurity.puedeVerExpediente(auth, #id)` | Trazabilidad y auditoría de transiciones de estado. |
| `GET` | `/api/expedientes/{id}/desglose-tasa` | `@expedienteSecurity.puedeVerExpediente(auth, #id)` | Consulta de liquidación tributaria según riesgo TUPA. |
| `GET` | `/api/expedientes/{id}/documentos/**` | `@expedienteSecurity.puedeDescargarDocumento(auth, #id)` | Descarga de PDFs (Anexo 1, Anexo 3, Anexo 4, Voucher SAT, Licencia). |
| `POST` | `/api/expedientes/{id}/subsanar` | `@expedienteSecurity.puedeSubsanar(auth, #id)` | Subsanación ciudadana de observaciones (solo titular y estado `OBSERVADO`). |
| `POST`| `/api/expedientes/{id}/pago` | `ROLE_CAJERO`, `ROLE_ADMIN` | Registro manual y conciliación de voucher SAT. |
| `POST`| `/api/expedientes/{id}/clasificar-riesgo` | `ROLE_EVALUADOR`, `ROLE_ADMIN` | Registro de nivel de riesgo ITSE y cálculo de tasa. |
| `POST`| `/api/expedientes/{id}/aprobar` | `ROLE_EVALUADOR`, `ROLE_ADMIN` | Emisión definitiva de licencia de funcionamiento y QR. |
| `POST`| `/api/expedientes/{id}/observar` | `ROLE_EVALUADOR`, `ROLE_ADMIN` | Notificación formal de observaciones técnicas. |
| `POST`| `/api/expedientes/{id}/rechazar` | `ROLE_EVALUADOR`, `ROLE_ADMIN` | Denegatoria fundada de la solicitud. |
| `GET` | `/api/public/licencias/{codigoLicencia}/ampliada` | `ROLE_INSPECTOR`, `ROLE_EVALUADOR`, `ROLE_ADMIN` | Consulta técnica en campo con georreferenciación y zonificación. |
| `GET` | `/api/integraciones/sat/validar-pago/**` | `ROLE_CAJERO`, `ROLE_ADMIN` | Consulta de conciliación de pagos SAT. |
| `GET` | `/api/integraciones/edificaciones/zonificacion`| `ROLE_EVALUADOR`, `ROLE_ADMIN` | Consulta referencial de zonificación PDU. |
| `GET` | `/api/tarifas-tupa/**` (modificaciones) | `ROLE_ADMIN` | Gestión y actualización de tasas TUPA municipales. |

---

### 5. MODELO DE ROLES ACTUALIZADO (`RolUsuario.java`)

Se incorporaron formalmente en el enum `pe.gob.munihuamanga.licencias.common.enums.RolUsuario`:

1. **`ROLE_ADMIN`**: Administrador integral del sistema municipal (gestión de usuarios, auditoría, configuración de tasas TUPA y supervisión general).
2. **`ROLE_EVALUADOR`**: Funcionario de la Subgerencia de Comercio y Licencias (calificación técnica de expedientes, registro de riesgo ITSE, observaciones, emisión de licencias y denegatorias).
3. **`ROLE_CAJERO`**: Funcionario de ventanilla de recaudación SAT Huamanga (emisión de vouchers de pago y registro manual de constancias de recaudación).
4. **`ROLE_INSPECTOR`** *(Nuevo en I02)*: Inspector municipal de campo. Cuenta únicamente con permisos de lectura para verificar autenticidad, vigencia, ubicación física autorizada y nivel de riesgo vía `/api/public/licencias/{cod}/ampliada`. **No posee facultades para modificar expedientes, validar pagos ni emitir actas ficticias**.
5. **`ROLE_CIUDADANO`** *(Nuevo en I02)*: Administrado autenticado en el portal municipal. Permite la visualización de sus propios expedientes, descarga de sus anexos oficiales y subsanación de observaciones en expedientes propios en estado `OBSERVADO`.

---

### 6. ARCHIVOS MODIFICADOS, CREADOS Y ELIMINADOS

#### 6.1 Archivos Creados
- `common-domain/src/main/java/pe/gob/munihuamanga/licencias/common/dto/VerificacionAmpliadaDto.java`: DTO para la inspección técnica en campo con coordenadas geográficas y zonificación.
- `servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/security/ExpedienteSecurity.java`: Bean de autorización granular a nivel de objeto (BOLA/IDOR).
- `servicio-expedientes/src/test/java/pe/gob/munihuamanga/licencias/expedientes/security/ExpedienteSecurityAuthorizationTest.java`: Suite de 20 pruebas automáticas cubriendo los 10 escenarios obligatorios.
- `docs/v2/IMPLEMENTACION_I02.md`: Presente documento de entrega y auditoría técnica.

#### 6.2 Archivos Modificados
- `common-domain/src/main/java/pe/gob/munihuamanga/licencias/common/enums/RolUsuario.java`: Incorporación de `ROLE_INSPECTOR` y `ROLE_CIUDADANO`.
- `servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/config/SecurityConfig.java`: Cierre de endpoints sensibles (`/detalle/**`, `/documentos/**`, `/subsanar`), configuración estricta de filtros.
- `servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/config/UsuarioDataInitializer.java`: Usuarios semilla para entornos dev/local/test (incorporando inspector y ciudadanos titular/tercero).
- `servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/controller/ExpedienteController.java`: Anotaciones `@PreAuthorize` en todos los métodos de consulta y modificación, alias `/documentos/anexo1`.
- `servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/controller/DocumentosFormulariosController.java`: Protección de generación de licencias para evaluadores y administradores.
- `servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/controller/PublicLicenciasController.java`: Endpoint `/ampliada` protegido con `@PreAuthorize("hasAnyRole('INSPECTOR', 'EVALUADOR', 'ADMIN')")`.
- `servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/controller/GlobalExceptionHandler.java`: Manejo de `NoResourceFoundException` (404) y excepciones de seguridad seguras sin stacktrace.
- `servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/service/ExpedienteService.java`: Ofuscación de documento en `verificarLicencia()` e implementación de `verificarLicenciaAmpliada()`.
- `servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/integracion/controller/AdaptadorController.java`: Eliminación de endpoints de fiscalización y simulación ITSE.
- `servicio-expedientes/src/main/resources/application-prod.yml`: Exigencia estricta de variables de entorno `${JWT_SECRET}` y credenciales de base de datos en producción.
- `.env.example`: Reemplazo del secreto estático por placeholder y advertencia de rotación obligatoria.
- `servicio-expedientes/src/test/java/pe/gob/munihuamanga/licencias/expedientes/security/SeguridadEndToEndTest.java`: Adaptación a endpoints protegidos de documentos.
- `servicio-expedientes/src/test/java/pe/gob/munihuamanga/licencias/expedientes/integracion/controller/AdaptadorControllerTest.java`: Adecuación tras retiro de stubs obsoletos.

#### 6.3 Archivos Eliminados (Código Obsoleto)
- `servicio-expedientes/.../integracion/port/FiscalizacionPort.java`
- `servicio-expedientes/.../integracion/adapter/FiscalizacionAdapterService.java`
- `servicio-expedientes/.../integracion/port/DefensaCivilPort.java`
- `servicio-expedientes/.../integracion/adapter/DefensaCivilAdapterService.java`

---

### 7. RESULTADOS DE PRUEBAS AUTOMATIZADAS (`mvn clean verify`)

La ejecución del comando `mvn clean verify` arrojó los siguientes resultados certificados:

```
[INFO] Reactor Summary for MuniHuamanga - Gestión de Licencias de Funcionamiento 1.0.0-SNAPSHOT:
[INFO] 
[INFO] MuniHuamanga - Gestión de Licencias de Funcionamiento SUCCESS [  0.094 s]
[INFO] Common Domain ...................................... SUCCESS [  2.892 s]
[INFO] MuniHuamanga - Monolito Modular .................... SUCCESS [ 25.002 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  28.290 s
[INFO] Finished at: 2026-10-14T00:31:00-05:00
```

#### Métricas de Pruebas:
- **Total de pruebas ejecutadas:** 123 (+19 pruebas sobre la línea base de 104)
- **Fallos (Failures):** 0
- **Errores (Errors):** 0
- **Omitidas (Skipped):** 0
- **Tasa de éxito:** 100%

#### Detalle de la Suite de Seguridad `ExpedienteSecurityAuthorizationTest` (20 pruebas):
1. `REQ-1`: Usuario anónimo es rechazado con 401 al consultar expediente por ID (`/api/expedientes/{id}`).
2. `REQ-1`: Usuario anónimo es rechazado con 401 al consultar detalle de expediente (`/api/expedientes/detalle/{num}`).
3. `REQ-1`: Usuario anónimo es rechazado con 401 al intentar descargar documentos internos (`/documentos/anexo1`).
4. `REQ-2`: Ciudadano tercero recibe 403 Forbidden al intentar ver expediente de otro ciudadano por ID (Protección BOLA/IDOR).
5. `REQ-2`: Ciudadano tercero recibe 403 Forbidden al intentar ver detalle de expediente ajeno por correlativo.
6. `REQ-2`: Ciudadano titular (DNI coincidente) puede consultar su propio expediente por ID (200 OK).
7. `REQ-2`: Ciudadano titular (DNI coincidente) puede consultar su propio detalle por correlativo (200 OK).
8. `REQ-3`: Ciudadano tercero recibe 403 Forbidden al intentar descargar Anexo 1 ajeno.
9. `REQ-3`: Ciudadano titular puede descargar el Anexo 1 en PDF de su propio trámite (200 OK, application/pdf).
10. `REQ-4`: Ciudadano tercero recibe 403 Forbidden al intentar subsanar observaciones de expediente ajeno.
11. `REQ-4`: Subsanación es rechazada con 403 Forbidden cuando el expediente NO se encuentra en estado `OBSERVADO`.
12. `REQ-5`: Funcionarios EVALUADOR y ADMIN pueden consultar cualquier expediente (200 OK).
13. `REQ-5`: Funcionario CAJERO no puede aprobar ni dictaminar licencias (403 Forbidden).
14. `REQ-6`: INSPECTOR no puede evaluar ni aprobar expedientes (403 Forbidden).
15. `REQ-6`: INSPECTOR no puede registrar pagos en ventanilla (403 Forbidden).
16. `REQ-6`: INSPECTOR SÍ puede consultar la verificación ampliada de licencia con georreferenciación (200 OK).
17. `REQ-7`: Consulta de QR pública minimiza y ofusca documento de identidad (`42***91`, Ley N° 29733).
18. `REQ-7`: Verificación ampliada sin credenciales es rechazada con 401 Unauthorized.
19. `REQ-8`: Seguimiento público por número de trámite funciona anónimamente con datos de titular ofuscados.
20. `REQ-9`: Intento no autorizado a expediente inexistente produce respuesta segura (403) sin fugar información ni exponer stacktrace.

---

### 8. COMPATIBILIDAD CON EL FRONTEND ACTUAL

- **Portal Ciudadano (`portal-ciudadano.html`):** Mantiene compatibilidad total. La consulta de seguimiento (`/api/expedientes/tramite/{num}`) y el registro inicial de solicitudes vía Mesa de Partes Virtual siguen operando sin alteraciones.
- **Portal de Verificación QR (`verificar-licencia.html`):** Mantiene compatibilidad con el DTO público `VerificacionLicenciaDto`. Muestra los datos de la licencia con el DNI ofuscado y estado vigente sin requerir inicio de sesión.
- **Portal Interno (`portal-interno.html`):** Los funcionarios autenticados (admin, evaluador, cajero) conservan acceso íntegro según sus tokens JWT. Se preparó el backend para soportar perfiles de inspectores y ciudadanos que se integrarán en la interfaz en los siguientes incrementos.

---

### 9. RIESGOS RESIDUALES Y PENDIENTES PARA INCREMENTO I03

1. **Gestión Completa de Cuentas Ciudadanas (I03):**
   - Actualmente, la verificación de propiedad ciudadana en `ExpedienteSecurity` se apoya en la coincidencia del `username` del usuario con el `documentoIdentidad` o `numeroRuc` del expediente, o con el `solicitanteId` (UUID).
   - En el Incremento I03 se formalizará el registro de cuentas ciudadanas, autenticación propia y vinculación relacional en base de datos.
2. **Rotación de Secretos en Despliegues Existentes:**
   - Si la clave `JWT_SECRET` documentada en versiones previas fue desplegada en algún entorno accesible, es mandatorio rotarla mediante la variable de entorno correspondiente antes del paso a producción.
3. **Mantenimiento del Esquema de Base de Datos:**
   - No se aplicaron modificaciones destructivas a PostgreSQL. La columna `rol` en la tabla `usuarios` admite los nuevos valores del enum sin requerir migración de datos existente.
