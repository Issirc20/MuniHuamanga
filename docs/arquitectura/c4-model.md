# Modelo de Arquitectura C4 — Sistema de Licencias de Funcionamiento
## MuniHuamanga V2 — Municipalidad Provincial de Huamanga (Ayacucho, Perú)

> **Marco Normativo:** Ley N° 28976 / TUO D.S. N° 046-2017-PCM / D.S. N° 002-2018-PCM / D.S. N° 163-2020-PCM / Ley N° 27269 / Ley N° 29733
> **Plataforma Tecnológica:** Java 21 LTS, Spring Boot 3.3.4, PostgreSQL 16, OpenPDF 2.0.3, ZXing 3.5.3
> **Estado:** Documento Canónico Vigente — MuniHuamanga V2
> **Ubicación:** `docs/arquitectura/c4-model.md`

---

## 1. Convenciones y Leyenda del Modelo C4

Para garantizar total transparencia técnica y fidelidad con la realidad del repositorio, los componentes se identifican bajo las siguientes categorías de estado:

| Distintivo Visual / Estado | Significado Técnico en MuniHuamanga V2 |
| :---: | :--- |
| **[IMPLEMENTADO]** | Código fuente, persistencia y pruebas automatizadas 100% funcionales en el repositorio. |
| **[PARCIAL]** | Backend implementado; interfaz de usuario o vinculación frontend en proceso de integración. |
| **[SIMULADO EN PRUEBAS]** | Mocks y adaptadores desacoplados empleados en suites de prueba unitarias/integración. |
| **[PENDIENTE INSTITUCIONAL]** | Requiere convenio, credenciales oficiales o infraestructura física municipal externa (IOFE, SAT, RENIEC). |

---

## 2. Nivel 1 — Diagrama de Contexto del Sistema

Describe las interacciones entre los usuarios (administrados, operadores, funcionarios e inspectores) y los sistemas externos del ecosistema municipal y gubernamental:

```mermaid
graph TD
    classDef person fill:#08427b,stroke:#073b6f,color:#fff;
    classDef system fill:#1168bd,stroke:#0b4884,color:#fff;
    classDef extManual fill:#718096,stroke:#4a5568,color:#fff;
    classDef extPendiente fill:#d69e2e,stroke:#b7791f,color:#fff;

    ciudadano["Ciudadano / Administrado<br>[Persona]<br>Registra cuenta DNI/RUC, elabora borradores,<br>presenta formalmente su solicitud y subsana observaciones"]:::person
    operador["Operador Delegado<br>[Persona]<br>Persona autorizada con permisos específicos<br>para gestionar trámites de una persona jurídica"]:::person
    cajeroSat["Cajero SAT Huamanga<br>[Persona]<br>Recauda tasas TUPA presencialmente<br>y valida comprobantes de pago"]:::person
    evaluador["Funcionario Evaluador<br>[Persona]<br>Evalúa zonificación PDU, verifica requisitos,<br>elabora informes técnicos y registra observaciones"]:::person
    autoridad["Autoridad Competente (Subgerente)<br>[Persona]<br>Aprueba informes técnicos, emite resoluciones<br>administrativas y autoriza licencias de funcionamiento"]:::person
    inspector["Inspector Municipal<br>[Persona]<br>Fiscaliza en campo, escanea QR y verifica<br>vigencia y georreferenciación oficial en mapa"]:::person

    sistema["MuniHuamanga V2<br>[Sistema de Software - Monolito Modular]<br>Gestión integral del trámite de licencias, 10 estados normativos,<br>cálculo TUPA, verificación QR y trazabilidad inmutable"]:::system

    sat["SAT Huamanga<br>[Sistema Externo - Proceso Manual / Ventanilla]<br>Recaudación física de tasas TUPA y emisión de comprobantes"]:::extManual
    defensaCivil["Subgerencia de Defensa Civil<br>[Entidad Externa - Trámite Presencial]<br>Inspección técnica de seguridad en edificaciones (ITSE)"]:::extManual
    iofe["Proveedor de Firma Digital IOFE<br>[Sistema Externo - Pendiente Activación]<br>Infraestructura Oficial de Firma Electrónica (Ley 27269)"]:::extPendiente
    pide["Plataforma PIDE (SUNAT / RENIEC / SUNARP)<br>[Sistema Externo - Pendiente Integración]<br>Validación automática de identidad y vigencia de poder"]:::extPendiente

    ciudadano -->|"Registra solicitud, carga documentos y subsana"| sistema
    operador -->|"Gestiona trámites autorizados"| sistema
    cajeroSat -->|"Valida comprobante de pago manualmente"| sistema
    evaluador -->|"Evalúa zonificación y elabora informe técnico"| sistema
    autoridad -->|"Aprueba informe y emite resolución"| sistema
    inspector -->|"Consulta autenticidad y ubicación en campo"| sistema

    sistema -.->|"Cotejo manual de comprobante de pago"| sat
    sistema -.->|"Presentación física de Anexos 3 y 4"| defensaCivil
    sistema -.->|"FirmaDigitalPort (Fail-closed si no está activo)"| iofe
    sistema -.->|"Verificación registral y de personería"| pide
```

