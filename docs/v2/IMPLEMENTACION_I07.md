# Informe de Implementación Técnica — Incremento I07
## M08: Evaluación Administrativa, Informes Técnicos y Preparación de Resolución

**Municipalidad Provincial de Huamanga — Gerencia de Licencias y Autorizaciones**
**Fecha:** 14 de octubre de 2026
**Rama:** `develop/munihuamanga-v2`
**Estado:** IMPLEMENTADO Y VERIFICADO (Línea base: 250 pruebas exitosas)
**Documentos Rectores:** [ERS-MH-V2-001 v1.0], [DAS-MH-V2-001 v1.0], [ADR-001..ADR-008]

---

## 1. Resumen Ejecutivo y Objetivos del Incremento

El Incremento **I07** implementa de forma integral el módulo **M08 (Evaluación Administrativa, Informes Técnicos y Preparación de Resolución)** para la Gerencia de Licencias de Funcionamiento de la Municipalidad Provincial de Huamanga, así como la resolución formal del criterio **CA-08 (Rectificación de Coordenadas de Establecimientos)**.

### Objetivos Cumplidos:
1. **Bandeja Unificada de Evaluación:** Proyección transversal en tiempo real que consolida datos de expediente, titularidad, establecimiento, pagos SAT, zonificación, sustento ITSE y alertas de plazo legal (sin duplicación de datos).
2. **Checklist Administrativo Integral de 9 Criterios:** Evaluación automatizada de cumplimiento legal y documental con estados `CUMPLE`, `NO_CUMPLE`, `NO_APLICA`, descripción motivada y fuente de evidencia.
3. **Informe Administrativo Técnico Versionado:** Entidad `InformeAdministrativo` con UUID, versionado inmutable, evaluador, fundamentos, referencias documentales, conclusión (`FAVORABLE`, `DESFAVORABLE`, `REQUIERE_SUBSANACION`) y estados del ciclo de vida (`BORRADOR`, `EN_REVISION`, `OBSERVADO`, `VISADO`, `APROBADO`).
4. **Flujo de Revisión, Visado y Segregación de Funciones:** Separación de roles: el Evaluador elabora, el Revisor visa o devuelve con observaciones motivadas, y la Autoridad Competente aprueba. Se prohíbe que el mismo funcionario evalúe y apruebe su propio informe (segregación de funciones institucional).
5. **Máquina de Estados V2:** Incorporación del estado `APTO_PARA_RESOLUCION` al catálogo de `EstadoExpediente`. Se bloquea la conclusión favorable y la aprobación del informe si existen pagos pendientes, zonificación incompatible, observaciones no resueltas o falta de sustento ITSE.
6. **Generador PDF de Informe Preliminar:** Generación vectorial nativa con OpenPDF con marca de agua diagonal visible: *«BORRADOR — NO CONSTITUYE RESOLUCIÓN NI LICENCIA»*.
7. **Criterio CA-08 (Rectificación de Coordenadas):** Flujo de solicitud ciudadana de corrección de coordenadas geográficas erróneas, cálculo geodésico de desplazamiento (fórmula de Haversine), detección y rechazo de traslados físicos (>300 metros), preservación de coordenadas oficiales inalteradas hasta dictamen humano aprobado por evaluador municipal.
8. **Seguridad RBAC y BOLA/IDOR:** Protección de endpoints administrativos con control estricto de roles (`ROLE_EVALUADOR`, `ROLE_ADMIN`, `ROLE_CIUDADANO`) y propiedad de recursos.
9. **Persistencia SQL Aditiva:** Migración DDL `06-v2-evaluacion-administrativa-informes.sql` con claves foráneas, restricciones CHECK, índices únicos parciales e historial inmutable.
10. **Interfaz Web Interna:** Actualización de `portal-interno.html` y `portal-interno.js` con bandeja unificada, visualizador de checklist, modal de redacción de informe, gestión de visado y aprobación, e historial de decisiones.

---

