# Auditoría Inicial del Repositorio — MuniHuamanga V2

Municipalidad Provincial de Huamanga — Gerencia de Licencias  
**Fecha de Auditoría:** 13 de octubre de 2026  
**Rama:** `develop/munihuamanga-v2`  
**Auditor:** Arquitecto de Software Senior & Ingeniero Full Stack  
**Documentos Rectores:** [ERS-MH-V2-001 v1.0] y [DAS-MH-V2-001 v1.0]

---

## 1. Resumen Ejecutivo

Se ha realizado una inspección minuciosa del repositorio existente en `d:\ArqSoftware\MuniHuamanga` sobre la rama `develop/munihuamanga-v2`. El proyecto cuenta con una base sólida de desarrollo construida como un **Monolito Modular** estructurado en dos submódulos Maven: `common-domain` y `servicio-expedientes`.

La compilación y la suite de pruebas automatizadas se ejecutaron satisfactoriamente:
- **Estado de Compilación:** `BUILD SUCCESS` con Apache Maven 3.9.16 y Eclipse Adoptium OpenJDK 21.0.12.1.
- **Pruebas Automatizadas:** **104 pruebas ejecutadas**, **0 fallos**, **0 errores**, **0 omitidas** (tiempo total: 14.3 s).
- **Módulos con alta madurez:** Motor de generación nativa de PDFs oficiales en OpenPDF (Anexo 1, Anexo 3, Anexo 4, Certificado Oficial de Licencia, Orden de Pago SAT) y generación de códigos QR (ZXing).
- **Frontends web operacionales:** Portales web estáticos responsivos en HTML5, CSS3 y JavaScript moderno para Mesa de Partes Virtual, Formato interactivo Anexo 1, Bandeja Interna Municipal y Verificación Pública QR.

No obstante, la auditoría identificó **vulnerabilidades críticas de seguridad (IDOR/BOLA, sobre-exposición de datos personales)** y **desalineamientos funcionales significativos** respecto a las decisiones de arquitectura de V2 (inexistencia de modelo normalizado de cuentas y establecimientos, ausencia de flujo de borrador, numeración prematura de expedientes y acoplamiento a simulaciones externas de Defensa Civil y Fiscalización).

---

## 2. Tecnologías y Stack Real Detectado

| Capa / Componente | Tecnología Detectada | Versión Real | Evaluación de Idoneidad |
| :--- | :--- | :--- | :--- |
| **Lenguaje de Programación** | Java | 21 LTS (Temurin 21.0.12.1) | Excelente. Máximo soporte LTS, Virtual Threads y modernidad. |
| **Framework Backend** | Spring Boot | 3.3.4 | Adecuado. Soporta Spring Framework 6 y Spring Security 6. |
| **Persistencia / ORM** | Spring Data JPA / Hibernate | 6.5.3.Final | Adecuado. Manejo transaccional robusto y dialecto PostgreSQL. |
| **Bases de Datos** | PostgreSQL (Prod) / H2 (Test) | PostgreSQL 16 (Docker) / H2 2.2 | Adecuado. Base relacional transaccional (ACID). |
| **Seguridad y Tokens** | Spring Security 6 + JJWT | 0.12.6 | Requiere endurecimiento de autorización a nivel de objeto. |
| **Generación de PDFs** | OpenPDF (Fork libre de iText) | 2.0.3 | Excelente. Generación nativa vectorial sin dependencias externas. |
| **Generación de Códigos QR** | Google ZXing | 3.5.3 | Robusto y ligero. |
| **Notificaciones por Correo** | Spring Boot Mail (JavaMail) + Thymeleaf | 3.3.4 | Operativo. Envío asíncrono con plantillas HTML institucionales. |
| **Documentación API** | Springdoc OpenAPI (Swagger UI) | 2.6.0 | Expuesto en `/swagger-ui.html` y `/api-docs`. |
| **Métricas y Monitoreo** | Spring Boot Actuator + Micrometer Prometheus | 3.3.4 | Expone `/actuator/prometheus` y `/actuator/health`. |
| **Frontend Web** | HTML5, Vanilla CSS3, JavaScript ES6+ | Nativo | Limpio, sin sobrecarga de frameworks, altamente adaptable. |

