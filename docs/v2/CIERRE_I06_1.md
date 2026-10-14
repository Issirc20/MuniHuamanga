# INFORME DE AUDITORÍA Y CIERRE I06.1
## Pagos SAT, Zonificación, Seguridad y Consistencia Administrativa

**Municipalidad Provincial de Huamanga — Gerencia de Licencias de Funcionamiento**
**Fecha:** 14 de octubre de 2026
**Rama de Trabajo:** `develop/munihuamanga-v2`
**Referencia:** Auditoría de Calidad Técnica, Seguridad y Consistencia Administrativa I06.1 previo a I07
**Documentos Rectores:** [ERS-MH-V2-001 v1.0], [DAS-MH-V2-001 v1.0], TUO Ley N° 27444 (D.S. 004-2019-JUS), Ley Marco de Licencia de Funcionamiento (Ley N° 28976 y modificatorias), D.S. N° 002-2018-PCM (Reglamento ITSE).

---

## 1. Resumen Ejecutivo

El presente informe certifica la ejecución y cierre exhaustivo de la auditoría técnica, funcional y de seguridad **I06.1**, abordando los riesgos residuales y corrigiendo deficiencias críticas detectadas en los módulos **M05 (Validación Manual de Pagos SAT y Tasas TUPA)** y **M07 (Evaluación Técnica de Zonificación y Compatibilidad de Uso)**.

Se han inspeccionado minuciosamente las implementaciones de código, modelos relacionales, contratos DTO, controladores REST, componentes de frontend administrativo (`portal-interno.html`, `portal-interno.js`) y la suite automatizada de pruebas, implementando correcciones estructurales sin comprometer los incrementos I01 a I06.

### Principales Logros de la Auditoría I06.1:
1. **Independencia Estricta de Estados Administrativos:** Se erradicó la transición colateral indebida que promovía automáticamente el expediente formal al estado `DOCUMENTOS_VALIDADOS` tras la validación manual de un comprobante de pago. La validación financiera ahora se circunscribe de forma estricta a su competencia, manteniendo el expediente en su estado administrativo real (`PRESENTADO`).
2. **Normalización, Integridad y Concurrencia de Comprobantes SAT:** Se incorporó un algoritmo de normalización canónica de comprobantes, validación estricta de formato por expresión regular, rechazo de fechas declaradas futuras y montos no positivos, verificación de cobertura de tasa TUPA vigente, bloqueo de concurrencia pesimista (`@Lock(LockModeType.PESSIMISTIC_WRITE)`), e inmutabilidad total de decisiones previas (bloqueo de doble validación y de validación de pagos rechazados).
3. **Consistencia y Competencia en Zonificación:** Se blindó la evaluación de zonificación exigiendo verificación de correspondencia del establecimiento respecto al expediente, reporte técnico institucional obligatorio (mínimo 8 caracteres), motivación sustantiva obligatoria (mínimo 15 caracteres), registro del documento versionado sustentatorio (`documentoSustentatorioId`) e historial acumulativo append-only.
4. **Resiliencia y Manejo de Errores en Frontend Administrativo:** Se dotó a `portal-interno.js` de un parser comprensivo de errores HTTP para códigos 400, 401, 403, 409 y 422, presentando mensajes claros y legibles al funcionario municipal y evitando fallos no controlados en navegador.
5. **Certificación de la Suite de Pruebas:** Se alcanzaron **227 pruebas automatizadas exitosas (0 errores, 0 fallos, 0 omitidas)** en el reactor Maven, incorporando pruebas específicas de concurrencia, doble validación, pagos rechazados, colisión de comprobantes y validación de competencia.

---

## 2. Independencia de Estados Administrativos

### 2.1 Diagnóstico de la Brecha Previa (I06)
En la implementación inicial del incremento I06, el método `validarPagoManual` de `PagoExpedienteService` ejecutaba de manera encadenada:
```java
// CÓDIGO DEFECTUOSO PREVIO (I06):
exp.setEstado(EstadoExpediente.DOCUMENTOS_VALIDADOS);
```
Esta mutación colateral violentaba el principio de separación funcional y competencias administrativas del procedimiento municipal:
- **Invasión de Competencia:** El cajero o personal de tesorería encargado de cotejar el comprobante SAT terminaba otorgando indebidamente la validación documental y técnica del trámite.
- **Riesgo Administrativo:** Un administrado que adjuntase documentos fraudulentos o incompletos obtenía un expediente con documentos formalmente validados con el mero hecho de que su tasa de pago fuera aprobada en caja.
- **Aprobaciones Indebidas:** En flujos con clasificación de riesgo bajo, dicha transición indebida reducía los controles posteriores, generando el riesgo inminente de aprobación prematura de licencias.

