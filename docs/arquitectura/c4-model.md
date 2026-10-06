# Modelo de Arquitectura C4 — Sistema de Gestión de Licencias de Funcionamiento
### Municipalidad Provincial de Huamanga (MuniHuamanga)
> **Marco Normativo:** Ley N° 28976 / TUO D.S. N° 046-2017-PCM / D.S. N° 002-2018-PCM / D.S. N° 163-2020-PCM  
> **Versión de Arquitectura:** Fase 04 Completada — Seguridad JWT RBAC, Notificaciones Email, Formato Oficial y TUPA Dinámico  
> **Modelo:** C4 Model (Contexto, Contenedores, Componentes y Diagrama de Clases de Dominio)

---

## 1. Nivel 1 — Diagrama de Contexto del Sistema

Describe la interacción del sistema con los usuarios clave de la municipalidad y las entidades e instancias externas del ecosistema municipal:

```mermaid
graph TD
    classDef person fill:#08427b,stroke:#073b6f,color:#fff;
    classDef system fill:#1168bd,stroke:#0b4884,color:#fff;
    classDef extSystem fill:#555555,stroke:#333333,color:#fff;

    solicitante["Ciudadano / Administrado<br>(Persona)<br>Registra solicitud en Wizard 3 pasos,<br>descarga PDFs y consulta seguimiento"]:::person
    inspectorItse["Inspector Defensa Civil<br>(Persona)<br>Aplica Matriz de Riesgo ITSE Anexo 3,<br>emite informes tecnicos via Portal Interno"]:::person
    cajeroSat["Cajero / Operador SAT<br>(Persona)<br>Liquida y valida el pago de tasas TUPA"]:::person
    funcionarioGL["Funcionario Gerencia Licencias<br>(Persona)<br>Evalua dictamenes, aprueba y emite<br>licencias definitivas con QR"]:::person
    fiscalizador["Fiscalizador / Inspectores<br>(Persona)<br>Escanea y verifica autenticidad in situ<br>mediante Codigo QR"]:::person

    sistema["Sistema de Licencias MuniHuamanga<br>(Software System)<br>Wizard ciudadano 3 pasos, generacion PDF Anexos 1/3/4,<br>maquina de estados, calculo tasas TUPA dinamico,<br>emision de licencias oficiales con QR, notificaciones<br>email asincronas, JWT RBAC y Dashboard KPI"]:::system

    sat["SAT Huamanga<br>(Sistema Externo)<br>Recaudacion de tasas,<br>vouchers y conciliacion bancaria"]:::extSystem
    defensaCivil["Subgerencia de Defensa Civil<br>(Sistema / Instancia Externa)<br>Inspecciones ITSE/ECSE"]:::extSystem
    desarrolloUrbano["Gerencia de Desarrollo Urbano<br>(Sistema / Instancia Externa)<br>Zonificacion y compatibilidad de uso del suelo"]:::extSystem
    sunarp["SUNARP / Plataforma PIDE<br>(Sistema Externo)<br>Validacion de personerias juridicas y poderes"]:::extSystem
    reniecPki["Plataforma de Firma Digital / RENIEC<br>(Sistema Externo)<br>Firma digital con validez legal y sellado de tiempo"]:::extSystem

    solicitante -->|"1. Completa wizard 3 pasos y descarga Anexos 1/3/4"| sistema
    inspectorItse -->|"2. Registra dictamen ITSE via Portal Interno"| sistema
    cajeroSat -->|"3. Valida pago y registra voucher SAT"| sistema
    funcionarioGL -->|"4. Aprueba y emite licencia oficial con QR"| sistema
    fiscalizador -->|"5. Verifica autenticidad y vigencia via QR"| sistema

    sistema -->|"Consulta y concilia recaudacion"| sat
    sistema -->|"Deriva expedientes para evaluacion de riesgo"| defensaCivil
    sistema -->|"Valida zonificacion y compatibilidad de uso"| desarrolloUrbano
    sistema -->|"Consulta vigencia de poderes y RUC"| sunarp
    sistema -->|"Aplica sellado digital y firma de documentos"| reniecPki
```

---

## 2. Nivel 2 — Diagrama de Contenedores (Monolito Modular)