---

## 3. Estructura de Módulos y Código Fuente

### 3.1 Submódulo `common-domain`
- **Propósito:** Contrato común de DTOs, enumeradores y excepciones de negocio desacoplado de frameworks web.
- **Paquetes:**
  - `pe.gob.munihuamanga.licencias.common.dto`: 16 DTOs (`CrearExpedienteDto`, `ExpedienteResponseDto`, `LoginRequestDto`, `LoginResponseDto`, `SeguimientoCiudadanoDto`, `VerificacionLicenciaDto`, `VoucherDto`, etc.).
  - `pe.gob.munihuamanga.licencias.common.enums`: 7 enums (`EstadoExpediente`, `NivelRiesgo`, `ModalidadTramite`, `FuncionEdificacion`, `RolUsuario`, `TipoDocumento`, `TipoPersona`).
  - `pe.gob.munihuamanga.licencias.common.exception`: Excepciones base (`RecursoNoEncontradoException`, `ValidacionNegocioException`).

### 3.2 Submódulo `servicio-expedientes`
- **Propósito:** Aplicación principal monolítica modular que implementa el dominio, controladores REST, adaptadores, persistencia y seguridad.
- **Paquetes y Componentes:**
  - `config`: `SecurityConfig`, `AsyncConfig`, `DataInitializer`, `TarifaTupaDataInitializer`, `UsuarioDataInitializer`.
  - `controller`: `ExpedienteController`, `DocumentosFormulariosController`, `AuthController`, `PublicLicenciasController`, `TarifaTupaController`, `GlobalExceptionHandler`.
  - `model`: `Expediente` (entidad JPA monolítica), `Anexo4Condiciones` (embebido), `HistorialEstado`, `Usuario`, `TarifaTupa`.
  - `repository`: `ExpedienteRepository`, `HistorialRepository`, `UsuarioRepository`, `TarifaTupaRepository`.
  - `security`: `JwtTokenProvider`, `JwtAuthenticationFilter`, `CustomUserDetailsService`, `CustomUserDetails`, `JwtAuthenticationEntryPoint`, `JwtAccessDeniedHandler`.
  - `service`: `ExpedienteService`, `DocumentoPdfService`, `Anexo1PdfGenerator`, `Anexo3PdfGenerator`, `Anexo4PdfGenerator`, `LicenciaPdfGenerator`, `CalculadoraDeTasa`, `TarifaTupaService`, `NotificacionEmailService`, `AuditoriaService`, `MetricasExpedienteService`, `AuthService`.
  - `validator`: `EstadoExpedienteValidator`.
  - `integracion`: Puertos y adaptadores para SAT, Defensa Civil, Edificaciones y Fiscalización (`SatPort`, `DefensaCivilPort`, `EdificacionesPort`, `FiscalizacionPort`, `AdaptadorController`).

---

## 4. Hallazgos Críticos de Seguridad

### 4.1 Vulnerabilidad de Acceso Horizontal (BOLA / IDOR)
- **Diagnóstico:** Los endpoints `GET /api/expedientes/{id}` y `GET /api/expedientes/detalle/{numeroTramite}` permiten recuperar el expediente completo (incluyendo DNI, representante, giro, dirección, etc.). En `SecurityConfig.java`, la ruta `/api/expedientes/detalle/**` está configurada como `permitAll()`, permitiendo a cualquier actor no autenticado o a un ciudadano extraer los datos de cualquier otro expediente con solo iterar números correlativos (`EXP-2026-00001`, `EXP-2026-00002`).
- **Impacto:** Fuga masiva de información confidencial y violación de la Ley N° 29733 (Protección de Datos Personales).
- **Remediación Requerida:** Restringir el detalle completo únicamente al titular autenticado (dueño del expediente) o a funcionarios autorizados (`ROLE_EVALUADOR`, `ROLE_ADMIN`). Mantener únicamente el endpoint de seguimiento acotado `/api/expedientes/tramite/{numeroTramite}` con campos públicos no sensibles.

