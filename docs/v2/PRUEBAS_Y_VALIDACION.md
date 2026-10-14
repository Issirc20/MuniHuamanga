# Estrategia de Pruebas y Validación de Calidad — MuniHuamanga V2

Municipalidad Provincial de Huamanga — Gerencia de Licencias
**Fecha:** 13 de octubre de 2026
**Documentos Rectores:** [ERS-MH-V2-001 v1.0] (Sección 10) y [DAS-MH-V2-001 v1.0] (Sección 13)

---

## 1. Línea Base y Ejecución Actual de Pruebas Automatizadas

Tras la conclusión y verificación exhaustiva del **Incremento I09 (M10: Verificación QR Diferenciada y Consulta Móvil para Inspectores)**, se ejecutó la suite completa de pruebas unitarias, de integración, de concurrencia y de seguridad mediante Maven Surefire:
- **Comando:** `mvn clean verify`
- **Resultado:** **301 pruebas ejecutadas, 0 fallos, 0 errores, 0 omitidas**
- **Tiempo de ejecución total:** ~48 s
- **Estado del Reactor Maven:**
  - `MuniHuamanga - Gestión de Licencias de Funcionamiento`: SUCCESS
  - `Common Domain`: SUCCESS
  - `MuniHuamanga - Monolito Modular` (`servicio-expedientes`): SUCCESS

### 1.1 Desglose por Clase de Prueba (301 Pruebas)