Ilustra la arquitectura consolidada de **Monolito Modular con Clean Architecture**, donde un único contenedor de aplicación Spring Boot atiende a los portales web cliente, gestiona el ciclo de vida del expediente, renderiza documentos PDF oficiales, expone puertos de integración desacoplados y persiste datos en PostgreSQL:

```mermaid
graph TD
    classDef container fill:#438dd5,stroke:#2e6295,color:#fff;
    classDef db fill:#23527c,stroke:#173752,color:#fff;
    classDef frontend fill:#63a0e2,stroke:#3b7abf,color:#fff;
    classDef ext fill:#555555,stroke:#333333,color:#fff;

    subgraph CapaPresentacion [Capa de Presentacion - Portales Web Institucionales]
        portalCiudadano["Portal Ciudadano<br>(HTML5 / CSS3 / Vanilla JS)<br>Wizard 3 pasos: Solicitante -> Local -> Confirmacion<br>Descarga inmediata de Anexos 1, 3 y 4<br>Mesa de Ayuda para tramitacion presencial"]:::frontend
        portalInterno["Sistema Interno de Gestion<br>(HTML5 / CSS3 / Vanilla JS)<br>Dashboard KPI con control RBAC JWT<br>Bandeja con columna Formatos PDF y Modales<br>Mantenimiento en caliente del Tarifario TUPA"]:::frontend
        portalVerificacion["Portal Publico de Verificacion<br>(HTML5 / CSS3 / Vanilla JS)<br>Consulta publica de autenticidad via QR RNF-20"]:::frontend
        landing["Landing Page Institucional<br>(HTML5 / CSS3)<br>Acceso centralizado al ecosistema municipal"]:::frontend
    end

    subgraph Backend [Contenedor Principal: Monolito Modular - Java 21 / Spring Boot 3.3.4 :8081]
        monolito["MuniHuamanga Monolito Modular<br>(servicio-expedientes :8081)<br>• Bounded Context: Expedientes (Workflow y 5 estados)<br>• Bounded Context: Documentos Oficiales (Anexo 1, 3, 4, Licencia QR)<br>• Bounded Context: TUPA Dinamico (Calculo automatizado)<br>• Bounded Context: Verificacion Publica QR (ZXing)<br>• Bounded Context: Integracion (Ports & Adapters SAT, DC)<br>• Bounded Context: Seguridad (Spring Security 6 + JJWT)"]:::container
    end

    subgraph Persistencia [Capa de Datos Relacional]
        postgres[("PostgreSQL 15 / H2<br>muni_licencias_db :5432<br>Expedientes 45 atributos, Usuarios RBAC,<br>Tarifario TUPA dinamico e historial auditoria")]:::db
    end

    subgraph Externos [Servicios y Entidades Externas]
        smtpServer["Servidor SMTP / JavaMail<br>(Notificaciones Electronicas Async)<br>Alertas de Registro, Aprobacion con PDF y Rechazo"]:::ext
        satExt["SAT Huamanga<br>(Recaudacion Tributaria)"]:::ext
        dcExt["Subgerencia de Defensa Civil<br>(Inspeccion ITSE Presencial)"]:::ext
    end

    landing -->|"Navegacion"| portalCiudadano
    landing -->|"Navegacion"| portalInterno
    landing -->|"Navegacion"| portalVerificacion

    portalCiudadano -->|"REST JSON / Descarga directa PDFs"| monolito
    portalInterno -->|"REST JSON con Bearer JWT / Formatos PDF"| monolito
    portalVerificacion -->|"GET /api/public/licencias/{codigo}"| monolito

    monolito -->|"Spring Data JPA / Transaccionalidad ACID"| postgres
    monolito -->|"SMTP / JavaMail asincrono"| smtpServer
    monolito -.->|"SatPort / Conciliacion de vouchers"| satExt
    monolito -.->|"DefensaCivilPort / Dictamenes ITSE"| dcExt
```

---

## 3. Nivel 3 — Diagrama de Componentes: Monolito Modular (:8081)

Detalla la arquitectura interna basada en **Clean Architecture** (Arquitectura Limpia y Hexagonal / Ports & Adapters) dentro del contenedor unificado:

```mermaid
graph TD
    classDef comp fill:#63a0e2,stroke:#3b7abf,color:#fff;
    classDef pdf fill:#d4a017,stroke:#a07812,color:#fff;
    classDef sec fill:#d9534f,stroke:#b52b27,color:#fff;
    classDef port fill:#2e7d32,stroke:#1b5e20,color:#fff;
    classDef db fill:#23527c,stroke:#173752,color:#fff;
    classDef frontend fill:#2d9e6b,stroke:#1f7048,color:#fff;

    subgraph ClientesWeb [Clientes Web Frontales]
        wizardCiudadano["Portal Ciudadano (Wizard 3 pasos + Mesa de Ayuda)"]:::frontend
        dashboardInterno["Portal Interno (Dashboard KPI + RBAC)"]:::frontend
    end

    subgraph MonolitoModular [Monolito Modular - servicio-expedientes]
        subgraph CapaPresentacionDelivery [Capa de Presentacion / Web Delivery]
            expController["ExpedienteController<br>(REST Controller)"]:::comp
            authController["AuthController<br>(POST /api/auth/login)"]:::sec
            tupaController["TarifaTupaController<br>(REST Controller)"]:::comp
            publicController["PublicLicenciasController<br>(REST Verificacion QR)"]:::comp
            docController["DocumentosFormulariosController<br>(REST /api/formularios)"]:::comp
            adaptController["AdaptadorController<br>(REST /api/integraciones)"]:::comp
        end

        subgraph CapaSeguridad [Capa de Seguridad Spring Security 6]
            secConfig["SecurityConfig (Stateless FilterChain)"]:::sec
            jwtFilter["JwtAuthenticationFilter"]:::sec
            jwtProvider["JwtTokenProvider (JJWT 0.12.6)"]:::sec
        end

        subgraph CapaAplicacion [Capa de Aplicacion / Casos de Uso]
            expService["ExpedienteService (Maquina de 5 Estados)"]:::comp
            tupaService["TarifaTupaService (Tarifario TUPA)"]:::comp
            calculadora["CalculadoraDeTasa (Calculo dinamico)"]:::comp
            auditoria["AuditoriaService (Trazabilidad inmutable)"]:::comp
            emailService["NotificacionEmailService (@Async)"]:::comp
            validator["EstadoExpedienteValidator / MesaPartesValidator"]:::comp
            pdfService["DocumentoPdfService (Coordinador PDF)"]:::pdf
        end

        subgraph CapaPuertosAdaptadores [Capa de Integracion / Ports & Adapters]
            satPort["SatPort (Interface)"]:::port
            dcPort["DefensaCivilPort (Interface)"]:::port
            edifPort["EdificacionesPort (Interface)"]:::port
            fiscPort["FiscalizacionPort (Interface)"]:::port

            satAdapter["SatAdapterService"]:::comp
            dcAdapter["DefensaCivilAdapterService"]:::comp
            edifAdapter["EdificacionesAdapterService"]:::comp
            fiscAdapter["FiscalizacionAdapterService"]:::comp
        end

        subgraph MotorPDF [Motor de Renderizado PDF OpenPDF / ZXing]
            a1Gen["Anexo1PdfGenerator (2 paginas)"]:::pdf
            a3Gen["Anexo3PdfGenerator (2 paginas Matriz ITSE)"]:::pdf
            a4Gen["Anexo4PdfGenerator (4 paginas Condiciones Seguridad)"]:::pdf
            licGen["LicenciaPdfGenerator (Certificado con QR ZXing)"]:::pdf
        end

        subgraph CapaPersistencia [Capa de Persistencia JPA]
            expRepo["ExpedienteRepository"]:::comp
            tupaRepo["TarifaTupaRepository"]:::comp
            histRepo["HistorialRepository"]:::comp
            userRepo["UsuarioRepository"]:::comp
        end
    end

    postgres[("PostgreSQL 15 / H2")]:::db

    wizardCiudadano --> expController
    wizardCiudadano --> docController
    dashboardInterno --> authController
    dashboardInterno --> expController
    dashboardInterno --> tupaController

    jwtFilter --> expController
    jwtFilter --> tupaController

    expController --> expService
    expController --> pdfService
    docController --> pdfService
    tupaController --> tupaService
    adaptController --> satPort
    adaptController --> dcPort
    adaptController --> edifPort
    adaptController --> fiscPort

    expService --> validator
    expService --> calculadora
    expService --> auditoria
    expService --> emailService
    expService --> expRepo
    tupaService --> tupaRepo
    tupaService --> calculadora

    pdfService --> a1Gen
    pdfService --> a3Gen
    pdfService --> a4Gen
    pdfService --> licGen

    satAdapter --> satPort
    dcAdapter --> dcPort
    edifAdapter --> edifPort
    fiscAdapter --> fiscPort

    expRepo --> postgres
    tupaRepo --> postgres
    histRepo --> postgres
    userRepo --> postgres
```