---

## 3. Nivel 2 — Diagrama de Contenedores

Ilustra la arquitectura técnica consolidada: clientes web ligeros, el contenedor backend Spring Boot 3.3.4 (Monolito Modular) y la persistencia en PostgreSQL 16:

```mermaid
graph TD
    classDef frontend fill:#63a0e2,stroke:#3b7abf,color:#fff;
    classDef container fill:#438dd5,stroke:#2e6295,color:#fff;
    classDef db fill:#23527c,stroke:#173752,color:#fff;
    classDef ext fill:#718096,stroke:#4a5568,color:#fff;

    subgraph ClientesWeb [Capa de Presentación: Interfaces Web Ligeras]
        landing["Portal Principal / Hub<br>(index.html)<br>[IMPLEMENTADO]"]:::frontend
        pCiudadano["Portal Ciudadano<br>(portal-ciudadano.html / js)<br>[PARCIAL - Pendiente enlace V2]"]:::frontend
        pInterno["Portal Interno Municipal<br>(portal-interno.html / js)<br>[IMPLEMENTADO - RBAC JWT]"]:::frontend
        pVerif["Verificación QR e Inspectores<br>(verificar-licencia.html / js)<br>[IMPLEMENTADO - Leaflet OpenStreetMap]"]:::frontend
        pAnexo1["Formato Anexo 1 Oficial<br>(formato-anexo1.html / js)<br>[IMPLEMENTADO]"]:::frontend
    end

    subgraph BackendApp [Contenedor de Aplicación: servicio-expedientes :8081]
        app["MuniHuamanga Monolito Modular<br>(Java 21 LTS / Spring Boot 3.3.4)<br>• M01 a M10 Bounded Contexts implementados<br>• Spring Security 6 Stateless (JWT)<br>• Generadores OpenPDF y ZXing QR en memoria<br>• FirmaDigitalPort con salvaguarda fail-closed"]:::container
    end

    subgraph Almacenamiento [Capa de Persistencia Relacional]
        postgres[("PostgreSQL 16 Relacional<br>(muni_licencias_db :5432)<br>• 8 scripts SQL aditivos (01..08)<br>• Restricciones UNIQUE y ON DELETE RESTRICT<br>• Binarios PDF inmutables en BYTEA")]:::db
    end

    subgraph Externos [Servicios Externos de Soporte]
        smtp["Servidor SMTP / JavaMail<br>[IMPLEMENTADO - Alertas asíncronas]"]:::ext
        satManual["Ventanilla SAT Huamanga<br>[PROCESO MANUAL]"]:::ext
    end

    landing --> pCiudadano
    landing --> pInterno
    landing --> pVerif
    landing --> pAnexo1

    pCiudadano -->|"HTTP REST / JSON"| app
    pInterno -->|"HTTP REST / Bearer JWT"| app
    pVerif -->|"HTTP REST / Rate Limiter (60/min)"| app
    pAnexo1 -->|"HTTP REST / Descarga PDF"| app

    app -->|"Spring Data JPA / HikariCP / Transacciones ACID"| postgres
    app -->|"JavaMail asíncrono"| smtp
    app -.->|"Validación manual por cajero"| satManual
```

---

## 4. Nivel 3 — Diagrama de Componentes: Monolito Modular

Detalla los componentes internos organizados según Clean Architecture dentro del backend `servicio-expedientes`:

```mermaid
graph TD
    classDef comp fill:#63a0e2,stroke:#3b7abf,color:#fff;
    classDef sec fill:#e53e3e,stroke:#c53030,color:#fff;
    classDef pdf fill:#d69e2e,stroke:#b7791f,color:#fff;
    classDef port fill:#38a169,stroke:#276749,color:#fff;
    classDef db fill:#23527c,stroke:#173752,color:#fff;

    subgraph CapaPresentacion [Capa 4: Controladores REST y Seguridad]
        authCtrl["AuthController<br>(POST /api/auth/login)"]:::sec
        cuentaCtrl["CuentaController<br>(/api/cuentas)"]:::comp
        borradorCtrl["SolicitudBorradorController<br>(/api/borradores)"]:::comp
        expCtrl["ExpedienteController<br>(/api/expedientes)"]:::comp
        pagoCtrl["PagoExpedienteController<br>(/api/pagos)"]:::comp
        zonifCtrl["EvaluacionZonificacionController<br>(/api/zonificacion)"]:::comp
        evalCtrl["EvaluacionAdministrativaController<br>(/api/evaluaciones)"]:::comp
        resCtrl["ResolucionAdministrativaController<br>(/api/resoluciones)"]:::comp
        licCtrl["LicenciaController<br>(/api/licencias)"]:::comp
        inspCtrl["InspectorLicenciasController<br>(/api/inspector/licencias)"]:::comp
        secConfig["SecurityConfig / JwtFilter / BOLA Evaluators"]:::sec
    end

    subgraph CapaAplicacion [Capa 2: Servicios de Casos de Uso]
        cuentaSvc["CuentaService<br>[IMPLEMENTADO]"]:::comp
        borradorSvc["SolicitudBorradorService<br>[IMPLEMENTADO]"]:::comp
        presentaSvc["PresentacionFormalService<br>[IMPLEMENTADO]"]:::comp
        expSvc["ExpedienteService<br>[IMPLEMENTADO]"]:::comp
        pagoSvc["PagoExpedienteService<br>[IMPLEMENTADO]"]:::comp
        zonifSvc["EvaluacionZonificacionService<br>[IMPLEMENTADO]"]:::comp
        evalSvc["InformeAdministrativoService<br>[IMPLEMENTADO]"]:::comp
        checklistSvc["ChecklistAdministrativoService<br>[IMPLEMENTADO]"]:::comp
        resSvc["ResolucionAdministrativaService<br>[IMPLEMENTADO]"]:::comp
        licSvc["LicenciaService<br>[IMPLEMENTADO]"]:::comp
        rateLimiter["RateLimiterService<br>[IMPLEMENTADO]"]:::comp
        auditoriaSvc["AuditoriaService<br>[IMPLEMENTADO]"]:::comp
    end

    subgraph CapaPuertos [Puertos Hexagonales e Interfaces]
        firmaPort["FirmaDigitalPort<br>(Interface de Salida)"]:::port
        firmaAdapter["FirmaDigitalAdapterService<br>(Fail-Closed Adapter)"]:::comp
    end

    subgraph CapaAdaptadores [Capa 3: Generadores Documentales y Persistencia]
        anexo1Gen["Anexo1PdfGenerator (OpenPDF)"]:::pdf
        licPdfGen["LicenciaPdfGenerator (OpenPDF + ZXing)"]:::pdf
        jpas["Repositorios Spring Data JPA<br>(Expediente, Resolucion, Licencia, Pago, etc.)"]:::db
    end

    expCtrl --> expSvc
    cuentaCtrl --> cuentaSvc
    borradorCtrl --> borradorSvc
    borradorCtrl --> presentaSvc
    pagoCtrl --> pagoSvc
    zonifCtrl --> zonifSvc
    evalCtrl --> evalSvc
    evalCtrl --> checklistSvc
    resCtrl --> resSvc
    licCtrl --> licSvc
    inspCtrl --> licSvc
    licCtrl --> rateLimiter

    resSvc --> firmaPort
    firmaPort --> firmaAdapter
    licSvc --> licPdfGen
    expSvc --> anexo1Gen
    expSvc --> auditoriaSvc

    cuentaSvc --> jpas
    borradorSvc --> jpas
    expSvc --> jpas
    pagoSvc --> jpas
    evalSvc --> jpas
    resSvc --> jpas
    licSvc --> jpas
```

---

## 4. Nivel 4 — Diagramas de Código y Flujos Críticos