| Clase de Prueba | Pruebas | Fallos | Errores | Alcance / Cobertura Funcional |
| :--- | :---: | :---: | :---: | :--- |
| `VerificacionQrI09Test` *(Nuevo I09)* | 12 | 0 | 0 | Pruebas de verificación QR pública y consulta para inspectores: minimización estricta de datos (DNI totalmente excluido en respuesta pública), manejo de token inexistente (400), no-vigencia de licencia revocada/suspendida/vencida, rechazo de validez oficial si resolución no está firmada digitalmente, consulta móvil con georreferenciación oficial para `ROLE_INSPECTOR`, registro inmutable de auditoría en `auditoria_consultas_inspeccion`, bloqueo BOLA/IDOR a ciudadanos (403), bloqueo a peticiones anónimas (401), diferenciación de coordenadas `RECTIFICADA_APROBADA` vs `DECLARADA_OFICIAL`, bloqueo de mutaciones a inspectores (403), y limitación de peticiones por IP (*Rate Limiting* HTTP 429 con `Retry-After: 60`). |
| `AuditoriaI08_1Test` *(Auditado I08.1)* | 8 | 0 | 0 | Pruebas de regresión y auditoría: bloqueo de emisión directa legada V1 con excepción, marca de agua obligatoria "VISTA PRELIMINAR - SIN VALIDEZ OFICIAL" en licencias sin firma válida, minimización de datos en QR público, servicio inmutable de bytes PDF desde base de datos, trazabilidad de operaciones de firma remota, restricción UNIQUE anti doble licencia concurrente, bloqueo RBAC de descargas a inspectores y fallo fail-closed sin firma previa. |
| `ResolucionAdministrativaServiceTest` *(Nuevo I08)* | 8 | 0 | 0 | Creación de borrador de resolución para expediente en `APTO_PARA_RESOLUCION`, bloqueo si el expediente no está apto, aprobación formal por autoridad competente, rechazo de auto-aprobación indebida (segregación de funciones), bloqueo ante servicio de firma institucional deshabilitado (`ERROR_FIRMA`), firma exitosa con proveedor activo, generación de OpenPDF vectorial preliminar con marca de agua "SIN VALIDEZ OFICIAL", y verificación de hash SHA-256 inmutable. |
| `LicenciaServiceTest` *(Auditado I09)* | 5 | 0 | 0 | Emisión de licencia oficial condicionada a resolución `FAVORABLE` y `FIRMADA`, rechazo si la resolución está pendiente de firma o es denegatoria, emisión atómica con correlativo `LIC-YYYY-XXXXX`, transición del expediente a `APROBADO`, generación de token QR criptográfico no enumerable (`SecureRandom`), e idempotencia ante concurrencia/reintentos (no doble licencia). |
| `SecuenciaResolucionLicenciaMultiInstanciaTest` *(Nuevo I08)* | 4 | 0 | 0 | Generación atómica secuencial de correlativos `RES-YYYY-XXXXX` y `LIC-YYYY-XXXXX` con bloqueo pesimista en base de datos (`PESSIMISTIC_WRITE`), inicialización anual atómica (`@PostConstruct` y `REQUIRES_NEW`), y pruebas de concurrencia multihilo paralelas (5 hilos) garantizando unicidad y cero colisiones. |
| `ResolucionLicenciaIntegrationTest` *(Nuevo I08)* | 4 | 0 | 0 | Pruebas MockMvc de seguridad y autorización: rechazo RBAC a ciudadanos para aprobar resoluciones (HTTP 403), rechazo RBAC a ciudadanos y evaluadores para emitir licencias (HTTP 403), protección BOLA/IDOR en resoluciones ajenas (HTTP 403), y acceso público no autenticado al endpoint de verificación QR `/api/licencias/verificar/{codigoQr}` (HTTP permitAll). |
| `ChecklistAdministrativoServiceTest` *(Auditado I07.1)* | 7 | 0 | 0 | Evaluación automatizada de 9 criterios normativos integrando módulos originales (presentación, titularidad, DJ Anexo 1, personería jurídica con vigencia de poder registral o datos SUNARP, autorizaciones sectoriales, descarte de documentos en OBSERVADO, hashes SHA-256 de documentos vigentes, observaciones pendientes, validación manual de pago SAT, reporte técnico de zonificación compatible, sustento ITSE previa según riesgo con planos técnicos y ausencia de impedimentos). |
| `InformeAdministrativoServiceTest` *(Auditado I07.1)* | 9 | 0 | 0 | Ciclo de vida completo del informe técnico administrativo: creación en BORRADOR con correlativo atómico `INF-YYYY-XXXXX`, bloqueo estricto de conclusión FAVORABLE si el checklist no está apto, flujo de revisión y devolución con observaciones motivadas, visado obligatorio por revisor, verificación estricta de segregación de funciones (se prohíbe auto-aprobación del evaluador sin excepción para admin_master), aprobación por autoridad competente con transición del expediente a APTO_PARA_RESOLUCION, bloqueo de aprobación directa sin visado, bloqueo de doble aprobación y generación/descarga de PDF preliminar borrador con marca de agua. |
| `SolicitudRectificacionUbicacionServiceTest` *(Auditado I07.1 - CA-08)* | 5 | 0 | 0 | Registro de solicitud de rectificación de coordenadas geográficas en estado PENDIENTE manteniendo intactas las coordenadas oficiales, cálculo geodésico Haversine de desplazamiento como alerta informativa de riesgo, validación de correspondencia de expediente, aprobación humana motivada con distancia mayor a 300m y dictamen con actualización oficial aprobada por evaluador municipal. |
| `EvaluacionAdministrativaIntegrationTest` *(Auditado I07.1)* | 9 | 0 | 0 | Pruebas MockMvc HTTP integrales: consulta de bandeja unificada por evaluador (200), denegación de acceso a ciudadanos (403), consulta de checklist integral (200), elaboración de informe favorable con requisitos completos (201), flujo integral BORRADOR -> VISADO -> APROBADO -> APTO_PARA_RESOLUCION (200), descarga de PDF preliminar (200), solicitud ciudadana de rectificación de coordenadas (201), evaluación/aprobación de rectificación por evaluador municipal (200) y control de seguridad BOLA/IDOR ante intentos de rectificación ajena (403). |
| `SecuenciaInformeMultiInstanciaTest` *(Nuevo I07.1)* | 2 | 0 | 0 | Generación transaccional segura ante concurrencia multihilo de correlativos `INF-YYYY-XXXXX` con bloqueo pesimista en base de datos (`PESSIMISTIC_WRITE`), verificación de inicialización atómica de fila anual (`REQUIRES_NEW`) y garantía de unicidad sin colisiones. |
| `PagoExpedienteServiceTest` *(Auditado I06.1)* | 10 | 0 | 0 | Declaración de voucher con estado PENDIENTE_VALIDACION, algoritmo de normalización y unicidad canónica, validación manual por cajero con registro de evidencia y sin auto-aprobación ni mutación indebida a DOCUMENTOS_VALIDADOS, bloqueo pesimista contra doble validación concurrente, rechazo de validación de comprobantes previamente rechazados, inmutabilidad de decisiones favorables, suficiencia de tasa TUPA requerida, y control BOLA/IDOR. |
| `EvaluacionZonificacionServiceTest` *(Auditado I06.1)* | 6 | 0 | 0 | Evaluación técnica motivada (COMPATIBLE/NO_COMPATIBLE) con base en PDU y reporte técnico oficial por funcionario evaluador, verificación estricta de correspondencia del establecimiento con el expediente, sustento técnico obligatorio (informe min 8 chars, motivación min 15 chars), enlace a documento versionado sustentatorio, historial append-only y control BOLA/IDOR en consulta ciudadana. |
| `PagoZonificacionIntegrationTest` *(Auditado I06.1)* | 12 | 0 | 0 | Pruebas MockMvc HTTP completas: declaración de pago (201), validación por cajero (200), rechazo de doble validación (400), rechazo de validación de pago rechazado (400), almacenamiento de código normalizado (201), denegación de validación a ciudadanos (403), rechazo de código duplicado (400), observación/rechazo de pagos (200), registro de zonificación por evaluador (201), denegación de zonificación a ciudadanos (403), control BOLA/IDOR en bandeja de pagos (403), y compatibilidad con expedientes V1. |
| `DocumentoVersionadoServiceTest` *(Auditado I05.1)* | 14 | 0 | 0 | Versionado inmutable v1..vN, no sobreescritura, hash SHA-256, magic numbers, mitigación path traversal, validación de pertenencia de observación a expediente, desmarcación atómica en memoria/BD y rechazo IDOR a trámites V1 no reclamados. |
| `ObservacionSubsanacionIntegrationTest` *(Auditado I05.1)* | 7 | 0 | 0 | Observaciones técnicas según LPAG (Art. 136.1: 2 días hábiles excluyendo feriados nacionales peruanos; ITSE: 5 días; Art. 143: 10 días), rechazo de subsanación parcial con observaciones pendientes, y denegación de observaciones de trámites V1 sin titularidad verificada a ciudadanos con DNI coincidente. |
| `AnexosItseGeneracionTest` | 2 | 0 | 0 | Reutilización íntegra de datos de borrador, cuenta y establecimiento para generar formatos oficiales PDF Anexo 1, 3 y 4. |
| `ExpedienteSecurityAuthorizationTest` | 24 | 0 | 0 | Seguridad BOLA/IDOR, autorización contextual, roles ciudadano/inspector, minimización QR y bloqueo estricto de creación directa a ciudadanos. |
| `PresentacionConcurrenciaIntegrationTest` | 4 | 0 | 0 | Pruebas de concurrencia multihilo sobre mismo borrador (pessimistic lock), borradores distintos, reintentos idempotentes y auditoría de descargas. |
| `SolicitudBorradorServiceTest` | 9 | 0 | 0 | Avances parciales de borrador, checklist de requisitos, magic numbers, mitigación de path traversal y cálculo SHA-256. |
| `PresentacionFormalServiceTest` | 6 | 0 | 0 | Transición atómica a PRESENTADO, generación de correlativo único, verificación de titularidad y bloqueo a personas jurídicas sin vigencia de poder. |
| `BorradorSecurityIntegrationTest` | 2 | 0 | 0 | Control de acceso BOLA/IDOR a nivel de borrador y operadores autorizados. |
| `CuentaEstablecimientoSecurityIntegrationTest` | 8 | 0 | 0 | Autorización de cuentas DNI/RUC, operadores y establecimientos comerciales. |
| `CuentaServiceTest` | 5 | 0 | 0 | Gestión de cuentas ciudadanas/jurídicas, unicidad de documento y operadores. |
| `EstablecimientoServiceTest` | 3 | 0 | 0 | Validación de coordenadas geográficas, zonificación preliminar y catálogos CIIU. |
| `ReclamacionExpedienteServiceTest` | 3 | 0 | 0 | Solicitud y resolución administrativa de reclamación de expedientes históricos V1. |
| `SecurityConfigProdTest` | 3 | 0 | 0 | Verificación de que perfiles de producción bloquean consola H2 y no exponen endpoints de test. |
| `SeguridadEndToEndTest` | 7 | 0 | 0 | Pruebas end-to-end de autenticación JWT, RBAC de cajero, ofuscación de trámite y descarga autorizada. |
| `ExpedienteServiceTest` | 13 | 0 | 0 | Creación de trámite, transiciones, subsanación, pago y aprobación de expedientes. |
| `EstadoExpedienteValidatorTest` | 10 | 0 | 0 | Reglas de transición entre estados y bloqueos de aprobación sin requisitos. |
| `DocumentoPdfServiceCompletoTest` | 8 | 0 | 0 | Renderizado en memoria de Anexo 1, Anexo 3, Anexo 4 y Licencia con OpenPDF. |
| `LicenciaPdfServiceTest` | 6 | 0 | 0 | Estructura visual y elementos del certificado oficial de licencia. |
| `Anexo1PdfGeneratorTest` | 10 | 0 | 0 | Generador nativo del Anexo 1 Declaración Jurada. |
| `NotificacionEmailServiceTest` | 7 | 0 | 0 | Envíos asíncronos JavaMail/SMTP y plantillas Thymeleaf con manejo de fallos. |
| `CalculadoraDeTasaTest` | 5 | 0 | 0 | Validación de cálculo de tasas según riesgo TUPA (Bajo, Medio, Alto, Muy Alto). |
| `CalculadoraDeTasaDesgloseTest` | 3 | 0 | 0 | Verificación del desglose de conceptos tributarios TUPA. |
| `MesaPartesValidationTest` | 6 | 0 | 0 | Validaciones de bean validation jakarta en DTOs de entrada. |
| `AuthControllerIntegrationTest` | 5 | 0 | 0 | Integración de endpoints de autenticación y sesiones. |
| `AuthServiceTest` | 4 | 0 | 0 | Lógica del servicio de login, BCrypt y generación de claims. |
| `JwtTokenProviderTest` | 3 | 0 | 0 | Generación, expiración y validación criptográfica de tokens JWT. |
| `TarifaTupaControllerIntegrationTest` | 4 | 0 | 0 | Endpoints REST de tarifas TUPA y seguridad administrativa. |
| `TarifaTupaServiceTest` | 4 | 0 | 0 | Mantenimiento dinámico de tasas TUPA y registro de auditoría. |
| `VerificacionPublicaTest` | 5 | 0 | 0 | Búsqueda por código QR y validación de licencias vigentes y no encontradas. |
| `AdaptadorControllerTest` | 2 | 0 | 0 | Integraciones reales desacopladas (SAT pago y Edificaciones zonificación). |
| `EdificacionesAdapterServiceTest` | 1 | 0 | 0 | Adaptador de zonificación desacoplado sin simulación directa. |
| `SatAdapterServiceTest` | 1 | 0 | 0 | Adaptador de pagos SAT sin confirmación automática ficticia. |
| `FirmaDigitalAdapterServiceTest` | 2 | 0 | 0 | Puerto y adaptador de firma digital institucional con bloqueo en fallback. |
| `Concurrencia150UsuariosTest` | 2 | 0 | 0 | Prueba de concurrencia y estrés con hilos simultáneos. |
| `QrGeneratorServiceTest` | 1 | 0 | 0 | Generación de imagen matricial QR mediante Google ZXing. |
| **Total General** | **301** | **0** | **0** | **100% de éxito certificado (0 fallos, 0 errores, 0 omitidas)** |

