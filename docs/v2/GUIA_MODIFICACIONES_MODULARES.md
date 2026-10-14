# Guía de Módulos del Sistema (M01 – M12) — MuniHuamanga V2
## Catálogo Funcional, Técnico y de Integración Modular

**Proyecto:** Sistema de Gestión de Licencias de Funcionamiento
**Entidad:** Municipalidad Provincial de Huamanga (Ayacucho, Perú)
**Fecha:** 14 de octubre de 2026
**Documento Canónico:** Especificación de Bounded Contexts y Módulos V2
**Código:** DAS-MH-V2-001 • MOD-M01-M12

---

## 1. Visión General de la Modularización

MuniHuamanga V2 implementa el estilo arquitectónico de **Monolito Modular** estructurado en doce (12) Bounded Contexts lógicos fuertemente cohesivos y débilmente acoplados, orquestados en memoria JVM dentro del artefacto único `servicio-expedientes.jar` y compartiendo contratos inmutables en `common-domain`.

```mermaid
graph TD
    subgraph Núcleo de Contratos
        CD["common-domain<br>(41 DTOs • 19 Enums • Excepciones)"]
    end

    subgraph Módulos Operativos V2 (servicio-expedientes)
        M01["M01: Cuentas y Operadores"]
        M02["M02: Solicitudes Borrador"]
        M03["M03: Expedientes y Documentos"]
        M04["M04: Pagos SAT"]
        M05["M05: Tarifario TUPA"]
        M06["M06: Zonificación y Rectificación"]
        M07["M07: Evaluación e Informes"]
        M08["M08: Resoluciones y Firma"]
        M09["M09: Licencias Oficiales"]
        M10["M10: Verificación QR"]
        M11["M11: Reclamaciones"]
        M12["M12: Auditoría y Seguridad"]
    end

    CD --> M01
    CD --> M02
    CD --> M03
    CD --> M04
    CD --> M05
    CD --> M06
    CD --> M07
    CD --> M08
    CD --> M09
    CD --> M10
    CD --> M11
    CD --> M12
```

---

## 2. Especificación Detallada de Módulos (M01 a M12)

### M01: Cuentas, Operadores y Establecimientos
* **Objetivo:** Administrar la identidad de los administrados, operadores municipales y los establecimientos comerciales desacoplados de los expedientes.
* **Requisitos:** RF-01, RF-14, RF-15.
* **Responsabilidades:** Registro y autenticación de ciudadanos; registro y activación de operadores institucionales con roles RBAC; gestión de establecimientos multi-local para una misma cuenta titular.
* **Clases Principales:** `Cuenta`, `Establecimiento`, `OperadorCuenta`, `CuentaService`, `EstablecimientoService`, `CuentaController`, `EstablecimientoController`, `CuentaRepository`, `EstablecimientoRepository`.
* **Endpoints:**
  - `POST /api/cuentas/registro` (Público: Registro de cuenta ciudadana).
  - `POST /api/cuentas/operadores` (Protegido `ROLE_ADMIN`: Alta de operadores).
  - `POST /api/cuentas/{id}/establecimientos` (Protegido `isOwner`: Registro de locales).
  - `GET /api/cuentas/{id}/establecimientos` (Protegido `isOwner`: Listado de locales).
* **DTOs Clave:** `RegistroCuentaDto`, `OperadorCuentaDto`, `EstablecimientoDto`, `CuentaDto`.
* **Persistencia:** Tablas `cuentas`, `establecimientos` en PostgreSQL 16.
* **Frontend:** Formularios en `portal-interno.html` (pestaña Operadores) y base para `portal-ciudadano.html`.
* **Reglas de Negocio / Seguridad:** Validación estricta de titularidad en backend (`@PreAuthorize("@cuentaSecurity.isOwner(authentication, #id)")`, ADR-02).
* **Pruebas:** `CuentaServiceTest`, `EstablecimientoServiceTest`, `CuentaSecurityTest`.
* **Estado:** ✅ Implementado y Verificado al 100%.

---

