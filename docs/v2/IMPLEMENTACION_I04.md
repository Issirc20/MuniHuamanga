# INFORME TÉCNICO DE IMPLEMENTACIÓN — INCREMENTO I04
## MuniHuamanga V2: M03 (Solicitudes y Borradores) y M06 (Máquina de Estados Inicial)

**Municipalidad Provincial de Huamanga**
**Subgerencia de Comercio, Licencias y Fiscalización**
**Fecha:** 14 de octubre de 2026
**Rama Git:** `develop/munihuamanga-v2`
**Referencia Normativa:** ERS-MH-V2-001 v1.0, DAS-MH-V2-001 v1.0, TUO Ley N° 28976, D.S. 002-2018-PCM
**Línea Base Previa:** 151 pruebas exitosas (Incrementos I01, I02, I02.1 e I03)
**Línea Base Actualizada:** 165 pruebas exitosas (+14 pruebas nuevas, 0 fallos, 0 errores)

---

## 1. Resumen Ejecutivo

En el marco del Incremento I04 se ha implementado de forma integral el módulo **M03 (Solicitudes y Borradores)** y el núcleo de la máquina de estados de transición formal **M06 (Presentación Formal de Expedientes)**.

Se resolvieron de raíz las vulnerabilidades históricas de generación prematura de números de expediente (`EXP-*`), garantizando que la creación y edición de borradores utilice exclusivamente identificadores técnicos internos (`UUID`) y control de concurrencia optimista (`@Version`). La asignación de correlativos formales (`EXP-YYYY-XXXXX`) se realiza ahora mediante un mecanismo transaccional atómico protegido con bloqueo pesimista en base de datos (`PESSIMISTIC_WRITE`), ejecutado únicamente tras superar un riguroso checklist de validación previa, verificación de titularidad y confirmación expresa del administrado.

Asimismo, se implementó el procedimiento auditable de reclamación de titularidad de expedientes históricos V1, impidiendo la vinculación automática o arbitraria por coincidencia simple de DNI/RUC.

---

## 2. Arquitectura Implementada

```
+---------------------------------------------------------------------------------------------------+
|                                      CAPA REST / SEGURIDAD (BOLA & RBAC)                           |
|  [SolicitudBorradorController]            [ReclamacionExpedienteController]                       |
|   - POST /api/borradores                   - POST /api/expedientes/{id}/reclamar                   |
|   - PUT /api/borradores/{id}               - POST /api/expedientes/reclamaciones/{id}/resolver     |
|   - POST /api/borradores/{id}/documentos   - GET  /api/expedientes/reclamaciones/pendientes        |
|   - POST /api/borradores/{id}/presentar                                                           |
+---------------------------------------------------------------------------------------------------+
                                            │
                                            ▼
+---------------------------------------------------------------------------------------------------+
|                                 AUTORIZACIÓN BOLA / IDOR CONTEXTUAL                               |
|   [BorradorSecurity]                                                                              |
|    ├── Titularidad de Cuenta (Persona Natural / Jurídica)                                         |
|    └── Operador Activo con Permiso: CREAR_BORRADOR, CONSULTAR_EXPEDIENTE, PRESENTAR_EXPEDIENTE     |
+---------------------------------------------------------------------------------------------------+
                                            │
                                            ▼
+---------------------------------------------------------------------------------------------------+
|                                  SERVICIOS TRANSACCIONALES DE DOMINIO                              |
|  [SolicitudBorradorService]          [PresentacionFormalService]      [ReclamacionService]         |
|   - CRUD Borrador (Sin EXP-*)         - Validar Cuenta ACTIVA + Email  - Reclamación con evidencia |
|   - Hash SHA-256 en adjuntos          - Validar Establecimiento Propio  - Dictamen evaluador        |
|   - Checklist de requisitos           - Correlativo EXP-YYYY-XXXXX     - Vinculación auditable     |
|   - Descarte con auditoría            - Instantánea inmutable                                      |
|                                       - Idempotencia estricta                                      |
|                                            │                                                      |
|                                            ▼                                                      |
|                             [SecuenciaTramiteService]                                             |
|                              (Lock PESSIMISTIC_WRITE)                                             |
+---------------------------------------------------------------------------------------------------+
                                            │
                                            ▼
+---------------------------------------------------------------------------------------------------+
|                                     PERSISTENCIA POSTGRESQL V2                                    |
|   - solicitudes_borrador (UUID, estado, version, datos formulario, avisos legales)                |
|   - documentos_borrador (UUID, hash_sha256, ruta, metadatos, disclaimer)                         |
|   - secuencias_expediente (anio PK, ultimo_correlativo)                                           |
|   - reclamaciones_expediente (UUID, expediente_id, cuenta_id, estado, evaluador, auditoria)        |
+---------------------------------------------------------------------------------------------------+
```