---

## 2. Matriz de Trazabilidad de Criterios de Aceptación (ERS-V2)

| ID Criterio | Requisito Trazado | Escenario de Prueba | Resultado Esperado | Estado Actual | Incremento V2 Objetivo |
| :---: | :--- | :--- | :--- | :--- | :---: |
| **CA-01** | RF-USR-006 / RNF-SEG-001 | Un usuario intenta abrir un expediente ajeno por URL o API REST | Acceso denegado (HTTP 403 Forbidden) | **CUMPLIDO** (`ExpedienteSecurity` BOLA/IDOR implementado) | Incremento I02 |
| **CA-02** | RF-PAG-006 / RN-01 | El ciudadano registra solicitud con documentos faltantes | Permanece como borrador; no se genera número formal de expediente | **CUMPLIDO** (`SolicitudBorrador` con UUID interno; no asigna `EXP-*`) | Incremento I04 / I04.1 |
| **CA-03** | RF-PAG-004 / RN-04 | El ciudadano carga comprobante de pago SAT | El comprobante queda pendiente de validación manual; no se valida automáticamente ni altera el estado documental o aprobación del expediente | **CUMPLIDO** (`PagoExpedienteService` en I06/I06.1: comprobante en `PENDIENTE_VALIDACION`, validación manual por cajero con registro de evidencia, sin mutar expediente a `DOCUMENTOS_VALIDADOS` ni `APROBADO`) | Incremento I06 / I06.1 |
| **CA-04** | RF-PAG-007 | El ciudadano presenta solicitud completa | Número único correlativo de expediente asignado una sola vez de forma transaccional | **CUMPLIDO** (`PresentacionFormalService` con lock pesimista y secuencia anual atómica) | Incremento I04 / I04.1 |
| **CA-05** | RF-EVA-001 / RN-05 | Funcionario evalúa expediente formalmente presentado | La evaluación de zonificación se ejecuta de forma manual posterior al pago y presentación | **CUMPLIDO** (`EvaluacionZonificacionService` en I06/I06.1: evaluación motivada por funcionario competente con base en PDU y reporte técnico oficial con longitud mínima, verificación de establecimiento y enlace documental) | Incremento I06 / I06.1 |
| **CA-06** | RF-EVA-006 / RN-06 | El ciudadano subsana un expediente en estado OBSERVADO | Se conserva el mismo número de expediente y se mantiene el historial documental | **CUMPLIDO** (`ObservacionExpedienteService` y `DocumentoVersionadoService` con versionado inmutable, conservando `EXP-YYYY-XXXXX` y creando versiones v2+) | Incremento I05 |
| **CA-07** | RF-LIC-004 / RNF-SEG-006 | Emisión de licencia de funcionamiento | PDF firmado digitalmente mediante servicio institucional y código QR no enumerable | **CUMPLIDO A NIVEL DE INTEGRACIÓN Y MOCKS / PENDIENTE PROVEEDOR INSTITUCIONAL** (`FirmaDigitalPort` fail-closed, OpenPDF preliminar con marca de agua, token QR `SecureRandom` no enumerable, inmutabilidad documental; pendiente activación de credenciales IOFE institucionales) | Incremento I08 / I08.1 |
| **CA-08** | RF-VER-002 / RN-10 | El ciudadano propone corrección de coordenadas | La ubicación oficial no cambia hasta aprobación expresa del evaluador | **CUMPLIDO** (`SolicitudRectificacionUbicacionService` en I07/I07.1: registro PENDIENTE sin alterar coordenadas oficiales, distancia Haversine como indicador de alerta, decisión humana motivada, protección BOLA con `@establecimientoSecurity` y validación de expediente asignado) | Incremento I07 / I07.1 |
| **CA-09** | RF-VER-006 / RNF-SEG-007 | Persona anónima escanea código QR | Solo visualiza datos públicos autorizados con minimización (sin DNI/RUC) | **CUMPLIDO AL 100%** (DNI totalmente excluido de la respuesta pública conforme a Ley 29733, `VerificacionPublicaQrDto`, Rate Limiting HTTP 429) | Incremento I09 |
| **CA-10** | RF-VER-007 | Inspector municipal autenticado escanea código QR | Visualiza ubicación oficial en mapa para cotejar con el establecimiento físico | **CUMPLIDO AL 100%** (Endpoint `/api/inspector/licencias/verificar/{token}`, mapa Leaflet interactivo, coordenadas oficiales `RECTIFICADA_APROBADA` vs `DECLARADA_OFICIAL`, auditoría inmutable de fiscalización) | Incremento I09 |
| **CA-11** | RF-VER-008 | Inspector municipal consulta licencia emitida | No encuentra funciones de actas de inspección, infracciones ni sanciones | **CUMPLIDO AL 100%** (Controlador de estricta solo lectura, sin botones de actas ni sanciones, sin descarga de expedientes internos) | Incremento I09 |
| **CA-12** | RNF-OPE-002 | Se ejecuta simulacro de recuperación | Se restaura base de datos y archivos íntegros según RPO/RTO aprobados | Pendiente documentación y ensayo | Incremento 10 |
| **CA-13** | RF-DOC-003 | El ciudadano intenta editar campos reservados a la municipalidad | Acción impedida por validación estricta de esquema | **CUMPLIDO** (Validación estricta de DTOs, permisos RBAC/BOLA y campos inmutables) | Incremento I05 |
| **CA-14** | RF-LIC-005 | Se altera el contenido de un PDF firmado emitido | La verificación criptográfica detecta inmediatamente la alteración | **CUMPLIDO A NIVEL DE PERSISTENCIA Y HASH / PENDIENTE PKI INSTITUCIONAL** (Almacenamiento binario `archivo_pdf`, hash SHA-256 preservado en BD e inmutable sin regeneración en caliente; validación PKI sujeta a proveedor institucional) | Incremento I08 / I08.1 |
| **CA-12** | RNF-OPE-002 | Se ejecuta simulacro de recuperación | Se restaura base de datos y archivos íntegros según RPO/RTO aprobados | Pendiente documentación y ensayo | Incremento 10 |
| **CA-13** | RF-DOC-003 | El ciudadano intenta editar campos reservados a la municipalidad | Acción impedida por validación estricta de esquema | **CUMPLIDO** (Validación estricta de DTOs, permisos RBAC/BOLA y campos inmutables) | Incremento I05 |
| **CA-14** | RF-LIC-005 | Se altera el contenido de un PDF firmado emitido | La verificación criptográfica detecta inmediatamente la alteración | **CUMPLIDO A NIVEL DE PERSISTENCIA Y HASH / PENDIENTE PKI INSTITUCIONAL** (Almacenamiento binario `archivo_pdf`, hash SHA-256 preservado en BD e inmutable sin regeneración en caliente; validación PKI sujeta a proveedor institucional) | Incremento I08 / I08.1 |

