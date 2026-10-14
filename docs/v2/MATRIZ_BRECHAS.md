# Matriz de Brechas — MuniHuamanga V2

Municipalidad Provincial de Huamanga — Gerencia de Licencias  
**Fecha:** 13 de octubre de 2026  
**Documentos Rectores:** [ERS-MH-V2-001 v1.0] y [DAS-MH-V2-001 v1.0]

---

## 1. Metodología de Clasificación

Para garantizar una transición controlada sin regresiones ni pérdida de funcionalidades maduras, los componentes, clases, módulos y configuraciones del repositorio se clasifican en siete categorías técnicas:

1. **Conservar:** Código, módulos o configuraciones maduras que cumplen con los requisitos de V2 y deben permanecer estables.
2. **Corregir:** Componentes que presentan fallos de seguridad, sobre-exposición de datos o incumplimientos normativos inmediatos.
3. **Refactorizar:** Componentes funcionales cuyo diseño interno debe reestructurarse para alinearse al modelo desacoplado y normalizado de V2.
4. **Fusionar:** Elementos redundantes o endpoints duplicados que deben consolidarse en un punto único de entrada.
5. **Archivar:** Documentación o artefactos de fases preliminares que deben preservarse como registro histórico sin interferir en V2.
6. **Eliminar:** Código muerto, simulaciones prohibidas expresamente por V2 o módulos fuera de las competencias de Licencias.
7. **Implementar:** Nuevos módulos y funcionalidades exigidas por la ERS/DAS V2 no presentes en el repositorio actual.

---

## 2. Matriz General de Brechas por Categoría