---

## 3. Modelo de Entidades y Migraciones

### 3.1 Migración de Base de Datos (`03-v2-solicitudes-borradores.sql`)
Se creó el script de migración DDL en `docker/postgres/init/03-v2-solicitudes-borradores.sql` (e integrado en `01-init-databases.sql`) sin realizar modificaciones destructivas sobre las tablas existentes:

1. **`secuencias_expediente`**:
   - `anio INTEGER PRIMARY KEY`
   - `ultimo_correlativo BIGINT NOT NULL DEFAULT 0`
   - `fecha_actualizacion TIMESTAMP NOT NULL`
2. **`solicitudes_borrador`**:
   - `id UUID PRIMARY KEY`
   - `cuenta_id UUID NOT NULL REFERENCES cuentas(id)`
   - `establecimiento_id UUID NOT NULL REFERENCES establecimientos(id)`
   - `creador_id UUID NOT NULL`
   - `estado VARCHAR(30) NOT NULL` (`EN_EDICION`, `LISTO_PARA_PRESENTAR`, `PRESENTADO`, `DESCARTADO`)
   - `version BIGINT NOT NULL DEFAULT 0` (Control de concurrencia optimista)
   - Datos del formulario de licencia (modalidad, giro, actividad, áreas, aforo, pisos, horarios, contacto)
   - Datos declarados de SAT y riesgo ITSE con trazabilidad
   - `expediente_id UUID REFERENCES expedientes(id)` (Vínculo formal una vez presentado)
3. **`documentos_borrador`**:
   - `id UUID PRIMARY KEY`
   - `borrador_id UUID NOT NULL REFERENCES solicitudes_borrador(id) ON DELETE CASCADE`
   - `tipo_documento VARCHAR(50) NOT NULL` (`DECLARACION_JURADA`, `ANEXO_ITSE`, `EVIDENCIA_RIESGO_ITSE`, `COMPROBANTE_PAGO_SAT`, etc.)
   - `hash_sha256 VARCHAR(64) NOT NULL`
   - `nombre_archivo`, `tipo_contenido`, `tamanio_bytes`, `ruta_almacenamiento`, `fecha_carga`, `cargado_por`
4. **`reclamaciones_expediente`**:
   - `id UUID PRIMARY KEY`
   - `expediente_id UUID NOT NULL REFERENCES expedientes(id)`
   - `cuenta_id UUID NOT NULL REFERENCES cuentas(id)`
   - `solicitante_username VARCHAR(50) NOT NULL`
   - `evidencia_titularidad TEXT NOT NULL`
   - `estado VARCHAR(30) NOT NULL` (`PENDIENTE`, `APROBADA`, `RECHAZADA`)
   - `evaluador_id UUID`, `fecha_resolucion`, `motivo_resolucion`

---

## 4. Endpoints y Contratos JSON

### 4.1 Creación de Borrador (Sin Número `EXP-*`)
- **Ruta:** `POST /api/borradores`
- **Seguridad:** `@PreAuthorize("@borradorSecurity.puedeCrearBorradorEnCuenta(authentication, #dto.cuentaId)")`
- **Request:**
```json
{
  "cuentaId": "66def348-e54d-4ac3-a9db-b00b72b9d911",
  "establecimientoId": "f25c9ef3-b804-4b7c-b20a-d30cce149a8d",
  "modalidadTramite": "LICENCIA_INDETERMINADA",
  "nombreComercialDeclarado": "Boutique Artesanal Huamanga",
  "giroNegocioDeclarado": "Venta de artesanías y retablos",
  "areaMetrosCuadrados": 35.50
}
```
- **Response (201 Created):**
```json
{
  "id": "762485db-3de1-4a10-9889-4beed7e44235",
  "cuentaId": "66def348-e54d-4ac3-a9db-b00b72b9d911",
  "estado": "EN_EDICION",
  "version": 0,
  "numeroTramiteFormal": null,
  "avisoLegal": "Solicitud preliminar en estado de borrador. No constituye inicio de procedimiento administrativo formal ni genera derecho adquirido.",
  "avisoZonificacion": "La ubicación y giro declarados NO constituyen zonificación municipal aprobada ni compatibilidad de uso vigente."
}
```