---

## 3. Matriz de Pruebas de Arquitectura (DAS-V2)

| ID Prueba | Escenario de Arquitectura | Evidencia Esperada | Método de Verificación |
| :---: | :--- | :--- | :--- |
| **AT-01** | Presentación concurrente / doble clic | Un solo expediente y número único generado (idempotencia) | Prueba de concurrencia con `CompletableFuture` / hilos paralelos. |
| **AT-02** | Acceso a expediente ajeno (BOLA/IDOR) | Respuesta HTTP 403 / 404 y registro en bitácora de auditoría | Prueba de integración `@WithMockUser(username="otro_ciudadano")`. |
| **AT-03** | Voucher SAT sin validación manual | El comprobante no habilita la aprobación ni emisión por sí solo | Verificación de máquina de estados y validación de precondición. |
| **AT-04** | Subsanación sucesiva de documentos | Mismo expediente, versiones anteriores accesibles e inmutables | Prueba de versionado con cálculo de hash SHA-256. |
| **AT-05** | Zonificación motivada | Decisión técnica sustentada, posterior a presentación y pago | Validación de orden cronológico en eventos de auditoría. |
| **AT-06** | Integración de firma digital | PDF validable criptográficamente, hash consistente | Mock de `FirmaDigitalPort` verificando hash firmado. |
| **AT-07** | QR alterado o inexistente | Detección de token inválido sin revelar datos del sistema | Prueba HTTP sobre `/api/public/licencias/{token}` con token aleatorio. |
| **AT-08** | Rectificación de ubicación pendiente | Coordenadas oficiales inalteradas hasta decisión de Licencias | **CUMPLIDO** (Pruebas unitarias e integración en `SolicitudRectificacionUbicacionServiceTest` y `EvaluacionAdministrativaIntegrationTest`) |
| **AT-09** | Consulta pública de autenticidad | Minimización de datos (sin exposición de datos restringidos) | Prueba de contrato JSON sobre `VerificacionLicenciaDto`. |
| **AT-10** | Perfil de Inspector Municipal | Consulta ampliada en mapa sin endpoints de edición ni actas | Prueba de autorización RBAC con `ROLE_INSPECTOR`. |
| **AT-11** | Fallo de servicio de firma institucional | Reintento seguro, sin licencia falsamente emitida ni QR activo | Prueba de inyección de excepción en `FirmaDigitalPort`. |
| **AT-12** | Restauración ante desastres | Base de datos PostgreSQL y documentos recuperados íntegros | Script de backup/restore ensayado en contenedor de prueba. |