## 2. Arquitectura y Componentes Implementados

### 2.1 Módulo `common-domain`
- **Enums incorporados:**
  - `EstadoChecklist`: `CUMPLE`, `NO_CUMPLE`, `NO_APLICA`.
  - `ConclusionInforme`: `FAVORABLE`, `DESFAVORABLE`, `REQUIERE_SUBSANACION`.
  - `EstadoInforme`: `BORRADOR`, `EN_REVISION`, `OBSERVADO`, `VISADO`, `APROBADO`.
  - `EstadoRectificacionUbicacion`: `PENDIENTE`, `APROBADA`, `RECHAZADA`.
  - Actualización de `EstadoExpediente`: adición de `APTO_PARA_RESOLUCION`.
- **DTOs incorporados:**
  - `ItemChecklistDto`, `ChecklistAdministrativoDto`.
  - `BandejaEvaluacionDto`, `FiltroBandejaEvaluacionDto`.
  - `CrearInformeAdministrativoDto`, `ActualizarInformeAdministrativoDto`, `RevisarInformeDto`, `AprobarInformeDto`, `HistorialDecisionInformeDto`, `InformeAdministrativoDto`.
  - `SolicitarRectificacionUbicacionDto`, `EvaluarRectificacionUbicacionDto`, `SolicitudRectificacionUbicacionDto`.

### 2.2 Módulo `servicio-expedientes`
- **Modelos de Persistencia (JPA):**
  - `InformeAdministrativo`: Entidad principal con control de versión actual (`esVersionActual`), UUID y campos auditables.
  - `HistorialInformeAdministrativo`: Registro inmutable (append-only) de cada acción y transición de estado del informe.
  - `SolicitudRectificacionUbicacion`: Registro de solicitudes de rectificación de coordenadas geográficas.
- **Repositorios Spring Data JPA:**
  - `InformeAdministrativoRepository`: Consultas con bloqueo pesimista `findByIdWithLock`, versión actual por expediente y conteo anual para numeración interna correlativa.
  - `HistorialInformeAdministrativoRepository`: Consulta de historial ordenado por fecha.
  - `SolicitudRectificacionUbicacionRepository`: Consultas por establecimiento y estado.
- **Servicios de Negocio:**
  - `ChecklistAdministrativoService`: Valida los 9 criterios normativos integrando los módulos de origen:
    1. Expediente formalmente presentado o en estado permitido.
    2. Titularidad y establecimiento consistentes.
    3. Declaración jurada Anexo 1 presente y válida.
    4. Versiones documentales vigentes e íntegras (SHA-256 verificado).
    5. Observaciones obligatorias resueltas (sin pendientes).
    6. Pago SAT validado manualmente por caja.
    7. Zonificación compatible y sustentada por reporte técnico.
    8. Sustento ITSE verificado administrativamente según riesgo y modalidad.
    9. Ausencia de impedimentos normativos o administrativos.
  - `BandejaEvaluacionService`: Realiza la proyección paginada en tiempo real sin desnormalizar campos sensibles, calculando alertas de plazo legal de 15 días hábiles.
  - `InformeAdministrativoService`: Gestiona el ciclo de vida de informes técnicos, validación de checklist previo a informe favorable, numeración secuencial anual (`INF-YYYY-XXXXX`), versionado inmutable v1..vN, flujo de visado y aprobación, comprobación de segregación de funciones, y transición del expediente a `APTO_PARA_RESOLUCION`.
  - `InformePdfGenerator`: Generador OpenPDF de informes técnicos preliminares con marca de agua transversal en todas las páginas (*«BORRADOR — NO CONSTITUYE RESOLUCIÓN NI LICENCIA»*).
  - `SolicitudRectificacionUbicacionService`: Implementa CA-08, calculando la distancia geodésica mediante Haversine, bloqueando traslados mayores a 300 metros, y manteniendo las coordenadas oficiales intactas hasta la aprobación motivada de un evaluador.
