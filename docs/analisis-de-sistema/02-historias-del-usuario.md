# 02. Historias de Usuario (User Stories)
## Sistema de Gestión Documentaria de Licencia de Funcionamiento
### Municipalidad Provincial de Huamanga (Ayacucho, Perú) — Versión 2.0

> **Marco Normativo:** Ley N° 28976 / TUO D.S. N° 046-2017-PCM / D.S. N° 002-2018-PCM / Ley N° 27444 / Ley N° 27269 / Ley N° 29733
> **Plataforma:** Java 21 LTS, Spring Boot 3.3.4, PostgreSQL 16, Spring Security 6 (RBAC)
> **Ubicación:** `docs/analisis-de-sistema/02-historias-del-usuario.md`
> **Estado:** Documento Canónico Vigente V2

---

## 1. Introducción y Convención de Estructura

Las historias de usuario capturan las necesidades funcionales desde la perspectiva de los diferentes roles y actores de MuniHuamanga V2. Cada historia sigue el formato estándar ágil:
- **Narrativa:** *Como [Actor], quiero [Acción / Funcionalidad], para [Beneficio / Valor de Negocio].*
- **Criterios de Aceptación:** Especificados con el formato *Dado que... Cuando... Entonces...* (BDD / Gherkin).
- **Prioridad MoSCoW:** Must Have (Debe tener), Should Have (Debería tener), Could Have (Podría tener), Won't Have (No tendrá en esta fase).
- **Bounded Context / Módulo V2:** Módulo responsable en la arquitectura de monolito modular (M01–M12).

---

## 2. Matriz Consolidada de Historias de Usuario V2

| ID | Título de la Historia de Usuario | Actor Principal | Prioridad | Módulo V2 | Estado |
|---|---|---|:---:|:---:|:---:|
| **US-01** | Registro de Solicitud en Wizard 3 Pasos (Anexo 1) | Administrado | **MUST** | M03 | Implementado |
| **US-02** | Determinación Objetiva de Nivel de Riesgo ITSE | Sistema / Evaluador | **MUST** | M03 / M07 | Implementado |
| **US-03** | Cálculo Automatizado de Tasas TUPA | Sistema / Administrado | **MUST** | M05 | Implementado |
| **US-04** | Liquidación y Generación de Voucher SAT | Administrado / SAT | **MUST** | M04 | Implementado |
| **US-05** | Renderizado en Memoria de Anexos 1, 3 y 4 en PDF | Administrado | **MUST** | M03 | Implementado |
| **US-06** | Conciliación y Registro Manual de Pago de Tasa | Cajero SAT | **MUST** | M04 | Implementado |
| **US-07** | Evaluación Técnica y Zonificación Comercial PDU | Evaluador Técnico | **MUST** | M06 | Implementado |
| **US-08** | Registro de Dictamen Técnico de Inspección ITSE | Evaluador / Inspector | **MUST** | M07 | Implementado |
| **US-09** | Emisión Atómica de Licencia Oficial con Correlativo Anual | Gerente / Evaluador | **MUST** | M09 | Implementado |
| **US-10** | Generación de Código QR Dinámico No Enumerable | Sistema / ZXing | **MUST** | M10 | Implementado |
| **US-11** | Consulta Pública de Autenticidad QR Minimizada (Ley 29733) | Ciudadano / General | **MUST** | M10 | Implementado |
| **US-12** | Notificaciones Asíncronas por Correo Electrónico | Sistema / Administrado | **SHOULD** | M07 | Implementado |
| **US-13** | Mantenimiento en Caliente del Tarifario TUPA | Administrador TI | **SHOULD** | M05 | Implementado |
| **US-14** | Autenticación Segura y Control de Acceso RBAC | Funcionario Municipal | **MUST** | M01 | Implementado |
| **US-15** | Asistente Guiado y Mesa de Ayuda para Anexos Físicos | Administrado | **COULD** | M03 | Implementado |
| **US-16** | Gestión de Cuentas Ciudadanas y Múltiples Establecimientos | Administrado | **MUST** | M01 | Implementado |
| **US-17** | Solicitud Borrador Autoguardada y Presentación Formal | Administrado | **MUST** | M02 | Implementado |
| **US-18** | Subsanación con Documentos Versionados y Hash SHA-256 | Administrado | **MUST** | M03 | Implementado |
| **US-19** | Validación Manual de Pagos con Estados Diferenciados | Cajero SAT | **MUST** | M04 | Implementado |
| **US-20** | Rectificación de Ubicación Georreferenciada de Locales | Administrado / Evaluador | **SHOULD** | M06 | Implementado |
| **US-21** | Resolución Administrativa y Firma Digital Fail-Closed | Autoridad Resolutiva | **MUST** | M08 | Implementado |
| **US-22** | Verificación Móvil con Mapa Interactivo para Inspectores | Inspector Municipal | **MUST** | M10 | Implementado |
| **US-23** | Presentación y Resolución de Reclamaciones Administrativas | Administrado / Evaluador | **SHOULD** | M11 | Backend Implementado |

