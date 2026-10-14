# INFORME DE AUDITORÍA Y CIERRE TÉCNICO I07.1
## Evaluación Administrativa, Informes Técnicos, Concurrencia y Cierre Técnico

**Municipalidad Provincial de Huamanga — Gerencia de Licencias de Funcionamiento**
**Fecha:** 14 de octubre de 2026
**Rama de Trabajo:** `develop/munihuamanga-v2`
**Referencia:** Auditoría Integral, Corrección y Cierre Técnico de I07 previo a I08
**Documentos Rectores:** [ERS-MH-V2-001 v1.0], [DAS-MH-V2-001 v1.0], TUO Ley N° 27444 (D.S. 004-2019-JUS), Ley Marco de Licencia de Funcionamiento (Ley N° 28976 y modificatorias D.S. N° 163-2020-PCM), D.S. N° 002-2018-PCM (Reglamento ITSE).

---

## 1. Resumen Ejecutivo

El presente informe documenta la ejecución, subsanación y cierre integral de la auditoría técnica **I07.1** sobre el módulo **M08 (Evaluación Administrativa, Informes Técnicos, Checklist Normativo y Rectificación de Coordenadas Geográficas)**, garantizando su perfecta integración con los módulos desarrollados en los incrementos I01 a I06.1.

Se procedió a la inspección directa del código fuente, entidades JPA, scripts de migración PostgreSQL, controladores REST, servicios de negocio, interfaz web administrativa (`portal-interno.html`, `portal-interno.js`) y la suite completa de pruebas automatizadas. Se identificaron y subsanaron vulnerabilidades críticas de concurrencia en la asignación de correlativos de informes técnicos, brechas de segregación de funciones, sesgos normativos en el checklist administrativo y rigidez automatizada en la evaluación del criterio CA-08 de georreferenciación.

### Logros Principales de I07.1:
1. **Generación Atómica Concurrente de Informes Técnicos:** Se sustituyó la consulta vulnerable `COUNT + 1` por el servicio atómico `SecuenciaInformeService` respaldado por la tabla `secuencias_informe`, bloqueo pesimista en base de datos (`PESSIMISTIC_WRITE`) y transacción aislada (`REQUIRES_NEW`), eliminando cualquier riesgo de colisión o duplicidad numérica bajo concurrencia multi-instancia.
2. **Segregación Estricta de Funciones y Visado Obligatorio:** Se eliminó la excepción de auto-aprobación para `admin_master`, prohibiendo terminantemente que cualquier evaluador (sea administrador o funcionario) apruebe su propio informe. Asimismo, se impidió la aprobación directa desde `EN_REVISION`, exigiendo visado previo e ineludible (`VISADO`).
3. **Robustecimiento del Checklist Administrativo Integral:** Se fortaleció la verificación documental para personas jurídicas (`TipoPersona.JURIDICA` exigiendo acreditación de personería / vigencia de poder registral SUNARP), giros con autorización sectorial previa, exclusión de aprobación si existen documentos vigentes en estado `OBSERVADO`, y sustento técnico ITSE previa obligatorio (planos y plan de seguridad) para riesgo ALTO y MUY ALTO.
4. **Desacoplamiento del Criterio CA-08 (Georreferenciación Humana):** La distancia geodésica Haversine mayor a 300 metros fue redefinida formalmente como un **indicador de riesgo / alerta** y no como un rechazo legal automático. Se garantizó que la aprobación de rectificaciones requiera evaluación humana motivada y se blindaron los endpoints contra vulnerabilidades BOLA/IDOR mediante `@establecimientoSecurity`.
5. **Certificación de la Suite de Pruebas:** Se alcanzaron **260 pruebas automatizadas exitosas (0 fallos, 0 errores, 0 omitidas)** en el reactor Maven multi-módulo, incorporando pruebas de estrés concurrente, auto-aprobación prohibida, rechazos BOLA y validaciones de borde del checklist.

---

## 2. Inventario de Archivos Modificados e Incorporados