### 4.2 Guardado Parcial de Avances
- **Ruta:** `PUT /api/borradores/{id}`
- **Seguridad:** `@PreAuthorize("@borradorSecurity.puedeEditarBorrador(authentication, #id)")`
- Permite actualizar campos de formulario, horarios, aforos, datos de comprobante SAT y nivel de riesgo estimado sin modificar el estado a formal.

### 4.3 Carga de Documentos con Hash SHA-256 y Disclaimers
- **Ruta:** `POST /api/borradores/{id}/documentos`
- **Response (201 Created):** Retorna metadatos del documento incluyendo hash inmutable SHA-256 y disclaimer legal:
  - Para comprobante SAT: *"El registro o carga de comprobante SAT es una constancia referencial ciudadana y NO constituye validación oficial de pago."*
  - Para evidencia ITSE: *"La clasificación de nivel de riesgo declarada es una estimación técnica preliminar y NO equivale a una certificación ni dictamen institucional de Defensa Civil."*

### 4.4 Validación de Requisitos Pendientes (Checklist)
- **Ruta:** `GET /api/borradores/{id}/requisitos-pendientes`
- **Response (200 OK):**
```json
{
  "borradorId": "762485db-3de1-4a10-9889-4beed7e44235",
  "aptoParaPresentacion": true,
  "camposFaltantes": [],
  "documentosFaltantes": [],
  "mensajeResumen": "El borrador cumple con todos los requisitos documentales y de formulario para ser presentado formalmente."
}
```

### 4.5 Presentación Formal Transaccional e Idempotente
- **Ruta:** `POST /api/borradores/{id}/presentar`
- **Seguridad:** `@PreAuthorize("@borradorSecurity.puedePresentarBorrador(authentication, #id)")`
- **Request:**
```json
{
  "declaracionJuradaAceptada": true,
  "confirmacionPresentacion": true,
  "comentarios": "Presentación formal de solicitud de licencia comercial"
}
```
- **Flujo Interno:**
  1. Verifica cuenta activa (`EstadoCuenta.ACTIVA`) y correo verificado (`emailVerificado = true`).
  2. Verifica que el establecimiento pertenece a la cuenta titular.
  3. Verifica permisos del operador (`PRESENTAR_EXPEDIENTE` si es persona jurídica).
  4. Ejecuta el checklist de requisitos obligatorios del TUPA.
  5. Obtiene de forma atómica el siguiente número correlativo (`EXP-2026-XXXXX`).
  6. Guarda instantánea inmutable de datos y crea el `Expediente` en estado `PRESENTADO`.
  7. Registra evento de auditoría en `historial_estados`.
  8. Marca el borrador como `PRESENTADO` enlazándolo al expediente.
  9. Ante reintentos con el mismo borrador, devuelve idempotentemente el expediente formal sin duplicar.

### 4.6 Procedimiento de Reclamación de Titularidad Histórica V1
- **Ruta:** `POST /api/expedientes/{id}/reclamar` (Ciudadano aporta evidencia)
- **Ruta:** `POST /api/expedientes/reclamaciones/{id}/resolver` (Evaluador aprueba/rechaza)
- **Garantía:** No se realiza vinculación ciega de expedientes V1 por DNI/RUC. Se exige sustento documental y resolución administrativa explícita.

---

## 5. Matriz de Seguridad y Reglas de Negocio

