# Modelo de Arquitectura C4 — Sistema de Gestión de Licencias
### Municipalidad Provincial de Huamanga

---

## 1. Nivel 1 — Diagrama de Contexto del Sistema

```mermaid
graph TD
    classDef person fill:#08427b,stroke:#073b6f,color:#fff;
    classDef system fill:#1168bd,stroke:#0b4884,color:#fff;
    classDef extSystem fill:#999999,stroke:#666666,color:#fff;

    solicitante["Ciudadano / Administrado<br>[Persona]<br>MYPES y comerciantes que solicitan licencia de funcionamiento"]:::person
    funcionario["Funcionario Gerencia de Licencias<br>[Persona]<br>Evalúa expedientes y emite licencias firmadas digitalmente"]:::person
    verificador["Fiscalizador / Público General<br>[Persona]<br>Escanea y verifica la vigencia de la licencia mediante QR"]:::person

    sistema["Sistema de Licencias MuniHuamanga<br>[Software System]<br>Digitaliza el trámite, gestiona estados, calcula tasas y emite licencias"]:::system

    sat["SAT Huamanga (Fase 2)<br>[External System]<br>Recaudación y conciliación de pagos de tasas"]:::extSystem
    defensaCivil["Defensa Civil / ITSE (Fase 2)<br>[External System]<br>Clasificación de riesgo de seguridad en edificaciones"]:::extSystem
    edificaciones["Gerencia de Edificaciones (Fase 2)<br>[External System]<br>Certificación de zonificación y compatibilidad de uso"]:::extSystem
    firmaDigital["Plataforma de Firma Digital (PKI / RENIEC)<br>[External System]<br>Firma digital con validez legal y no repudio"]:::extSystem

    solicitante -->|Presenta solicitud y consulta estado en línea| sistema
    funcionario -->|Evalúa, valida requisitos y emite resolución| sistema
    verificador -->|Verifica autenticidad escaneando código QR| sistema

    sistema -.->|Consulta estado de pagos| sat
    sistema -.->|Registra dictamen de riesgo ITSE| defensaCivil
    sistema -.->|Registra certificado de zonificación| edificaciones
    sistema -->|Solicita firma digital de documentos| firmaDigital
```

---

## 2. Nivel 2 — Diagrama de Contenedores

```mermaid
graph TD
    classDef container fill:#438dd5,stroke:#2e6295,color:#fff;
    classDef db fill:#23527c,stroke:#173752,color:#fff;
    classDef gateway fill:#08427b,stroke:#073b6f,color:#fff;

    subgraph Frontends ["Capa de Presentación"]
        portalCiudadano["Portal Ciudadano<br>[React SPA]<br>Mesa de partes virtual y seguimiento de expedientes"]:::container
        portalInterno["Sistema Interno de Gestión<br>[React SPA]<br>Bandeja de evaluación para la Gerencia de Licencias"]:::container
        portalVerificacion["Portal Público de Verificación<br>[Web Pública]<br>Consulta pública de autenticidad vía QR"]:::container
    end

    gateway["API Gateway<br>[Spring Cloud Gateway :8080]<br>Enrutamiento, CORS, seguridad perimetral y rate limiting"]:::gateway

    subgraph Backend ["Microservicios Java / Spring Boot 3"]
        servicioExpedientes["Servicio de Expedientes<br>[Spring Boot :8081]<br>Máquina de estados, lógica de negocio y auditoría"]:::container
        servicioFormularios["Servicio de Formularios<br>[Spring Boot :8082]<br>Generación de Declaración Jurada en PDF y firma"]:::container
        servicioVerificacion["Servicio de Verificación y QR<br>[Spring Boot :8083]<br>Generación de QR (ZXing) y API pública"]:::container
        adaptadorIntegracion["Adaptador de Integración (Fase 2)<br>[Spring Boot :8084]<br>Stubs y conectores para SAT, Defensa Civil y Edificaciones"]:::container
    end

    subgraph Persistencia ["Capa de Datos"]
        postgres[("PostgreSQL 15<br>[Base de Datos Relacional]<br>Almacena expedientes, historial de estados e índices")]:::db
    end

    portalCiudadano -->|HTTPS / REST| gateway
    portalInterno -->|HTTPS / REST| gateway
    portalVerificacion -->|HTTPS / REST| gateway

    gateway -->|HTTP / REST| servicioExpedientes
    gateway -->|HTTP / REST| servicioFormularios
    gateway -->|HTTP / REST| servicioVerificacion
    gateway -->|HTTP / REST| adaptadorIntegracion

    servicioExpedientes -->|JPA / JDBC| postgres
    servicioExpedientes -->|REST| servicioFormularios
    servicioExpedientes -->|REST| adaptadorIntegracion
```

---

## 3. Nivel 3 — Diagrama de Componentes: Servicio de Expedientes

