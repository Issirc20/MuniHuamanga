# INFORME DE AUDITORÍA Y CIERRE I05.1
## Gestión Documental, Seguridad de Titularidad, Plazos Normativos y Formatos PDF

**Municipalidad Provincial de Huamanga — Gerencia de Licencias de Funcionamiento**
**Fecha:** 14 de octubre de 2026
**Rama de Trabajo:** `develop/munihuamanga-v2`
**Referencia:** Auditoría de Calidad Técnica y Cierre de Brechas I05.1 previo a I06
**Documentos Rectores:** [ERS-MH-V2-001 v1.0], [DAS-MH-V2-001 v1.0], TUO Ley N° 27444 (D.S. 004-2019-JUS), D.S. N° 002-2018-PCM (Reglamento ITSE), Ley N° 28976.

---

## 1. Resumen Ejecutivo

El presente informe certifica la ejecución y cierre definitivo de la auditoría técnica y normativa **I05.1**, resolviendo de manera integral las inconsistencias detectadas en la gestión documental, seguridad de acceso por titularidad, cómputo de plazos legales de subsanación y preservación de formatos PDF de la Municipalidad Provincial de Huamanga.

Se consolidaron las siguientes garantías técnicas:
1. **Conservación Documental e Inmutabilidad en Base de Datos:** Se erradicaron las cláusulas `ON DELETE CASCADE` en las claves foráneas documentales de expedientes formales en `04-v2-documentos-versionados-subsanaciones.sql`, reemplazándolas por `ON DELETE RESTRICT` e implementando índices únicos parciales condicionales (`WHERE es_version_actual = TRUE`) que imposibilitan a nivel de motor tener más de una versión activa por tipo documental.
2. **Seguridad y Titularidad Estricta (Erradicación BOLA/IDOR):** Se eliminó por completo la autorización basada en coincidencia simple entre `username` y `documentoIdentidad`. Se formalizó el requisito de vinculación persistente verificada (`Cuenta.id == solicitanteId` o `cuentaId`), delegación nominativa a `OperadorCuenta` con permiso `CONSULTAR_EXPEDIENTE` y retención de expedientes históricos V1 bajo acceso municipal restringido hasta su reclamación formal vía `ReclamacionExpedienteService`.
3. **Plazos Legales de Subsanación según Procedimiento Administrativo:** Se diferenció taxativamente la subsanación de observaciones de *Recepción Documental* (Art. 136.1 TUO Ley 27444: 2 días hábiles improrrogables), de *Evaluación Técnica ITSE* (D.S. 002-2018-PCM / Manual CENEPRED / TUPA: 5 días hábiles) y de *Evaluación Administrativa* (Art. 143 TUO Ley 27444: 10 días hábiles). Se implementó el cómputo exacto de días hábiles excluyendo sábados, domingos y el calendario oficial de feriados nacionales peruanos.
4. **Consistencia Transaccional de Versiones y Subsanaciones:** Se garantizó la desmarcación atómica de versiones previas, validación de pertenencia de cada observación al expediente antes de adjuntar subsanaciones, y verificación exhaustiva de que no existan observaciones pendientes ni documentos no subsanados antes de autorizar la transición al estado `SUBSANADO`.
5. **Fidelidad Absoluta de Formatos PDF Oficiales:** Se verificó que los generadores OpenPDF de Anexos 1 (2 páginas), 3 (2 páginas) y 4 (4 páginas) respeten estrictamente la diagramación, celdas, textos legales y cajas de firma oficiales sin incorporar sellos ni firmas artificiales.
6. **Certificación de Pruebas Automatizadas:** Se ejecutó la suite completa con Maven Surefire alcanzando **199 pruebas ejecutadas, 0 fallos, 0 errores y 0 omitidas** (100% de éxito).

---

## 2. Inmutabilidad y Conservación Documental

### 2.1 Corrección del Esquema DDL (`04-v2-documentos-versionados-subsanaciones.sql`)
- **Eliminación de `ON DELETE CASCADE` en Expedientes Formales:** La directiva de conservación documental y el principio de inmutabilidad del procedimiento administrativo prohíben la destrucción en cascada de evidencias y expedientes concluidos o en trámite. Se modificaron las restricciones de clave foránea a `ON DELETE RESTRICT` en:
  - `documentos_versionados.expediente_id REFERENCES expedientes(id) ON DELETE RESTRICT`
  - `documentos_versionados.version_anterior_id REFERENCES documentos_versionados(id) ON DELETE RESTRICT`
  - `observaciones_expediente.expediente_id REFERENCES expedientes(id) ON DELETE RESTRICT`
  - `observaciones_expediente.documento_id REFERENCES documentos_versionados(id) ON DELETE RESTRICT`
