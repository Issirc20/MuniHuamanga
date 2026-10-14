# Estrategia de Pruebas y Validación de Calidad — MuniHuamanga V2

Municipalidad Provincial de Huamanga — Gerencia de Licencias  
**Fecha:** 13 de octubre de 2026  
**Documentos Rectores:** [ERS-MH-V2-001 v1.0] (Sección 10) y [DAS-MH-V2-001 v1.0] (Sección 13)

---

## 1. Línea Base y Ejecución Actual de Pruebas Automatizadas

Tras la conclusión del **Incremento I02 (Seguridad, Autorización y Protección de Datos)**, se ejecutó la suite completa de pruebas unitarias, de integración y de seguridad mediante Maven Surefire:
- **Comando:** `mvn clean verify`
- **Resultado:** **123 pruebas ejecutadas, 0 fallos, 0 errores, 0 omitidas**
- **Tiempo de ejecución total:** 28.29 segundos
- **Estado del Reactor Maven:**
  - `MuniHuamanga - Gestión de Licencias de Funcionamiento`: SUCCESS
  - `Common Domain`: SUCCESS
  - `MuniHuamanga - Monolito Modular` (`servicio-expedientes`): SUCCESS

### 1.1 Desglose por Clase de Prueba (123 Pruebas)

| Clase de Prueba | Pruebas | Fallos | Errores | Alcance / Cobertura Funcional |
| :--- | :---: | :---: | :---: | :--- |
| `ExpedienteSecurityAuthorizationTest` *(Nuevo I02)* | 20 | 0 | 0 | Seguridad a nivel de objeto BOLA/IDOR, autorización contextual, roles ciudadano e inspector, minimización de datos en QR y denegación de accesos anónimos. |
| `SeguridadEndToEndTest` | 7 | 0 | 0 | Pruebas end-to-end de autenticación JWT, RBAC de cajero, ofuscación de trámite y descarga autorizada. |
| `CalculadoraDeTasaTest` | 5 | 0 | 0 | Validación de cálculo de tasas según riesgo TUPA (Bajo, Medio, Alto, Muy Alto). |
| `CalculadoraDeTasaDesgloseTest` | 4 | 0 | 0 | Verificación del desglose de conceptos tributarios TUPA. |
| `DocumentoPdfServiceCompletoTest` | 8 | 0 | 0 | Renderizado en memoria de Anexo 1, Anexo 3, Anexo 4 y Licencia con OpenPDF. |
| `ExpedienteServiceTest` | 12 | 0 | 0 | Creación de trámite, transiciones, subsanación, pago y aprobación de expedientes. |
| `LicenciaPdfServiceTest` | 6 | 0 | 0 | Estructura visual y elementos del certificado oficial de licencia. |
| `Anexo1PdfGeneratorTest` | 3 | 0 | 0 | Generador nativo del Anexo 1 Declaración Jurada. |
| `NotificacionEmailServiceTest` | 7 | 0 | 0 | Envíos asíncronos JavaMail/SMTP y plantillas Thymeleaf con manejo de fallos. |
| `QrGeneratorServiceTest` | 1 | 0 | 0 | Generación de imagen matricial QR mediante Google ZXing. |
| `TarifaTupaServiceTest` | 4 | 0 | 0 | Mantenimiento dinámico de tasas TUPA y registro de auditoría. |
| `VerificacionPublicaTest` | 4 | 0 | 0 | Búsqueda por código QR y validación de licencias vigentes y no encontradas. |
| `EstadoExpedienteValidatorTest` | 10 | 0 | 0 | Reglas de transición entre estados y bloqueos de aprobación sin requisitos. |
| `MesaPartesValidationTest` | 6 | 0 | 0 | Validaciones de bean validation jakarta en DTOs de entrada. |
| `AuthControllerIntegrationTest` | 4 | 0 | 0 | Integración de endpoints de autenticación y sesiones. |
| `AuthServiceTest` | 3 | 0 | 0 | Lógica del servicio de login, BCrypt y generación de claims. |
| `JwtTokenProviderTest` | 7 | 0 | 0 | Generación, expiración y validación criptográfica de tokens JWT. |
| `TarifaTupaControllerIntegrationTest` | 4 | 0 | 0 | Endpoints REST de tarifas TUPA y seguridad administrativa. |
| `AdaptadorControllerTest` | 2 | 0 | 0 | Integraciones reales desacopladas (SAT pago y Edificaciones zonificación). |
| `Concurrencia150UsuariosTest` | 6 | 0 | 0 | Prueba de concurrencia y estrés con 150 hilos simultáneos. |
| **Total General** | **123** | **0** | **0** | **100% de éxito certificado (0 fallos, 0 errores)** |