### M02: Solicitudes Borrador y Asistente
* **Objetivo:** Permitir la captura progresiva y autoguardado de solicitudes comerciales antes de su presentación formal.
* **Requisitos:** RF-02, RF-15.
* **Responsabilidades:** Creación de borradores; guardado automático por secciones (Datos del Titular, Local, Anexo 4); validación de checklist de requisitos; formalización y generación atómica de expediente formal.
* **Clases Principales:** `SolicitudBorrador`, `BorradorService`, `BorradorController`, `SolicitudBorradorRepository`.
* **Endpoints:**
  - `POST /api/borradores` (Protegido: Crea nuevo borrador).
  - `GET /api/borradores/{id}` (Protegido `isOwner`: Consulta borrador).
  - `PUT /api/borradores/{id}` (Protegido `isOwner`: Actualiza campos/secciones).
  - `POST /api/borradores/{id}/presentar` (Protegido `isOwner`: Transición a presentado y creación de expediente).
* **DTOs Clave:** `SolicitudBorradorDto`, `PresentarBorradorDto`.
* **Persistencia:** Tabla `solicitudes_borradores`.
* **Frontend:** Lógica de formulario interactivo en `formato-anexo1.html`.
* **Brecha Detectada en Fase A:** `portal-ciudadano.js` todavía envía directamente a `POST /api/expedientes`, sin aprovechar la API de borradores autoguardados.
* **Pruebas:** `BorradorServiceTest`, `BorradorControllerTest`.
* **Estado:** ✅ Backend implementado; interfaz de usuario pendiente de reconexión.

---

### M03: Expedientes Administrativos y Documentos
* **Objetivo:** Gestionar el ciclo de vida de los trámites, su máquina de 10 estados y el repositorio documental inmutable con versiones SHA-256.
* **Requisitos:** RF-03, RF-07, RF-08, RF-16.
* **Responsabilidades:** Gobierno de la máquina de estados finita (`EstadoExpediente.java`); renderizado in-memory de Anexos 1, 3 y 4 en OpenPDF; versionado inmutable de documentos con cálculo de hash SHA-256; subsanación de observaciones dentro del plazo de 5 días hábiles.
* **Clases Principales:** `Expediente`, `DocumentoAdjunto`, `ExpedienteService`, `DocumentoService`, `ExpedienteController`, `DocumentosFormulariosController`, `ExpedienteRepository`, `DocumentoAdjuntoRepository`.
* **Endpoints:**
  - `POST /api/expedientes` (Creación formal con correlativo `EXP-YYYY-XXXXX`).
  - `GET /api/expedientes/{id}` (Consulta detallada del trámite).
  - `POST /api/expedientes/{id}/subsanar` (Carga de documentos subsanados).
  - `GET /api/expedientes/{id}/documentos/anexo1-declaracion-jurada` (PDF Anexo 1).
  - `GET /api/expedientes/{id}/documentos/anexo3-matriz-riesgo-itse` (PDF Anexo 3).
  - `GET /api/expedientes/{id}/documentos/anexo4-condiciones-seguridad` (PDF Anexo 4).
* **DTOs Clave:** `CrearExpedienteDto`, `ExpedienteResponseDto`, `SubsanarObservacionesDto`, `DocumentoAdjuntoDto`.
* **Persistencia:** Tablas `expedientes`, `documentos_adjuntos`.
* **Frontend:** Vistas de seguimiento en `portal-ciudadano.html` y gestión de expedientes en `portal-interno.html`.
* **Pruebas:** `ExpedienteServiceTest`, `DocumentoServiceTest`, `SubsanacionTest`, `EstadoExpedienteValidatorTest`.
* **Estado:** ✅ Implementado y Verificado al 100%.

---

### M04: Pagos SAT y Liquidación Tributaria
* **Objetivo:** Controlar la recaudación de los derechos de trámite municipal e inspección técnica según el TUPA.
* **Requisitos:** RF-04, RF-17.
* **Responsabilidades:** Emisión de órdenes de pago con código de barras (`VCH-YYYY-XXXXXX`); registro de comprobantes de pago por administrados; validación manual por cajeros municipales con estados diferenciados (`EN_REVISION`, `VALIDADO_MANUALMENTE`, `OBSERVADO`, `RECHAZADO`).
* **Clases Principales:** `PagoExpediente`, `PagoService`, `PagoController`, `PagoExpedienteRepository`.
* **Endpoints:**
  - `POST /api/expedientes/{id}/pago` (Registro de comprobante de pago).
  - `PUT /api/pagos/{id}/validar-manual` (Protegido `ROLE_CAJERO`: Validación de abono).
  - `PUT /api/pagos/{id}/observar` (Protegido `ROLE_CAJERO`: Observación de pago).
  - `PUT /api/pagos/{id}/rechazar` (Protegido `ROLE_CAJERO`: Rechazo de comprobante).