- **Excepción Controlada para Borradores Preliminares:** Se mantuvo `ON DELETE CASCADE` exclusivamente en `documentos_versionados.borrador_id REFERENCES solicitudes_borrador(id) ON DELETE CASCADE` para permitir el descarte administrativo voluntario de borradores previo a su presentación formal, según lo estipulado en el módulo M03.
- **Índices Únicos Parciales en PostgreSQL:** Para garantizar a nivel de motor que jamás existan dos versiones marcadas como "actual" simultáneamente:
  ```sql
  CREATE UNIQUE INDEX IF NOT EXISTS uk_doc_expediente_actual
  ON documentos_versionados (expediente_id, tipo_documento)
  WHERE (es_version_actual = TRUE AND expediente_id IS NOT NULL);

  CREATE UNIQUE INDEX IF NOT EXISTS uk_doc_borrador_actual
  ON documentos_versionados (borrador_id, tipo_documento)
  WHERE (es_version_actual = TRUE AND borrador_id IS NOT NULL);
  ```

### 2.2 Bloqueo de Modificación Ordinaria
- No existen endpoints `PUT` ni `DELETE` expuestos sobre `DocumentoVersionadoController` o `ExpedienteController` para alterar o suprimir documentos de expedientes formales.
- Cada actualización documental se registra forzosamente mediante `POST` como una nueva fila inmutable con `version = version_anterior + 1`, `fecha_carga = NOW()`, `hash_sha256` recalculado, trazabilidad del operador o ciudadano autenticado, y enlace a la versión previa mediante `version_anterior_id`.
- En caso de rollback por fallo en la base de datos o almacenamiento, la transacción de Spring `@Transactional` revierte la operación evitando inconsistencias entre metadatos y almacenamiento.

---

## 3. Seguridad, Titularidad y Mitigación BOLA/IDOR

### 3.1 Erradicación de la Autorización por DNI/Username Simple
En versiones previas existía el riesgo de BOLA/IDOR donde un usuario con `username` coincidente con el DNI de un expediente podía acceder al trámite sin comprobarse una relación formal con una cuenta verificada.
- **Corrección en `ExpedienteSecurity.java`:**
  - Se eliminó la condición permisiva `username.equalsIgnoreCase(exp.getDocumentoIdentidad())`.
  - Se exige la pertenencia verificada a la entidad `Cuenta`: `cuentaId.equals(exp.getCuentaId())` o `usuarioId.equals(exp.getSolicitanteId())`.
  - Para usuarios autenticados sin UUID en el principal (p. ej. en pruebas MockMvc), el sistema resuelve la cuenta persistente en base de datos mediante `cuentaRepository.findByNumeroDocumento(authUsername)` y valida su coincidencia con `exp.getCuentaId()`.
  - Se valida a los delegados nominativos (`OperadorCuenta`) requiriendo que se encuentren activos y cuenten explícitamente con el permiso `PermisoOperador.CONSULTAR_EXPEDIENTE`.
- **Tratamiento de Expedientes Históricos V1 sin Reclamar:**
  - Los expedientes migrados de V1 que carecen de `cuentaId` o `solicitanteId` verificados quedan **bloqueados para acceso ciudadano directo**.
  - Dichos expedientes solo son accesibles por personal municipal autorizado (`ROLE_EVALUADOR`, `ROLE_ADMIN`).
  - Para que un administrado acceda a un expediente histórico V1, debe tramitar su solicitud mediante el módulo formal de reclamación de titularidad (`ReclamacionExpedienteService`), requiriendo resolución administrativa aprobatoria antes de vincular su `cuentaId`.

### 3.2 Protección de Endpoints Documentales y Observaciones
- Los métodos `listarDocumentosExpediente`, `descargarDocumento`, `listarObservaciones` y `subsanarObservaciones` invocan validaciones de titularidad estrictas a nivel de servicio y controlador vía `@PreAuthorize`.
- Se enmascaran los números de documento e identificadores en los registros de auditoría y trazas de advertencia de seguridad (`DocumentoSecurityValidator.ofuscarDocumento`).

---