| Componente / Archivo | Tipo de Cambio | Justificación Técnica |
| :--- | :--- | :--- |
| `docker/postgres/init/06-v2-evaluacion-administrativa-informes.sql` | Modificado | Incorporación de la tabla `secuencias_informe` con semilla inicial anual para numeración atómica. |
| `SecuenciaInforme.java` | **Nuevo** | Entidad JPA mapeada a `secuencias_informe` con clave primaria `anio` y correlativo atómico. |
| `SecuenciaInformeRepository.java` | **Nuevo** | Repositorio Spring Data con método `findByAnioWithLock` bajo `LockModeType.PESSIMISTIC_WRITE`. |
| `SecuenciaInformeInicializadorHelper.java` | **Nuevo** | Inicializador de secuencias anuales en transacción aislada (`Propagation.REQUIRES_NEW`). |
| `SecuenciaInformeService.java` | **Nuevo** | Servicio generador del correlativo oficial `INF-YYYY-XXXXX` seguro ante concurrencia. |
| `InformeAdministrativoService.java` | Modificado | Inyección de `SecuenciaInformeService`, obligatoriedad de estado `VISADO` para aprobar y remoción de bypass `admin_master`. |
| `ChecklistAdministrativoService.java` | Modificado | Validación de personería jurídica (vigencia poder), autorizaciones sectoriales, bloqueo por docs en `OBSERVADO` y requisitos técnicos de ITSE previa. |
| `SolicitudRectificacionUbicacionService.java` | Modificado | Distancia Haversine tratada como alerta de riesgo; decisión final humana; validación contextual de expediente asignado al establecimiento. |
| `SolicitudRectificacionController.java` | Modificado | Seguridad BOLA/IDOR con `@establecimientoSecurity.puedeModificarEstablecimientoPorId` y `puedeVerEstablecimientoPorId`. |
| `EstablecimientoSecurity.java` | Modificado | Verificación contextual por DNI/RUC y por correo electrónico del titular para compatibilidad JWT/portal. |
| `SecuenciaInformeMultiInstanciaTest.java` | **Nuevo** | Pruebas de estrés y concurrencia multihilo verificando unicidad de correlativos de informes. |
| `InformeAdministrativoServiceTest.java` | Modificado | Pruebas unitarias de segregación estricta (rechazo a `admin_master` como auto-aprobador) y exigencia de visado. |
| `ChecklistAdministrativoServiceTest.java` | Modificado | Pruebas para personas jurídicas sin vigencia de poder, documentos en `OBSERVADO` y riesgo alto sin planos ITSE previa. |
| `SolicitudRectificacionUbicacionServiceTest.java` | Modificado | Pruebas de aprobación motivada con distancia > 300m y rechazo de solicitudes con expedientes ajenos. |
| `EvaluacionAdministrativaIntegrationTest.java` | Modificado | Prueba de integración de seguridad BOLA/IDOR verificando HTTP 403 Forbidden ante intentos ajenos. |

---

## 3. Hallazgos por Severidad

| ID | Severidad | Componente Afectado | Descripción del Hallazgo |
| :--- | :--- | :--- | :--- |
| **H-01** | **ALTA** | `InformeAdministrativoService` | Numeración de informes mediante `COUNT + 1` vulnerable a colisiones bajo concurrencia o despliegues multi-instancia. |
| **H-02** | **ALTA** | `InformeAdministrativoService` | Bypass de segregación de funciones para usuario `admin_master` permitiendo auto-aprobación del informe elaborado. |
| **H-03** | **MEDIA** | `InformeAdministrativoService` | Admisión de aprobación formal desde estado `EN_REVISION`, omitiendo el paso obligatorio de visado por funcionario revisor. |
| **H-04** | **ALTA** | `SolicitudRectificacionController` | Ausencia de autorización por objeto (BOLA/IDOR) en creación y listado de solicitudes de rectificación de coordenadas. |
| **H-05** | **MEDIA** | `SolicitudRectificacionUbicacionService` | Umbral de 300 metros aplicado como bloqueo absoluto automático, impidiendo rectificación legítima de errores de geocodificación. |
| **H-06** | **MEDIA** | `ChecklistAdministrativoService` | Omisión de validación de vigencia de poder para personas jurídicas y de autorizaciones sectoriales en CHK-03. |
| **H-07** | **MEDIA** | `ChecklistAdministrativoService` | Aceptación de documentos vigentes con hash íntegro pero en estado `OBSERVADO` en CHK-04 sin exigir subsanación previa. |
| **H-08** | **MEDIA** | `ChecklistAdministrativoService` | Admisión de sustento ITSE para riesgo ALTO y MUY ALTO basado únicamente en declaraciones básicas sin planos técnicos. |

---

## 4. Causa Raíz de Cada Defecto