---

---

## 5. Diagrama de Clases del Dominio — Actualizado Fase 04

Estructura completa de entidades JPA, objetos embebidos (`@Embeddable`), enums normativos, DTOs y servicios:

```mermaid
classDiagram
    class Expediente {
        - UUID id
        - String numeroTramite
        - UUID solicitanteId
        - ModalidadTramite modalidadTramite
        - Integer plazoTemporalMeses
        - String tipoAnuncio
        - String numeroLicenciaPrincipal
        - TipoPersona tipoPersona
        - TipoDocumento tipoDocumento
        - String nombreTitular
        - String documentoIdentidad
        - String razonSocial
        - String correoElectronico
        - String telefono
        - Boolean autorizaNotificacion
        - String partidaSunarp
        - String asientoSunarp
        - String dniRepresentante
        - String nombreRepresentante
        - String poderSunarp
        - String nombreComercial
        - String ciiuCodigo
        - String giroNegocio
        - String actividadDetallada
        - String zonificacion
        - FuncionEdificacion funcionEdificacion
        - String direccionEstablecimiento
        - String tipoVia
        - String nombreVia
        - String numeroVivienda
        - String interior
        - String manzana
        - String lote
        - String urbanizacion
        - String distrito
        - String provincia
        - String departamento
        - String referenciaUbicacion
        - BigDecimal areaMetrosCuadrados
        - BigDecimal areaTerreno
        - BigDecimal areaTechadaTotal
        - BigDecimal areaOcupadaTotal
        - Integer aforoPersonas
        - Integer numeroPisos
        - Integer antiguedadEdificacion
        - Integer antiguedadGiro
        - Boolean requiereAutorizacionSectorial
        - String sectorEntidad
        - String sectorDenominacion
        - String sectorFecha
        - String sectorNumero
        - EstadoExpediente estado
        - NivelRiesgo nivelRiesgo
        - BigDecimal montoTasa
        - String voucherId
        - String licenciaQrCode
        - String numeroLicencia
        - String categoriaEstablecimiento
        - String horaInicio
        - String horaFin
        - LocalDateTime fechaAprobacion
        - String numeroInformeItse
        - LocalDateTime fechaInformeItse
        - String numeroOperacionSat
        - LocalDateTime fechaPagoSat
        - Anexo4Condiciones anexo4Condiciones
        - LocalDateTime fechaCreacion
        - LocalDateTime fechaLimite
        + getTipoItse() String
        + cambiarEstado(EstadoExpediente) void
    }

    class TarifaTupa {
        - Long id
        - NivelRiesgo nivelRiesgo
        - BigDecimal monto
        - String descripcion
        - LocalDateTime fechaActualizacion
    }

    class Usuario {
        - Long id
        - String username
        - String password
        - String nombreCompleto
        - RolUsuario rol
        - Boolean activo
    }

    class Anexo4Condiciones {
        - BigDecimal areaTerreno
        - BigDecimal areaTechadaTotal
        - BigDecimal areaOcupadaTotal
        - Integer aforoPersonas
        - Integer antiguedadEdificacionAnios
        - Integer antiguedadGiroAnios
        - Boolean noEnProcesoConstruccion
        - Boolean cuentaServiciosBasicos
        - Boolean cuentaMobiliarioBasico
        - Boolean tieneEquiposInstalados
        - Boolean mediosEvacuacionLibres
        - Boolean senalizacionSeguridad
        - Boolean lucesEmergenciaOperativas
        - Boolean tableroElectricoProtegido
        - Boolean interruptoresDiferenciales
        - Boolean pozoTierraVigente
        - Boolean extintoresOperativos
        - Boolean estructurasSinRiesgoColapso
        - Boolean cablesProtegidosTubosPvc
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

    class RolUsuario {
        <<enum>>
        ROLE_ADMIN
        ROLE_EVALUADOR
        ROLE_CAJERO
    }

    class TipoPersona {
        <<enum>>
        NATURAL
        JURIDICA
    }

    class TipoDocumento {
        <<enum>>
        DNI
        RUC
        CARNET_EXTRANJERIA
    }

    class ModalidadTramite {
        <<enum>>
        LICENCIA_INDETERMINADA
        LICENCIA_TEMPORAL
        LICENCIA_CON_ANUNCIO
        LICENCIA_CESIONARIO
        LICENCIA_MERCADOS_GALERIAS
        CAMBIO_DENOMINACION
        TRANSFERENCIA_LICENCIA
        CESE_ACTIVIDADES
        OTROS
    }

    class FuncionEdificacion {
        <<enum>>
        SALUD
        ENCUENTRO
        HOSPEDAJE
        EDUCACION
        INDUSTRIAL
        OFICINAS_ADMINISTRATIVAS
        COMERCIO
        ALMACEN
    }

    class ExpedienteService {
        + crearExpediente(CrearExpedienteDto) Expediente
        + registrarClasificacionRiesgo(UUID, NivelRiesgo, String, String) void
        + generarVoucher(UUID) VoucherDto
        + registrarPago(UUID, String, String) void
        + aprobar(UUID) void
        + rechazar(UUID, String) void
        + verificarLicencia(String) VerificacionLicenciaDto
        + obtenerPorId(UUID) Expediente
        + obtenerPorNumeroTramite(String) Expediente
    }

    class CalculadoraDeTasa {
        + calcularTasa(NivelRiesgo) BigDecimal
        + calcularDesglose(NivelRiesgo) Map
    }

    class DocumentoPdfService {
        + generarAnexo1DeclaracionJurada(ExpedienteResponseDto) byte[]
        + generarAnexo3MatrizRiesgoItse(ExpedienteResponseDto) byte[]
        + generarAnexo4CondicionesSeguridad(ExpedienteResponseDto) byte[]
        + generarDeclaracionJurada(ExpedienteResponseDto) byte[]
        + generarVoucherSatPdf(VoucherDto) byte[]
        + generarLicenciaPdf(ExpedienteResponseDto) byte[]
        + generarImagenQr(String, int, int) byte[]
    }

    ExpedienteService --> Expediente : orquesta
    ExpedienteService --> CalculadoraDeTasa : calcula tasa TUPA
    Expediente *-- Anexo4Condiciones : contiene embebido
    Expediente --> EstadoExpediente : estado actual
    Expediente --> NivelRiesgo : nivelRiesgo
    Expediente --> TipoPersona : tipoPersona
    Expediente --> TipoDocumento : tipoDocumento
    Expediente --> ModalidadTramite : modalidadTramite
    Expediente --> FuncionEdificacion : funcionEdificacion
    Expediente --> HistorialEstado : tiene historial
    HistorialEstado --> EstadoExpediente : estadoAnterior
    HistorialEstado --> EstadoExpediente : estadoNuevo
    Usuario --> RolUsuario : asignado
    TarifaTupa --> NivelRiesgo : nivelRiesgo
    CalculadoraDeTasa --> TarifaTupa : consulta tasa en BD
    DocumentoPdfService ..> Expediente : lee para generar PDF
```