### 2.2 Corrección Arquitectónica Implementada
Se desacopló completamente la máquina de estados del expediente de la validación del comprobante de pago:
1. `PagoExpedienteService.validarPagoManual` ahora actualiza de manera aislada los campos financieros del expediente:
   - `exp.setVoucherId(pago.getCodigoComprobanteNormalizado())`
   - `exp.setNumeroOperacionSat(pago.getNumeroOperacion())`
   - `exp.setFechaPagoSat(pago.getFechaValidacion())`
2. **El estado del expediente permanece inalterado (`PRESENTADO`).** La transición a `DOCUMENTOS_VALIDADOS` solo puede ser dictaminada por la mesa de partes o evaluador documental competente mediante el servicio formal de revisión documental (`ExpedienteService` / `DocumentoVersionadoService`).
3. Se auditan de forma independiente el evento de validación del comprobante de pago y cualquier transición de estado del expediente mediante `AuditoriaService`.

---

## 3. Integridad de Comprobantes SAT

### 3.1 Algoritmo de Normalización y Unicidad Canónica
Para impedir la evasión de restricciones de unicidad mediante espacios, tabulaciones o caracteres especiales superfluos:
- Se implementó el método `normalizarCodigoComprobante`:
  ```java
  public String normalizarCodigoComprobante(String raw) {
      if (raw == null) return null;
      String cleaned = raw.trim().toUpperCase()
              .replaceAll("[\\s\\t\\r\\n]+", "-")
              .replaceAll("-{2,}", "-");
      return cleaned;
  }
  ```
- Se validó el formato canónico mediante la expresión regular `^[A-Z0-9][A-Z0-9_/-]{1,48}[A-Z0-9]$` (longitud entre 3 y 50 caracteres alfanuméricos con guiones o barras intermedias).
- Se aplica control de colisión antes del registro: no se admite declarar un código de comprobante cuyo formato normalizado ya exista registrado en estado `PENDIENTE` o `VALIDADO_MANUALMENTE`.

### 3.2 Reglas de Validación de Integridad Financiera
- **Fecha Declarada:** Se rechaza cualquier declaración donde `fechaDeclarada` sea posterior a la fecha actual (`LocalDate.now()`).
- **Monto Declarado:** Debe ser estrictamente positivo (`montoDeclarado > 0.00`).
- **Suficiencia de Tasa TUPA:** En `validarPagoManual`, se verifica que el `montoValidado` cubra como mínimo el importe legal de la tasa TUPA asociada al nivel de riesgo del expediente (`pago.getMontoTupaRequerido()`). No se autoriza la validación manual si el monto es inferior.
- **Sustento de Cotejo Manual:** Se exige que el funcionario consigne una justificación obligatoria con un mínimo de 10 caracteres que deje constancia de la verificación física o digital del comprobante SAT.

### 3.3 Concurrencia y Bloqueo de Decisiones Contradictorias
- **Bloqueo Pesimista:** Se agregó en `PagoExpedienteRepository` la consulta con candado exclusivo:
  ```java
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT p FROM PagoExpediente p WHERE p.id = :id")
  Optional<PagoExpediente> findByIdWithLock(@Param("id") UUID id);
  ```
- **Prevención de Doble Validación:** Si el comprobante ya cuenta con `estado == VALIDADO_MANUALMENTE`, cualquier invocación concurrente o subsecuente de validación es rechazada con `IllegalStateException` ("El comprobante ya se encuentra validado").
- **Protección de Pagos Rechazados:** No se permite validar un comprobante que fue previamente `RECHAZADO` ("No se puede validar un pago en estado: RECHAZADO"). Si el administrado desea aportar un nuevo comprobante, debe registrar una nueva declaración de pago.
- **Protección de Pagos Validados:** No se permite observar ni rechazar un comprobante que ya fue validado favorablemente.
- **Historial Inmutable:** Cada decisión administrativa genera un registro inmutable en `HistorialDecisionPago` con tipo de decisión, fecha exacta, funcionario responsable, monto validado y observaciones.
- **Ausencia de Simulación Automática:** Se ratifica que no existen adaptadores simulados de conexión con SAT; todo el flujo se sustenta en cotejo manual fidedigno con firma del actor.

---

## 4. Zonificación y Competencia Administrativa

### 4.1 Validación de Pertenencia y Consistencia Territorial
En `EvaluacionZonificacionService`:
- Se valida obligatoriamente que el `establecimientoId` proporcionado en el DTO coincida exactamente con el establecimiento asignado al expediente (`exp.getEstablecimientoId()`). Ante cualquier discrepancia, se aborta la operación con `IllegalArgumentException`.