* **DTOs Clave:** `PagoExpedienteDto`, `ValidarPagoManualDto`, `ObservarPagoDto`, `RechazarPagoDto`.
* **Persistencia:** Tabla `pagos_expedientes`.
* **Frontend:** Pestaña "Pagos" en `portal-interno.html` con badges de estado y modales de validación.
* **Reglas de Negocio:** No se admiten aprobaciones de expedientes sin pago previamente en estado `VALIDADO_MANUALMENTE` (ADR-06).
* **Pruebas:** `PagoServiceTest`, `PagoControllerTest`.
* **Estado:** ✅ Implementado y Verificado al 100%.

---

### M05: Tarifario TUPA Dinámico
* **Objetivo:** Administrar los conceptos y costos de las tasas TUPA con actualización en tiempo real sin recompilación.
* **Requisitos:** RF-09, RF-10.
* **Responsabilidades:** Catálogo de tarifas por nivel de riesgo ITSE; cálculo desglosado de derechos e inspecciones; mantenimiento en caliente por administradores de TI; fallback resiliente a `application.yml`.
* **Clases Principales:** `TarifaTupa`, `TarifaTupaService`, `CalculadoraDeTasa`, `TarifaTupaController`, `TarifaTupaRepository`.
* **Endpoints:**
  - `GET /api/tupa/tarifas` (Público/Interno: Catálogo vigente).
  - `PUT /api/tupa/tarifas/{id}` (Protegido `ROLE_ADMIN`: Actualización en caliente de tasa).
* **DTOs Clave:** `TarifaTupaDto`.
* **Persistencia:** Tabla `tarifas_tupa`.
* **Frontend:** Pestaña "TUPA" en `portal-interno.html`.
* **Pruebas:** `TarifaTupaServiceTest`, `CalculadoraDeTasaTest`.
* **Estado:** ✅ Implementado y Verificado al 100%.

---

### M06: Zonificación Urbana PDU y Rectificación de Ubicación
* **Objetivo:** Evaluar la compatibilidad de uso del suelo comercial conforme al PDU y permitir rectificaciones cartográficas.
* **Requisitos:** RF-05, RF-06, RF-18.
* **Responsabilidades:** Registro de dictamen de compatibilidad de uso PDU por el evaluador municipal; flujo de solicitudes de rectificación de dirección o coordenadas por el titular comercial; resolución técnica de rectificaciones.
* **Clases Principales:** `EvaluacionZonificacion`, `SolicitudRectificacionUbicacion`, `ZonificacionService`, `RectificacionUbicacionService`, `ZonificacionController`, `RectificacionController`.
* **Endpoints:**
  - `POST /api/expedientes/{id}/zonificacion` (Protegido `ROLE_EVALUADOR`: Evaluación PDU).
  - `POST /api/establecimientos/{id}/rectificacion-ubicacion` (Protegido: Solicitud de cambio).
  - `PUT /api/rectificaciones-ubicacion/{id}/resolver` (Protegido `ROLE_EVALUADOR`: Dictamen).
* **DTOs Clave:** `RegistrarEvaluacionZonificacionDto`, `SolicitudRectificacionUbicacionDto`.
* **Persistencia:** Tablas `evaluaciones_zonificacion`, `solicitudes_rectificacion_ubicacion`.
* **Frontend:** Pestaña "Zonificación" y "Rectificaciones" en `portal-interno.html`.
* **Pruebas:** `ZonificacionServiceTest`, `RectificacionUbicacionTest`.
* **Estado:** ✅ Implementado y Verificado al 100%.

---

