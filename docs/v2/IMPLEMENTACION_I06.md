# INFORME TÉCNICO DE IMPLEMENTACIÓN — INCREMENTO I06
## Validación Manual SAT y Evaluación de Zonificación

**Municipalidad Provincial de Huamanga — Gerencia de Licencias y Autorizaciones Comerciales**
**Fecha:** 14 de octubre de 2026
**Rama:** `develop/munihuamanga-v2`
**Estado:** Concluido y Verificado — Pendiente de Auditoría Técnica

---

## 1. Resumen Ejecutivo

El Incremento **I06** formaliza dos componentes administrativos esenciales para la tramitación legal de licencias de funcionamiento bajo el marco de la **Ley N° 28976** (Ley Marco de Licencia de Funcionamiento), el **TUO de la Ley N° 27444** y la **Ordenanza Municipal N° 018-2024-MPH**:

1. **Módulo Administrativo de Pagos SAT (M05):**
   - Recepción de comprobantes declarados por los administrados (`PENDIENTE_VALIDACION`).
   - Criterio administrativo estricto: la carga del comprobante/voucher **no constituye pago acreditado ni extingue la tasa municipal**.
   - Validación manual obligatoria por funcionarios autorizados (`ROLE_CAJERO`, `ROLE_ADMIN`) tras cotejo de autenticidad y coincidencia contra la tasa TUPA oficial calculada.
   - Prohibición expresa de integración automática falsa con el SAT.
   - Restricción única de comprobante (`uk_pago_codigo_comprobante`) que impide vincular un mismo voucher a múltiples expedientes.
   - Auditoría inmutable de decisiones (`HistorialDecisionPago`) con registro de motivo, usuario y marca de tiempo.
   - Regla de no aprobación automática: la validación manual del pago **no cambia el expediente a APROBADO**, evitando la elusión de la evaluación técnica e ITSE.

2. **Módulo de Evaluación de Zonificación y Compatibilidad de Giro (M07):**
   - Registro administrativo de evaluación emitido exclusivamente por evaluador humano competente (`ROLE_EVALUADOR`, `ROLE_ADMIN`).
   - Evaluación motivada basada en el Plan de Desarrollo Urbano (PDU) vigente de Huamanga y reporte técnico oficial (`Informe Técnico / Dictamen GDUR`).
   - Estados normativos: `PENDIENTE_EVALUACION`, `EN_EVALUACION`, `COMPATIBLE`, `NO_COMPATIBLE`, `REQUIERE_SUBSANACION`.
   - Prohibición de emitir certificados automáticos de zonificación o dictámenes simulados basados exclusivamente en coordenadas o códigos CIIU.
   - Protección contra IDOR/BOLA: los administrados no pueden alterar el dictamen ni acceder a expedientes ajenos.

---

## 2. Arquitectura de Dominio y Persistencia

### 2.1 Modelo Entidad-Relación (DDL I06)