| Control de Seguridad | Vector de Ataque Mitigado | Implementación Técnica |
| :--- | :--- | :--- |
| **BOLA / IDOR en Borradores** | Modificación o visualización de borradores de otros administrados | Evaluador `BorradorSecurity`: Valida titularidad directa o membresía activa de operador con permiso específico (`CREAR_BORRADOR`, `CONSULTAR_EXPEDIENTE`, `PRESENTAR_EXPEDIENTE`). |
| **Generación Prematura de EXP-*** | Consumo de números oficiales sin iniciar trámite legal | Separación estricta de dominios: `SolicitudBorrador` usa solo UUID interno; el número formal `EXP-YYYY-XXXXX` se genera únicamente en `PresentacionFormalService`. |
| **Doble Clic / Concurrencia** | Generación de números duplicados o expedientes duplicados | Bloqueo pesimista `PESSIMISTIC_WRITE` en `SecuenciaExpedienteRepository` + comprobación de idempotencia antes de la asignación. |
| **Secuestro de Cuentas Jurídicas** | Tramitación no autorizada a nombre de empresas | Validación estricta de permisos de operador nominativo en presentación formal. |
| **Falsas Certificaciones SAT/ITSE** | Suplantación de pagos oficiales o licencias automáticas | Disclaimers legales obligatorios en JSON y BD: Comprobante SAT queda `PENDIENTE_VALIDACION`; riesgo declarado no sustituye dictamen de Defensa Civil. |
| **Apropiación de Expedientes V1** | Reclamación fraudulenta de licencias históricas ajenas | Tabla `reclamaciones_expediente` con estado `PENDIENTE`, resolución motivada por evaluador municipal y registro en `HistorialEstado`. |

---

## 6. Resultados de la Suite de Pruebas

Se ejecutó la suite completa del monolito modular mediante Maven:

```bash
mvn clean verify
```

### Resumen de Ejecución
- **Módulos Ejecutados:** `muni-licencias-parent`, `common-domain`, `servicio-expedientes`
- **Total de Pruebas:** 165 pruebas automatizadas
- **Fallos:** 0
- **Errores:** 0
- **Omitidas:** 0
- **Resultado:** `BUILD SUCCESS`

### Distribución de Pruebas de I04
1. **`SolicitudBorradorServiceTest` (5 pruebas):**
   - Creación de borrador con identificador UUID sin número EXP-*.
   - Guardado parcial de avances y actualización de fecha/versión.
   - Adjuntar documentos probatorios con cálculo de hash SHA-256 y disclaimers.
   - Validación de requisitos pendientes detectando omisiones obligatorias.
   - Descarte de borrador conservando trazabilidad y fecha de descarte.
2. **`PresentacionFormalServiceTest` (4 pruebas):**
   - Presentación formal exitosa con correlativo atómico `EXP-2026-00101` y estado `PRESENTADO`.
   - Idempotencia estricta ante reintentos.
   - Rechazo de presentación si el correo electrónico de la cuenta no está verificado.
   - Rechazo de presentación por operador sin permiso `PRESENTAR_EXPEDIENTE`.
3. **`ReclamacionExpedienteServiceTest` (3 pruebas):**
   - Solicitud de reclamación de titularidad histórica con evidencia documental.
   - Aprobación de reclamación por evaluador y transferencia formal de titularidad.
   - Rechazo motivado de reclamación sin alterar la titularidad del expediente.
4. **`BorradorSecurityIntegrationTest` (2 pruebas completas de integración MockMvc):**
   - Flujo integral de borrador (creación, edición, adjuntos, checklist, BOLA 403 a terceros, presentación formal y reintento idempotente).
   - Flujo integral de reclamación y resolución administrativa de titularidad de expediente histórico V1.

---

## 7. Riesgos Residuales y Transición hacia I05

1. **Evaluación Administrativa y Zonificación (M07):** El expediente formalizado pasa a estado `PRESENTADO`. El Incremento I05 deberá implementar la evaluación técnica de zonificación municipal conforme al Plan de Desarrollo Urbano (PDU) y emisión de observaciones.
2. **Generación de Informes Técnicos (M08):** Se requerirá articular los generadores PDF de anexos y licencias al flujo de evaluación administrativa previa a la resolución.
3. **Integraciones Externas:** Se mantiene la política de no implementar integración real directa con SAT, Defensa Civil o SUNARP hasta contar con los convenios y credenciales institucionales definitivos, operando con validación manual por funcionarios autorizados.
4. **Firma Digital Institucional (M09):** La emisión final y generación de QR oficial continuará bloqueada hasta la implementación del adaptador seguro de firma digital conforme a la Ley N° 27269.

---

## 8. Conclusión

El Incremento I04 ha sido completado con éxito, cumpliendo todos los criterios de aceptación, integridad transaccional, protección BOLA/IDOR y compatibilidad regresiva con los módulos I01–I03. Se procede al cierre y detención de actividades para la auditoría técnica correspondiente antes de dar inicio al Incremento I05.