---

## 2. Matriz de Trazabilidad de Criterios de Aceptación (ERS-V2)

| ID Criterio | Requisito Trazado | Escenario de Prueba | Resultado Esperado | Estado Actual | Incremento V2 Objetivo |
| :---: | :--- | :--- | :--- | :---: | :---: |
| **CA-01** | RF-USR-006 / RNF-SEG-001 | Un usuario intenta abrir un expediente ajeno por URL o API REST | Acceso denegado (HTTP 403 Forbidden) | **CUMPLIDO** (`ExpedienteSecurity` BOLA/IDOR implementado) | Incremento I02 |
| **CA-02** | RF-PAG-006 / RN-01 | El ciudadano registra solicitud con documentos faltantes | Permanece como borrador; no se genera número formal de expediente | **BRECHA** (Se asigna número desde el primer guardado) | Incremento 04 |
| **CA-03** | RF-PAG-004 / RN-04 | El ciudadano carga comprobante de pago SAT | El comprobante queda pendiente de validación manual; no se valida automáticamente | **BRECHA** (Pasa directo a evaluación final) | Incremento 06 |
| **CA-04** | RF-PAG-007 | El ciudadano presenta solicitud completa | Número único correlativo de expediente asignado una sola vez de forma transaccional | Parcial (Número asignado pero no sujeto a presentación) | Incremento 04 |
| **CA-05** | RF-EVA-001 / RN-05 | Funcionario evalúa expediente formalmente presentado | La evaluación de zonificación se ejecuta de forma manual posterior al pago | Pendiente de desacoplamiento | Incremento 06 |
| **CA-06** | RF-EVA-006 / RN-06 | El ciudadano subsana un expediente en estado OBSERVADO | Se conserva el mismo número de expediente y se mantiene el historial documental | Soportado en lógica; protegido con `ExpedienteSecurity` | Incremento 05 |
| **CA-07** | RF-LIC-004 / RNF-SEG-006 | Emisión de licencia de funcionamiento | PDF firmado digitalmente mediante servicio institucional y código QR no enumerable | Soportado con PDF y QR; pendiente firma institucional | Incremento 08 |
| **CA-08** | RF-VER-002 / RN-10 | El ciudadano propone corrección de coordenadas | La ubicación oficial no cambia hasta aprobación expresa del evaluador | No implementado (Campo de texto plano en local) | Incremento 07 |
| **CA-09** | RF-VER-006 / RNF-SEG-007 | Persona anónima escanea código QR | Solo visualiza datos públicos autorizados con minimización (sin DNI/RUC) | **CUMPLIDO** (DNI ofuscado según Ley 29733, `VerificacionLicenciaDto`) | Incremento I02 |
| **CA-10** | RF-VER-007 | Inspector municipal autenticado escanea código QR | Visualiza ubicación oficial en mapa para cotejar con el establecimiento físico | **CUMPLIDO EN BACKEND** (Endpoint `/ampliada` y `ROLE_INSPECTOR`) | Incremento I02 / 09 |
| **CA-11** | RF-VER-008 | Inspector municipal consulta licencia emitida | No encuentra funciones de actas de inspección, infracciones ni sanciones | **CUMPLIDO** (Eliminados stubs obsoletos y bloqueos en endpoints) | Incremento I02 |
| **CA-12** | RNF-OPE-002 | Se ejecuta simulacro de recuperación | Se restaura base de datos y archivos íntegros según RPO/RTO aprobados | Pendiente documentación y ensayo | Incremento 10 |
| **CA-13** | RF-DOC-003 | El ciudadano intenta editar campos reservados a la municipalidad | Acción impedida por validación estricta de esquema | Soportado parcialmente | Incremento 05 |
| **CA-14** | RF-LIC-005 | Se altera el contenido de un PDF firmado emitido | La verificación criptográfica detecta inmediatamente la alteración | Pendiente adaptador de firma digital | Incremento 08 |

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
| **AT-08** | Rectificación de ubicación pendiente | Coordenadas oficiales inalteradas hasta decisión de Licencias | Prueba unitaria sobre flujo de solicitud de rectificación. |
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