| Categoría | Componente / Archivo | Estado Actual | Requisito V2 Relacionado | Acción y Justificación Técnica |
| :--- | :--- | :--- | :--- | :--- |
| **CONSERVAR** | `pom.xml` (Raíz, `common-domain`, `servicio-expedientes`) | Multi-módulo Maven configurado con Java 21 y Spring Boot 3.3.4 | DAS-01 (Monolito modular) | Mantener estructura base multi-módulo y versiones estables de dependencias. |
| **CONSERVAR** | Motores PDF (`Anexo1PdfGenerator`, `Anexo3PdfGenerator`, `Anexo4PdfGenerator`, `LicenciaPdfGenerator`) | Generadores nativos OpenPDF vectoriales de alta fidelidad | RF-DOC-001, RF-DOC-007, RF-LIC-004 | Conservar los motores de renderizado; reutilizarlos conectándolos al nuevo modelo documental. |
| **CONSERVAR** | Motor QR (`QrGeneratorService`, ZXing) | Genera matriz QR en imagen PNG y streams de bytes | RF-VER-005, RNF-SEG-007 | Conservar para inserción en licencias y resolución de URL oficial. |
| **CONSERVAR** | Notificaciones Email (`NotificacionEmailService`, plantillas Thymeleaf) | Envío asíncrono JavaMail de eventos clave (registro, rechazo, aprobación) | RF-ADM-001, RF-ADM-002 | Conservar servicio e infraestructura SMTP; integrar en el módulo unificado de eventos. |
| **CONSERVAR** | Calculadora TUPA (`CalculadoraDeTasa`, `TarifaTupaService`) | Calcula tasas según nivel de riesgo con base en Ordenanza N° 018-2024-MPH | RF-PAG-001 | Conservar lógica de cálculo y mantenimiento dinámico de tasas. |
| **CONSERVAR** | Frontends Web (`index.html`, `portal-ciudadano.html`, `formato-anexo1.html`, `verificar-licencia.html`) | Interfaces limpias en HTML5/CSS3/JS | RNF-OPE-001 | Conservar el diseño y UX actual; adaptar llamadas API hacia contratos V2. |
| **CONSERVAR** | Suite de Pruebas Unitarias e Integración (104 tests) | 104 pruebas exitosas en Maven | RNF-OPE-005 | Conservar como salvaguarda contra regresiones funcionales. |
| **CORREGIR** | `SecurityConfig.java` | Endpoints `/api/expedientes/detalle/**`, `/api/expedientes/*/documentos/**` y `subsanar` en `permitAll()` | RNF-SEG-001, RN-15, CA-01 | **RESUELTO EN I02:** Restringido acceso anónimo; autorización contextual y control de propiedad de recurso (BOLA/IDOR). |
| **CORREGIR** | `VerificacionLicenciaDto` | Atributo `documentoIdentidad` expuesto en la consulta pública QR | RNF-SEG-007, CA-09 | **RESUELTO EN I02:** Enmascarado según Ley 29733 (`42***91`). Endpoint `/ampliada` creado para inspectores. |
| **CORREGIR** | Generación de Número de Expediente en `ExpedienteService` | Se asigna `EXP-2026-xxxxx` al crear borrador en `FORMATOS_GENERADOS` | RN-01, RN-02, CA-02, CA-04 | El número formal único se debe asignar **únicamente tras la presentación formal completa** y carga de pago. |
| **CORREGIR** | Registro y Aprobación de Pagos SAT | El registro de voucher cambia estado a `EN_EVALUACION_FINAL` directamente | RF-PAG-004, RN-04, CA-03 | El pago cargado por ciudadano queda `PENDIENTE_VALIDACION`; solo un cajero/funcionario puede validarlo. |
| **CORREGIR** | `RolUsuario.java` | Solo contenía `ROLE_ADMIN`, `ROLE_EVALUADOR`, `ROLE_CAJERO` | RF-USR-001, RF-USR-006, INT-04 | **RESUELTO EN I02:** Incorporados `ROLE_CIUDADANO` y `ROLE_INSPECTOR`. |
| **REFACTORIZAR** | `Expediente.java` (Entidad) | Tabla monolítica con más de 55 columnas desnormalizadas | DAS-Sección 6 (Modelo de datos) | Desacoplar en entidades de dominio: `Cuenta`, `Establecimiento`, `SolicitudBorrador`, `Expediente`, `Documento`. |
| **REFACTORIZAR** | `EstadoExpediente.java` | 6 estados legados (`FORMATOS_GENERADOS`, `DOCUMENTOS_VALIDADOS`, etc.) | ERS-3.1 (10 estados oficiales de V2) | Migrar al catálogo completo: `BORRADOR`, `PRESENTADO`, `EN_EVALUACION`, `OBSERVADO`, `SUBSANADO`, `APTO_PARA_RESOLUCION`, `APROBADO`, `EMITIDO`, `DENEGADO`, `CONCLUIDO`. |
| **REFACTORIZAR** | `ExpedienteService.java` | Clase con más de 550 líneas que asume responsabilidades cruzadas | DAS-01, Principio SRP | Modularizar en servicios especializados: `CuentaService`, `EstablecimientoService`, `ZonificacionService`, etc. |
| **REFACTORIZAR** | Autorización por Objeto (BOLA/IDOR) | Control de acceso solo a nivel de rol HTTP en `SecurityConfig` | RNF-SEG-001, CA-01 | **RESUELTO EN I02:** Implementado evaluador `@PreAuthorize("@expedienteSecurity.puedeVerExpediente(...)")`. |
| **FUSIONAR** | `DocumentosFormulariosController` y `ExpedienteController` | Rutas de descarga documental redundantes (`/api/formularios/anexo-1/{id}/pdf` vs `/api/expedientes/{id}/documentos/...`) | DAS-02 | Consolidar la descarga y gestión documental en un único controlador coherente bajo `/api/v2/expedientes/{id}/documentos`. |
| **FUSIONAR** | Endpoints de Seguimiento y Detalle | `/api/expedientes/tramite/{num}` vs `/api/expedientes/detalle/{num}` | RF-USR-006, RNF-SEG-007 | Unificar en un endpoint con filtro de proyección según el rol del actor (público vs autenticado). |
| **ARCHIVAR** | Documentación de Entregas Preliminares (`docs/entrega-fase-01.md` a `docs/entrega-fase-04-*.md`) | Documentos históricos de sprints anteriores | ERS-1.1 | Mantener en `docs/historico/` como referencia documental de evolución. |
| **ELIMINAR** | `DefensaCivilAdapterService.simularDictamenItse` | Simula emisión de dictámenes de Defensa Civil | ERS-1.3, RN-03 | **ELIMINADO EN I02:** Clases eliminadas y dependencias desacopladas con éxito. |
| **ELIMINAR** | `FiscalizacionAdapterService.registrarActaFiscalizacion` | Endpoint para registrar actas de inspección y sanciones | ERS-1.3, RF-VER-008, CA-11 | **ELIMINADO EN I02:** Clases y puertos eliminados con éxito. |
| **IMPLEMENTAR** | **M01:** Usuarios, Cuentas y Operadores | No existe registro ciudadano ni cuenta institucional por RUC | RF-USR-001 a RF-USR-006 | Registro con DNI/RUC, validación de correo, cuenta única por RUC y operadores nominativos. |
| **IMPLEMENTAR** | **M02:** Establecimientos y Catálogo CIIU | Los datos del local están embebidos en el expediente | RF-EST-001 a RF-EST-005 | Entidad `Establecimiento`, coordenadas geográficas (lat/lon), historial y catálogo de giros CIIU. |
| **IMPLEMENTAR** | **M03:** Borradores y Repositorio Documental | No hay guardado borrador; archivos no tienen hash SHA-256 ni versionado | RF-DOC-001 a RF-DOC-007 | Guardado como `BORRADOR` sin generar número formal; repositorio documental con hash inmutable. |
| **IMPLEMENTAR** | **M04:** Flujo ITSE Administrativo V2 | Se asumía dictamen directo de Defensa Civil | RF-DOC-004, RN-03 | Registro de nivel de riesgo declarado con evidencia adjunta y validación administrativa. |
| **IMPLEMENTAR** | **M05:** Registro de Pago SAT y Validación Manual | Pago se confirma automáticamente sin control de caja | RF-PAG-001 a RF-PAG-005 | Instrucción imprimible para pago presencial, carga de voucher y validación manual por Licencias. |
| **IMPLEMENTAR** | **M06:** Presentación Formal y Transiciones | No hay paso formal de presentación con verificación de requisitos | RN-01, RN-02, RF-PAG-006, CA-04 | Transición atómica de `BORRADOR` a `PRESENTADO` con generación única de número de expediente. |
| **IMPLEMENTAR** | **M07:** Evaluación de Zonificación y Rectificación | Zonificación es solo un texto plano sin dictamen formal | RF-EVA-003, RF-VER-001 a 004 | Evaluación motivada con ordenanza de referencia; flujo de rectificación de coordenadas geográficas. |
| **IMPLEMENTAR** | **M08:** Informes Administrativos | No se genera informe técnico previo a la licencia | RF-LIC-001, RF-LIC-002 | Generación de borrador de informe y aprobación previa a la resolución. |
| **IMPLEMENTAR** | **M09:** Adaptador de Firma Digital Institucional | Sello digital actual es solo texto plano sobre el PDF | RF-LIC-003, RNF-SEG-006 | Interfaz para servicio institucional de firma; bloqueo de emisión oficial si no hay firma válida. |
| **IMPLEMENTAR** | **M10:** Verificación QR Diferenciada | Misma vista para todo público con datos personales expuestos | RF-VER-005 a 007, CA-09, CA-10 | Vista pública con minimización de datos y vista ampliada con mapa para inspectores autorizados. |
| **IMPLEMENTAR** | **M11:** Notificaciones Integradas (Portal + WhatsApp) | Solo existe correo electrónico SMTP | RF-ADM-001 a 003 | Bandeja de avisos en portal y adaptador para mensajería oficial por WhatsApp. |
| **IMPLEMENTAR** | **M12:** Administración de Parámetros y Catálogos | Solo existe CRUD de tasas TUPA | RF-ADM-004, RF-EST-003 | Mantenimiento de giros CIIU, zonificaciones PDU, plantillas y trazabilidad de cambios. |