### 4.2 Exigencia de Sustento y Motivación
De acuerdo con las garantías del debido procedimiento administrativo (Art. 6 TUO Ley N° 27444):
- Para resultados `COMPATIBLE` o `NO_COMPATIBLE`:
  - **Informe Técnico:** Se exige consignar obligatoriamente la identificación del informe institucional (p. ej., `Informe Técnico N° 088-2026-MPH-GDUR`) con un mínimo de 8 caracteres.
  - **Motivación:** Se exige detallar los fundamentos técnicos y urbanísticos con un mínimo de 15 caracteres.
- **Documento Sustentatorio:** Se incorporó el campo `documentoSustentatorioId` referenciando la tabla `documentos_versionados` para anexar digitalmente el PDF del informe pericial emitido por la Subgerencia de Acondicionamiento Territorial y Catastro.

### 4.3 Competencia y Dictamen Humano Obligatorio
- La evaluación requiere autenticación de personal con roles `ROLE_EVALUADOR` o `ROLE_ADMIN`.
- Se prohibió cualquier mecanismo algorítmico o heurístico que califique la compatibilidad basándose únicamente en coordenadas geográficas, códigos CIIU o consultas directas al PDU. Todo dictamen requiere firma, informe y motivación humana de un inspector o arquitecto municipal habilitado.
- Los dictámenes son append-only: cada evaluación se registra como una nueva fila en `evaluaciones_zonificacion`, preservando el histórico completo sin sobrescribir evaluaciones precedentes.

---

## 5. Auditoría de Interfaces Administrativas (Portal Interno)

### 5.1 Verificación de `portal-interno.html` y `portal-interno.js`
Se inspeccionaron los flujos de la interfaz administrativa para funcionarios municipales:
- **Bandeja de Pagos:** Carga reactiva de comprobantes pendientes y filtrado por estado.
- **Modal de Validación:** Captura de número de operación SAT, fecha de acreditación, monto validado y justificación de cotejo (mínimo 10 caracteres).
- **Modal de Observación y Rechazo:** Captura de motivo formal de observación o rechazo.
- **Modal de Zonificación:** Captura de resultado (`COMPATIBLE`, `NO_COMPATIBLE`, `REQUIERE_INSPECCION_CAMPO`), número de informe técnico y motivación detallada.

### 5.2 Manejo Exhaustivo de Códigos de Error HTTP
Se implementó en `portal-interno.js` la función centralizada `parsearErrorHttp(res, operacion)` que garantiza que ningún error de red o de negocio se muestre como un objeto genérico o falle silenciosamente:
- **HTTP 400 (Bad Request):** Extrae los mensajes de validación específicos (p. ej., sustento insuficiente, establecimiento no coincide, fecha futura).
- **HTTP 401 (Unauthorized):** Notifica expiración de sesión y redirige al formulario de inicio de sesión.
- **HTTP 403 (Forbidden):** Notifica al operador que carece de los privilegios municipales requeridos para la acción.
- **HTTP 409 (Conflict):** Alerta sobre colisión por comprobante duplicado o concurrencia administrativa.
- **HTTP 422 (Unprocessable Entity):** Explica inconsistencias en el estado administrativo del trámite.

---

## 6. Estado del Entorno de Base de Datos y Certificación PostgreSQL

### 6.1 Limitación de Infraestructura Local (Docker Desktop)
Se intentó validar los scripts de base de datos (`01` a `05`) directamente sobre un contenedor PostgreSQL 16 con Docker Desktop. Sin embargo, el daemon de Docker no se encuentra activo en el entorno local de ejecución:
```
error during connect: Get "http://%2F%2F.%2Fpipe%2FdockerDesktopLinuxEngine/v1.47/containers/json":
open //./pipe/dockerDesktopLinuxEngine: The system cannot find the file specified.
```
Asimismo, no existe una instancia nativa de PostgreSQL ejecutándose en el puerto 5432.

### 6.2 Declaración Formal de No Certificación PostgreSQL
> [!WARNING]
> **REGISTRO DE BLOQUEO DE INFRAESTRUCTURA:**
> Debido a la indisponibilidad de Docker Desktop y del servicio local PostgreSQL 16 en el entorno de desarrollo, **NO SE EMITE CERTIFICACIÓN FORMAL DE MOTOR POSTGRESQL REAL** en este informe.
> Las pruebas automatizadas fueron ejecutadas satisfactoriamente utilizando el motor H2 en modo de compatibilidad PostgreSQL (`MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH`).
> Se dispone de los scripts DDL actualizados y saneados (`01-v1-schema.sql` a `05-v2-pagos-sat-zonificacion.sql`), los cuales deben ser ejecutados en un entorno de integración continua (CI/CD) con PostgreSQL 16 previo a cualquier despliegue en ambiente productivo.

