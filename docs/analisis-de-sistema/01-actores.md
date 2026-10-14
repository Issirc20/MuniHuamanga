# 01. Actores del Sistema
## Sistema de Gestión Documentaria de Licencia de Funcionamiento
### Municipalidad Provincial de Huamanga (Ayacucho, Perú) — Versión 2.0

> **Marco Normativo:** Ley N° 28976 / TUO D.S. N° 046-2017-PCM / D.S. N° 002-2018-PCM / Ley N° 27444 / Ley N° 27269 / Ley N° 29733
> **Plataforma:** Java 21 LTS, Spring Boot 3.3.4, PostgreSQL 16, Spring Security 6 (RBAC Stateless)
> **Ubicación:** `docs/analisis-de-sistema/01-actores.md`
> **Estado:** Documento Canónico Vigente V2

---

## 1. Introducción

En el marco del procedimiento administrativo para el otorgamiento de **Licencias de Funcionamiento** e **Inspecciones Técnicas de Seguridad en Edificaciones (ITSE)** en la Municipalidad Provincial de Huamanga, intervienen actores humanos (ciudadanos, funcionarios y servidores públicos) así como sistemas institucionales y servicios de infraestructura técnica.

En MuniHuamanga V2, el modelo de control de acceso se rige estrictamente por **RBAC (Role-Based Access Control)** con autorización centralizada en backend (`@PreAuthorize`) y protección contra manipulación de identificadores (IDOR/BOLA, ADR-02).

---

## 2. Diagrama General de Actores

```mermaid
graph TD
    subgraph Actores Humanos
        A1["👤 Administrado / Solicitante<br>ROLE_CIUDADANO (Portal Web / Cuentas V2)"]
        A2["👤 Cajero / Operador SAT<br>ROLE_CAJERO (Validación manual pagos)"]
        A3["👤 Evaluador Técnico y Legal<br>ROLE_EVALUADOR (Subgerencia de Licencias)"]
        A4["👤 Autoridad Resolutiva<br>ROLE_ADMIN (Emisión resoluciones con firma)"]
        A5["👤 Inspector Municipal en Campo<br>ROLE_INSPECTOR (Consulta móvil QR ampliada)"]
        A6["👤 Administrador del Sistema<br>ROLE_ADMIN (TI Municipal / Configuración)"]
        A7["🌐 Público General / Vecinos<br>Acceso público minimizado (QR Ley 29733)"]
    end

    subgraph Plataforma MuniHuamanga V2
        SIS["🏛️ Monolito Modular<br>(servicio-expedientes.jar)"]
    end

    subgraph Instancias y Servicios Externos (ADR-04, ADR-06)
        E1["🏛️ SAT Huamanga<br>(Pago en ventanilla externa)"]
        E2["🚒 Subgerencia Defensa Civil<br>(Inspección física y anexos presenciales)"]
        E3["📐 Gerencia de Desarrollo Urbano<br>(Cartografía PDU manual)"]
        E4["🔑 Proveedor IOFE / Firma Digital<br>(FirmaDigitalPort - Fail-Closed)"]
        E5["✉️ Servidor SMTP Institucional<br>(Notificaciones Asíncronas)"]
    end

    A1 --> SIS
    A2 --> SIS
    A3 --> SIS
    A4 --> SIS
    A5 --> SIS
    A6 --> SIS
    A7 --> SIS

    SIS -.->|"Registro manual comprobante"| E1
    SIS -.->|"Declaración y revisión física"| E2
    SIS -.->|"Zonificación manual en sistema"| E3
    SIS <-->|"Sello criptográfico PKI"| E4
    SIS -->|"Alertas asíncronas"| E5
```

---

## 3. Catálogo Detallado de Actores Humanos