---

## 4. Plan de Pruebas de Carga y Rendimiento (K6)

En `tests/load-test/` se dispone de un script de pruebas de carga en K6 (`k6-load-test.js`) y un ejecutor PowerShell (`run-load-test-150.ps1`):
- **Carga Objetivo:** 150 usuarios virtuales concurrentes (`VUs = 150`).
- **Escenarios Clave:**
  1. Consultas públicas de autenticidad QR (`GET /api/public/licencias/{id}`).
  2. Descarga de Anexo 1 en PDF bajo demanda (`GET /api/formularios/anexo-1/{id}/pdf`).
  3. Consulta de estado ciudadano (`GET /api/expedientes/tramite/{num}`).
- **Métricas de Aceptación:**
  - p95 de tiempo de respuesta < 2.0 segundos para consultas API.
  - p95 de tiempo de generación/descarga de PDF < 3.5 segundos.
  - Tasa de errores HTTP (5xx) inferior al 0.1%.

---

## 5. Declaración de Limitaciones de Infraestructura y Entorno Local

1. **Disponibilidad de Motor Físico PostgreSQL 16:**
   - Durante la auditoría técnica del Incremento I06.1, el motor Docker Desktop Linux Engine continuó inactivo en el entorno local de desarrollo (falla al conectar con el named pipe del demonio Docker).
   - **Registro explícito:** La suite automatizada de 227 pruebas se ejecutó con 100% de éxito sobre H2 in-memory en dialecto PostgreSQL (`MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE`). Los scripts DDL acumulativos (`01` a `05-v2-pagos-sat-zonificacion.sql`) fueron validados sintácticamente, pero **no se emite certificación formal de motor PostgreSQL 16 real** hasta su ejecución en un entorno con demonio de base de datos aprovisionado (p. ej. runner CI/CD).
2. **Servicio Antivirus / Antimalware:**
   - La validación de documentos inspecciona firmas binarias (*magic numbers*) y sanitiza rutas. La integración con el socket de ClamAV permanece como tarea delegada de infraestructura y se audita con el estado `ESCANEO_DELEGADO_PENDIENTE_CONFIGURACION_CLAMAV`.
3. **Integración Externa SAT y Edificaciones:**
   - Se ratifica la ausencia de adaptadores que simulen conciliaciones automáticas bancarias o resoluciones de zonificación ficticias. Todo el proceso administrativo se sustenta en cotejo manual por personal municipal autorizado y competente.
