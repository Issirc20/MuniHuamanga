# 🏛️ Sistema de Gestión Digital del Trámite de Licencia de Funcionamiento
### Municipalidad Provincial de Huamanga (MuniHuamanga)
> **Arquitectura de Software Empresarial para la Digitalización Integral del Trámite de Licencia de Funcionamiento, con Generación Automática de Formatos Oficiales en PDF (Anexos 1, 3 y 4), Wizard Ciudadano Multipaso, Flujo de Subsanaciones LPAG, Firma Digital, Verificación de Licencias mediante Código QR, Seguridad Blindada OWASP ASVS y Empaquetado en Contenedores Docker, Desarrollada en Java 21 LTS y PostgreSQL 15, Modelada con C4 hasta el Nivel de Componentes.**

![Versión](https://img.shields.io/badge/Versi%C3%B3n-v0.9.0--rc1-blue?style=flat-square)
![Java 21](https://img.shields.io/badge/Java-21%20LTS-orange?logo=openjdk&style=flat-square)
![Spring Boot 3.3.4](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen?logo=springboot&style=flat-square)
![PostgreSQL 15](https://img.shields.io/badge/PostgreSQL-15-blue?logo=postgresql&style=flat-square)
![Docker](https://img.shields.io/badge/Docker-Multi--Stage%20(Temurin%2021)-2496ED?logo=docker&style=flat-square)
![Tests](https://img.shields.io/badge/Tests-94%20passing%20(100%25)-brightgreen?style=flat-square)
![Concurrencia](https://img.shields.io/badge/Concurrencia-%E2%89%A5150%20usuarios%20(<110ms)-success?style=flat-square)
![Auditoría](https://img.shields.io/badge/Auditor%C3%ADa-16%2F16%20Hallazgos%20Cerrados-brightgreen?style=flat-square)
![Normativa](https://img.shields.io/badge/Marco%20Legal-Ley%20N%C2%B0%2028976%20%2F%20Ley%2027444-red?style=flat-square)

---

## 📋 1. Resumen de la Solución

El presente proyecto implementa la arquitectura de software empresarial para digitalizar, transparentar y dar trazabilidad completa al trámite de **Licencia de Funcionamiento** en el ámbito municipal peruano, anclado al marco normativo de la **Ley N° 28976** (Ley Marco de Licencia de Funcionamiento), el **D.S. N° 002-2018-PCM** (Reglamento de Inspecciones Técnicas de Seguridad en Edificaciones - ITSE), la **Ley N° 27444** (Ley del Procedimiento Administrativo General - LPAG) y la **Ley N° 29733** (Ley de Protección de Datos Personales).

### 🎯 Estado Consolidado de Fases del Proyecto

| Fase | Estado | Descripción del Alcance y Entregables |
|:---:|:---:|---|
| **Fase 01** | ✅ Completada | Digitalización de base de datos (45 atributos normativos), capa de dominio, máquina de estados finitos, APIs REST y auditoría inmutable de transiciones. |
| **Fase 02** | ✅ Completada | Motor de generación de formatos oficiales en PDF con OpenPDF: Anexo 1 (2 pág.), Anexo 3 Matriz ITSE CENEPRED (2 pág.), Anexo 4 Condiciones de Seguridad (4 pág.), Voucher SAT con Code 128 y Certificado Oficial con QR. |
| **Fase 03** | ✅ Completada | Frontend Wizard Multipaso Ciudadano (3 pasos), descarga de documentos en memoria, Dashboard de Gestión Interna con KPIs y módulo de Mesa de Ayuda. |
| **Fase 04** | ✅ Completada | Autenticación JWT Stateless, Spring Security 6 RBAC (`ADMIN`, `EVALUADOR`, `CAJERO`), notificaciones asíncronas por correo electrónico (Thymeleaf + JavaMailSender) y gestión dinámica del tarifario TUPA (Ordenanza N° 018-2024-MPH). |
| **Auditoría & Estabilización** | ✅ Certificada (`v0.9.0-rc1`) | **Auditoría Integral de 5 Fases:** Cierre del 100% de los 16 hallazgos técnicos (H01-H16), blindaje Default-Deny, mitigación BOLA/IDOR con `SeguimientoCiudadanoDto`, soporte de estado `OBSERVADO` y subsanaciones LPAG en portales, suite de 94 tests automatizados, pruebas de concurrencia de 150 usuarios sobre BD real (<110ms) y Dockerfile multi-stage con manuales de despliegue y rollback. |

---

## 🏗️ 2. Estructura de Módulos del Repositorio

El proyecto está estructurado bajo el estilo arquitectónico de **Monolito Modular (Modular Monolith)** con diseño interno de **Clean Architecture (Arquitectura Limpia y Hexagonal / Ports & Adapters)**, organizado como un proyecto multi-módulo Maven (`muni-licencias-parent`):

```text
MuniHuamanga/
├── pom.xml                                  # POM Padre Multi-Módulo Maven (Java 21 / SB 3.3.4, OpenPDF, JJWT, ZXing)
├── Dockerfile                               # Empaquetado Multi-Stage (Maven 3.9 + Temurin 21 JRE no-root UID 10001, G1GC)
├── .dockerignore                            # Exclusiones optimizadas para construcción de imágenes Docker
├── docker-compose.yml                       # Orquestación integral: PostgreSQL 15, backend Spring Boot y pgAdmin 4
├── .env.example                             # Plantilla exhaustiva de variables de entorno (JWT_SECRET, BD, SMTP)
├── .gitignore                               # Exclusiones optimizadas para Java, Maven e IDEs
│
├── common-domain/                           # Módulo Bounded Context: Contratos y DTOs del dominio compartidos
│   └── src/main/java/.../common/
│       ├── enums/
│       │   ├── EstadoExpediente.java       # FORMATOS_GENERADOS → DOCUMENTOS_VALIDADOS ⇄ OBSERVADO → EN_EVALUACION_FINAL → APROBADO/RECHAZADO
│       │   ├── NivelRiesgo.java           # BAJO, MEDIO, ALTO, MUY_ALTO (Matriz CENEPRED)
│       │   ├── RolUsuario.java            # ROLE_ADMIN, ROLE_EVALUADOR, ROLE_CAJERO (RBAC institucional)
│       │   ├── TipoPersona.java           # NATURAL, JURIDICA
│       │   ├── TipoDocumento.java         # DNI, RUC, CARNET_EXTRANJERIA
│       │   ├── ModalidadTramite.java      # Sección I Anexo 1 (Indeterminada, Temporal, Cesionaria, etc.)
│       │   └── FuncionEdificacion.java    # Anexos 3 y 4 ITSE (Salud, Encuentro, Comercio, Almacén, etc.)
│       ├── dto/                            # 16 DTOs de contrato del dominio
│       │   ├── CrearExpedienteDto.java    # Solicitud Mesa de Partes (45 campos normativos, Anexo 1 + SUNARP + Ubicación)
│       │   ├── ExpedienteResponseDto.java # Payload interno completo (para administradores y evaluadores)
│       │   ├── SeguimientoCiudadanoDto.java # Payload público de consulta con datos personales ofuscados (Ley N° 29733 / H07)
│       │   ├── SubsanacionExpedienteDto.java# DTO formal de subsanación de observaciones ciudadanas (Ley 27444 / H11)
│       │   ├── Anexo4CondicionesDto.java  # Checklist de seguridad en edificación (23 campos booleanos de inspección)
│       │   ├── ClasificacionRiesgoDto.java# Calificación ITSE (Nivel + N° Informe Técnico + Observaciones)
│       │   ├── RegistroPagoDto.java       # Validación SAT (Voucher + N° Operación Bancaria + Monto)
│       │   ├── VoucherDto.java            # Orden de pago SAT con código Code 128
│       │   ├── VerificacionLicenciaDto.java# Consulta pública ciudadana vía QR (RNF-20)
│       │   ├── ResolucionExpedienteDto.java# Dictamen de aprobación o motivo fundamentado de observación/rechazo
│       │   ├── HistorialEstadoDto.java    # Línea de tiempo de auditoría inmutable
│       │   ├── LoginRequestDto.java       # Credenciales para autenticación JWT de funcionarios
│       │   ├── LoginResponseDto.java      # Token Bearer JWT, roles, username y tiempo de expiración
│       │   ├── UsuarioDto.java            # Perfil público y rol del funcionario autenticado
│       │   ├── TarifaTupaDto.java         # Catálogo oficial de tasas municipales vigentes
│       │   └── ActualizarTarifaDto.java   # DTO validado para actualización en caliente de montos TUPA
│       └── exception/
│           ├── TransicionInvalidaException.java
│           └── RecursoNoEncontradoException.java
│
├── servicio-expedientes/                    # Aplicación Principal: Monolito Modular con Clean Architecture (Puerto 8081)
│   ├── src/main/java/.../expedientes/
│   │   ├── ExpedientesApplication.java      # Punto de entrada autónomo Spring Boot 3.3.4
│   │   ├── config/
│   │   │   ├── AsyncConfig.java             # Pool de hilos @Async para notificaciones por correo
│   │   │   ├── SecurityConfig.java          # Spring Security 6: Default-Deny, filtros JWT y reglas estrictas RBAC
│   │   │   ├── DataInitializer.java          # Datos semilla de prueba (@Profile("!prod"), expedientes demo)
│   │   │   ├── UsuarioDataInitializer.java   # Usuarios semilla institucionales (@Profile("!prod"), BCrypt)
│   │   │   └── TarifaTupaDataInitializer.java# Catálogo inicial de tasas TUPA (Ordenanza N° 018-2024-MPH)
│   │   │
│   │   ├── controller/                      # Capa Web / Adaptadores de Entrega REST
│   │   │   ├── AuthController.java          # Autenticación JWT (/api/auth/login, /api/auth/me)
│   │   │   ├── ExpedienteController.java    # Ciclo de vida del trámite, /observar, /subsanar y consultas
│   │   │   ├── PublicLicenciasController.java # Endpoints públicos de verificación QR y consultas (RNF-20)
│   │   │   ├── TarifaTupaController.java    # CRUD y actualización en caliente del tarifario TUPA
│   │   │   ├── DocumentosFormulariosController.java # Generación y descarga blindada de formatos oficiales en PDF
│   │   │   └── GlobalExceptionHandler.java  # Manejo global de errores HTTP (RFC-7807, mensajes institucionalmente seguros)
│   │   │
│   │   ├── integracion/                     # Bounded Context: Puertos y Adaptadores Externos (Clean Architecture)
│   │   │   ├── port/                        # Puertos de salida (Interfaces del dominio)
│   │   │   │   ├── DefensaCivilPort.java    # Puerto de integración con Defensa Civil (ITSE D.S. 002-2018-PCM)
│   │   │   │   ├── EdificacionesPort.java   # Puerto de compatibilidad de zonificación PDU
│   │   │   │   ├── FiscalizacionPort.java   # Puerto de actas de control y fiscalización posterior
│   │   │   │   └── SatPort.java             # Puerto de recaudación y conciliación de vouchers SAT
│   │   │   ├── adapter/                     # Adaptadores de infraestructura
│   │   │   │   ├── DefensaCivilAdapterService.java
│   │   │   │   ├── EdificacionesAdapterService.java
│   │   │   │   ├── FiscalizacionAdapterService.java
│   │   │   │   └── SatAdapterService.java
│   │   │   └── controller/
│   │   │       └── AdaptadorController.java # Endpoints protegidos de simulación e interoperabilidad (/api/integraciones)
│   │   │
│   │   ├── service/                         # Capa de Aplicación / Casos de Uso
│   │   │   ├── ExpedienteService.java       # Máquina de estados (6 estados), observar(), subsanar() y persistencia
│   │   │   ├── CalculadoraDeTasa.java       # Motor de cálculo dinámico con desglose y fallback a YAML
│   │   │   ├── TarifaTupaService.java       # Gestión transaccional de tasas municipales TUPA
│   │   │   ├── AuditoriaService.java        # Log inmutable de transiciones con sellado de tiempo
│   │   │   ├── MetricasExpedienteService.java # Micrometer/Actuator: SLAs, alertas de vencimiento (15 días hábiles)
│   │   │   ├── NotificacionEmailService.java # Envío asíncrono de correos con plantillas Thymeleaf y PDF adjunto
│   │   │   ├── AuthService.java             # Lógica de login con BCrypt y emisión JWT
│   │   │   ├── DocumentoPdfService.java     # Coordinador de renderizado PDF en memoria (OpenPDF 2.0.3)
│   │   │   ├── LicenciaPdfGenerator.java    # Certificado oficial de Licencia (doble marco, escudo, QR ZXing, 6 notas)
│   │   │   ├── Anexo1PdfGenerator.java      # Generador Anexo 1 (2 páginas, Ley 28976)
│   │   │   ├── Anexo3PdfGenerator.java      # Generador Matriz ITSE (2 páginas, CENEPRED)
│   │   │   └── Anexo4PdfGenerator.java      # Generador Condiciones de Seguridad (4 páginas)
│   │   │
│   │   ├── security/                        # Capa de seguridad JWT sin estado (Stateless)
│   │   │   ├── JwtTokenProvider.java        # Firma HMAC-SHA256, validación de secreto robusto en prod y expiración 8h
│   │   │   ├── JwtAuthenticationFilter.java # Filtro OncePerRequest para interceptar Bearer token
│   │   │   ├── JwtAuthenticationEntryPoint.java # Manejo de error 401 Unauthorized en JSON
│   │   │   ├── JwtAccessDeniedHandler.java  # Manejo de error 403 Forbidden en JSON
│   │   │   ├── CustomUserDetails.java       # Wrapper UserDetails de Spring Security
│   │   │   └── CustomUserDetailsService.java # Carga de usuario desde base de datos
│   │   │
│   │   ├── validator/                       # Validadores de Reglas de Negocio
│   │   │   ├── EstadoExpedienteValidator.java # Precondiciones legales de aprobación y matriz de transiciones
│   │   │   └── MesaPartesValidator.java      # Validación estricta de solicitud y requisitos TUPA
│   │   ├── mapper/ExpedienteMapper.java      # MapStruct: cómputo 15 días hábiles, Anexo 4 y ofuscación DTO ciudadano
│   │   ├── model/                           # Entidades del Dominio (JPA)
│   │   │   ├── Expediente.java               # Entidad central con 45 atributos normativos + campos de observación/subsanación
│   │   │   ├── Anexo4Condiciones.java        # Objeto embebido JPA (@Embeddable) 23 campos
│   │   │   ├── HistorialEstado.java          # Auditoría inmutable @Entity
│   │   │   ├── Usuario.java                  # Entidad JPA de usuarios con roles RBAC y BCrypt
│   │   │   └── TarifaTupa.java               # Entidad JPA de tarifario municipal con constraints
│   │   └── repository/                      # Adaptadores de Persistencia (Spring Data JPA)
│   │       ├── ExpedienteRepository.java
│   │       ├── HistorialRepository.java
│   │       ├── UsuarioRepository.java
│   │       └── TarifaTupaRepository.java
│   │
│   ├── src/main/resources/
│   │   ├── application.yml                   # Configuración principal Spring, JPA, Mail, JWT, Actuator
│   │   ├── application-dev.yml               # Perfil de desarrollo local con H2 (modo PostgreSQL)
│   │   ├── application-local.yml             # Perfil local para PostgreSQL
│   │   ├── application-prod.yml              # Perfil blindado de producción (ddl-auto: validate, pool Hikari optimizado)
│   │   ├── templates/email/                  # Plantillas Thymeleaf para notificaciones asíncronas
│   │   │   ├── email-registro.html           # Notificación de registro con N° Expediente
│   │   │   ├── email-aprobacion.html         # Notificación de aprobación con Licencia PDF adjunta
│   │   │   └── email-rechazo.html            # Notificación motivada de observación o rechazo
│   │   └── static/                          # Frontend Institucional Completo
│   │       ├── index.html                    # Landing page con tarjetas de acceso institucional
│   │       ├── portal-ciudadano.html         # Wizard Ciudadano Multipaso: 3 pasos + Descarga PDFs + Modal Subsanación Online
│   │       ├── portal-interno.html           # Dashboard KPI + Bandeja Formatos PDF + Login Modal + Acciones Observar/Subsanar + CRUD TUPA
│   │       ├── verificar-licencia.html       # Portal Público de Verificación QR (RNF-20)
│   │       ├── css/styles.css                # Sistema de diseño integral (wizard, kpi-cards, modales, alertas, badge-observado)
│   │       ├── js/
│   │       │   ├── portal-ciudadano.js       # Wizard logic: navegación, validaciones, seguimiento ofuscado y subsanación LPAG
│   │       │   ├── portal-interno.js         # Sesión JWT, renderFormatosPdf(), modales de observación, subsanación y gestión TUPA
│   │       │   └── verificar-licencia.js     # Consulta QR pública en tiempo real
│   │       └── img/escudo-huamanga.png       # Escudo oficial de Huamanga
│   │
│   └── src/test/                            # 94 tests automatizados pasando al 100%
│       ├── java/.../concurrencia/
│       │   └── Concurrencia150UsuariosTest.java # Carga real con 150 hilos sobre HikariCP y motor JPA (H13)
│       └── java/.../security/
│           └── SeguridadEndToEndTest.java       # Pruebas E2E de seguridad: Default-Deny, RBAC, ofuscación BOLA y Actuator
│
├── docker/
│   └── postgres/init/01-init-databases.sql # DDL canónico: expedientes (con motivo/subsanación), historial, usuarios y tarifas
│
├── tests/                                   # Pruebas de Carga y Rendimiento Externas
│   └── load-test/
│       ├── k6-load-test.js                  # Suite k6 para simulación de ≥150 usuarios virtuales concurrentes
│       └── run-load-test-150.ps1            # Arnés multi-escenario PowerShell (150 usuarios concurrentes en ráfaga HTTP)
│
└── docs/                                    # Documentación Técnica Oficial del Proyecto
    ├── auditoria/
    │   ├── AUDITORIA_INTEGRAL.md            # Informe Maestro de Auditoría: 16/16 hallazgos resueltos y certificación v0.9.0-rc1
    │   └── REPORTE_QA.md                    # Reporte formal QA: Matriz consolidada de 94 tests, carga 150 usuarios < 110ms
    ├── despliegue/
    │   ├── GUIA_DESPLIEGUE.md               # Manual de despliegue con Docker Compose, .env y smoke tests
    │   ├── PLAN_ROLLBACK.md                 # Plan de contingencia, reversión de versiones y restauración de backups
    │   └── CHECKLIST_PRODUCCION.md          # Lista de verificación pre-pase a producción (20 controles OWASP/LPAG)
    ├── arquitectura/
    │   ├── diseno-arquitectonico-monolito-modular-clean-architecture.md # Documento Maestro de Diseño
    │   └── c4-model.md                      # Modelo C4 (Contexto, Contenedores, Componentes, Dominio)
    ├── auditoria-codigo-calidad-resolucion-101-problemas.md # Auditoría previa de 101 advertencias de calidad resueltas
    ├── entrega-fase-01.md                   # Entrega Fase 01: Dominio, BD y APIs
    ├── entrega-fase-02.md                   # Entrega Fase 02: Motor PDF (Anexos 1, 3, 4)
    ├── entrega-fase-03.md                   # Entrega Fase 03: Frontend Wizard + Descarga PDFs
    ├── entrega-fase-04-consolidado.md       # Entrega Fase 04 Completa: Consolidado (80/80 tests)
    ├── normativa-legal.md                   # Marco Legal: Ley 28976, Anexos 1, 3 y 4 ITSE
    ├── v0.1-inventario-matriz-campos.md     # Matriz de trazabilidad campo por campo (45 atributos)
    └── scrum/                               # Product Backlogs e Historias de Usuario (Sprints 0 a 4)
```

---

## ⚙️ 3. Requisitos del Entorno

| Herramienta | Versión Mínima | Finalidad |
|-------------|:--------------:|-----------|
| **JDK (Java Development Kit)** | **21 LTS** | Eclipse Adoptium Temurin 21 recomendado |
| **Apache Maven** | **3.9+** | Compilación y empaquetado multi-módulo POM |
| **Docker Engine & Compose** | **24.0+ / Compose v2** | Orquestación contenerizada de PostgreSQL 15 y Backend |
| **Git** | **2.40+** | Control de versiones y trazabilidad |
| **PowerShell / Bash** | 7.x / 5.x | Ejecución de scripts de prueba de carga y automatización |

---

## 🚀 4. Guía Rápida de Inicio y Ejecución

### Opción A: Despliegue con Docker Compose (Recomendado para Producción y Staging)

1. **Configurar el archivo de variables de entorno:**
   ```bash
   cp .env.example .env
   ```
2. **Construir y levantar todos los contenedores en segundo plano:**
   ```bash
   docker compose up -d --build
   ```
3. **Verificar que los servicios estén saludables:**
   ```bash
   docker compose ps
   # Muestra: muni_licencias_postgres (healthy), muni_licencias_backend (healthy), muni_licencias_pgadmin (running)
   ```

### Opción B: Ejecución Rápida en Desarrollo Local (Perfil `dev` con H2 en modo Postgres)

No requiere levantar Docker ni bases de datos externas:
```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

### Opción C: Ejecución Local con PostgreSQL de Docker

1. **Iniciar únicamente PostgreSQL:**
   ```bash
   docker compose up -d postgres
   ```
2. **Ejecutar el backend:**
   ```powershell
   mvn spring-boot:run "-Dspring-boot.run.profiles=local"
   ```

> El servicio se inicia en el puerto **`8081`**, exponiendo tanto la API REST, el motor documental PDF, los servicios de seguridad y los portales web frontales.

### Enlaces a los Portales Web del Sistema

| Portal Institucional | URL | Audiencia / Función |
|----------------------|-----|---------------------|
| **Landing Page Principal** | http://localhost:8081/ | Página institucional de bienvenida con accesos directos |
| **Portal Ciudadano (Wizard)** | http://localhost:8081/portal-ciudadano.html | Registro de trámites (3 pasos) + Descarga de Anexos + Subsanación de Observaciones |
| **Gestión Interna (Dashboard RBAC)** | http://localhost:8081/portal-interno.html | Bandeja de expedientes, evaluación, observar, subsanar, emisión y tarifario TUPA |
| **Verificación QR Pública (RNF-20)** | http://localhost:8081/verificar-licencia.html | Fiscalización ciudadana y validación de autenticidad de licencias emitidas |

---

## 🧪 5. Pruebas Automatizadas y de Concurrencia

### 5.1 Ejecución de la Suite Completa de Pruebas (Maven)
```powershell
mvn clean test
```
*Salida esperada:*
```text
[INFO] Results:
[INFO] Tests run: 94, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

### 5.2 Ejecución de Pruebas de Estrés HTTP en Vivo (150 Usuarios Concurrentes)
Con el servidor backend iniciado en el puerto 8081, ejecute el arnés de carga:
```powershell
pwsh -ExecutionPolicy Bypass -File .\tests\load-test\run-load-test-150.ps1
```
*Resultados consolidados de la prueba de carga en vivo:*
```text
==================================================================
                   RESUMEN CONSOLIDADO QA
==================================================================
Escenario                                  |   Reqs |  Avg(ms) |  P95(ms) | Throughput
--------------------------------------------------------------------------------
1. Verificacion Publica QR (RNF-20)        |    150 |   102.63 |      107 |    1415.09 req/s
2. Consulta Tramite Ciudadano (RNF-01/H07) |    150 |    66.78 |       69 |    2173.91 req/s
--------------------------------------------------------------------------------
[EVALUACION FINAL]: TODOS LOS ESCENARIOS CUMPLEN RNF-01 (>= 150 usuarios) Y RNF-02 (< 3.00s).
```

---

## 📖 6. Documentación Interactiva de APIs (OpenAPI / Swagger)

| Servicio | URL Swagger | Entorno |
|----------|-------------|---------|
| **Monolito Modular Unificado** | http://localhost:8081/swagger-ui.html | Habilitado en `dev` y `local` (Desactivado en `prod` por seguridad) |
| **Descriptor OpenAPI JSON** | http://localhost:8081/api-docs | Especificación OpenAPI 3.0 |

---

## 🔌 7. Catálogo Completo de Endpoints REST

### 7.1 Gestión de Expedientes y Mesa de Partes (`/api/expedientes/**`)

| Método | Endpoint | Descripción | Acceso / Rol |
|:---:|---|---|:---:|
| `POST` | `/api/expedientes` | Registrar nueva solicitud en Mesa de Partes Virtual (45 campos) | Público |
| `GET` | `/api/expedientes` | Consultar bandeja general de expedientes (filtros por estado/alerta) | `ROLE_ADMIN`, `ROLE_EVALUADOR`, `ROLE_CAJERO` |
| `GET` | `/api/expedientes/{id}` | Consultar detalle completo de un expediente por su UUID | `ROLE_ADMIN`, `ROLE_EVALUADOR` |
| `GET` | `/api/expedientes/tramite/{numero}` | **Consulta de seguimiento ciudadano:** Retorna datos personales ofuscados (`SeguimientoCiudadanoDto` Ley N° 29733) | **Público** |
| `GET` | `/api/expedientes/{id}/historial` | Consultar historial inmutable de auditoría del expediente | `ROLE_ADMIN`, `ROLE_EVALUADOR` |
| `POST` | `/api/expedientes/{id}/clasificacion-riesgo` | Registrar calificación de riesgo ITSE de Defensa Civil | `ROLE_EVALUADOR`, `ROLE_ADMIN` |
| `POST` | `/api/expedientes/{id}/voucher` | Generar orden de pago / voucher SAT | `ROLE_CAJERO`, `ROLE_ADMIN` |
| `POST` | `/api/expedientes/{id}/pago` | Registrar y validar constancia de pago SAT | `ROLE_CAJERO`, `ROLE_ADMIN` |
| `POST` | `/api/expedientes/{id}/observar` | **LPAG (Ley 27444):** Declarar expediente en estado `OBSERVADO` con fundamentación | `ROLE_EVALUADOR`, `ROLE_ADMIN` |
| `POST` | `/api/expedientes/{id}/subsanar` | **LPAG (Ley 27444):** Subsanar observaciones por administrado o funcionario | **Público** / Autenticado |
| `POST` | `/api/expedientes/{id}/aprobar` | Dictamen final aprobatorio y emisión oficial de Licencia con QR | `ROLE_EVALUADOR`, `ROLE_ADMIN` |
| `POST` | `/api/expedientes/{id}/rechazar` | Dictamen final denegatorio con resolución motivada | `ROLE_EVALUADOR`, `ROLE_ADMIN` |
| `GET` | `/api/expedientes/{id}/desglose-tasa` | Consultar desglose de conceptos tributarios TUPA | Público |

### 7.2 Descarga y Generación de Documentos Oficiales en PDF

| Método | Endpoint | Descripción | Regla de Negocio / Acceso |
|:---:|---|---|:---:|
| `GET` | `/api/expedientes/{id}/documentos/declaracion-jurada` | 📄 PDF Anexo 1 — Declaración Jurada oficial (Ley 28976) | Descarga inmediata pública / funcionario |
| `GET` | `/api/expedientes/{id}/documentos/anexo3-matriz-riesgo-itse` | 🛡️ PDF Anexo 3 — Reporte de Nivel de Riesgo ITSE | Descarga inmediata para firma Defensa Civil |
| `GET` | `/api/expedientes/{id}/documentos/anexo4-condiciones-seguridad` | 🔒 PDF Anexo 4 — Condiciones Técnicas de Seguridad (4 pág.) | Descarga inmediata para suscripción |
| `GET` | `/api/expedientes/{id}/documentos/voucher-sat` | 🧾 PDF Voucher SAT con código de barras Code 128 | Generación de orden de pago |
| `GET` | `/api/expedientes/{id}/documentos/licencia` | 📜 **Certificado Oficial de Licencia con QR y Sello:** Blindado bajo validación estricta (H08) | **Solo expedientes en estado `APROBADO`** |

### 7.3 Verificación Pública de Autenticidad QR (RNF-20)

| Método | Endpoint | Descripción | Acceso |
|:---:|---|---|:---:|
| `GET` | `/api/public/licencias/{codigoLicencia}` | Verificación ciudadana y fiscalización de licencias emitidas | Público (Solo lectura) |
| `GET` | `/api/public/licencias/{codigoLicencia}/qr` | Obtención de imagen PNG del código QR de la licencia | Público |
| `GET` | `/api/public/verificar/{codigoLicencia}` | Alias de compatibilidad para validación por código QR | Público |

### 7.4 Autenticación, Usuarios y Tarifario TUPA

| Método | Endpoint | Descripción | Rol Requerido |
|:---:|---|---|:---:|
| `POST` | `/api/auth/login` | Inicio de sesión de personal municipal (emisión JWT 8h) | Público |
| `GET` | `/api/auth/me` | Obtener datos de la sesión del usuario actual | Autenticado |
| `GET` | `/api/tupa/tarifas` | Catálogo de tasas TUPA vigentes (Ordenanza N° 018-2024-MPH) | Público |
| `PUT` | `/api/tupa/tarifas/{id}` | Actualización en caliente de tasas municipales | `ROLE_ADMIN` |

---

## 🔄 8. Máquina de Estados Finitos y Ciclo de Vida del Expediente

El ciclo de vida del trámite municipal respeta rigurosamente las garantías del debido procedimiento administrativo (Ley N° 27444 LPAG):

```text
       [ Portal Ciudadano: Wizard 3 Pasos ]
                        │
                        ▼ (POST /api/expedientes)
             [ FORMATOS_GENERADOS ]
                        │
                        ▼ (Defensa Civil registra calificación ITSE)
             [ DOCUMENTOS_VALIDADOS ] ◄─────────────────────────┐
                   │          │                                  │
      (Observación)│          │ (Pago validado en SAT)           │ (Subsanación
       Ley N° 27444│          ▼                                  │  conforme)
                   │    [ EN_EVALUACION_FINAL ]                  │
                   ▼          │                │                 │
             [ OBSERVADO ] ───┴────────────────┼─────────────────┘
                   │                           │
                   ▼ (Subsanación denegada)    ▼ (Evaluación conforme)
              [ RECHAZADO ]               [ APROBADO ]
        (Resolución Denegatoria)       (Licencia Oficial PDF + QR)
```

---

## 🛡️ 9. Seguridad, Blindaje OWASP y Protección de Datos

En el marco de la auditoría integral, se implementaron controles técnicos avanzados:
1. **Default-Deny:** Regla estricta `.anyRequest().authenticated()` en Spring Security, protegiendo todo nuevo endpoint desarrollado.
2. **Mitigación BOLA / IDOR (Ley N° 29733):** La consulta pública de trámites no devuelve la entidad interna completa, sino [`SeguimientoCiudadanoDto`](file:///d:/ArqSoftware/MuniHuamanga/common-domain/src/main/java/pe/gob/munihuamanga/licencias/common/dto/SeguimientoCiudadanoDto.java) con datos personales ofuscados (`M*** Q***`, `42***91`), eliminando correos y teléfonos.
3. **Control RBAC Estricto:** Separación de privilegios entre cajeros (`ROLE_CAJERO`), evaluadores (`ROLE_EVALUADOR`) y administradores (`ROLE_ADMIN`).
4. **Protección de Clickjacking:** Cabecera `X-Frame-Options: SAMEORIGIN` activa.
5. **Aislamiento Actuator:** Endpoint `/actuator/**` protegido bajo rol de administrador, exponiendo únicamente `/actuator/health` para sondas de infraestructura.
6. **Contenedor No-Root:** Ejecución de la JVM bajo el usuario sin privilegios `appuser` (UID 10001).

---

## 📚 10. Referencias de Documentación Técnica

- [**Informe Maestro de Auditoría Integral (`AUDITORIA_INTEGRAL.md`)**](docs/auditoria/AUDITORIA_INTEGRAL.md): Matriz de 16 hallazgos resueltos y certificación oficial `v0.9.0-rc1`.
- [**Reporte Formal de Aseguramiento de Calidad (`REPORTE_QA.md`)**](docs/auditoria/REPORTE_QA.md): Resultados de las 94 pruebas automatizadas y pruebas de carga con 150 usuarios.
- [**Manual Institucional de Despliegue (`GUIA_DESPLIEGUE.md`)**](docs/despliegue/GUIA_DESPLIEGUE.md): Procedimiento paso a paso para despliegue en producción con Docker Compose.
- [**Plan de Contingencia y Rollback (`PLAN_ROLLBACK.md`)**](docs/despliegue/PLAN_ROLLBACK.md): Procedimiento estandarizado de reversión y contingencias.
- [**Lista de Verificación Pre-Producción (`CHECKLIST_PRODUCCION.md`)**](docs/despliegue/CHECKLIST_PRODUCCION.md): Checklist pre-pase a producción con 20 ítems de control.
- [**Diseño Arquitectónico y Modelo C4 (`c4-model.md`)**](docs/arquitectura/c4-model.md): Diagramas de Contexto, Contenedores, Componentes y Dominio.

---

*Municipalidad Provincial de Huamanga — Gerencia de Licencias y Autorizaciones*  
*Equipo de Arquitectura de Software, Ciberseguridad & DevOps*  
*Marco Legal: Ley N° 28976 / TUO D.S. N° 046-2017-PCM / D.S. N° 002-2018-PCM / Ley N° 27444 LPAG / Ley N° 29733*