### M07: Evaluación Administrativa e Informes Técnicos
* **Objetivo:** Conducir la fase de instrucción interna, evaluación de requisitos y emisión de informes técnicos previos.
* **Requisitos:** RF-10, RF-19.
* **Responsabilidades:** Verificación de admisibilidad documental; elaboración de **Informe Administrativo Técnico** (acto preparatorio de instrucción interna, ADR-07); gestión de observaciones técnicas; despacho asíncrono de notificaciones por correo electrónico.
* **Clases Principales:** `InformeAdministrativo`, `EvaluacionService`, `NotificacionEmailService`, `EvaluacionController`, `InformeAdministrativoRepository`.
* **Endpoints:**
  - `POST /api/expedientes/{id}/informe-tecnico` (Protegido `ROLE_EVALUADOR`: Emisión informe).
  - `GET /api/expedientes/{id}/informes` (Consulta de informes técnicos).
* **DTOs Clave:** `InformeAdministrativoDto`, `ValidacionRequisitosDto`.
* **Persistencia:** Tabla `informes_administrativos`.
* **Frontend:** Pestaña "Evaluación" en `portal-interno.html`.
* **Pruebas:** `EvaluacionServiceTest`, `InformeAdministrativoTest`.
* **Estado:** ✅ Implementado y Verificado al 100%.

---

### M08: Resoluciones Administrativas y Firma Digital
* **Objetivo:** Materializar el acto resolutivo vinculante que aprueba o deniega la solicitud comercial y gestionar la firma institucional.
* **Requisitos:** RF-11, RF-20, RF-21.
* **Responsabilidades:** Generación de correlativos anuales atómicos pesimistas (`RES-YYYY-XXXXX`); estados de resolución (`BORRADOR`, `APROBADA_PENDIENTE_FIRMA`, `EN_PROCESO_FIRMA`, `FIRMADA`, `ERROR_FIRMA`, `ANULADA`); invocación a `FirmaDigitalPort`; **bloqueo estricto fail-closed** ante fallos de firma.
* **Clases Principales:** `ResolucionAdministrativa`, `ResolucionService`, `ResolucionController`, `FirmaDigitalPort`, `FirmaDigitalAdapterService`, `ResolucionAdministrativaRepository`.
* **Endpoints:**
  - `POST /api/resoluciones` (Protegido `ROLE_ADMIN`: Creación de resolución).
  - `POST /api/resoluciones/{id}/firmar` (Protegido `ROLE_ADMIN`: Firma digital IOFE).
  - `GET /api/resoluciones/{id}/vista-preliminar` (PDF preliminar con marca de agua).
* **DTOs Clave:** `ResolucionAdministrativaDto`, `SolicitarFirmaDto`.
* **Persistencia:** Tabla `resoluciones_administrativas`, `secuencias_anuales`.
* **Frontend:** Pestaña "Resoluciones" en `portal-interno.html`.
* **Pruebas:** `ResolucionServiceTest`, `FirmaDigitalAdapterTest`.
* **Estado:** ✅ Implementado y Verificado al 100%.

---

### M09: Emisión de Licencias Oficiales
* **Objetivo:** Expedir el Certificado Oficial de Licencia de Funcionamiento con plena validez jurídica.
* **Requisitos:** RF-12, RF-22.
* **Responsabilidades:** Generación de correlativo atómico anual (`LIC-YYYY-XXXXX`); doble borde ornamental institucional; inserción de notas legales del Art. 3° de la Ley 28976; vigencia indeterminada taxativa; incrustación de código QR dinámico ZXing.
* **Clases Principales:** `Licencia`, `LicenciaService`, `LicenciaPdfGenerator`, `LicenciaController`, `LicenciaRepository`.
* **Endpoints:**
  - `POST /api/licencias/emitir` (Protegido `ROLE_ADMIN`: Emisión atómica post-firma).
  - `GET /api/licencias/{id}/pdf` (Descarga de certificado oficial PDF).
* **DTOs Clave:** `LicenciaDto`.
* **Persistencia:** Tabla `licencias`.
* **Frontend:** Visualización y descarga en `portal-interno.html` y `portal-ciudadano.html`.
* **Reglas de Negocio:** No se puede emitir una licencia si no existe resolución previa en estado `FIRMADA` (ADR-07).
* **Pruebas:** `LicenciaServiceTest`, `LicenciaPdfGeneratorTest`.
* **Estado:** ✅ Implementado y Verificado al 100%.

---