### 4.1. Flujo Crítico de Emisión Oficial de Licencia (M08 -> M09 -> M10)
Demuestra el principio *fail-closed* donde ninguna licencia adquiere carácter oficial ni genera QR válido sin la firma digital de la resolución administrativa previa:

```mermaid
sequenceDiagram
    autonumber
    actor A as Autoridad Competente (ROLE_ADMIN)
    participant RC as ResolucionAdministrativaController
    participant RS as ResolucionAdministrativaService
    participant FP as FirmaDigitalPort
    participant LC as LicenciaController
    participant LS as LicenciaService
    participant DB as PostgreSQL 16

    A->>RC: POST /api/resoluciones/{id}/firmar
    RC->>RS: firmarResolucionAdministrativa(id, firmanteDni, nombre, cargo)
    RS->>FP: firmarDocumento(pdfBytes, dni, nombre, cargo)
    alt Proveedor de firma no habilitado / Falla técnica
        FP-->>RS: ResultadoFirmaDigital.error("Servicio no configurado")
        RS->>DB: Actualiza estado = ERROR_FIRMA
        RS-->>RC: HTTP 422 Unprocessable Entity
        RC-->>A: Emisión bloqueada: Resolución pendiente de firma oficial
    else Firma digital exitosa
        FP-->>RS: ResultadoFirmaDigital.exito(pdfFirmadoBytes, hashSha256)
        RS->>DB: Actualiza estado = FIRMADA, archivo_pdf = pdfFirmadoBytes
        RS-->>RC: HTTP 200 OK (Resolución Oficial Firme)

        A->>LC: POST /api/licencias/emitir
        LC->>LS: emitirLicencia(resolucionId, tipoVigencia)
        LS->>DB: Verifica resolucion.estado == FIRMADA
        LS->>LS: Genera token QR criptográfico no enumerable (SecureRandom)
        LS->>LS: Renderiza Licencia Oficial OpenPDF con QR ZXing
        LS->>DB: Guarda LicenciaEmitida (LIC-YYYY-XXXXX, archivo_pdf, hashSha256)
        LS->>DB: Transiciona Expediente -> APROBADO (licenciaQrCode)
        LS-->>LC: LicenciaEmitidaDto
        LC-->>A: HTTP 201 Created (Licencia Oficial Emitida)
    end
```

---

## 5. Resumen de Estados de Implementación

| Componente | Estado Arquitectónico | Evidencia en el Código |
| :--- | :---: | :--- |
| **M01: Cuentas y Operadores** | **IMPLEMENTADO** | `CuentaController`, `CuentaService`, `Cuenta`, `OperadorCuenta` (13 tests). |
| **M02: Establecimientos** | **IMPLEMENTADO** | `EstablecimientoController`, `EstablecimientoService`, `CatalogoCiiu` (11 tests). |
| **M03: Borradores** | **IMPLEMENTADO** | `SolicitudBorradorController`, `SolicitudBorradorService` (15 tests). |
| **M04: Documentación ITSE** | **IMPLEMENTADO** | `DocumentoVersionadoController`, generadores de Anexos 1, 3 y 4 (43 tests). |
| **M05: Pagos SAT** | **IMPLEMENTADO** | `PagoExpedienteController`, `PagoExpedienteService` (22 tests). |
| **M06: Expedientes** | **IMPLEMENTADO** | `ExpedienteController`, `ExpedienteService`, 10 estados (33 tests). |
| **M07: Zonificación** | **IMPLEMENTADO** | `EvaluacionZonificacionController`, `SolicitudRectificacionController` (17 tests). |
| **M08: Evaluación e Informes** | **IMPLEMENTADO** | `EvaluacionAdministrativaController`, `InformeAdministrativoService` (27 tests). |
| **M09: Resoluciones y Licencias** | **IMPLEMENTADO** | `ResolucionAdministrativaController`, `LicenciaService`, `FirmaDigitalPort` (22 tests). |
| **M10: Verificación QR e Inspector** | **IMPLEMENTADO** | `LicenciaController`, `InspectorLicenciasController`, Leaflet JS (17 tests). |
| **M11: Notificaciones** | **PARCIAL** | Solo JavaMail SMTP activo; WhatsApp pendiente de integración institucional. |
| **M12: Administración** | **PARCIAL** | Tarifas TUPA dinámicas implementadas; catálogos PDU pendientes de pantalla UI. |