### 3.1. Administrado / Solicitante (Ciudadano / Representante Legal)
* **Descripción:** Persona natural con DNI o representante legal de persona jurídica con RUC que tramita una licencia comercial en la provincia de Huamanga.
* **Canal de Acceso:** Portal Web Ciudadano (`portal-ciudadano.html`) y asistente de solicitud Anexo 1 (`formato-anexo1.html`).
* **Rol RBAC:** `ROLE_CIUDADANO` (autenticado) y acceso anónimo para registro inicial y seguimiento público por código de expediente.
* **Responsabilidades:**
  1. Gestionar su cuenta ciudadana, personas jurídicas y establecimientos comerciales (modelo desacoplado V2).
  2. Completar formularios normativos Anexo 1 (45 atributos normativos) y declaración de condiciones de seguridad Anexo 4.
  3. Registrar comprobantes de pago de la tasa TUPA abonados en ventanillas del SAT Huamanga.
  4. Descargar formatos en PDF y subsanar observaciones notificadas dentro del plazo legal.
  5. Consultar y descargar el Certificado Oficial de Licencia una vez firmada la resolución.

### 3.2. Cajero / Operador de Pagos SAT
* **Descripción:** Funcionario responsable de registrar y verificar los abonos de tasas TUPA cancelados por los administrados.
* **Canal de Acceso:** Portal Interno de Gestión (`portal-interno.html`), pestaña Pagos.
* **Rol RBAC:** `ROLE_CAJERO` (o `ROLE_ADMIN`).
* **Responsabilidades:**
  1. Cotejar el comprobante registrado por el administrado contra el sistema de caja SAT Huamanga (proceso manual institucional, ADR-06).
  2. Validar manualmente el pago indicando número de comprobante, monto y fecha de abono.
  3. Observar o rechazar pagos inconsistentes con motivo fundamentado.

### 3.3. Evaluador Técnico y Legal (Subgerencia de Licencias)
* **Descripción:** Profesional técnico-legal adscrito a la Subgerencia de Licencias y Autorizaciones Comerciales.
* **Canal de Acceso:** Portal Interno de Gestión (`portal-interno.html`), bandejas de Evaluación y Zonificación.
* **Rol RBAC:** `ROLE_EVALUADOR` (o `ROLE_ADMIN`).
* **Responsabilidades:**
  1. Realizar la evaluación administrativa de requisitos y admisibilidad.
  2. Evaluar compatibilidad de zonificación según Plano de Desarrollo Urbano (PDU) de Huamanga.
  3. Emitir observaciones formales otorgando plazo improrrogable de subsanación.
  4. Redactar el **Informe Administrativo Técnico** con dictamen favorable o desfavorable (acto de instrucción interna, ADR-07).
  5. Gestionar y resolver solicitudes de rectificación de ubicación física o cartográfica.

### 3.4. Autoridad Resolutiva (Gerencia / Subgerencia)
* **Descripción:** Funcionario titular con facultades delegadas para resolver expedientes administrativos mediante resolución y emitir la licencia.
* **Canal de Acceso:** Portal Interno de Gestión (`portal-interno.html`), módulo de Resoluciones.
* **Rol RBAC:** `ROLE_ADMIN`.
* **Responsabilidades:**
  1. Revisar los informes técnicos elaborados por el evaluador.
  2. Aprobar o denegar el expediente mediante **Resolución Administrativa** formal con numeración atómica anual (`RES-YYYY-XXXXX`, ADR-07).
  3. Ejecutar la firma digital institucional mediante `FirmaDigitalPort`.
  4. Habilitar la emisión atómica de la Licencia de Funcionamiento oficial (`LIC-YYYY-XXXXX`) exclusivamente tras verificar la firma válida.

### 3.5. Inspector Municipal en Campo
* **Descripción:** Personal de la Subgerencia de Licencias que realiza inspecciones operativas y verificaciones in situ en locales comerciales.
* **Canal de Acceso:** Módulo de Verificación Móvil (`verificar-licencia.html`) con credenciales institucionales.
* **Rol RBAC:** `ROLE_INSPECTOR` (o `ROLE_ADMIN`).
* **Responsabilidades:**
  1. Escanear el código QR impreso en el certificado de licencia mediante dispositivo móvil.
  2. Iniciar sesión para desbloquear la **Vista de Inspector**, que incluye coordenadas geográficas exactas, mapa interactivo (Leaflet) y fecha de autorización.
  3. Verificar en campo si el giro real y la ubicación física coinciden con lo autorizado.
  4. *Nota técnica (ADR-06):* El sistema actúa como herramienta de consulta de autenticidad y ubicación; no incluye módulos de fiscalización sancionadora ni levantamiento de actas de clausura.