### M10: Verificación QR Diferenciada y Consulta Móvil
* **Objetivo:** Brindar verificación de autenticidad en campo cumpliendo la Ley N° 29733 de Protección de Datos Personales.
* **Requisitos:** RF-13, RF-23, RF-24.
* **Responsabilidades:** Token QR dinámico aleatorio no enumerable; doble vista de verificación:
  - **Vista Pública Minimizada:** Razón social, giro, dirección y estado (sin DNI, RUC ni coordenadas).
  - **Vista de Inspector Móvil:** Autenticada con `ROLE_INSPECTOR`, expone georreferenciación completa y renderizado automático en mapa interactivo Leaflet.
* **Clases Principales:** `PublicLicenciasController`, `LicenciaInspectorController`, `VerificacionService`.
* **Endpoints:**
  - `GET /api/public/licencias/{tokenQr}` (Público sin token: Vista ciudadana).
  - `GET /api/licencias/inspeccion/{tokenQr}` (Protegido `ROLE_INSPECTOR`: Vista ampliada con mapa).
* **DTOs Clave:** `VerificacionPublicaQrDto`, `VerificacionInspectorDto`.
* **Persistencia:** Índices dedicados en `licencias(codigo_qr)`.
* **Frontend:** Interfaz web adaptativa `verificar-licencia.html` y script `verificar-licencia.js`.
* **Pruebas:** `VerificacionPublicaTest`, `VerificacionQrI09Test`.
* **Estado:** ✅ Implementado y Verificado al 100%.

---

### M11: Reclamaciones Administrativas Ciudadanas
* **Objetivo:** Canalizar recursos administrativos, quejas o reclamaciones sobre expedientes observados o denegados.
* **Requisitos:** RF-14, RF-25.
* **Responsabilidades:** Registro de reclamación con sustento; asignación de evaluador; resolución motivada de la controversia.
* **Clases Principales:** `ReclamacionExpediente`, `ReclamacionService`, `ReclamacionController`, `ReclamacionExpedienteRepository`.
* **Endpoints:**
  - `POST /api/expedientes/{id}/reclamaciones` (Protegido: Registro de reclamación).
  - `PUT /api/reclamaciones/{id}/resolver` (Protegido `ROLE_EVALUADOR` / `ROLE_ADMIN`: Resolución).
* **DTOs Clave:** `ReclamacionExpedienteDto`, `ResolverReclamacionDto`.
* **Persistencia:** Tabla `reclamaciones_expedientes`.
* **Frontend:** Interfaz backend completa; pantalla ciudadana específica pendiente de integrar en el menú del portal web.
* **Pruebas:** `ReclamacionServiceTest`.
* **Estado:** 🟡 Backend Completo / Frontend Pendiente.

---

### M12: Seguridad Integral y Auditoría Inmutable
* **Objetivo:** Proteger el acceso a recursos del sistema y registrar de forma inmutable cada acto administrativo.
* **Requisitos:** RF-15, RNF-04, RNF-05, RNF-08.
* **Responsabilidades:** Autenticación sin estado mediante JWT HMAC-SHA256; autorización basada en roles (`ROLE_ADMIN`, `ROLE_EVALUADOR`, `ROLE_CAJERO`, `ROLE_INSPECTOR`, `ROLE_CIUDADANO`); mitigación IDOR/BOLA con anotaciones `@PreAuthorize`; bitácora de auditoría inmutable (*append-only*).
* **Clases Principales:** `SecurityConfig`, `JwtAuthenticationFilter`, `JwtTokenProvider`, `AuditoriaExpediente`, `AuditoriaService`, `ExpedienteSecurity`, `CuentaSecurity`, `EstablecimientoSecurity`.
* **Endpoints:**
  - `POST /api/auth/login` (Autenticación y entrega de token Bearer).
  - `GET /api/expedientes/{id}/auditoria` (Protegido `ROLE_ADMIN`: Trazabilidad inmutable).
* **Persistencia:** Tabla `auditoria_expedientes`.
* **Frontend:** Login modal y control de sesiones en `portal-interno.js` y `portal-ciudadano.js`.
* **Pruebas:** `ExpedienteSecurityAuthorizationTest`, `SecurityConfigProdTest`, `AuditTest`.
* **Estado:** 🟡 Backend Completo / Consola Gráfica de Auditoría Pendiente.