- **Controladores REST:**
  - `EvaluacionAdministrativaController` (`/api/evaluaciones-administrativas`): Bandeja unificada, checklist, ciclo de vida de informe (creación, edición, revisión, visado, devolución, aprobación, consulta, historial y descarga de PDF preliminar).
  - `SolicitudRectificacionController` (`/api/rectificaciones-ubicacion`): Solicitud ciudadana de rectificación y evaluación administrativa por funcionario competente.

---

## 3. Base de Datos y Persistencia SQL

Se creó el script aditivo:
`docker/postgres/init/06-v2-evaluacion-administrativa-informes.sql`

### Estructuras Definidas:
1. **Tabla `informes_administrativos`:**
   - Clave primaria UUID.
   - Foránea a `expedientes(id)` con `ON DELETE RESTRICT`.
   - Campos de versión, evaluador, revisor, aprobador, conclusiones y estados.
   - Restricciones CHECK para conclusiones y estados válidos.
   - Índice único parcial:
     `CREATE UNIQUE INDEX uk_informe_expediente_actual ON informes_administrativos(expediente_id) WHERE es_version_actual = TRUE;`
2. **Tabla `historial_informes_administrativos`:**
   - Registro inmutable de transiciones con fecha, actor, rol, acción, estado anterior/resultante y observaciones.
3. **Tabla `solicitudes_rectificacion_ubicacion`:**
   - Foráneas a `establecimientos(id)` y opcionalmente a `expedientes(id)`.
   - Coordenadas originales y propuestas con precisión decimal (10, 7).
   - Distancia aproximada en metros y flag `es_traslado_fisico`.
   - Restricción CHECK de estado (`PENDIENTE`, `APROBADA`, `RECHAZADA`).

---

## 4. Endpoints y Contratos API