---

## 7. Resultados de las Pruebas Automatizadas

Se ejecutó la suite completa del proyecto mediante Maven Surefire:
- **Total de Pruebas Ejecutadas:** 227
- **Fallos (Failures):** 0
- **Errores (Errors):** 0
- **Omitidas (Skipped):** 0
- **Tasa de Éxito:** 100.00%
- **Tiempo de Ejecución:** ~45.88 segundos

### 7.1 Distribución del Incremento de Pruebas I06.1
| Módulo / Componente Evaluado | Pruebas I06 | Pruebas I06.1 | Incremento | Estado |
| :--- | :---: | :---: | :---: | :---: |
| Pagos SAT y Tasa TUPA (`PagoExpedienteServiceTest`) | 6 | 10 | +4 | Exitoso |
| Zonificación y Competencia (`EvaluacionZonificacionServiceTest`) | 4 | 6 | +2 | Exitoso |
| Integración REST Pagos y Zonificación (`PagoZonificacionIntegrationTest`) | 9 | 12 | +3 | Exitoso |
| Suites Documentales, Seguridad, Borradores y Núcleo V1/V2 | 199 | 199 | 0 | Exitoso |
| **TOTAL GENERAL** | **218** | **227** | **+9** | **100% OK** |

### 7.2 Casos de Seguridad y Concurrencia Incorporados
1. `validarPagoManual_pagoYaValidado_lanzaExcepcion`: Verifica que un comprobante en `VALIDADO_MANUALMENTE` rechace una segunda validación.
2. `validarPagoManual_pagoRechazado_lanzaExcepcion`: Verifica que no sea posible validar un comprobante que fue previamente rechazado.
3. `declararPago_codigoNormalizadoExiste_lanzaExcepcion`: Verifica que el intento de registrar comprobantes equivalentes por espacios o mayúsculas sea detectado y bloqueado.
4. `validarPagoManual_montoInferiorATupa_lanzaExcepcion`: Verifica el bloqueo cuando el monto validado no cubre la tasa oficial del procedimiento.
5. `registrarEvaluacion_establecimientoInconsistente_lanzaExcepcion`: Verifica que la evaluación de zonificación pertenezca forzosamente al establecimiento del expediente.
6. `registrarEvaluacion_sustentoInsuficiente_lanzaExcepcion`: Verifica que informes técnicos de menos de 8 caracteres o motivaciones de menos de 15 caracteres sean rechazados.
7. `validarPago_dobleValidacion_retornaError400`: Prueba de integración MockMvc que valida el rechazo ante solicitudes duplicadas.
8. `validarPago_pagoRechazado_retornaError400`: Prueba de integración MockMvc que verifica la respuesta HTTP 400 ante pagos rechazados.
9. `declararPago_codigoNormalizado_exitoso`: Prueba de integración MockMvc que comprueba el almacenamiento canónico normalizado.

---

## 8. Dictamen Final y Recomendaciones para I07

### Dictamen Técnico
> [!IMPORTANT]
> **DICTAMEN: APTO PARA INICIO DEL INCREMENTO I07.**
>
> Los riesgos residuales de consistencia administrativa, independencia de estados, integridad de comprobantes SAT y evaluación de zonificación han sido saneados en su totalidad. El código fuente cumple con los estándares de arquitectura limpia, inmutabilidad de decisiones y control de concurrencia requeridos.

### Riesgos Residuales y Recomendaciones Operativas para I07:
1. **Validación en PostgreSQL 16 Real:** Se debe aprovisionar un pipeline de CI con runner Linux y servicio PostgreSQL 16 para validar formalmente la ejecución de los scripts DDL `01` a `05` antes de promover a la rama `main`.
2. **Fase I07 (Inspecciones Técnicas de Seguridad en Edificaciones - ITSE):** Al iniciar I07, se deberá conectar el flujo de zonificación `COMPATIBLE` con la habilitación del panel de programación y registro de actas ITSE, asegurando que ningún expediente con zonificación `NO_COMPATIBLE` pueda ser programado para inspección de seguridad.
3. **Mantenimiento de Independencia:** Se deberá continuar con la política de no modificar el estado formal del expediente hasta la emisión del informe técnico ITSE final y la resolución de subgerencia correspondiente.