*(Basado en la arquitectura C4 Nivel 3 proporcionada en el documento)*

```mermaid
graph TD
    classDef comp fill:#63a0e2,stroke:#3b7abf,color:#fff;
    classDef db fill:#23527c,stroke:#173752,color:#fff;
    classDef ext fill:#777777,stroke:#555555,color:#fff;

    gateway["API Gateway"]:::ext

    subgraph ServicioExpedientes ["Servicio de Expedientes (Límite del contenedor)"]
        controller["ExpedienteController<br>[Spring REST Controller]<br>Expone endpoints para crear, consultar y actualizar"]:::comp
        service["ExpedienteService<br>[Spring Service]<br>Orquesta la lógica de negocio y las transiciones"]:::comp
        mapper["ExpedienteMapper<br>[MapStruct]<br>Convierte entre entidades y DTOs"]:::comp
        validator["EstadoExpedienteValidator<br>[Spring Component]<br>Valida que una transición de estado esté permitida"]:::comp
        calculadora["CalculadoraDeTasa<br>[Spring Component]<br>Calcula el monto según nivel de riesgo"]:::comp
        auditoria["AuditoriaService<br>[Spring Service]<br>Registra cada cambio con usuario y motivo"]:::comp
        expedienteRepo["ExpedienteRepository<br>[Spring Data JPA]<br>Persiste y consulta expedientes"]:::comp
        historialRepo["HistorialRepository<br>[Spring Data JPA]<br>Persiste el historial de transiciones"]:::comp
    end

    servicioFormularios["Servicio de Formularios"]:::ext
    adaptadorIntegracion["Adaptador de Integración"]:::ext
    postgres[("PostgreSQL 15")]:::db

    gateway -->|"REST / JSON"| controller
    controller --> service
    controller --> mapper

    service --> validator
    service --> calculadora
    service --> auditoria
    service --> expedienteRepo
    auditoria --> historialRepo

    service -.->|"Solicita generación de formatos (REST)"| servicioFormularios
    service -.->|"Registra dictamen / pago (REST)"| adaptadorIntegracion

    expedienteRepo -->|"SQL / JDBC"| postgres
    historialRepo -->|"SQL / JDBC"| postgres
```

---

## 4. Diagrama de Clases del Dominio de Expedientes

*(Basado en el diseño estructural del modelo C4)*

```mermaid
classDiagram
    class Expediente {
        - UUID id
        - String numeroTramite
        - UUID solicitanteId
        - String nombreTitular
        - String documentoIdentidad
        - String razonSocial
        - String nombreComercial
        - String giroNegocio
        - String direccionEstablecimiento
        - BigDecimal areaMetrosCuadrados
        - EstadoExpediente estado
        - NivelRiesgo nivelRiesgo
        - BigDecimal montoTasa
        - String voucherId
        - String licenciaQrCode
        - LocalDateTime fechaCreacion
        - LocalDateTime fechaLimite
        + cambiarEstado(EstadoExpediente nuevo) void
    }

    class HistorialEstado {
        - UUID id
        - UUID expedienteId
        - EstadoExpediente estadoAnterior
        - EstadoExpediente estadoNuevo
        - String usuario
        - String motivo
        - LocalDateTime fecha
    }

    class EstadoExpediente {
        <<enum>>
        FORMATOS_GENERADOS
        DOCUMENTOS_VALIDADOS
        EN_EVALUACION_FINAL
        APROBADO
        RECHAZADO
    }

    class NivelRiesgo {
        <<enum>>
        BAJO
        MEDIO
        ALTO
        MUY_ALTO
    }

    class EstadoExpedienteValidator {
        + validarTransicion(EstadoExpediente actual, EstadoExpediente nuevo) void
    }

    class ExpedienteRepository {
        <<interface>>
        + findById(UUID id) Optional~Expediente~
        + findByNumeroTramite(String n) Optional~Expediente~
        + save(Expediente e) Expediente
    }

    class ExpedienteService {
        + crearExpediente(CrearExpedienteDto dto) Expediente
        + registrarClasificacionRiesgo(UUID id, NivelRiesgo nivel) void
        + generarVoucher(UUID id) VoucherDto
        + registrarPago(UUID id, String voucherId) void
        + aprobar(UUID id) void
        + rechazar(UUID id, String motivo) void
    }

    ExpedienteService --> Expediente : usa
    ExpedienteService --> EstadoExpedienteValidator : valida con
    ExpedienteService --> ExpedienteRepository : persiste con
    Expediente --> NivelRiesgo : nivelRiesgo
    Expediente --> EstadoExpediente : estado
    Expediente "1" --> "*" HistorialEstado : tiene historial
    HistorialEstado --> EstadoExpediente : estadoAnterior
    HistorialEstado --> EstadoExpediente : estadoNuevo
    EstadoExpedienteValidator ..> EstadoExpediente : valida
```