Se implementó el script de migración aditiva versionada [05-v2-pagos-sat-zonificacion.sql](file:///d:/ArqSoftware/MuniHuamanga/docker/postgres/init/05-v2-pagos-sat-zonificacion.sql), el cual respeta el principio de inmutabilidad y retención histórica con integridad referencial estricta (`ON DELETE RESTRICT`):

```sql
-- TABLA: pagos_expediente
CREATE TABLE IF NOT EXISTS pagos_expediente (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    expediente_id UUID NOT NULL,
    codigo_comprobante VARCHAR(100) NOT NULL,
    fecha_declarada TIMESTAMP WITH TIME ZONE NOT NULL,
    monto_declarado NUMERIC(12, 2) NOT NULL,
    documento_id UUID,
    observaciones_administrado VARCHAR(500),
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE_VALIDACION',
    validador_id UUID,
    validador_nombre VARCHAR(150),
    fecha_validacion TIMESTAMP WITH TIME ZONE,
    motivo_observacion_rechazo VARCHAR(500),
    numero_operacion_sat VARCHAR(100),
    fecha_pago_real TIMESTAMP WITH TIME ZONE,
    monto_validado NUMERIC(12, 2),
    tasa_tupa_esperada NUMERIC(12, 2),
    fecha_registro TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pago_expediente FOREIGN KEY (expediente_id) REFERENCES expedientes(id) ON DELETE RESTRICT,
    CONSTRAINT uk_pago_codigo_comprobante UNIQUE (codigo_comprobante),
    CONSTRAINT chk_monto_declarado_positivo CHECK (monto_declarado > 0),
    CONSTRAINT chk_estado_pago_valido CHECK (estado IN ('PENDIENTE_VALIDACION', 'EN_REVISION', 'VALIDADO_MANUALMENTE', 'OBSERVADO', 'RECHAZADO'))
);

-- TABLA: historial_decisiones_pago (Inmutable)
CREATE TABLE IF NOT EXISTS historial_decisiones_pago (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pago_id UUID NOT NULL,
    estado_anterior VARCHAR(30) NOT NULL,
    estado_nuevo VARCHAR(30) NOT NULL,
    usuario_id UUID NOT NULL,
    usuario_nombre VARCHAR(150) NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    fecha_decision TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_historial_pago FOREIGN KEY (pago_id) REFERENCES pagos_expediente(id) ON DELETE RESTRICT
);

-- TABLA: evaluaciones_zonificacion
CREATE TABLE IF NOT EXISTS evaluaciones_zonificacion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    expediente_id UUID NOT NULL,
    establecimiento_id UUID,
    direccion_declarada VARCHAR(255) NOT NULL,
    ciiu_codigo VARCHAR(10) NOT NULL,
    giro_declarado VARCHAR(255) NOT NULL,
    zonificacion_pdu VARCHAR(50) NOT NULL,
    fuente_informe VARCHAR(255) NOT NULL,
    resultado VARCHAR(30) NOT NULL,
    evaluador_id UUID NOT NULL,
    evaluador_nombre VARCHAR(150) NOT NULL,
    observaciones VARCHAR(1000),
    fecha_evaluacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_zonificacion_expediente FOREIGN KEY (expediente_id) REFERENCES expedientes(id) ON DELETE RESTRICT,
    CONSTRAINT fk_zonificacion_establecimiento FOREIGN KEY (establecimiento_id) REFERENCES establecimientos(id) ON DELETE RESTRICT,
    CONSTRAINT chk_resultado_zonificacion CHECK (resultado IN ('PENDIENTE_EVALUACION', 'EN_EVALUACION', 'COMPATIBLE', 'NO_COMPATIBLE', 'REQUIERE_SUBSANACION'))
);
```

### 2.2 Entidades Java JPA

- [PagoExpediente.java](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/pagos/model/PagoExpediente.java): Mapeo ORM con validación de montos positivos y estados controlados.
- [HistorialDecisionPago.java](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/pagos/model/HistorialDecisionPago.java): Mapeo ORM inmutable de trazabilidad de cambios de estado.
- [EvaluacionZonificacion.java](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/zonificacion/model/EvaluacionZonificacion.java): Mapeo ORM de evaluaciones técnicas de compatibilidad urbana.

---

## 3. Endpoints REST y Contratos JSON

### 3.1 Módulo de Pagos SAT (`/api/pagos`)

| Método | Endpoint | Roles Permitidos | Propósito |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/pagos/declarar` | `ROLE_CIUDADANO`, `ROLE_ADMIN` | Declarar voucher por administrado (`PENDIENTE_VALIDACION`) |
| `GET` | `/api/pagos/pendientes` | `ROLE_CAJERO`, `ROLE_ADMIN` | Consultar bandeja general de pagos pendientes |
| `GET` | `/api/pagos/expediente/{expedienteId}` | Autenticado (Contextual) | Listar pagos asociados a un expediente (con filtro BOLA/IDOR) |
| `GET` | `/api/pagos/historial/{pagoId}` | `ROLE_CAJERO`, `ROLE_ADMIN` | Consultar trazabilidad inmutable de un pago |
| `POST` | `/api/pagos/{pagoId}/validar` | `ROLE_CAJERO`, `ROLE_ADMIN` | Validación manual con evidencia y número SAT |
| `POST` | `/api/pagos/{pagoId}/observar` | `ROLE_CAJERO`, `ROLE_ADMIN` | Observar comprobante con motivo registrado |
| `POST` | `/api/pagos/{pagoId}/rechazar` | `ROLE_CAJERO`, `ROLE_ADMIN` | Rechazar comprobante nulo o falso |

#### Contrato JSON — Declarar Pago:
```json
{
  "expedienteId": "91d318d4-ad05-4735-ba2b-33ea1668969d",
  "codigoComprobante": "SAT-2026-998877",
  "fechaDeclarada": "2026-10-14T10:00:00",
  "montoDeclarado": 154.50,
  "documentoId": "88e7bf5e-213c-42fe-bd1b-10f76de6f8ff",
  "observacionesAdministrado": "Pago realizado en ventanilla SAT Jr. Arequipa"
}
```

#### Contrato JSON — Validar Pago Manualmente:
```json
{
  "numeroOperacionSat": "OP-SAT-445522",
  "montoValidado": 154.50,
  "fechaPagoReal": "2026-10-14T09:45:00",
  "justificacion": "Cotejado físicamente contra el sistema de caja SAT. Coincide monto y concepto de tasa TUPA."
}
```

### 3.2 Módulo de Evaluación de Zonificación (`/api/zonificacion`)

| Método | Endpoint | Roles Permitidos | Propósito |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/zonificacion/expediente/{expedienteId}/evaluar` | `ROLE_EVALUADOR`, `ROLE_ADMIN` | Registrar evaluación formal de zonificación |
| `GET` | `/api/zonificacion/expediente/{expedienteId}/ultima` | Autenticado (Contextual) | Consultar última evaluación del expediente |
| `GET` | `/api/zonificacion/expediente/{expedienteId}/historial` | Autenticado (Contextual) | Historial de evaluaciones del expediente |
| `GET` | `/api/zonificacion/resultado/{resultado}` | `ROLE_EVALUADOR`, `ROLE_ADMIN` | Listar evaluaciones por dictamen |

#### Contrato JSON — Registrar Evaluación de Zonificación:
```json
{
  "establecimientoId": "19b4e183-b097-4089-8d19-b541ae497b77",
  "direccionDeclarada": "Jr. 28 de Julio N° 450, Huamanga",
  "ciiuCodigo": "G4711",
  "giroDeclarado": "Venta al por menor en comercios no especializados",
  "zonificacionPdu": "ZRE-CH (Zona de Reglamentación Especial - Centro Histórico)",
  "fuenteInforme": "Informe Técnico N° 088-2026-MPH-GDUR",
  "resultado": "COMPATIBLE",
  "observaciones": "Actividad comercial compatible con el nivel de uso comercial admisible en eje vial secundario según PDU vigente."
}
```

---

## 4. Matriz de Permisos y Control de Acceso (BOLA / IDOR)

| Acción | `ROLE_CIUDADANO` | `ROLE_CAJERO` | `ROLE_EVALUADOR` | `ROLE_ADMIN` | Control Implementado |
| :--- | :---: | :---: | :---: | :---: | :--- |
| Declarar Voucher de Pago |  (Solo propios) | ❌ | ❌ |  | Verificación de titularidad sobre el expediente |
| Consultar Bandeja General de Pagos | ❌ (403) |  | ❌ (403) |  | Filtro por rol en `SecurityConfig` |
| Validar Comprobante Manualmente | ❌ (403) |  | ❌ (403) |  | Restringido exclusivamente a funcionarios de recaudación |
| Observar / Rechazar Pago | ❌ (403) |  | ❌ (403) |  | Registro inmutable de motivo y funcionario |
| Consultar Pagos de Expediente Propio |  |  |  |  | Verificación de titularidad o rol interno |
| Consultar Pagos de Expediente Ajeno | ❌ (403) |  |  |  | Control BOLA/IDOR en `validarTitularidad()` |
| Registrar Evaluación de Zonificación | ❌ (403) | ❌ (403) |  |  | Restringido a evaluadores urbanos |
| Consultar Evaluación de Zonificación |  (Solo propios) |  |  |  | Protección IDOR por solicitante / cuenta / operador |

---

## 5. Pruebas y Validación Técnica

Se incorporaron tres suites de pruebas automatizadas:
1. [PagoExpedienteServiceTest.java](file:///d:/ArqSoftware\MuniHuamanga\servicio-expedientes\src\test\java\pe\gob\munihuamanga\licencias\expedientes\pagos\PagoExpedienteServiceTest.java) (6 tests unitarios):
   - Voucher registrado inicia en `PENDIENTE_VALIDACION`.
   - Rechazo de comprobante con código duplicado.
   - Validación manual por cajero con registro de evidencia y transición a `DOCUMENTOS_VALIDADOS` (garantizando que no se aprueba automáticamente el expediente).
   - Observación y rechazo de comprobante con historial inmutable.
   - Control BOLA/IDOR ante intentos de consulta de expediente ajeno.
   - Recuperación de auditoría completa de decisiones.
2. [EvaluacionZonificacionServiceTest.java](file:///d:/ArqSoftware\MuniHuamanga\servicio-expedientes\src\test\java\pe\gob\munihuamanga\licencias\expedientes\zonificacion\EvaluacionZonificacionServiceTest.java) (4 tests unitarios):
   - Registro de evaluación compatible por evaluador autorizado con sustento técnico.
   - Registro de incompatibilidad motivada según PDU.
   - Control BOLA/IDOR ante administrados accediendo a zonificaciones ajenas.
   - Filtrado de evaluaciones por resultado.
3. [PagoZonificacionIntegrationTest.java](file:///d:/ArqSoftware\MuniHuamanga\servicio-expedientes\src\test\java\pe\gob\munihuamanga\licencias\expedientes\pagos\PagoZonificacionIntegrationTest.java) (9 tests de integración Spring Boot MockMvc):
   - Declaración de pago por ciudadano (HTTP 201).
   - Validación manual por cajero (HTTP 200).
   - Bloqueo de auto-validación ciudadana (HTTP 403 Forbidden).
   - Denegación de comprobante duplicado (HTTP 400 Bad Request).
   - Registro de observación de comprobante (HTTP 200).
   - Registro de zonificación por evaluador (HTTP 201).
   - Bloqueo a ciudadanos de emitir evaluación de zonificación (HTTP 403 Forbidden).
   - Rechazo de acceso a expediente ajeno en bandeja de pagos (HTTP 403 Forbidden).
   - Compatibilidad completa con expedientes migrados V1.

### Resumen de Ejecución Surefire:
- **Total Tests Ejecutados:** **218** (199 incrementos anteriores + 19 nuevos I06)
- **Fallos:** 0
- **Errores:** 0
- **Omitidas:** 0
- **Tiempo de ejecución:** 34.37 segundos
- **Compilación de artefacto empaquetado (`mvn verify -DskipTests`):** BUILD SUCCESS (8.52 s)

---

## 6. Limitaciones de Entorno e Infraestructura

1. **Motor de Base de Datos Local:** El daemon Docker local de PostgreSQL 16 no se encuentra en ejecución en este entorno. Las pruebas automatizadas corren exitosamente con H2 Database en modo de compatibilidad PostgreSQL (`MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH`).
2. **Migración en Ambientes Productivos:** El archivo `docker/postgres/init/05-v2-pagos-sat-zonificacion.sql` debe ejecutarse mediante Flyway/Liquibase o por el Administrador de Base de Datos antes de desplegar el binario de I06.

---

## 7. Declaración de Cumplimiento

- **No se inició el Incremento I07.**
- **No se realizaron commits automáticos a git.**
- **No se alteró la base de datos de producción.**
- **Se detiene la ejecución para dar paso a la auditoría técnica y revisión del usuario.**