### 3.6. Administrador del Sistema (TI Municipal)
* **Descripción:** Personal de la Subgerencia de Tecnologías de la Información.
* **Canal de Acceso:** Portal Interno, configuración del sistema y endpoints de gestión.
* **Rol RBAC:** `ROLE_ADMIN`.
* **Responsabilidades:**
  1. Crear y administrar cuentas de operadores institucionales (`/api/cuentas/operadores`).
  2. Mantenimiento del tarifario TUPA dinámico en base de datos.
  3. Monitoreo de auditoría inmutable, logs y salud de la plataforma (Spring Actuator).
  4. Configuración de parámetros de conexión institucional y firma digital.

### 3.7. Público General / Vecinos / Consumidores
* **Descripción:** Cualquier persona que escanea el código QR de un certificado comercial o ingresa al portal de verificación pública.
* **Canal de Acceso:** `verificar-licencia.html?token=...` (acceso anónimo).
* **Rol RBAC:** Público (sin autenticación requerida).
* **Responsabilidades / Alcance:**
  1. Visualizar la **Vista Ciudadana Minimizada** (Ley N° 29733 de Protección de Datos Personales, ADR-05): Razón social, nombre comercial, giro autorizado, dirección pública, estado de vigencia y validez de firma.
  2. Restricción estricta: No se exponen DNI, RUC ni coordenadas geográficas en esta vista.

---

## 4. Matriz de Control de Acceso Basado en Roles (RBAC V2)

| Módulo / Acción en el Sistema | Público Anónimo | `ROLE_CIUDADANO` | `ROLE_CAJERO` | `ROLE_EVALUADOR` | `ROLE_INSPECTOR` | `ROLE_ADMIN` |
|---|:---:|:---:|:---:|:---:|:---:|:---:|
| **Registro de Solicitud / Borrador V2** | ❌ | ✅ | ❌ | ❌ | ❌ | ✅ |
| **Consulta Pública de Expediente (Código)** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Gestión de Cuentas y Establecimientos** | ❌ | ✅ (Propios) | ❌ | ❌ | ❌ | ✅ (Todos) |
| **Validación Manual de Pagos SAT** | ❌ | ❌ | ✅ | ❌ | ❌ | ✅ |
| **Evaluación Técnica y Zonificación PDU** | ❌ | ❌ | ❌ | ✅ | ❌ | ✅ |
| **Emisión de Informes Técnicos** | ❌ | ❌ | ❌ | ✅ | ❌ | ✅ |
| **Emisión y Firma Digital de Resolución** | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| **Emisión Oficial de Licencia con QR** | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ (Post-firma) |
| **Verificación QR: Vista Pública (Minimizada)** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Verificación QR: Vista Inspector (Mapa/Geo)** | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ |
| **Administración de Operadores y TUPA** | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| **Auditoría Inmutable del Sistema** | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |

---

## 5. Referencias Normativas y Arquitectónicas

1. **Ley N° 28976:** Ley Marco de Licencia de Funcionamiento y sus modificatorias.
2. **Ley N° 27269:** Ley de Firmas y Certificados Digitales.
3. **Ley N° 29733:** Ley de Protección de Datos Personales (minimización en QR público).
4. **ADR-02:** Autorización centralizada en Backend y protección IDOR/BOLA.
5. **ADR-05:** Verificación pública QR con enlace dinámico y minimización de datos.
6. **ADR-06:** Procesos externos manuales para SAT, Defensa Civil y Edificaciones (sin acoplamiento).
7. **ADR-07:** Acto administrativo resolutivo, correlativo atómico y bloqueo de emisión sin firma digital.