### 4.2 Descarga Pública No Autorizada de Documentos
- **Diagnóstico:** Los endpoints de descarga documental en `SecurityConfig.java`:
  - `/api/expedientes/*/documentos/declaracion-jurada`
  - `/api/expedientes/*/documentos/anexo1-declaracion-jurada`
  - `/api/expedientes/*/documentos/voucher-sat`
  - `/api/expedientes/*/documentos/licencia`
  están declarados como `permitAll()`.
- **Impacto:** Cualquier usuario con el UUID del expediente puede descargar declaraciones juradas y licencias emitidas ajenas.
- **Remediación Requerida:** Exigir autenticación y verificación de pertenencia (`isOwner(expediente, auth)` o `hasRole('EVALUADOR')`).

### 4.3 Violación del Principio de Minimización de Datos en Verificación QR
- **Diagnóstico:** El endpoint público `GET /api/public/licencias/{codigoLicencia}` devuelve el DTO `VerificacionLicenciaDto`, el cual expone en texto plano el atributo `documentoIdentidad` (DNI o RUC completo del titular) a cualquier persona anónima que escanee el código QR.
- **Impacto:** No cumple con el requisito **RNF-SEG-007** de la ERS V2 ("Se aplicará minimización de datos en la verificación pública") ni con el criterio de aceptación **CA-09**.
- **Remediación Requerida:** Enmascarar u omitir el documento de identidad en la vista pública, mostrando únicamente nombre comercial, razón social, giro, dirección y estado de vigencia. Reservar los datos ampliados para el inspector municipal autenticado (**CA-10**).

### 4.4 Subsanación sin Control de Identidad
- **Diagnóstico:** El endpoint `POST /api/expedientes/*/subsanar` está configurado como `permitAll()`.
- **Impacto:** Un tercero no autorizado podría inyectar texto o alterar el flujo de un expediente en estado OBSERVADO.
- **Remediación Requerida:** Exigir autenticación del solicitante y validar que el usuario coincida con el titular del trámite.

### 4.5 Secretos y Parámetros por Defecto
- **Diagnóstico:** En `application.yml` existen valores por defecto para `jwt.secret` (`MunicipalidadProvincialDeHuamangaGestionLicenciasSecretKeySegura2026!`) y contraseñas de PostgreSQL (`muni_pass123`).
- **Impacto:** Riesgo en entornos productivos si no se inyectan como variables de entorno de contenedor.
- **Remediación Requerida:** Mantener el mecanismo `${JWT_SECRET:...}` pero documentar la obligación de inyección estricta en producción mediante `.env`.

---

## 5. Auditoría de Base de Datos y Modelo Conceptual

### 5.1 Estado Actual del Esquema
El esquema PostgreSQL actual (`01-init-databases.sql`) está centrado en una sola tabla monolítica:
1. `expedientes`: Contiene más de 55 columnas mezclando datos del solicitante, persona jurídica, representante SUNARP, datos del local comercial, dirección, dimensionamiento, condiciones de seguridad (Anexo 4 embebido), estado del trámite, pago SAT y datos de licencia.
2. `historial_estados`: Registra transiciones con `expediente_id`, `estado_anterior`, `estado_nuevo`, `usuario`, `motivo`, `fecha`.
3. `usuarios`: Registra usuarios internos municipales con contraseñas BCrypt (`username`, `password`, `rol`, `activo`).
4. `tarifas_tupa`: Registra tarifas aprobadas por Ordenanza N° 018-2024-MPH para riesgos Bajo, Medio, Alto y Muy Alto.