| Método | Endpoint | Rol Requerido | Descripción |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/evaluaciones-administrativas/bandeja` | `EVALUADOR`, `ADMIN` | Bandeja unificada con filtros (estado expediente, pago SAT, zonificación, texto) y paginación |
| `GET` | `/api/evaluaciones-administrativas/expedientes/{id}/checklist` | `EVALUADOR`, `ADMIN` | Checklist integral de 9 puntos normativos en tiempo real |
| `POST` | `/api/evaluaciones-administrativas/informes` | `EVALUADOR`, `ADMIN` | Elaboración de informe técnico en estado BORRADOR |
| `PUT` | `/api/evaluaciones-administrativas/informes/{id}` | `EVALUADOR`, `ADMIN` | Edición de fundamentos y referencias de informe en BORRADOR u OBSERVADO |
| `POST` | `/api/evaluaciones-administrativas/informes/{id}/enviar-revision` | `EVALUADOR`, `ADMIN` | Transición de BORRADOR / OBSERVADO a EN_REVISION |
| `POST` | `/api/evaluaciones-administrativas/informes/{id}/devolver` | `EVALUADOR`, `ADMIN` | Devolución de informe a estado OBSERVADO con motivo obligatorio |
| `POST` | `/api/evaluaciones-administrativas/informes/{id}/visar` | `EVALUADOR`, `ADMIN` | Visado de informe por revisor competente (transición a VISADO) |
| `POST` | `/api/evaluaciones-administrativas/informes/{id}/aprobar` | `ADMIN` | Aprobación de informe por autoridad competente (segregación de funciones obligatoria); promueve expediente a `APTO_PARA_RESOLUCION` |
| `GET` | `/api/evaluaciones-administrativas/informes/{id}` | `EVALUADOR`, `ADMIN` | Consulta detallada del informe administrativo |
| `GET` | `/api/evaluaciones-administrativas/informes/{id}/historial` | `EVALUADOR`, `ADMIN` | Historial inmutable de revisiones y decisiones del informe |
| `GET` | `/api/evaluaciones-administrativas/informes/{id}/pdf-preliminar` | `EVALUADOR`, `ADMIN` | Descarga de PDF borrador con marca de agua institucional |
| `POST` | `/api/rectificaciones-ubicacion` | `CIUDADANO`, `ADMIN` | Solicitud ciudadana de rectificación de coordenadas geográficas |
| `POST` | `/api/rectificaciones-ubicacion/{id}/evaluar` | `EVALUADOR`, `ADMIN` | Dictamen motivado de rectificación (aprobación actualiza coordenadas oficiales; rechazo las mantiene intactas) |

---

## 5. Criterio CA-08: Rectificación de Ubicación Geográfica

- **Problema previo:** Las coordenadas geográficas eran campos editables sin trazabilidad, lo que permitía modificar arbitrariamente la ubicación del establecimiento o encubrir un traslado de local como corrección.
- **Solución implementada:**
  1. El ciudadano solo puede registrar una solicitud formal aportando coordenadas propuestas, justificación y documento sustentatorio opcional.
  2. El sistema calcula automáticamente la distancia entre el punto oficial y el punto propuesto mediante la fórmula de Haversine.
  3. Si la distancia supera los **300 metros**, el sistema marca la solicitud como presunto traslado físico (`es_traslado_fisico = true`).
  4. La ubicación oficial del establecimiento **permanece 100% inalterada** mientras la solicitud se encuentra en estado `PENDIENTE`.
  5. Un evaluador municipal autorizado examina la solicitud y emite un dictamen motivado (`APROBADA` o `RECHAZADA`).
  6. Si es aprobada, se actualizan las coordenadas del establecimiento en la base de datos y se registra la auditoría. Si es rechazada, no se altera ningún dato. Un traslado físico no puede aprobarse como simple rectificación de error geográfico.

---

## 6. Resultados de Pruebas Automatizadas

Se ejecutó la suite completa del proyecto con `mvn clean verify`:

```text
[INFO] Reactor Summary for MuniHuamanga - Gestión de Licencias de Funcionamiento 1.0.0-SNAPSHOT:
[INFO]
[INFO] MuniHuamanga - Gestión de Licencias de Funcionamiento SUCCESS [  0.097 s]
[INFO] Common Domain ...................................... SUCCESS [  3.900 s]
[INFO] MuniHuamanga - Monolito Modular .................... SUCCESS [ 41.656 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] Total time:  45.958 s
[INFO] Tests run: 250, Failures: 0, Errors: 0, Skipped: 0
```

### Desglose de Pruebas Nuevas del Incremento I07 (23 Pruebas):
1. **`ChecklistAdministrativoServiceTest` (4 pruebas unitarias):**
   - Expediente con requisitos completos califica como apto.
   - Bloqueo por pago SAT no validado.
   - Bloqueo por zonificación `NO_COMPATIBLE`.
   - Bloqueo por observaciones no subsanadas.
2. **`InformeAdministrativoServiceTest` (7 pruebas unitarias):**
   - Creación exitosa de informe favorable en estado BORRADOR.
   - Bloqueo de conclusión favorable cuando el checklist no está apto.
   - Flujo de revisión: envío a revisión y devolución con observaciones.
   - Visado conforme por revisor.
   - Bloqueo de aprobación por el mismo evaluador (segregación de funciones).
   - Aprobación formal por autoridad competente y promoción del expediente a `APTO_PARA_RESOLUCION`.
   - Bloqueo de doble aprobación.
   - Generación y descarga de PDF preliminar con marca de agua.
3. **`SolicitudRectificacionUbicacionServiceTest` (3 pruebas unitarias):**
   - Registro de solicitud en estado `PENDIENTE` sin alterar coordenadas oficiales (CA-08).
   - Aprobación de rectificación por evaluador actualizando coordenadas con éxito.
   - Rechazo de rectificación ante traslado físico del establecimiento (>300 metros).
4. **`EvaluacionAdministrativaIntegrationTest` (8 pruebas de integración MockMvc):**
   - Bandeja unificada con filtros para funcionario evaluador (HTTP 200).
   - Denegación de acceso a bandeja para rol ciudadano (HTTP 403 Forbidden).
   - Consulta de checklist integral por evaluador (HTTP 200).
   - Elaboración de informe técnico con requisitos completos (HTTP 201 Created).
   - Flujo integral de informe: BORRADOR -> VISADO -> APROBADO -> estado `APTO_PARA_RESOLUCION` (HTTP 200).
   - Descarga de PDF preliminar borrador (HTTP 200 `application/pdf`).
   - Solicitud ciudadana de rectificación de coordenadas (HTTP 201 Created).
   - Evaluación y aprobación de rectificación por evaluador municipal (HTTP 200 OK).

---

## 7. Matriz de Permisos y Segregación de Funciones

| Acción | Rol Mínimo Requerido | Segregación de Funciones |
| :--- | :--- | :--- |
| Consultar Bandeja Unificada | `ROLE_EVALUADOR`, `ROLE_ADMIN` | Exclusivo para funcionarios de Licencias |
| Consultar Checklist Integral | `ROLE_EVALUADOR`, `ROLE_ADMIN` | Exclusivo para funcionarios de Licencias |
| Elaborar / Editar Informe | `ROLE_EVALUADOR`, `ROLE_ADMIN` | El funcionario se registra como evaluador |
| Enviar a Revisión | `ROLE_EVALUADOR`, `ROLE_ADMIN` | El evaluador somete el borrador a control |
| Devolver / Observar Informe | `ROLE_EVALUADOR`, `ROLE_ADMIN` | Requiere motivo fundamentado obligatorio |
| Visar Informe | `ROLE_EVALUADOR`, `ROLE_ADMIN` | Revisor registra visado técnico |
| Aprobar Informe Administrativo | `ROLE_ADMIN` | **Prohibido auto-aprobar:** el aprobador debe ser un usuario distinto al evaluador |
| Descargar PDF Preliminar Borrador | `ROLE_EVALUADOR`, `ROLE_ADMIN` | Generado con watermark visible obligatorio |
| Solicitar Rectificación Coordenadas | `ROLE_CIUDADANO`, `ROLE_ADMIN` | Solo el titular o su operador registrado |
| Evaluar Rectificación Coordenadas | `ROLE_EVALUADOR`, `ROLE_ADMIN` | Dictamen motivado con verificación geodésica |

---

## 8. Riesgos Residuales y Estado de Certificación

1. **Certificación de PostgreSQL 16:**
   *Estado:* Continúa sin certificación en entorno real debido a que Docker local está inactivo en el host de desarrollo. Todas las pruebas automatizadas se ejecutaron y certificaron sobre H2 en modo compatibilidad PostgreSQL (`MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH`). La certificación en PostgreSQL 16 real queda programada para el entorno CI/CD o fase previa a producción.
2. **Firma Digital y Emisión Oficial (I08):**
   *Estado:* En I07 se detiene estrictamente el flujo en el estado `APTO_PARA_RESOLUCION`. No se generan certificados oficiales de licencia, códigos QR definitivos ni firmas simuladas. La emisión oficial corresponde al Incremento I08.

---

## 9. Dictamen Técnico de Cierre

El Incremento **I07** cumple a cabalidad con todos los requisitos del pliego técnico, la ERS y el DAS V2:
- La bandeja unificada permite a los funcionarios de Licencias evaluar expedientes de forma integral.
- El checklist asegura que ningún expediente sea aprobado favorablemente sin cumplir los 9 requisitos normativos.
- El ciclo de vida de informes técnicos garantiza inmutabilidad, trazabilidad y segregación de funciones.
- El criterio CA-08 queda cerrado con verificación geodésica y dictamen humano.
- La línea base de pruebas se elevó exitosamente a **250 pruebas con 100% de éxito**.

**Incremento I07 CONCLUIDO y listo para auditoría.** Se suspende cualquier trabajo de I08 conforme a las restricciones vigentes.