---

## 3. Análisis de Referencias e Impacto Antes de Eliminación

Conforme a la **Regla Obligatoria 3**, antes de proceder a la eliminación de código funcional, se documentan las dependencias actuales y el impacto proyectado:

### 3.1 `DefensaCivilAdapterService.simularDictamenItse`
- **Ubicación:** `servicio-expedientes/.../integracion/adapter/DefensaCivilAdapterService.java`
- **Referencias:** Llamado únicamente desde `AdaptadorController.java` (`POST /api/integraciones/defensa-civil/simular-dictamen`). No es invocado por `ExpedienteService` ni por el flujo principal.
- **Impacto de Eliminación:** Cero impacto en el trámite regular. Evita confusión funcional y asegura el cumplimiento de **ERS-1.3** y **RN-03**.

### 3.2 `FiscalizacionAdapterService.registrarActaFiscalizacion`
- **Ubicación:** `servicio-expedientes/.../integracion/adapter/FiscalizacionAdapterService.java`
- **Referencias:** Llamado únicamente desde `AdaptadorController.java` (`POST /api/integraciones/fiscalizacion/acta`). No tiene tablas asociadas ni persistencia en base de datos.
- **Impacto de Eliminación:** Cero impacto en expedientes y licencias. Garantiza la restricción institucional (**ERS-1.3** y **RF-VER-008**) de no construir módulos sancionadores ni actas de fiscalización en MuniHuamanga V2.