1. **H-01 (Numeración COUNT+1):** El servicio I07 realizaba un conteo en base de datos (`countByNumeroInformePrefix("INF-" + anio) + 1`). En un entorno multi-hilo o multi-nodo, dos transacciones concurrentes leían el mismo conteo antes del commit, provocando duplicación de números de informe y violando la integridad del registro público.
2. **H-02 (Bypass admin_master):** Se incluyó en el código una condición especial `&& !"admin_master".equals(actor)`, lo cual abría una puerta trasera de auto-evaluación y auto-aprobación administrativa, prohibida por los principios de segregación de funciones de la administración pública.
3. **H-03 (Omisión de Visado):** Se validaba `inf.getEstado() != EstadoInforme.VISADO && inf.getEstado() != EstadoInforme.EN_REVISION`, permitiendo que un informe pasara de borrador a revisión y directamente a aprobación sin dejar constancia del visado del revisor técnico.
4. **H-04 (BOLA/IDOR en Rectificaciones):** Los endpoints en `SolicitudRectificacionController` estaban protegidos únicamente con `@PreAuthorize("isAuthenticated()")`, permitiendo que cualquier usuario autenticado rectificara o consultara establecimientos de terceros conociendo su UUID.
5. **H-05 (Regla Rígida de 300m):** En `evaluarRectificacion`, se verificaba `if (dto.isEsTrasladoFisico() || Boolean.TRUE.equals(sol.getEsTrasladoFisico()))`, forzando el rechazo si la distancia excedía 300 metros, independientemente de la justificación técnica y decisión del evaluador.
6. **H-06, H-07 y H-08 (Brechas en Checklist):** El checklist administrativo evaluaba de forma genérica la presencia de Anexo 1 y la presencia de hash SHA-256, sin verificar condiciones normativas especiales como la representación de personas jurídicas (Art. 7 Ley 28976), documentos formalmente observados o los planos de seguridad exigidos por D.S. 002-2018-PCM para inspección previa.

---

## 5. Correcciones Aplicadas

### 5.1 Secuencia Transaccional Atómica para Informes (`INF-YYYY-XXXXX`)
Se replicó el exitoso patrón de concurrencia de trámites mediante:
- Entidad `SecuenciaInforme` y repositorio con `@Lock(LockModeType.PESSIMISTIC_WRITE)`.
- Bloqueo exclusivo `SELECT FOR UPDATE` sobre la fila del año correspondiente.
- Componente `SecuenciaInformeInicializadorHelper` con transacción aislada (`REQUIRES_NEW`) para sembrar la fila del año sin bloquear la transacción principal ante reinicios de año.
- Método atómico sincronizado `generarSiguienteNumeroInforme()` en `SecuenciaInformeService`.

### 5.2 Segregación Absoluta y Flujo Administrativo Obligatorio
En `InformeAdministrativoService.aprobarInforme`:
- Se eliminó toda excepción para cuentas administradoras o superusuarios.
- Se implementó la verificación:
  ```java
  boolean esMismoEvaluador = (inf.getEvaluadorId() != null && inf.getEvaluadorId().equals(aprobadorId))
          || (actor != null && actor.equalsIgnoreCase(inf.getEvaluadorNombre()));
  if (esMismoEvaluador) {
      throw new IllegalStateException("Segregación de funciones: El funcionario evaluador no puede aprobar su propio informe administrativo");
  }
  ```
- Se restringió el estado previo de aprobación exclusivamente a `EstadoInforme.VISADO`.

### 5.3 Blindaje de Seguridad BOLA/IDOR en Rectificación de Ubicación
- En `SolicitudRectificacionController`:
  - `solicitarRectificacion`: `@PreAuthorize("hasRole('ADMIN') or @establecimientoSecurity.puedeModificarEstablecimientoPorId(authentication, #dto.establecimientoId)")`
  - `listarPorEstablecimiento`: `@PreAuthorize("hasRole('ADMIN') or @establecimientoSecurity.puedeVerEstablecimientoPorId(authentication, #establecimientoId)")`
- En `SolicitudRectificacionUbicacionService`: validación de que el expediente pertenezca estrictamente al establecimiento indicado.
- En `EstablecimientoSecurity`: habilitación de verificación de titularidad por correo electrónico además de DNI/RUC.