---

## 3. Especificación Detallada de Historias de Usuario V2

### US-01: Registro de Solicitud en Mesa de Partes Virtual (Wizard 3 Pasos)
* **Narrativa:**
  > **Como** ciudadano o representante legal de un negocio en Huamanga,
  > **quiero** registrar mi solicitud de licencia completando un formulario interactivo por pasos (Wizard),
  > **para** formalizar mi petición administrativa con los 45 campos normativos exigidos por el D.S. N° 046-2017-PCM.
* **Criterios de Aceptación:**
  - *Dado* que el solicitante ingresa al Wizard,
  - *Cuando* completa los datos de titular, establecimiento, giro y Anexo 4,
  - *Entonces* el sistema valida la coherencia sintáctica, genera el código de expediente único (`EXP-YYYY-XXXXX`) y registra la solicitud en estado `REGISTRADO`.

### US-16: Gestión de Cuentas Ciudadanas y Establecimientos Desacoplados (M01)
* **Narrativa:**
  > **Como** administrado titular con RUC o DNI,
  > **quiero** registrar una cuenta de usuario y vincular uno o varios establecimientos comerciales independientes,
  > **para** gestionar múltiples locales o sucursales comerciales sin reingresar datos maestros en cada trámite.
* **Criterios de Aceptación:**
  - *Dado* un ciudadano que envía `POST /api/cuentas/registro`,
  - *Cuando* los datos de identificación son válidos y la contraseña cumple requisitos de seguridad,
  - *Entonces* se crea la cuenta en estado `ACTIVA` y se habilita la creación de establecimientos vinculados (`POST /api/cuentas/{id}/establecimientos`).
  - *Seguridad (ADR-02):* Solo el titular autenticado o un administrador con `ROLE_ADMIN` puede consultar o asociar locales a su cuenta.

### US-17: Solicitud Borrador Autoguardada y Presentación Formal (M02)
* **Narrativa:**
  > **Como** solicitante,
  > **quiero** guardar avances de mi trámite en estado borrador,
  > **para** completar los requisitos paulatinamente antes de formalizar la presentación oficial ante la municipalidad.
* **Criterios de Aceptación:**
  - *Dado* un borrador en estado `BORRADOR`,
  - *Cuando* el usuario actualiza secciones mediante `PUT /api/borradores/{id}`,
  - *Entonces* el sistema persiste los cambios sin generar expediente administrativo formal.
  - *Cuando* el usuario invoca `POST /api/borradores/{id}/presentar`,
  - *Entonces* el sistema valida que el checklist esté completo, realiza la transición a `PRESENTADO` y genera el expediente formal asociado.

### US-18: Subsanación con Documentos Versionados y Hash SHA-256 (M03)
* **Narrativa:**
  > **Como** administrado con un expediente observado,
  > **quiero** cargar documentos corregidos manteniendo el historial de versiones previas,
  > **para** subsanar observaciones legales o técnicas garantizando la inmutabilidad y valor probatorio de los recaudos.
* **Criterios de Aceptación:**
  - *Dado* un expediente en estado `OBSERVADO`,
  - *Cuando* el administrado envía `POST /api/expedientes/{id}/subsanar` con nuevos documentos adjuntos,
  - *Entonces* cada documento se registra con un nuevo número correlativo de versión (`version = version_previa + 1`), estado `SUBSANADO`, cálculo de hash criptográfico SHA-256, y el expediente transiciona a `EN_EVALUACION`.

### US-19: Validación Manual de Pagos SAT con Estados Diferenciados (M04)
* **Narrativa:**
  > **Como** cajero municipal con `ROLE_CAJERO`,
  > **quiero** registrar y calificar los comprobantes de pago de los administrados con estados controlados,
  > **para** evitar fraudes de recaudación y asegurar que solo expedientes con abonos reales pasen a evaluación.
