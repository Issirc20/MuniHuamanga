# 08. Decisiones Arquitectónicas (ADR) — Índice y Registro Canónico
## Sistema de Gestión Documentaria de Licencia de Funcionamiento
### Municipalidad Provincial de Huamanga (Ayacucho, Perú)

> **Documento Canónico Maestro Vigente (V2):** [`docs/v2/DECISIONES_ARQUITECTURA.md`](../v2/DECISIONES_ARQUITECTURA.md)
> **Código de Documento:** DAS-MH-V2-001 • ADR-Consolidado
> **Fecha de Actualización:** 14 de octubre de 2026
> **Formato:** Markdown Architectural Decision Records (MADR) / Michael Nygard
> **Ubicación:** `docs/analisis-de-sistema/08-decisiones-arquitectonicas.md`

---

## 1. Declaración de Fuente Canónica Única

Para evitar divergencias técnicas y mantener una única fuente de verdad en la arquitectura de software, **el registro maestro y vinculante de Decisiones de Arquitectura de MuniHuamanga V2 reside en**:

👉 **[`docs/v2/DECISIONES_ARQUITECTURA.md`](../v2/DECISIONES_ARQUITECTURA.md)**

El presente documento actúa como **índice transversal de trazabilidad** y conserva los **antecedentes históricos de la Fase V1**, permitiendo auditar la evolución de las decisiones desde el diseño preliminar hasta la consolidación de MuniHuamanga V2.

---

## 2. Matriz Maestra de Decisiones Arquitectónicas V2 (Vigentes)

Las siguientes 7 decisiones arquitectónicas rigen el diseño, implementación y seguridad de MuniHuamanga V2:

| Identificador | Título de la Decisión | Estado | Bounded Context / Módulo Impactado | Referencia Detallada |
| :--- | :--- | :---: | :--- | :--- |
| **ADR-01** | Preservación de Monolito Modular con Clean Architecture | Aceptado | Core / Todos los módulos (M01–M12) | [ADR-01 en V2](../v2/DECISIONES_ARQUITECTURA.md#adr-01-preservación-de-monolito-modular-con-clean-architecture) |
| **ADR-02** | Autorización Centralizada en Backend y Protección IDOR/BOLA | Aceptado | Seguridad / M01, M02, M03, M06, M09, M10 | [ADR-02 en V2](../v2/DECISIONES_ARQUITECTURA.md#adr-02-autorización-centralizada-en-backend-y-protección-idorbola) |
| **ADR-03** | Normalización de Entidades y Repositorio Documental Inmutable | Aceptado | Dominio / M01, M02, M03, M04, M07 | [ADR-03 en V2](../v2/DECISIONES_ARQUITECTURA.md#adr-03-normalización-de-entidades-y-repositorio-documental-inmutable) |
| **ADR-04** | Integración de Firma Digital Mediante Puerto y Bloqueo de Emisión Falsa | Aceptado | Integración / M08, M09, M10 | [ADR-04 en V2](../v2/DECISIONES_ARQUITECTURA.md#adr-04-integración-de-firma-digital-mediante-puerto-y-bloqueo-de-emisión-falsa) |
| **ADR-05** | Verificación Pública QR con Enlace Dinámico y Minimización de Datos | Aceptado | Fiscalización / M10 (Ley N° 29733) | [ADR-05 en V2](../v2/DECISIONES_ARQUITECTURA.md#adr-05-verificación-pública-qr-con-enlace-dinámico-y-minimización-de-datos) |
| **ADR-06** | Procesos Externos Manuales para SAT, Defensa Civil y Edificaciones | Aceptado | Integración / M05, M06, M07 | [ADR-06 en V2](../v2/DECISIONES_ARQUITECTURA.md#adr-06-procesos-externos-manuales-para-sat-defensa-civil-y-edificaciones) |
| **ADR-07** | Acto Administrativo Resolutivo, Correlativo Atómico y Bloqueo de Emisión sin Firma Digital | Aceptado | Resoluciones y Licencias / M08, M09 | [ADR-07 en V2](../v2/DECISIONES_ARQUITECTURA.md#adr-07-acto-administrativo-resolutivo-correlativo-atómico-y-bloqueo-de-emisión-sin-firma-digital) |

---

## 3. Antecedentes Históricos: Registro de Decisiones Fase V1

Durante la fase inicial de concepción del proyecto (Fase V1) se adoptaron decisiones preliminares que sentaron las bases técnicas del sistema y que luego fueron consolidadas o perfeccionadas en V2:

### Histórico V1 — ADR-01: Monolito Modular vs. Microservicios Distribuidos
* **Fecha:** Septiembre 2026.
* **Contexto:** Se evaluó una arquitectura distribuida en 5 microservicios (`api-gateway`, `servicio-expedientes`, `servicio-formularios`, `servicio-verificacion-licencias`, `adaptador-integracion`).
* **Decisión:** Consolidar en un Monolito Modular (`servicio-expedientes.jar`) por viabilidad técnica y operativa municipal.
* **Evolución a V2:** Reconfirmado y profundizado en **ADR-01 (V2)**, formalizando límites modulares de paquetes.

### Histórico V1 — ADR-02: Clean Architecture y Puertos Hexagonales
* **Fecha:** Septiembre 2026.
* **Contexto:** Necesidad de aislar la lógica de expedientes respecto de marcos legales cambiantes y dependencias externas.
* **Decisión:** Regla de dependencia concéntrica e interfaces portuarias puras.
* **Evolución a V2:** Reconfirmado en **ADR-01 (V2)** y concretado en **ADR-04 (V2)** para `FirmaDigitalPort`.

### Histórico V1 — ADR-03: Stack Java 21 LTS y Spring Boot 3.3.4
* **Fecha:** Septiembre 2026.
* **Contexto:** Estandarización en tecnología empresarial de soporte a largo plazo con Jakarta EE 10.
* **Decisión:** Adopción de Java 21 LTS + Spring Boot 3.3.4 + Maven.
* **Evolución a V2:** Mantenido como stack tecnológico base de toda la plataforma V2.

### Histórico V1 — ADR-04: Renderizado Documental In-Memory OpenPDF y ZXing
* **Fecha:** Septiembre 2026.
* **Contexto:** Emisión de Anexos 1, 3, 4 y licencias sin almacenamiento de archivos temporales en disco.
* **Decisión:** Generación en memoria volátil (`ByteArrayOutputStream`) retornando `application/pdf`.
* **Evolución a V2:** Mantenido y enriquecido en **ADR-07 (V2)** con marcas de agua dinámicas vectoriales ("VISTA PRELIMINAR - SIN VALIDEZ OFICIAL").

### Histórico V1 — ADR-05: Seguridad Stateless con Spring Security 6 y JJWT (RBAC)
* **Fecha:** Septiembre 2026.
* **Contexto:** Autenticación por roles para funcionarios sin sesiones en servidor (`HttpSession`).
* **Decisión:** Tokens JWT HMAC-SHA256 y roles `ROLE_ADMIN`, `ROLE_EVALUADOR`, `ROLE_CAJERO`.
* **Evolución a V2:** Evolucionado en **ADR-02 (V2)** hacia autorización a nivel de método (`@PreAuthorize`) y protección IDOR/BOLA, incorporando `ROLE_INSPECTOR` y `ROLE_CIUDADANO`.

### Histórico V1 — ADR-06: Tarifario TUPA Dinámico con Persistencia Relacional
* **Fecha:** Septiembre 2026.
* **Contexto:** Actualización de tasas municipales aprobadas por ordenanzas sin recompilar el código.
* **Decisión:** Tabla relacional `tarifas_tupa` con mantenimiento en caliente y fallback a `application.yml`.
* **Evolución a V2:** Mantenido en el módulo M05.

### Histórico V1 — ADR-07: Notificaciones Asíncronas por Correo Electrónico
* **Fecha:** Septiembre 2026.
* **Contexto:** Envíos SMTP sin bloquear los hilos HTTP principales.
* **Decisión:** Anotación `@Async("emailExecutor")` y plantillas de correo en segundo plano.
* **Evolución a V2:** Mantenido en el módulo M07.

### Histórico V1 — ADR-08: Base de Datos Relacional PostgreSQL 15 ACID
* **Fecha:** Septiembre 2026.
* **Contexto:** Almacenamiento estructurado de trámites y tasas con integridad transaccional.
* **Decisión:** Adopción de PostgreSQL como motor primario (actualizado a **PostgreSQL 16** en V2).
* **Evolución a V2:** Perfeccionado en **ADR-03 (V2)** con normalización de entidades (`cuentas`, `establecimientos`, `documentos_adjuntos`).

### Histórico V1 — ADR-09: Tramitación Híbrida de Anexos ITSE
* **Fecha:** Septiembre 2026.
* **Contexto:** Requisitos físicos y presenciales de CENEPRED y D.S. N° 002-2018-PCM para Anexos 3 y 4.
* **Decisión:** Tramitación digital de datos con presentación y suscripción física ante Defensa Civil.
* **Evolución a V2:** Consolidado en **ADR-06 (V2)**, delimitando estrictamente los procesos manuales sin acoplamiento no probado.

---

## 4. Trazabilidad de Evolución V1 ➡️ V2

```mermaid
graph LR
    subgraph Fase V1 (Antecedentes)
        V1_01["ADR-01: Monolito vs Microservicios"]
        V1_02["ADR-02: Clean Architecture"]
        V1_03["ADR-03: Java 21 / Spring Boot 3"]
        V1_04["ADR-04: OpenPDF / ZXing"]
        V1_05["ADR-05: Spring Security / JWT"]
        V1_06["ADR-06: TUPA Dinámico"]
        V1_07["ADR-07: Email Asíncrono"]
        V1_08["ADR-08: PostgreSQL ACID"]
        V1_09["ADR-09: Anexos ITSE Híbridos"]
    end

    subgraph Fase V2 (Registro Canónico Maestro)
        V2_01["ADR-01 (V2): Monolito Modular & Clean Architecture"]
        V2_02["ADR-02 (V2): Autorización Backend & IDOR/BOLA"]
        V2_03["ADR-03 (V2): Cuentas, Establecimientos e Inmutabilidad"]
        V2_04["ADR-04 (V2): Firma Digital Port & Bloqueo Emisión"]
        V2_05["ADR-05 (V2): Verificación QR & Minimización Datos"]
        V2_06["ADR-06 (V2): Procesos Externos Manuales SAT/ITSE/PDU"]
        V2_07["ADR-07 (V2): Resolución Administrativa & Secuencia Atómica"]
    end

    V1_01 --> V2_01
    V1_02 --> V2_01
    V1_05 --> V2_02
    V1_08 --> V2_03
    V1_02 --> V2_04
    V1_04 --> V2_05
    V1_09 --> V2_06
    V1_04 --> V2_07
```

---

## 5. Referencia Cruzada y Consulta

Para consultar las motivaciones, evaluaciones de alternativas, diagramas específicos y justificaciones normativas completas de las decisiones que rigen el estado actual del repositorio, remitirse directamente al documento maestro:

📄 **[Decisiones de Arquitectura de Software (ADR) — MuniHuamanga V2](../v2/DECISIONES_ARQUITECTURA.md)**
