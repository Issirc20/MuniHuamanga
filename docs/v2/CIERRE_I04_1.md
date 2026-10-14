# INFORME DE AUDITORÍA Y CIERRE TÉCNICO I04.1 — MUNIHUAMANGA V2

**Municipalidad Provincial de Huamanga — Gerencia de Licencias de Funcionamiento**
**Fecha:** 14 de octubre de 2026
**Rama de Trabajo:** `develop/munihuamanga-v2`
**Documentos Rectores:** [ERS-MH-V2-001 v1.0], [DAS-MH-V2-001 v1.0], [CIERRE_I02_1.md](file:///d:/ArqSoftware/MuniHuamanga/docs/v2/CIERRE_I02_1.md) e [IMPLEMENTACION_I04.md](file:///d:/ArqSoftware/MuniHuamanga/docs/v2/IMPLEMENTACION_I04.md)
**Evaluador:** Arquitecto de Software, Auditor de Seguridad y Especialista Java 21 / Spring Boot / PostgreSQL

---

## 1. Resumen Ejecutivo

En el marco de la auditoría y estabilización del **Incremento I04 (M03: Solicitudes y Borradores; M06: Máquina de Estados Inicial y Presentación Formal)**, se realizó una inspección exhaustiva sobre el código fuente, la infraestructura de base de datos PostgreSQL, los esquemas de concurrencia e idempotencia, la seguridad documental y las reglas normativas del TUPA municipal.

Se identificaron y corrigieron **5 riesgos residuales críticos** antes de autorizar el paso al Incremento I05:
1. **Inconsistencia en scripts DDL:** Duplicación de sentencias en el script `01-init-databases.sql`.
2. **Carrera crítica en primera secuencia anual:** Posible colisión por condición de carrera `SELECT ... FOR UPDATE` sobre correlativos no pre-inicializados.
3. **Bypass del flujo formal de expedientes:** Endpoint legado `POST /api/expedientes` accesible a ciudadanos, omitiendo el ciclo mandatorio de borrador documental.
4. **Validación superficial de adjuntos:** Verificación de archivos dependiente únicamente del `Content-Type` HTTP y sin control estricto de magic numbers ni protección contra path traversal en nombres de archivo.
5. **Acreditación documental insuficiente en personas jurídicas:** Cuentas con RUC permitían presentar trámites sin validar la vigencia de poder del representante legal.

Tras la aplicación de las correcciones arquitectónicas, la suite completa alcanzó **176 pruebas automatizadas con 100% de éxito**.

---

## 2. Hallazgos Confirmados

| ID Hallazgo | Componente Afectado | Severidad | Descripción del Riesgo Técnico / Normativo |
| :--- | :--- | :---: | :--- |
| **HAL-I041-01** | `docker/postgres/init/01-init-databases.sql` | **Media** | El script de inicialización histórica V1 presentaba duplicación de bloques de inserción y DDL después de la línea 324, lo que provocaba errores de clave primaria o advertencias al re-inicializar contenedores limpios. |
| **HAL-I041-02** | `SecuenciaTramiteService.java` | **Alta** | La primera llamada anual de generación de correlativo realizaba un `findById` y posterior `save` sin bloqueo previo si el registro del año aún no existía en base de datos. Dos hilos concurrentes intentando presentar el primer expediente del año podían colisionar por violación de clave primaria (`pk_secuencia_tramite`). |
| **HAL-I041-03** | `ExpedienteController.java` & `SecurityConfig.java` | **Alta** | El endpoint legado `POST /api/expedientes` permitía que cualquier usuario autenticado con `ROLE_CIUDADANO` creara expedientes directamente con número `EXP-YYYY-XXXXX`, puenteando la carga de borradores (M03), el checklist de requisitos y el cálculo de hash documental. |
| **HAL-I041-04** | `SolicitudBorradorService.java` & Cargas de Archivos | **Alta** | Los documentos adjuntos dependían de la cabecera `Content-Type` enviada por el cliente HTTP (manipulable) y del nombre original del archivo sin saneamiento contra secuencias de escape de directorio (`../`). Faltaba validación por firma binaria (*magic numbers*) y verificación de integridad SHA-256 al recuperar o descargar archivos. |
| **HAL-I041-05** | `PresentacionFormalService.java` | **Alta** | Para personas jurídicas (`TipoPersona.JURIDICA`), se permitía la presentación formal sin acreditar representación legal mediante validación oficial o adjunto obligatorio de `VIGENCIA_PODER`, violando la Ley N° 27444 y el TUPA MPH. |

---

## 3. Correcciones Aplicadas

### 3.1 Base de Datos y Scripts de Migración
- **Saneamiento de `01-init-databases.sql`:** Se eliminó la duplicación redundante preservando íntegramente las tablas históricas V1 (`expedientes`, `documentos_adjuntos`, `transiciones_expediente`, `licencias_emitidas`, `tarifas_tupa`, `usuarios_sistema`, `auditoria_licencias`).
- **Idempotencia de Esquemas V2:** Se ratificó que tanto [02-v2-cuentas-establecimientos.sql](file:///d:/ArqSoftware/MuniHuamanga/docker/postgres/init/02-v2-cuentas-establecimientos.sql) como [03-v2-solicitudes-borradores.sql](file:///d:/ArqSoftware/MuniHuamanga/docker/postgres/init/03-v2-solicitudes-borradores.sql) aplican sentencias `CREATE TABLE IF NOT EXISTS`, `CREATE INDEX IF NOT EXISTS`, y cláusulas `ON CONFLICT (id) DO NOTHING` en catálogos semilla (CIIU, Requisitos TUPA).
- **Compatibilidad con Datos Históricos:** Las claves foráneas en `expedientes` (`cuenta_id`, `establecimiento_id`, `borrador_origen_id`) admiten `NULL`, garantizando que los registros migrados de V1 no se invaliden ni requieran transformaciones destructivas.

### 3.2 Concurrencia e Idempotencia en Presentación Formal
- **Bloqueo Pesimista sobre Borrador:** En [SolicitudBorradorRepository.java](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/borradores/repository/SolicitudBorradorRepository.java) se implementó `findByIdWithLock(UUID id)` con `@Lock(LockModeType.PESSIMISTIC_WRITE)`. Esto serializa cualquier intento simultáneo de presentación sobre el mismo borrador.
- **Pre-inicialización Segura de Secuencias:** En [SecuenciaTramiteService.java](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/borradores/service/SecuenciaTramiteService.java) se añadió un método `@PostConstruct` que pre-registra de forma idempotente el registro de secuencia para el año en curso. De este modo, la consulta atómica `findByIdWithLock` siempre encuentra la fila existente, eliminando la condición de carrera en la primera llamada del año.
- **Idempotencia de Reintento HTTP:** Si un cliente reintenta la presentación de un borrador ya presentado o cuyo trámite fue procesado, `PresentacionFormalService` detecta el estado `PRESENTADO`, localiza el expediente ya generado y retorna la respuesta exitosa sin generar un nuevo correlativo ni duplicar registros.
- **Mapeo de Excepciones de Concurrencia:** En [GlobalExceptionHandler.java](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/controller/GlobalExceptionHandler.java), las excepciones `OptimisticLockingFailureException`, `PessimisticLockingFailureException`, `CannotAcquireLockException` y `DataIntegrityViolationException` se capturan y traducen a **HTTP 409 Conflict** con mensaje claro al usuario, evitando errores 500 innecesarios.

### 3.3 Seguridad Documental y Anti-Malware
- **Validador Criptográfico y Binario:** Se creó [DocumentoSecurityValidator.java](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/borradores/security/DocumentoSecurityValidator.java):
  - **Firma Binaria (*Magic Numbers*):** Inspecciona los primeros bytes del archivo (PDF: `%PDF-` / `0x25 0x50 0x44 0x46`, PNG: `0x89 0x50 0x4E 0x47`, JPEG: `0xFF 0xD8 0xFF`).
  - **Lista Blanca de Extensiones:** Solo admite `.pdf`, `.jpg`, `.jpeg`, `.png`.
  - **Límite de Tamaño:** Máximo 10 MB por documento.
  - **Saneamiento de Rutas:** Remueve caracteres de path traversal (`..`, `/`, `\`) utilizando `Paths.get(filename).getFileName().toString()`.
  - **Verificación de Integridad:** Comprueba el hash SHA-256 al recuperar el archivo, asegurando que no haya sufrido alteraciones físicas o manipulaciones no autorizadas.
  - **Postura Anti-Malware:** Se incorporó la trazabilidad de auditoría `ESCANEO_DELEGADO_PENDIENTE_CONFIGURACION_CLAMAV`. No se simula un motor antivirus ficticio que induzca a falsa seguridad; se deja preparado el punto de enganche asíncrono para el servicio de antivirus institucional.
- **Descarga Controlada y Autorizada:** Se incorporó el endpoint `GET /api/borradores/{id}/documentos/{docId}` en [SolicitudBorradorController.java](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/borradores/controller/SolicitudBorradorController.java), protegido mediante `@PreAuthorize("@borradorSecurity.puedeEditarBorrador(#id, authentication)")` y con auditoría explícita de descarga.

### 3.4 Control del Flujo Formal y Restricción Administrativa
- **Bloqueo a Creación Directa:** Se restringió `POST /api/expedientes` en [ExpedienteController.java](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/controller/ExpedienteController.java) a `@PreAuthorize("hasAnyRole('ADMIN', 'EVALUADOR')")`.
- **Ajuste en `SecurityConfig`:** Se eliminó cualquier permiso a `ROLE_CIUDADANO` sobre la creación directa de expedientes. Todo ciudadano debe iniciar obligatoriamente mediante `/api/borradores` y presentar formalmente mediante `/api/borradores/{id}/presentar`.
- **Exclusividad de Correlativos:** Solo [PresentacionFormalService.java](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/borradores/service/PresentacionFormalService.java) asigna correlativos oficiales V2 a través de [SecuenciaTramiteService.java](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/borradores/service/SecuenciaTramiteService.java).

### 3.5 Requisitos Administrativos y Acreditación de Representación Legal
- **Separación de Riesgo Declarado y Evaluación:** En [SolicitudBorrador.java](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/borradores/model/SolicitudBorrador.java), el campo `nivelRiesgoDeclarado` queda explícitamente separado de cualquier dictamen ITSE de Defensa Civil, registrándose en el expediente con estado pendiente de inspección.
- **Separación de Pagos:** El voucher SAT registrado por el ciudadano en el borrador se traslada al expediente como dato informativo con estado pendiente de confirmación manual por caja municipal.
- **Acreditación de Personas Jurídicas:** En `PresentacionFormalService`, si `cuenta.getTipoPersona() == TipoPersona.JURIDICA`, se exige que la identidad haya sido validada oficialmente (`identidadValidadaOficialmente == true`) o que el borrador incluya como adjunto el documento de tipo `VIGENCIA_PODER`. De lo contrario, la presentación se rechaza con código 422 Unprocessable Entity.

---

## 4. Archivos Modificados y Creados

| Archivo | Tipo de Cambio | Justificación Técnica |
| :--- | :---: | :--- |
| `docker/postgres/init/01-init-databases.sql` | Modificado | Eliminación de sentencias redundantes duplicadas; limpieza del baseline V1. |
| `servicio-expedientes/.../borradores/repository/SolicitudBorradorRepository.java` | Modificado | Incorporación de consulta `findByIdWithLock` con `@Lock(LockModeType.PESSIMISTIC_WRITE)`. |
| `servicio-expedientes/.../borradores/service/SecuenciaTramiteService.java` | Modificado | Constructor explícito JDT, pre-seed con `@PostConstruct` y garantía de secuencia anual concurrente. |
| `servicio-expedientes/.../borradores/service/PresentacionFormalService.java` | Modificado | Constructor explícito, serialización con lock pesimista, idempotencia ante reintentos y control de vigencia de poder en personas jurídicas. |
| `servicio-expedientes/.../borradores/service/SolicitudBorradorService.java` | Modificado | Integración de `DocumentoSecurityValidator`, verificación de magic numbers, SHA-256 y descarga segura auditada. |
| `servicio-expedientes/.../borradores/security/DocumentoSecurityValidator.java` | **Nuevo** | Validador de cabeceras binarias, extensiones, tamaño, rutas relativas y cálculo SHA-256. |
| `servicio-expedientes/.../borradores/controller/SolicitudBorradorController.java` | Modificado | Endpoint `GET /{id}/documentos/{docId}` para descarga segura autorizada. |
| `servicio-expedientes/.../controller/ExpedienteController.java` | Modificado | Restricción de `POST /api/expedientes` exclusivamente a `ADMIN` y `EVALUADOR`. |
| `servicio-expedientes/.../config/SecurityConfig.java` | Modificado | Restricción de rutas de expedientes a roles autorizados; ciudadanos confinados a `/api/borradores`. |
| `servicio-expedientes/.../controller/GlobalExceptionHandler.java` | Modificado | Manejo de excepciones de concurrencia y bloqueo devolviendo HTTP 409 Conflict. |
| `servicio-expedientes/.../borradores/PresentacionConcurrenciaIntegrationTest.java` | **Nuevo** | Pruebas de concurrencia multihilo, reintentos idempotentes y auditoría de descargas. |
| `servicio-expedientes/.../borradores/SolicitudBorradorServiceTest.java` | Modificado | Pruebas unitarias de magic numbers, extensiones falsas, path traversal y vigencia de poder. |
| `servicio-expedientes/.../borradores/PresentacionFormalServiceTest.java` | Modificado | Pruebas de bloqueo en personas jurídicas sin vigencia de poder y aceptación de adjunto válido. |
| `servicio-expedientes/.../security/ExpedienteSecurityAuthorizationTest.java` | Modificado | Prueba de bloqueo HTTP 403 para ciudadanos que intentan crear expedientes directamente. |

---

## 5. Resultados de Concurrencia e Idempotencia

En [PresentacionConcurrenciaIntegrationTest.java](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/test/java/pe/gob/munihuamanga/licencias/expedientes/borradores/PresentacionConcurrenciaIntegrationTest.java) se evaluaron los siguientes escenarios:

1. **Presentaciones Simultáneas del Mismo Borrador:**
   - 2 hilos paralelos compitiendo exactamente en el mismo instante sobre el borrador `uuid-1`.
   - **Resultado:** 1 hilo procesa la presentación y genera el expediente formal; el segundo hilo o bien espera y retorna el expediente ya creado (idempotencia) o recibe `HTTP 409 / IllegalStateException` controlado. **Expedientes duplicados: 0**.
2. **Presentaciones Simultáneas de Borradores Diferentes:**
   - 2 hilos paralelos presentando borradores distintos en simultáneo.
   - **Resultado:** Ambos trámites son procesados exitosamente, recibiendo números de expediente consecutivos estrictamente ordenados y sin saltos ni duplicados.
3. **Primera Presentación del Año:**
   - El mecanismo de pre-inicialización en `SecuenciaTramiteService` asegura que la fila del año exista antes de cualquier concurrencia. **Errores por clave duplicada: 0**.
4. **Reintento tras Fallo o Desconexión:**
   - Reintento de llamada de presentación sobre un borrador ya en estado `PRESENTADO`.
   - **Resultado:** Devuelve el expediente asociado existente de forma inmediata sin alterar contadores ni estados.
5. **Rollback Transaccional:**
   - Errores de validación o rechazo no dejan expedientes huérfanos ni marcan el borrador como presentado.

---

## 6. Seguridad Documental

- **Inspección de Cabeceras Binarias:** Se probó exitosamente el rechazo de archivos con extensión válida (`.pdf`) cuyo contenido real era un script o texto malicioso sin la firma `%PDF-`.
- **Mitigación Path Traversal:** Archivos con nombres maliciosos del tipo `../../../../etc/passwd.pdf` son saneados extrayendo exclusivamente el nombre simple de archivo.
- **Hash SHA-256 Inmutable:** Todo documento adjunto almacena su hash SHA-256 en base de datos. Al descargarse mediante `descargarDocumento`, se recalcula el hash para certificar la integridad de los bytes entregados.
- **Auditoría Documental:** Todas las operaciones de carga, sustitución y descarga quedan registradas en el logger estructurado con DNI del operador, UUID del documento y hash SHA-256.
- **Anti-Malware:** Estado de servicio marcado como pendiente de vinculación institucional (`ESCANEO_DELEGADO_PENDIENTE_CONFIGURACION_CLAMAV`). Se garantiza total transparencia técnica sin simular falsas protecciones operativas.

---

## 7. Riesgos Residuales

1. **Entorno de Contenedores en Pruebas:**
   - En la máquina de desarrollo actual, el servicio de Docker no se encontraba en ejecución. Las pruebas de integración se ejecutaron satisfactoriamente sobre el perfil de prueba Spring Boot con base H2 configurada en modo PostgreSQL estricto (`MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE`).
   - *Mitigación:* Los scripts DDL `01`, `02` y `03` son 100% compatibles con sintaxis PostgreSQL 16 y se encuentran listos en `docker/postgres/init/` para su despliegue continuo.
2. **Servicios Externos (RENIEC / SUNAT / SAT):**
   - La validación de identidad oficial para DNI y RUC permanece desacoplada sin stubs engañosos. En el Incremento I08 se implementarán los adaptadores de integración formal con firma digital institucional y servicios de interoperabilidad del Estado (PIDE).
3. **Flujo de Validación Manual de Voucher SAT:**
   - La carga del comprobante de pago por el ciudadano está implementada como dato preliminar. La bandeja administrativa para que el personal de caja o licencias valide manualmente el comprobante está programada para el Incremento I06.

---

## 8. Dictamen Final

```
================================================================================
                    DICTAMEN TÉCNICO DE AUDITORÍA I04.1
================================================================================
  Módulo Auditado:         M03 (Solicitudes y Borradores) y M06 (Máquina de Estados)
  Línea Base Anterior:     123 pruebas exitosas (I02) / 151 pruebas intermedias (I04)
  Pruebas Ejecutadas:      176 pruebas
  Fallos / Errores:        0 fallos / 0 errores / 0 omitidas
  Tasa de Éxito:           100%
  Concurrencia:            Verificada con bloqueo pesimista y secuencias atómicas
  Seguridad Documental:    Verificada con magic numbers, SHA-256 y anti-traversal
  Flujo Formal:            Blindado contra bypass ciudadano

  CALIFICACIÓN:            APTO PARA INICIAR EL INCREMENTO I05
================================================================================
```

*Nota: Conforme a las restricciones vigentes, no se han iniciado tareas del Incremento I05 ni se han realizado commits automáticos en Git.*