### 5.2 Desalineamiento con el DAS V2 (Sección 6)
El DAS V2 establece un modelo conceptual normalizado con entidades separadas:
- `Cuenta` y `OperadorCuenta`: Para permitir cuentas individuales por DNI y cuentas institucionales únicas por RUC con múltiples operadores nominativos (evitando credenciales compartidas).
- `Establecimiento`: Entidad independiente que posee dirección, coordenadas geográficas (lat, lon), versión de ubicación y catálogo de giros con código CIIU. Una cuenta puede tener múltiples establecimientos.
- `SolicitudBorrador`: Permite al ciudadano guardar avances sin que se genere prematuramente un número de expediente formal (**RN-01**).
- `Documento`: Entidad con `tipo`, `version`, `hash_sha256`, `storage_key` y estado para garantizar inmutabilidad y trazabilidad de versiones reemplazadas en subsanación (**RF-DOC-006**).
- `PagoSAT`: Entidad con `codigo`, `monto`, `comprobante_id`, `estado_validacion` (con validación manual obligatoria, **RF-PAG-004**).
- `EvaluacionZona`: Registro de evaluación manual de zonificación posterior a presentación y pago (**RN-05**).
- `Informe`: Borrador e informe final revisado antes de la firma.
- `Licencia`: Documento oficial con identificador único, hash de PDF firmado y token QR no enumerable.
- `RectificacionUbicacion`: Trazabilidad de solicitudes de corrección de coordenadas frente a traslados de local.

---

## 6. Auditoría de Módulos de Integración y Restricciones Institucionales

### 6.1 Acoplamiento a Simulaciones Externas
En `pe.gob.munihuamanga.licencias.expedientes.integracion`:
- `DefensaCivilAdapterService` expone `simularDictamenItse(...)`. Esto contradice expresamente la restricción de V2: *"Sin simular dictámenes de Defensa Civil"* y *"Defensa Civil mantiene procesos externos en esta etapa"*.
- `FiscalizacionAdapterService` expone `registrarActaFiscalizacion(...)`. Esto contradice la restricción: *"No crear módulos de fiscalización, actas de inspección o procedimientos sancionadores. La Gerencia de Inspección solo utilizará MuniHuamanga como herramienta de consulta de autenticidad y ubicación"*.
- `SatAdapterService` simula conciliación automática, contraviniendo la regla de que el registro de comprobantes es manual y la validación es potestad del cajero/funcionario de Licencias.

### 6.2 Firma Digital
Actualmente la emisión de licencia en `LicenciaPdfGenerator` estampa un texto visual de "Sello Digital MPH".  
Conforme al DAS V2 (**DA-04** y **RF-LIC-003**), la firma digital debe realizarse a través de un adaptador hacia el servicio institucional de firma digital. Si el servicio aún no está disponible, se debe mantener una interfaz desacoplada y bloquear la emisión oficial en lugar de generar firmas digitales falsas.

---

## 7. Pruebas Automatizadas y Línea Base de Calidad

La suite actual en `servicio-expedientes/src/test` cuenta con:
- `CalculadoraDeTasaTest`: 5 pruebas (tasas TUPA según riesgo).
- `DocumentoPdfServiceCompletoTest`: 8 pruebas (generación de Anexo 1, 3, 4 y Licencia).
- `ExpedienteServiceTest`: 12 pruebas (creación, transiciones, subsanación, pago y aprobación).
- `LicenciaPdfServiceTest`: 6 pruebas (estructura del certificado oficial de licencia).
- `NotificacionEmailServiceTest`: 7 pruebas (plantillas y envíos asíncronos).
- `QrGeneratorServiceTest`: 1 prueba (generación de matriz QR).
- `TarifaTupaServiceTest`: 4 pruebas (mantenimiento de tasas TUPA).
- `VerificacionPublicaTest`: 4 pruebas (consulta pública de licencias).
- `EstadoExpedienteValidatorTest`: 10 pruebas (máquina de estados).
- `MesaPartesValidationTest`: 6 pruebas (validaciones de entrada DTO).

**Total de Pruebas:** 104 pruebas exitosas.  
**Brecha de Pruebas para V2:** Se requiere incorporar pruebas de seguridad BOLA/IDOR (intentos de acceso cruzado entre ciudadanos), pruebas de idempotencia en presentación, pruebas de minimización de datos en QR y pruebas de validación manual de pagos.