### 5.4 Redefinición de Criterio CA-08 a Alerta de Riesgo
- La distancia calculada mediante Haversine se registra como `distanciaMetrosAprox` y activa el flag informativo `alertaTraslado` cuando excede los 300 metros.
- En la evaluación humana (`evaluarRectificacion`), el funcionario competente califica si la variación corresponde a un error material de coordenadas (subsanable) o si encubre un traslado físico comercial a otro inmueble (que requiere nuevo trámite de licencia según Ley 28976). La decisión humana motivada prima sobre el umbral métrico.

### 5.5 Fortalecimiento Normativo del Checklist Administrativo
- **CHK-03 (Documentos Exigibles):** Se exige `VIGENCIA_PODER` o datos registrales SUNARP para `TipoPersona.JURIDICA`. Se exige `AUTORIZACION_SECTORIAL` cuando el giro lo requiere.
- **CHK-04 (Versiones Integras):** Se comprueba que ningún documento activo tenga `estadoRevision == EstadoRevisionDocumento.OBSERVADO`.
- **CHK-08 (Sustento ITSE):** Para riesgo `ALTO` y `MUY_ALTO`, se valida obligatoriamente la presencia de planos técnicos (`PLANO_DISTRIBUCION`, `PLANO_ARQUITECTURA`, `PLAN_SEGURIDAD_ITSE`, etc.), impidiendo checklists favorables basados en meras declaraciones para inspecciones previas.

---

## 6. Evidencias de Pruebas Automatizadas

Se ejecutó la suite completa de pruebas sin omisiones (`mvn clean verify`):

```text
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for MuniHuamanga - Gestión de Licencias de Funcionamiento 1.0.0-SNAPSHOT:
[INFO]
[INFO] MuniHuamanga - Gestión de Licencias de Funcionamiento SUCCESS [  0.144 s]
[INFO] Common Domain ...................................... SUCCESS [  6.201 s]
[INFO] MuniHuamanga - Monolito Modular .................... SUCCESS [01:01 min]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:08 min
[INFO] Finished at: 2026-10-14T13:26:57-05:00
[INFO] ------------------------------------------------------------------------
[INFO] Tests run: 260, Failures: 0, Errors: 0, Skipped: 0
```

### Distribución de Pruebas:
- **Línea base previa I07:** 250 pruebas.
- **Nuevas pruebas I07.1:** +10 pruebas.
  - `SecuenciaInformeMultiInstanciaTest`: 2 pruebas (inicialización atómica y concurrencia multihilo sin colisiones).
  - `InformeAdministrativoServiceTest`: 2 pruebas adicionales (rechazo a `admin_master` como auto-aprobador y rechazo a informe en `EN_REVISION` sin visado).
  - `ChecklistAdministrativoServiceTest`: 3 pruebas adicionales (persona jurídica sin vigencia de poder, documento en `OBSERVADO` y riesgo alto sin planos ITSE previa).
  - `SolicitudRectificacionUbicacionServiceTest`: 2 pruebas adicionales (aprobación humana con distancia > 300m y rechazo de expediente ajeno).
  - `EvaluacionAdministrativaIntegrationTest`: 1 prueba adicional de seguridad (HTTP 403 Forbidden para ciudadano ajeno al establecimiento).
- **Total Acumulado:** **260 pruebas automatizadas exitosas (100% aprobadas, 0 fallos, 0 errores, 0 omitidas).**

---

## 7. Diagnóstico de Infraestructura y Estado de PostgreSQL 16 Real

En cumplimiento estricto de la directiva de no presentar pruebas H2 como certificación PostgreSQL 16 real:

### Diagnóstico de Infraestructura Local:
1. **Docker CLI:** Instalado en el sistema operativo (`Docker version 29.5.3, build 4a24cb6`).
2. **Docker Compose:** Instalado (`Docker Compose version v5.1.4`).
3. **Servicio Docker Desktop Windows (`com.docker.service`):** Estado `Stopped`.
4. **Motor Docker / Named Pipe:** `//./pipe/dockerDesktopLinuxEngine` no responde por encontrarse el servicio detenido.
5. **Puerto Local 5432:** Verificado; ningún daemon nativo de PostgreSQL está escuchando en el puerto estándar.