---

## 6. Máquina de Estados — Normativa, Actores y Documentos

| Estado C4 | Actor Responsable | Documentos Generados | Precondición Legal para Transicionar |
|-----------|-------------------|----------------------|--------------------------------------|
| **`FORMATOS_GENERADOS`** | Administrado -> Mesa de Partes | Anexo 1 (DJ) + Anexo 4 (Cond. Seguridad) + Anexo 3 (Riesgo) | Validación DNI/RUC y presunción de veracidad (Ley N° 27444 Art. 42°) |
| **`DOCUMENTOS_VALIDADOS`** | Subgerencia de Defensa Civil | Dictamen ITSE + Informe Técnico Nro. | Clasificación de riesgo asignada; tasa TUPA calculada automáticamente |
| **`EN_EVALUACION_FINAL`** | SAT Huamanga / Tesorería | Voucher SAT con Code 128 | Constancia de pago registrada con número de operación bancaria |
| **`APROBADO`** | Gerencia de Licencias | Certificado Oficial con QR + Firma Digital | Dictámenes ITSE y zonificación favorables; pago verificado |
| **`RECHAZADO`** | Gerencia de Licencias | Resolución de Denegatoria | Dictamen desfavorable o zonificación no compatible |

---

## 7. Diagrama de Flujo del Wizard Ciudadano (Fase 3 y 4)

