# Índice Especializado de Documentación V2 — MuniHuamanga
## Registro de Instrumentos Técnicos, Incrementos y Cierres de Versión

**Entidad:** Municipalidad Provincial de Huamanga (Ayacucho, Perú)
**Gerencia:** Subgerencia de Licencias y Autorizaciones Comerciales
**Rama de Trabajo:** `develop/munihuamanga-v2`
**Documentos Rectores:** ERS-MH-V2-001 (v1.0) y DAS-MH-V2-001 (v1.0)
**Línea Base Automatizada:** 301 pruebas unitarias e integradas (0 fallos, 0 errores)

---

## 1. Propósito de esta Carpeta

La carpeta `docs/v2/` constituye el repositorio canónico de los instrumentos de ingeniería de software, diagnósticos iniciales, matrices de brechas, especificaciones modulares, reportes de implementación y actas de cierre de cada incremento iterativo ejecutado para la consolidación de **MuniHuamanga V2**.

---

## 2. Instrumentos Rectores y Guías Especializadas V2

| Documento | Descripción | Estado |
| :--- | :--- | :---: |
| [ESTADO_ACTUAL_SISTEMA.md](ESTADO_ACTUAL_SISTEMA.md) | Informe integral del estado operativo, módulos completos, parciales y pendientes al corte I10 Fase A. | Canónico Vigente |
| [GUIA_MODIFICACIONES_MODULARES.md](GUIA_MODIFICACIONES_MODULARES.md) | Especificación funcional, técnica, endpoints, clases y reglas de negocio de los 12 módulos M01 a M12. | Canónico Vigente |
| [GUIA_INTERFACES_FRONTEND.md](GUIA_INTERFACES_FRONTEND.md) | Manual de diseño visual, componentes UI, Leaflet maps, tokens CSS y flujos de pantallas. | Canónico Vigente |
| [DECISIONES_ARQUITECTURA.md](DECISIONES_ARQUITECTURA.md) | **Registro Canónico Maestro de ADRs (ADR-01 a ADR-07)** que norman la arquitectura de MuniHuamanga V2. | Canónico Vigente |
| [AUDITORIA_INICIAL.md](AUDITORIA_INICIAL.md) | Diagnóstico técnico inicial del código fuente previo a la evolución V2. | Histórico Conservado |
| [MATRIZ_BRECHAS.md](MATRIZ_BRECHAS.md) | Mapeo de discrepancias y plan de acción frente a la ERS y DAS V2. | Histórico Conservado |
| [PLAN_IMPLEMENTACION.md](PLAN_IMPLEMENTACION.md) | Hoja de ruta estratégica por incrementos iterativos (I01 a I10). | Histórico Conservado |
| [PRUEBAS_Y_VALIDACION.md](PRUEBAS_Y_VALIDACION.md) | Estrategia de aseguramiento de calidad, reporte de 301 pruebas automatizadas y cobertura. | Canónico Vigente |

---

## 3. Registro Histórico de Incrementos y Actas de Cierre

| Incremento | Documento de Implementación | Acta de Cierre Técnico | Alcance Principal |
| :---: | :--- | :--- | :--- |
| **I02** | [IMPLEMENTACION_I02.md](IMPLEMENTACION_I02.md) | [CIERRE_I02_1.md](CIERRE_I02_1.md) | M01: Cuentas ciudadanas, operadores institucionales y locales. |
| **I03** | [IMPLEMENTACION_I03.md](IMPLEMENTACION_I03.md) | — | M02: Solicitudes borrador, autoguardado y checklist. |
| **I04** | [IMPLEMENTACION_I04.md](IMPLEMENTACION_I04.md) | [CIERRE_I04_1.md](CIERRE_I04_1.md) | M03: Repositorio inmutable y subsanaciones SHA-256. |
| **I05** | [IMPLEMENTACION_I05.md](IMPLEMENTACION_I05.md) | [CIERRE_I05_1.md](CIERRE_I05_1.md) | M04/M05: Pagos SAT manuales y tarifario TUPA dinámico. |
| **I06** | [IMPLEMENTACION_I06.md](IMPLEMENTACION_I06.md) | [CIERRE_I06_1.md](CIERRE_I06_1.md) | M06: Zonificación PDU y rectificación de ubicación. |
| **I07** | [IMPLEMENTACION_I07.md](IMPLEMENTACION_I07.md) | [CIERRE_I07_1.md](CIERRE_I07_1.md) | M07: Evaluación de admisibilidad e informes técnicos previos. |
| **I08** | [IMPLEMENTACION_I08.md](IMPLEMENTACION_I08.md) | [CIERRE_I08_1.md](CIERRE_I08_1.md) | M08/M09: Resoluciones con correlativo atómico y firma fail-closed. |
| **I09** | [IMPLEMENTACION_I09.md](IMPLEMENTACION_I09.md) | — | M10: Verificación QR diferenciada y mapa móvil inspector. |

---

## 4. Navegación hacia Documentación General

Para la comprensión global del sistema y las especificaciones arquitectónicas formales:
- [README Principal](../../README.md) — Punto de entrada maestro del repositorio.
- [Estilo Arquitectónico](../arquitectura/estilo-arquitectonico.md) — Monolito Modular.
- [Enfoque Arquitectónico](../arquitectura/enfoque-arquitectonico.md) — Clean Architecture y Puertos.
- [Modelo C4](../arquitectura/c4-model.md) — Niveles 1 al 4 de la solución.
- [Informe de Consolidación Documental](../auditoria/CONSOLIDACION_DOCUMENTAL_V2.md) — Auditoría de documentos.