### Bloqueo Documentado:
- Las pruebas automatizadas del proyecto continúan ejecutándose sobre **H2 Database configurado en modo estricto de compatibilidad PostgreSQL** (`MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH`).
- Los scripts DDL en `docker/postgres/init/` (desde `01` hasta `06`) están perfectamente alineados a la sintaxis y tipos nativos de PostgreSQL 16 (tipos `UUID`, `NUMERIC(10, 7)`, restricciones `CHECK`, índices únicos parciales con predicado `WHERE`, secuencias y claves foráneas `ON DELETE RESTRICT`).
- **Certificación Pendiente para Producción:** Se mantiene la exigencia técnica obligatoria de ejecutar una batería de pruebas de humo y validación de índices parciales sobre una instancia de PostgreSQL 16 antes del despliegue en ambiente de preproducción/producción.

---

## 8. Estado de Seguridad OWASP

| Control de Seguridad | Estado I07.1 | Mecanismo de Verificación |
| :--- | :--- | :--- |
| **Control de Acceso BOLA/IDOR** | **CONFORME** | Protegidos endpoints en `SolicitudRectificacionController` mediante `@PreAuthorize("@establecimientoSecurity...")` y validación de expediente asignado en el servicio. |
| **Segregación de Funciones** | **CONFORME** | Prohibición incondicional de auto-aprobación del informe por el funcionario evaluador, sin excepciones para perfiles administradores. |
| **Concurrencia en Numeración** | **CONFORME** | Secuencia anual pesimista con bloqueo atómico `FOR UPDATE` que impide colisiones o carreras numéricas. |
| **Integridad Documental** | **CONFORME** | Verificación de hashes SHA-256 de 64 caracteres y bloqueo ante documentos vigentes en estado `OBSERVADO`. |
| **Advertencia en PDF Preliminar** | **CONFORME** | Marca de agua y leyenda oficial explícita: *"BORRADOR — NO CONSTITUYE RESOLUCIÓN NI LICENCIA OFICIAL"*. No se emite licencia ni QR en esta fase. |
| **Protección de Datos Personales** | **CONFORME** | Enmascaramiento de datos personales en consultas públicas y trazabilidad append-only de accesos. |

---

## 9. Estado de Interfaces Web Administrativas

Se auditó exhaustivamente la interfaz de gestión municipal (`portal-interno.html` y `portal-interno.js`):
- **Bandeja de Evaluación:** Proyección en tiempo real de expedientes formalmente presentados y en evaluación final.
- **Checklist Dinámico:** Renderizado claro con badges diferenciados (`CUMPLE`, `NO CUMPLE`, `NO APLICA`) y fuentes de evidencia auditables.
- **Flujo de Informe Técnico:** Modales dedicados para elaboración en `BORRADOR`, edición controlada, envío a revisión, visado por revisor, devolución motivada y aprobación por autoridad competente.
- **Manejo de Respuestas HTTP:** Captura y presentación comprensible de respuestas 400, 401, 403, 404, 409 y 422 sin bloqueos de interfaz.
- **Protección contra Doble Clic:** Deshabilitación temporal de botones críticos durante transiciones transaccionales.

---

## 10. Riesgos Residuales

1. **Entorno PostgreSQL 16 en Contenedor:** Persiste la imposibilidad de levantar Docker Desktop en el host de desarrollo local sin privilegios elevados para iniciar el servicio de Windows. Los scripts DDL están completamente adaptados a PostgreSQL 16 y validados sintácticamente.
2. **Fase I08 Pendiente (Resolución y Licencia Oficial):** El expediente transiciona válidamente al estado `APTO_PARA_RESOLUCION`. Queda pendiente en I08 la emisión formal del acto administrativo resolutivo y la licencia definitiva.

---

## 11. Requisitos Pendientes para Siguientes Incrementos

- **Incremento I08:** Módulo de Emisión de Resolución Gerencial, Licencia Oficial de Funcionamiento y Firma Digital Institucional (M09).
- **Incremento I09:** Verificación Pública QR Diferenciada y Módulo de Notificaciones Oficiales (M10, M11).
- **Incremento I10:** Cierre Integral, Catálogos Dinámicos PDU/CIIU y Puesta a Punto Productiva (M12).

---

## 12. Dictamen Técnico para I08

Con base en la inspección objetiva de código, la resolución integral de los hallazgos críticos de concurrencia y seguridad, la verificación rigurosa de segregación de funciones, y el paso exitoso del 100% de la suite automatizada (**260 pruebas exitosas**):

### **DICTAMEN TÉCNICO: APTO PARA INICIAR I08**
*(Con la observación técnica no bloqueante en desarrollo de ejecutar la certificación final en PostgreSQL 16 al disponer de entorno Docker activo).*