```mermaid
graph TD
    A["Ciudadano accede a<br>portal-ciudadano.html"] --> B

    subgraph Wizard [Wizard 3 Pasos - Mesa de Partes Virtual]
        B["PASO 1: Datos del Solicitante<br>Nombre Titular<br>DNI/RUC validado<br>Telefono celular<br>Correo electronico"] -->|"irPaso(2) - validarPaso(1)"| C
        C["PASO 2: Datos del Establecimiento<br>Nombre comercial y Giro CIIU<br>Direccion completa<br>Area m2 determina riesgo ITSE<br>N de pisos"] -->|"irPaso(3) - validarPaso(2)"| D
        D["PASO 3: Confirmacion<br>Resumen de 10 campos<br>Declaracion Jurada checkbox<br>Boton Enviar con spinner"] -->|"registrarSolicitud() - POST /api/expedientes"| E
    end

    E["Banner de Exito<br>EXP-2026-XXXXX generado"] --> F
    E --> G
    E --> H

    F["Descargar Anexo 1<br>GET /api/expedientes/:id/documentos/declaracion-jurada"]
    G["Descargar Anexo 3<br>POST /api/formularios/anexo3-matriz-riesgo-itse"]
    H["Descargar Anexo 4<br>POST /api/formularios/anexo4-condiciones-seguridad"]

    E --> I["Ir a Seguimiento<br>Consultar estado por N tramite"]
```

---

## 8. Stack Tecnológico Completo

| Capa | Tecnología | Versión | Justificación |
|------|-----------|---------|---------------|
| **Lenguaje** | Java | 21 LTS | Records, sealed classes, virtual threads |
| **Framework Backend** | Spring Boot | 3.3.4 | Estabilidad enterprise, soporte largo plazo |
| **Seguridad & RBAC** | Spring Security + JJWT | 6.3 / 0.12.6 | Autenticación sin estado, RBAC por endpoint |
| **Email Asíncrono** | Spring JavaMail + Thymeleaf | 3.3.4 | Plantillas HTML dinámicas, hilo pool desacoplado |
| **ORM / Persistencia** | Spring Data JPA + Hibernate | 6.5 | Portabilidad, validaciones de integridad |
| **Base de Datos** | PostgreSQL / H2 | 15 / In-Memory | Confiabilidad, soporte JSON, tests portables |
| **Generación PDF** | OpenPDF (LibrePDF fork) | 2.0.3 | Open source, formatos oficiales idénticos |
| **Códigos QR** | ZXing | 3.5.3 | Estándar de facto para QR y Code 128 |
| **Mapeo DTO-Entity** | MapStruct | 1.5.5 | Generación en tiempo de compilación |
| **Reducción boilerplate** | Lombok | Latest SB | Builders, getters, @Slf4j |
| **API Docs** | SpringDoc OpenAPI (Swagger UI) | 2.6.0 | Integración nativa Spring Boot 3 |
| **Gateway** | Spring Cloud Gateway | 2023.0.3 | Enrutamiento reactivo, filtros |
| **Frontend** | HTML5 + CSS3 + Vanilla JS | — | Sin dependencias: máxima portabilidad |
| **Contenedores** | Docker + Docker Compose | v2 | Reproducibilidad del entorno |
| **CI/CD** | GitHub (repositorio) | — | Control de versiones + auditoría de ramas |

---

*Versión del Documento: Fase 04 — Completada el 2026-10-02*  
*Municipalidad Provincial de Huamanga — Gerencia de Licencias y Autorizaciones*