* **Criterios de Aceptación:**
  - *Dado* un pago registrado en estado `EN_REVISION`,
  - *Cuando* el cajero valida el abono en `PUT /api/pagos/{id}/validar-manual`,
  - *Entonces* el pago transiciona a `VALIDADO_MANUALMENTE` y el expediente avanza a `EN_EVALUACION`.
  - *Si* el comprobante es ilegible o inconsistente, el cajero puede marcar `OBSERVADO` o `RECHAZADO` con justificación obligatoria.

### US-20: Rectificación de Ubicación Georreferenciada de Locales (M06)
* **Narrativa:**
  > **Como** titular de un establecimiento comercial,
  > **quiero** solicitar la rectificación de la dirección física o coordenadas geográficas de mi local,
  > **para** corregir inconsistencias cartográficas detectadas durante la zonificación o inspección.
* **Criterios de Aceptación:**
  - *Dado* un establecimiento con coordenadas declaradas,
  - *Cuando* el titular envía `POST /api/establecimientos/{id}/rectificacion-ubicacion`,
  - *Entonces* se genera una solicitud en estado `PENDIENTE`.
  - *Cuando* el evaluador técnico (`ROLE_EVALUADOR`) revisa y resuelve la rectificación (`PUT /api/rectificaciones-ubicacion/{id}/resolver`), las coordenadas maestras del establecimiento se actualizan atómicamente si el dictamen es favorable.

### US-21: Resolución Administrativa y Firma Digital Fail-Closed (M08)
* **Narrativa:**
  > **Como** autoridad resolutiva municipal (`ROLE_ADMIN`),
  > **quiero** emitir la resolución administrativa aprobatoria o denegatoria y firmarla digitalmente con certificación institucional,
  > **para** otorgar validez jurídica conforme al TUO de la Ley N° 27444 y Ley N° 27269.
* **Criterios de Aceptación:**
  - *Dado* un expediente con informe técnico favorable,
  - *Cuando* la autoridad crea la resolución (`POST /api/resoluciones`),
  - *Entonces* se genera un correlativo anual atómico pesimista (`RES-YYYY-XXXXX`) en estado `APROBADA_PENDIENTE_FIRMA`.
  - *Cuando* se invoca `POST /api/resoluciones/{id}/firmar`, el sistema contacta `FirmaDigitalPort`:
    - *Si* la firma es válida, pasa a `FIRMADA` y se habilita la emisión oficial de la licencia (`POST /api/licencias/emitir`).
    - *Si* la firma falla o el proveedor no está disponible, pasa a `ERROR_FIRMA` y el sistema **bloquea la emisión oficial de la licencia y la activación del QR público** (política *fail-closed*, ADR-04/ADR-07).

### US-22: Verificación Móvil con Mapa Interactivo para Inspectores (M10)
* **Narrativa:**
  > **Como** inspector municipal en campo (`ROLE_INSPECTOR`),
  > **quiero** autenticarme en el portal de verificación móvil al escanear el QR de una licencia,
  > **para** visualizar la georreferenciación en mapa interactivo (Leaflet), coordenadas exactas y datos de control sin vulnerar la privacidad pública.
* **Criterios de Aceptación:**
  - *Dado* el escaneo del QR de una licencia vigente (`GET /api/public/licencias/{codigoQr}`),
  - *Cuando* el usuario consulta sin autenticación, recibe únicamente la **Vista Ciudadana Minimizada** (Ley N° 29733: razón social, nombre comercial, giro, vigencia, sin DNI ni geolocalización).
  - *Cuando* el inspector inicia sesión con credenciales institucionales e invoca `GET /api/licencias/inspeccion/{codigoQr}`,
  - *Entonces* recibe la **Vista de Inspector Ampliada** con latitud, longitud, dirección desglosada, titular y renderizado automático en mapa interactivo Leaflet.

---

## 4. Trazabilidad con Requisitos y Atributos de Calidad

| Historia | Requisito Funcional | Atributo de Calidad / Driver | Decisión Arquitectónica |
|---|---|---|---|
| **US-01 / US-17** | RF-01, RF-02 | Usabilidad (AC-01) | ADR-01, ADR-03 |
| **US-16** | RF-14, RF-15 | Escalabilidad (AC-03) | ADR-03 |
| **US-18** | RF-05, RF-16 | Inmutabilidad e Integridad (AC-02) | ADR-03 |
| **US-19** | RF-04, RF-17 | Fiabilidad y Trazabilidad (AC-04) | ADR-06 |
| **US-21** | RF-08, RF-18 | Seguridad Criptográfica y Legalidad (AC-02) | ADR-04, ADR-07 |
| **US-22** | RF-10, RF-19 | Protección de Datos y Rendimiento (AC-02, AC-03) | ADR-02, ADR-05 |