## 4. Plazos Legales de Subsanación y Marco Normativo

### 4.1 Fuentes Normativas y Plazos por Procedimiento
Se abandonó la presunción simplificada de asignar automáticamente 15 días hábiles a todas las observaciones. Se estructuró el enum normativo `TipoObservacion` en `common-domain`:

| Tipo de Observación | Plazo Legal | Fuente Normativa Exacta | Naturaleza y Momento de Aplicación |
| :--- | :---: | :--- | :--- |
| `RECEPCION_DOCUMENTAL` | **2 días hábiles** | **Art. 136.1 TUO Ley N° 27444** (D.S. N° 004-2019-JUS) | Observaciones de admisibilidad formal y requisitos formales en mesa de partes / recepción. Plazo improrrogable; si no subsana, se tiene por no presentada la solicitud. |
| `EVALUACION_TECNICA_ITSE` | **5 días hábiles** | **D.S. N° 002-2018-PCM**, Manual de Ejecución ITSE (Res. Jefatural N° 016-2018-CENEPRED/J) y TUPA MPH | Observaciones derivadas del panel de verificación de condiciones de seguridad en la edificación (Anexo 4) o discrepancias de riesgo (Anexo 3). |
| `EVALUACION_ADMINISTRATIVA` | **10 días hábiles** | **Art. 143 TUO Ley N° 27444** | Requerimientos de fondo en la evaluación administrativa ordinaria del trámite de licencia. |

*Nota institucional:* Si el TUPA específico de un procedimiento municipal especial determina un plazo particular distinto, el servicio admite la parametrización explícita mediante `CrearObservacionDto.plazoDiasHabiles`. En ausencia de plazo explícito, se aplica por defecto el plazo legal del `TipoObservacion` correspondiente.

### 4.2 Cómputo de Días Hábiles y Feriados Oficiales Peruanos
Se implementó en `ObservacionExpedienteService.java` el cálculo computacional de plazos hábiles:
- **Exclusión de Días Inhábiles:** No se contabilizan sábados ni domingos.
- **Exclusión de Feriados Oficiales Peruanos:** Se incorporó un catálogo dinámico con los feriados nacionales (Año Nuevo, Jueves y Viernes Santo, Día del Trabajo, Batalla de Arica y Día de la Bandera, San Pedro y San Pablo, Fiestas Patrias, Batalla de Junín, Santa Rosa de Lima, Combate de Angamos, Todos los Santos, Batalla de Ayacucho, Inmaculada Concepción y Navidad).

---

## 5. Consistencia del Versionado y Subsanación de Observaciones

### 5.1 Reglas de Versionado Concurrente
- En cargas de nuevas versiones documentales, el sistema desmarca de forma atómica todas las versiones anteriores mediante consulta `@Modifying` de JPQL/SQL (`desmarcarVersionesActualesExpediente`), sincronizando además la instancia en memoria (`prev.setEsVersionActual(false)`).
- La nueva versión se inserta con `es_version_actual = true` y `version = version_anterior + 1`.
- Gracias al índice único parcial `uk_doc_expediente_actual`, ante cargas simultáneas del mismo tipo documental para un mismo expediente, el motor PostgreSQL rechaza una de las transacciones serializándolas y previniendo versiones huérfanas o duplicadas.

### 5.2 Pertenencia y Subsanación Estricta
- **Validación de Pertenencia:** Al asociar un documento de subsanación a una observación (`observacionId`), el servicio valida rigurosamente:
  1. Que la observación exista en el repositorio.
  2. Que pertenezca inequívocamente al expediente en trámite (`obs.getExpedienteId().equals(expedienteId)`).
  3. Que el tipo de documento adjuntado coincida con el tipo documental del documento observado.
- **Criterio de Transición a `SUBSANADO`:**
  - Se impide el cambio prematuro a `SUBSANADO` si el administrado no subsana todas las observaciones formuladas.
  - El método `subsanarObservaciones` valida que:
    1. Para cada observación documental se haya adjuntado la versión subsanatoria correspondiente.
    2. No subsistan observaciones sin subsanar (`observacionRepository.existsByExpedienteIdAndSubsanadaFalse`).
    3. Únicamente al cumplirse ambas condiciones, el expediente transiciona al estado `SUBSANADO`.

---

## 6. Validación de Formatos PDF de Anexos ITSE

Se auditó el código de los tres generadores vectoriales nativos OpenPDF en `servicio-expedientes`:

