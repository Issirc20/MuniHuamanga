# 🏛️ Sistema de Gestión Documentaria del Trámite de Licencia de Funcionamiento
### Municipalidad Provincial de Huamanga (MuniHuamanga)
> **Propuesta de Arquitectura de Software para la Digitalización del Trámite de Licencia de Funcionamiento a cargo de la Gerencia de Licencias, con Firma Digital y Verificación de Licencias mediante Código QR, Desarrollada en Java y PostgreSQL, Modelada con C4 hasta el Nivel de Contenedores y Diseñada para su Integración Futura con Defensa Civil, Edificaciones y el SAT.**

![Java 21](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot 3.3.4](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen?logo=springboot)
![PostgreSQL 15](https://img.shields.io/badge/PostgreSQL-15-blue?logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker)
![Architecture C4](https://img.shields.io/badge/Architecture-C4%20Model-indigo)
![Metodología](https://img.shields.io/badge/Metodolog%C3%ADa-Scrum-yellow)
![Normativa](https://img.shields.io/badge/Marco%20Legal-Ley%20N%C2%B0%2028976-red)

---

## 📋 1. Resumen de la Solución

El presente proyecto implementa la arquitectura de software empresarial para digitalizar y dar trazabilidad al trámite de **Licencia de Funcionamiento** en el ámbito municipal peruano, anclado al marco normativo de la **Ley N° 28976** y su **TUO aprobado por D.S. N° 046-2017-PCM**.

### 🎯 Enfoque por Fases y Viabilidad Realista
- **Fase 1 (Alcance actual):** Digitalización del flujo a cargo de la **Gerencia de Licencias**: recepción virtual, cálculo automático de tasas según riesgo ITSE, generación de órdenes de pago, validación y dictamen final, emisión de licencia firmada digitalmente y verificación ciudadana inmediata vía **código QR único**.
- **Fase 2 (Prevista en el diseño):** Integración directa en línea con el **SAT Huamanga** (pasarela y conciliación bancaria), **Defensa Civil** (clasificación de riesgo ITSE automatizada), **Gerencia de Edificaciones** (zonificación digital) y **Fiscalización**. Mientras tanto, el sistema cuenta con un **Adaptador de Integración** desacoplado para registrar estos dictámenes sin alterar los microservicios centrales.
- **Capacidad Objetivo:** $\ge 150$ usuarios concurrentes (dimensionamiento representativo de una municipalidad provincial).

---

## 🏗️ 2. Estructura de Módulos del Repositorio

El proyecto está organizado como un repositorio multi-módulo Maven (`muni-licencias-parent`):

```text
MuniHuamanga/
├── pom.xml                                  # POM Padre Multi-Módulo
├── docker-compose.yml                       # Orquestación de PostgreSQL 15 y pgAdmin
├── .gitignore                               # Exclusiones optimizadas para Java, Maven e IDEs
│
├── common-domain/                           # Modelos, DTOs compartidos, Enums y Excepciones
│   └── src/main/java/.../domain/
│       ├── enums/EstadoExpediente.java       # FORMATOS_GENERADOS, DOCUMENTOS_VALIDADOS, etc.
│       ├── enums/NivelRiesgo.java           # BAJO, MEDIO, ALTO, MUY_ALTO
│       └── exception/TransicionInvalidaException.java
│
├── servicio-expedientes/                    # Microservicio Central (Puerto 8081)
│   └── src/main/java/.../expedientes/
│       ├── controller/ExpedienteController.java
│       ├── service/ExpedienteService.java
│       ├── service/CalculadoraDeTasa.java   # Configurable según TUPA municipal (RNF-17)
│       ├── service/AuditoriaService.java    # Historial inmutable de estados
│       ├── validator/EstadoExpedienteValidator.java
│       ├── mapper/ExpedienteMapper.java     # MapStruct con cálculo de 15 días hábiles
│       └── repository/ExpedienteRepository.java
│
├── servicio-formularios/                    # Generación de Formatos PDF y Firma (Puerto 8082)
│   └── src/main/java/.../formularios/
│       ├── controller/FormulariosController.java
│       └── service/GeneradorDocumentoService.java  # OpenPDF Anexo 1 Declaración Jurada
│
├── servicio-verificacion-licencias/         # QR ZXing y API Pública (Puerto 8083)
│   └── src/main/java/.../verificacion/
│       ├── controller/VerificacionController.java  # Endpoint público RNF-20
│       └── service/QrGeneratorService.java         # Generador de QR PNG criptográfico (RNF-13)
│
├── adaptador-integracion/                   # Adaptador de Integración Fase 2 (Puerto 8084)
│   └── src/main/java/.../adaptador/
│       └── controller/AdaptadorController.java     # Stubs para SAT, Defensa Civil y Zonificación
│
├── api-gateway/                             # Spring Cloud Gateway (Puerto 8080)
│   └── src/main/resources/application.yml   # Enrutamiento perimetral, CORS y Rate Limiting
│
├── docker/
│   └── postgres/init/01-init-databases.sql  # Script DDL con tablas, índices y datos semilla
│
└── docs/
    ├── arquitectura/c4-model.md             # Diagramas C4 Contexto, Contenedores, Componentes
    ├── scrum/product-backlog.md             # Product Backlog completo con Historias de Usuario
    ├── scrum/sprint-0-backlog.md            # Entregables y definición de terminado de Sprint 0
    └── normativa-legal.md                   # Análisis de la Ley N° 28976 y D.S. 046-2017-PCM
```

---

## ⚙️ 3. Requisitos del Entorno

Para compilar y ejecutar este proyecto localmente, necesitas tener instalado:
- **JDK 21** (Eclipse Adoptium Temurin o similar LTS).
- **Apache Maven 3.9+**
- **Docker Desktop** (con soporte Docker Compose).
- **Git**

---

## 🚀 4. Guía Rápida de Inicio

### Paso 1: Iniciar la Base de Datos PostgreSQL
En la raíz del proyecto, ejecuta Docker Compose para levantar PostgreSQL 15 con el esquema DDL y datos semilla:
```bash
docker-compose up -d postgres
```
> **Credenciales:**  
> - Host: `localhost:5432`  
> - Base de datos: `muni_licencias_db`  
> - Usuario: `muni_user`  
> - Contraseña: `muni_pass123`  
> - (Opcional) Interfaz visual pgAdmin: http://localhost:5050 (`admin@munihuamanga.gob.pe` / `admin`)

### Paso 2: Compilar y Ejecutar Pruebas Automatizadas
Compila la suite multi-módulo completa y ejecuta los tests unitarios:
```powershell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
$env:PATH="$env:JAVA_HOME\bin;C:\tools\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin;$env:PATH"

mvn clean test
```

### Paso 3: Ejecutar los Microservicios
Puedes iniciar los servicios individualmente o mediante el Gateway:
```powershell
# En terminal 1 (Núcleo de Expedientes):
mvn spring-boot:run -pl servicio-expedientes

# En terminal 2 (Formularios PDF):
mvn spring-boot:run -pl servicio-formularios

# En terminal 3 (Verificación y QR):
mvn spring-boot:run -pl servicio-verificacion-licencias

# En terminal 4 (Adaptador Fase 2):
mvn spring-boot:run -pl adaptador-integracion

# En terminal 5 (API Gateway principal):
mvn spring-boot:run -pl api-gateway
```

---

## 📖 5. Documentación Interactiva de APIs (Swagger / OpenAPI)

Cada microservicio expone su documentación Swagger UI para pruebas inmediatas:
- **Servicio de Expedientes:** http://localhost:8081/swagger-ui.html
- **Servicio de Formularios:** http://localhost:8082/swagger-ui.html
- **Servicio de Verificación y QR:** http://localhost:8083/swagger-ui.html
- **Adaptador de Integración:** http://localhost:8084/swagger-ui.html
- **Entrada Perimetral (Gateway):** http://localhost:8080

---

## 🔄 6. Máquina de Estados del Trámite

```text
[ Ingreso de Solicitud ]
           │
           ▼
 [ FORMATOS_GENERADOS ]  ──(Defensa Civil registra clasificación ITSE)──► [ DOCUMENTOS_VALIDADOS ]
                                                                                   │
                                                                       (Pago validado en SAT)
                                                                                   │
                                                                                   ▼
                                                                        [ EN_EVALUACION_FINAL ]
                                                                          │               │
                                              (Favorable: Emisión con QR) │               │ (Desfavorable)
                                                                          ▼               ▼
                                                                     [ APROBADO ]   [ RECHAZADO ]
```

---

## 📦 7. Cómo Subir este Proyecto a tu Repositorio de GitHub

El repositorio Git ya se encuentra inicializado localmente. Para publicarlo en tu cuenta de GitHub, sigue estos pasos:

1. Crea un repositorio vacío en tu cuenta de GitHub (ejemplo: `muni-huamanga-licencias`).
2. En tu terminal dentro de la carpeta `d:\ArqSoftware\MuniHuamanga`, ejecuta:

```powershell
# 1. Verificar estado local
git status

# 2. Agregar todos los archivos estructurados
git add .

# 3. Realizar el primer commit de Sprint 0
git commit -m "feat(sprint-0): inicializar arquitectura base, modulos C4, docker y documentacion"

# 4. Asignar la rama principal
git branch -M main

# 5. Vincular a tu repositorio remoto de GitHub:
git remote add origin https://github.com/Issirc20/MuniHuamanga.git

# 6. Subir los cambios a GitHub
git push -u origin main
```

---

## 👥 8. Hoja de Ruta de Sprints Ágiles (Scrum)

- **✅ Sprint 0:** Setup, Arquitectura base, Multi-módulo, Docker, C4, Repositorios y Tests.
- **🚀 Sprint 1:** Mesa de Partes Virtual, Flujo de Expedientes, Máquina de Estados y Auditoría Inmutable.
- **⏳ Sprint 2:** Generación de Formatos PDF oficiales (Anexo 1), Cálculo TUPA y Vouchers SAT.
- **⏳ Sprint 3:** Evaluación Técnica, Emisión de Licencia con QR Criptográfico y Portal Ciudadano de Verificación.
- **⏳ Sprint 4:** Adaptador de Integración Fase 2 (SAT / Defensa Civil / Edificaciones), Métricas y Pruebas de Carga (150 usuarios concurrentes).

---
*Municipalidad Provincial de Huamanga — Gerencia de Licencias y Autorizaciones*