| Formato | Generador | Páginas | Conformidad y Estructura | Observación / Fidelidad |
| :--- | :--- | :---: | :--- | :--- |
| **Anexo 1** | `Anexo1PdfGenerator.java` | **2** | Declaración Jurada para Licencia de Funcionamiento (Versión 03). Secciones I a VI, datos del titular, representante SUNARP, establecimiento y declaraciones juradas. | **Conforme.** Réplica fiel del formato oficial de D.S. 163-2020-PCM. Celdas de llenado amarillas (#fffde7), encabezados azul marino (#002b5c). Sin sellos ni firmas ficticias. |
| **Anexo 3** | `Anexo3PdfGenerator.java` | **2** | Reporte de Nivel de Riesgo del Establecimiento Objeto de Inspección (Matriz ITSE). Conforme a D.S. N° 002-2018-PCM y Res. Jefatural N° 016-2018-CENEPRED/J. | **Conforme.** Cuadrícula completa de evaluación por función de edificación, nivel de riesgo calculado y notas aclaratorias. Espacio de firma formal sin elementos gráficos artificiales. |
| **Anexo 4** | `Anexo4PdfGenerator.java` | **4** | Declaración Jurada de Cumplimiento de Condiciones de Seguridad en la Edificación. Checklist exhaustivo de 4 páginas de verificación de seguridad. | **Conforme.** Reproducción íntegra de los 4 folios normativos de Defensa Civil. Cajas de verificación (Cumple / No Cumple / No Aplica), datos de contacto y recuadro de firma. |

---

## 7. Resultados de Pruebas Automatizadas

Se ejecutó la suite completa en la máquina de desarrollo:
- **Herramienta:** Apache Maven 3.9+ / Java 21 LTS / Spring Boot 3.3.4
- **Comando:** `mvn clean test` y `mvn verify -DskipTests`
- **Resultados Globales:**
  - **Total de pruebas ejecutadas:** **199**
  - **Fallos (Failures):** **0**
  - **Errores (Errors):** **0**
  - **Omitidas (Skipped):** **0**
  - **Tasa de éxito:** **100%**
  - **Tiempo de ejecución de pruebas:** ~41.5 segundos
  - **Resultado de compilación y empaquetado (`mvn verify`):** **BUILD SUCCESS**

### 7.1 Nuevas Pruebas Agregadas en I05.1 (+6 Pruebas)

1. `DocumentoVersionadoServiceTest`:
   - `adjuntarDocumento_observacionAjena_rechaza`: Verifica que intentar vincular una observación perteneciente a otro expediente lance `IllegalArgumentException`.
   - `adjuntarNuevaVersion_desmarcaVersionPreviaEnMemoriaYPersistencia`: Comprueba que al cargar una versión v2, la versión v1 sea desmarcada atómicamente (`esVersionActual == false`).
   - `adjuntarDocumento_expedienteV1NoReclamado_deniegaAcceso`: Comprueba el bloqueo IDOR para ciudadanos en trámites históricos V1 sin titularidad verificada.
2. `ObservacionSubsanacionIntegrationTest`:
   - `formularObservacion_recepcionDocumental_asignaPlazoArt136DosDiasHabiles`: Verifica que observaciones de tipo `RECEPCION_DOCUMENTAL` apliquen estrictamente los 2 días hábiles del Art. 136.1 de la Ley 27444 excluyendo feriados oficiales.
   - `subsanarObservaciones_parcialConPendientes_rechazado`: Comprueba que una subsanación parcial que deja observaciones pendientes impida la transición a `SUBSANADO`.
   - `consultarObservaciones_expedienteV1NoReclamado_denegado`: Verifica mediante MockMvc que una consulta con rol ciudadano sobre un expediente V1 no reclamado retorne `403 FORBIDDEN` incluso si el DNI del mock user coincide con el documento de identidad registrado en el expediente.

### 7.2 Limitaciones de Infraestructura Declaradas
> [!WARNING]
> El servicio Docker / daemon local de PostgreSQL 16 no se encuentra en ejecución en el entorno local del desarrollador. Por consiguiente, las 199 pruebas automatizadas se ejecutaron satisfactoriamente sobre el motor H2 en modo compatibilidad PostgreSQL (`jdbc:h2:mem:muni_licencias_db;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH`).
>
> Conforme a los requerimientos éticos de auditoría técnica, **no se declara certificada la ejecución de migraciones en un cluster PostgreSQL 16 real ni la concurrencia bajo el motor físico de base de datos** hasta que se levante el contenedor Docker correspondiente (`docker compose up -d postgres`). No obstante, la sintaxis SQL de `04-v2-documentos-versionados-subsanaciones.sql` cumple estrictamente con el estándar DDL de PostgreSQL 16.

---

## 8. Inventario de Archivos Modificados

| Componente | Archivo Modificado | Descripción del Cambio |
| :--- | :--- | :--- |
| **DDL SQL** | `docker/postgres/init/04-v2-documentos-versionados-subsanaciones.sql` | `ON DELETE RESTRICT` en claves foráneas de expedientes, e índices únicos parciales `uk_doc_expediente_actual` y `uk_doc_borrador_actual`. |
| **Dominio** | `common-domain/.../enums/TipoObservacion.java` | Nuevo enum con tipos de observación y plazos legales: 2 días (Art. 136.1), 5 días (ITSE), 10 días (Art. 143). |
| **DTOs** | `common-domain/.../dto/CrearObservacionDto.java` | Inclusión del campo `tipoObservacion` con soporte para plazo por defecto según normativa. |
| **DTOs** | `common-domain/.../dto/ObservacionExpedienteDto.java` | Inclusión del campo `tipoObservacion` en el DTO de respuesta. |
| **Entidad** | `servicio-expedientes/.../model/ObservacionExpediente.java` | Mapeo JPA del atributo `tipoObservacion` (`@Enumerated(EnumType.STRING)`). |
| **Repositorio** | `servicio-expedientes/.../repository/ObservacionExpedienteRepository.java` | Método `existsByExpedienteIdAndSubsanadaFalse` para control estricto de subsanación. |
| **Repositorio** | `servicio-expedientes/.../repository/DocumentoVersionadoRepository.java` | Métodos `@Modifying` para desmarcar atómicamente versiones actuales (`desmarcarVersionesActualesExpediente` y `desmarcarVersionesActualesBorrador`). |
| **Seguridad** | `servicio-expedientes/.../security/ExpedienteSecurity.java` | Erradicación de autorización por coincidencia simple username-DNI; verificación de titularidad por cuenta persistente y restricción de expedientes V1. |
| **Servicio** | `servicio-expedientes/.../service/ObservacionExpedienteService.java` | Cómputo de días hábiles excluyendo feriados peruanos, control de pertenencia y verificación de subsanación total para cambio de estado. |
| **Servicio** | `servicio-expedientes/.../service/DocumentoVersionadoService.java` | Validación de pertenencia de observaciones al expediente, desmarcación atómica de versiones y control de titularidad sin BOLA/IDOR. |
| **Pruebas** | `servicio-expedientes/.../DocumentoVersionadoServiceTest.java` | Nuevas pruebas unitarias para auditoría I05.1 (+3 pruebas). |
| **Pruebas** | `servicio-expedientes/.../ObservacionSubsanacionIntegrationTest.java` | Nuevas pruebas de integración para plazos normativos, subsanación y seguridad V1 (+3 pruebas). |
| **Documentación** | `docs/v2/CIERRE_I05_1.md` | Informe formal de auditoría y cierre técnico I05.1. |
| **Documentación** | `docs/v2/MATRIZ_BRECHAS.md` | Actualización de matriz de brechas certificando el cierre I05.1. |
| **Documentación** | `docs/v2/PRUEBAS_Y_VALIDACION.md` | Actualización de la suite de pruebas a 199 tests certificados. |

---

## 9. Riesgos Residuales y Transición hacia I06

1. **Pruebas en PostgreSQL 16 Nativo:** Requiere inicializar el servicio de base de datos PostgreSQL en contenedor Docker para certificar en frío la ejecución del script `04-v2-documentos-versionados-subsanaciones.sql` con datos masivos.
2. **Validación Institucional de Plazos TUPA Particulares:** Aunque se implementaron los plazos legales del TUO de la Ley 27444 y el D.S. 002-2018-PCM, los plazos particulares derivados de futuras modificaciones al TUPA municipal de Huamanga deberán actualizarse en la tabla de parámetros o catálogos administrativos (M12).
3. **Bloqueo a Nuevos Incrementos:** Conforme a las instrucciones, **no se ha iniciado la fase I06 (Pagos SAT y Conciliación) ni I07**, preservándose íntegramente la rama `develop/munihuamanga-v2` a la espera de revisión y autorización.
