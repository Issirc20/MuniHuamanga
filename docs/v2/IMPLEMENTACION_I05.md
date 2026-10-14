# INFORME TÉCNICO DE IMPLEMENTACIÓN — INCREMENTO I05
## MuniHuamanga V2: Gestión Documental, Versionado Inmutable, Anexos ITSE y Subsanaciones

**Municipalidad Provincial de Huamanga**
**Subgerencia de Comercio, Licencias y Fiscalización**
**Fecha:** 14 de octubre de 2026
**Rama Git:** `develop/munihuamanga-v2`
**Referencia Normativa:** ERS-MH-V2-001 v1.0, DAS-MH-V2-001 v1.0, TUO Ley N° 27444 (LPAG Art. 136), Ley N° 28976, D.S. N° 002-2018-PCM
**Línea Base Previa:** 176 pruebas exitosas (Incrementos I01 a I04.1)
**Línea Base Actualizada:** 193 pruebas exitosas (+17 pruebas nuevas, 0 fallos, 0 errores)

---

## 1. Resumen Ejecutivo

En el marco del **Incremento I05**, se ha completado el ciclo documental integral tanto para las solicitudes en borrador como para los expedientes administrativos formalmente admitidos, garantizando la **inmutabilidad de versiones**, **integridad criptográfica SHA-256**, **trazabilidad documental histórica**, **reutilización de datos para la generación nativa de formatos y anexos ITSE**, y el **flujo formal de formulación de observaciones técnicas y subsanación por el administrado** bajo el mismo número de expediente.

Se eliminaron los riesgos de sobrescritura de archivos presentados y alteraciones retroactivas, estableciendo un esquema en base de datos con restricciones únicas (`uk_doc_expediente_version` y `uk_doc_borrador_version`) que impiden duplicidad o colisiones de versiones. Asimismo, se corrigió la dependencia de inicialización de secuencias anuales mediante un mecanismo atómico concurrente aislado (`SecuenciaInicializadorHelper` en `REQUIRES_NEW`), previniendo inanición en el pool de conexiones JDBC e inconsistencias ante cambio de año o despliegues multi-instancia.

---

## 2. Marco Normativo Aplicado

1. **TUO de la Ley N° 27444 — Ley del Procedimiento Administrativo General (LPAG):**
   - **Artículo 136 (Subsanación de requisitos):** Toda observación sobre documentación o requisitos presentados debe formularse de manera integral y motivada en un solo acto, otorgando al administrado un plazo perentorio improrrogable (15 días hábiles conforme al TUPA municipal). La subsanación se efectiviza dentro del mismo expediente administrativo, conservando su número y fecha de inicio original.
2. **Ley N° 28976 — Ley Marco de Licencia de Funcionamiento:**
   - Estandarización de requisitos documentales, prohibición de exigir documentos adicionales a los establecidos en el TUPA y formalización mediante Declaración Jurada (Anexo 1).
3. **Decreto Supremo N° 002-2018-PCM — Reglamento de Inspecciones Técnicas de Seguridad en Edificaciones (ITSE):**
   - Regula los formatos oficiales de la Matriz de Riesgo ITSE (Anexo 3) y la Declaración Jurada de Cumplimiento de Condiciones de Seguridad en la Edificación (Anexo 4), asegurando que la información de zonificación, aforo, área y condiciones no sea alterada de forma unilateral.
4. **Ley N° 29733 — Ley de Protección de Datos Personales:**
   - Enmascaramiento de documentos de identidad (DNI/RUC) en registros de auditoría y trazas operativas, evitando la fuga de información sensible.

---

## 3. Arquitectura y Componentes Implementados

```mermaid
graph TD
    subgraph Capa REST y Seguridad BOLA/IDOR
        C1[DocumentoVersionadoController<br>/api/expedientes/{id}/documentos]
        C2[ObservacionExpedienteController<br>/api/expedientes/{id}/observaciones]
        C3[DocumentosFormulariosController<br>/api/formularios/anexo-*]
        SEC[ExpedienteSecurity & DocumentoSecurityValidator]
    end

    subgraph Capa de Servicios de Dominio
        S1[DocumentoVersionadoService]
        S2[ObservacionExpedienteService]
        S3[SolicitudBorradorService]
        S4[DocumentoPdfService & Motores OpenPDF]
        S5[SecuenciaTramiteService & SecuenciaInicializadorHelper]
    end

    subgraph Persistencia PostgreSQL V2
        DB1[(documentos_versionados)]
        DB2[(observaciones_expediente)]
        DB3[(solicitudes_borrador)]
        DB4[(expedientes)]
        DB5[(secuencias_expediente)]
    end

    C1 --> SEC
    C2 --> SEC
    C3 --> SEC
    SEC --> S1
    SEC --> S2
    SEC --> S3
    S1 --> DB1
    S2 --> DB2
    S2 --> S1
    S3 --> S4
    S5 --> DB5
    DB1 -.-> DB4
    DB2 -.-> DB4
```

### 3.1 Componentes Principales

- **`DocumentoVersionadoService`:** Gestiona el ciclo de vida inmutable de documentos. Calcula y valida hashes SHA-256, verifica cabeceras binarias (*magic numbers*), sanitiza nombres de archivo frente a ataques de *path traversal*, controla que solo expedientes en estados autorizados (`OBSERVADO` para ciudadanos) admitan nuevas versiones, y audita cada operación de lectura y escritura.
- **`ObservacionExpedienteService`:** Gestiona la formulación colegiada de observaciones por evaluadores autorizados (`ROLE_EVALUADOR`, `ROLE_ADMIN`), calcula la fecha límite de subsanación según días hábiles TUPA/LPAG, transiciona el expediente a `OBSERVADO`, y procesa la subsanación ciudadana transicionando formalmente a `SUBSANADO`.
- **`DocumentoSecurityValidator`:** Componente de seguridad transversal que ejecuta:
  - Inspección de cabeceras binarias (%PDF-, PNG, JPG).
  - Sanitización estricta de nombres de archivo y mitigación de *path traversal* (`..`, `/`, `\`).
  - Verificación criptográfica de integridad de contenido (SHA-256).
  - Enmascaramiento de datos personales en logs (`ofuscarDocumento`).
  - Registro de auditoría preventiva antimalware con estado `ESCANEO_DELEGADO_PENDIENTE_CONFIGURACION_CLAMAV`.
- **`SecuenciaInicializadorHelper`:** Componente transaccional con propagación `REQUIRES_NEW` que garantiza la inicialización atómica y concurrente de registros anuales en `secuencias_expediente`, inmune a condiciones de carrera multihilo y cambio de año calendario.

---

## 4. Modelo de Datos y Migración SQL

### 4.1 Script DDL Aditivo (`04-v2-documentos-versionados-subsanaciones.sql`)
La migración se diseñó de manera estrictamente aditiva, sin alterar ni eliminar las tablas históricas de V1 (`expedientes`, `usuarios`, `roles_usuarios`, etc.):

```sql
-- 1. Tabla de Documentos Versionados Inmutables
CREATE TABLE IF NOT EXISTS documentos_versionados (
    id UUID PRIMARY KEY,
    expediente_id UUID REFERENCES expedientes(id) ON DELETE CASCADE,
    borrador_id UUID REFERENCES solicitudes_borrador(id) ON DELETE CASCADE,
    tipo_documento VARCHAR(50) NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    nombre_archivo VARCHAR(255) NOT NULL,
    tipo_contenido VARCHAR(100),
    tamanio_bytes BIGINT,
    hash_sha256 VARCHAR(64) NOT NULL,
    ruta_almacenamiento TEXT,
    fecha_carga TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cargado_por UUID NOT NULL,
    cargado_por_nombre VARCHAR(100),
    estado_revision VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    version_anterior_id UUID REFERENCES documentos_versionados(id),
    motivo_actualizacion TEXT,
    es_version_actual BOOLEAN NOT NULL DEFAULT TRUE,
    observacion_id UUID,
    CONSTRAINT uk_doc_expediente_version UNIQUE (expediente_id, tipo_documento, version),
    CONSTRAINT uk_doc_borrador_version UNIQUE (borrador_id, tipo_documento, version)
);

CREATE INDEX IF NOT EXISTS idx_doc_versionado_expediente ON documentos_versionados(expediente_id);
CREATE INDEX IF NOT EXISTS idx_doc_versionado_actual ON documentos_versionados(expediente_id, es_version_actual);

-- 2. Tabla de Observaciones Técnicas y Documentales (Art. 136 LPAG)
CREATE TABLE IF NOT EXISTS observaciones_expediente (
    id UUID PRIMARY KEY,
    expediente_id UUID NOT NULL REFERENCES expedientes(id) ON DELETE CASCADE,
    documento_id UUID REFERENCES documentos_versionados(id),
    requisito_afectado VARCHAR(100) NOT NULL,
    motivo TEXT NOT NULL,
    fundamento_legal VARCHAR(255),
    fecha_observacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    observador_id UUID,
    observador_username VARCHAR(100),
    plazo_dias_habiles INTEGER NOT NULL DEFAULT 15,
    fecha_limite_subsanacion TIMESTAMP NOT NULL,
    subsanada BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_subsanacion TIMESTAMP,
    documento_subsanacion_id UUID REFERENCES documentos_versionados(id),
    detalle_subsanacion TEXT
);

CREATE INDEX IF NOT EXISTS idx_obs_expediente ON observaciones_expediente(expediente_id);
CREATE INDEX IF NOT EXISTS idx_obs_subsanada ON observaciones_expediente(expediente_id, subsanada);
```

---

## 5. Endpoints y Contratos de la API

### 5.1 Documentos Versionados

| Método | Endpoint | Roles Autorizados | Descripción |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/expedientes/{id}/documentos` | Ciudadano Titular / Operador / Evaluador | Adjunta una nueva versión inmutable de un documento. En ciudadanos, requiere que el expediente esté en estado `OBSERVADO`. |
| `GET` | `/api/expedientes/{id}/documentos` | Ciudadano Titular / Operador / Evaluador / Admin | Lista documentos del expediente (`soloActuales=true` o histórico completo). |
| `GET` | `/api/expedientes/{id}/documentos/{docId}` | Ciudadano Titular / Operador / Evaluador / Admin | Descarga binaria del documento previa verificación criptográfica de integridad SHA-256. |
| `GET` | `/api/expedientes/{id}/documentos/historial/{tipo}` | Ciudadano Titular / Operador / Evaluador / Admin | Consulta cronológica de versiones de un tipo documental determinado. |

#### Contrato: Adjuntar Nueva Versión Documental
- **Request (`POST /api/expedientes/{id}/documentos`):**
```json
{
  "tipoDocumento": "PLANO_DISTRIBUCION",
  "nombreArchivo": "plano_distribucion_subsanado_v2.pdf",
  "tipoContenido": "application/pdf",
  "contenidoBase64": "JVBERi0xLjQKJcTl8uXrp/...",
  "motivoActualizacion": "Subsanación de la observación técnica N° 1 sobre área de pasadizos",
  "observacionId": "4a737f9e-324c-4e89-9801-b3bcf7cb0248"
}
```
- **Response (`201 Created`):**
```json
{
  "id": "e58f0012-78d1-42ab-b190-2ff586a111a9",
  "expedienteId": "e200c776-df0d-4519-99ab-6527b818b832",
  "tipoDocumento": "PLANO_DISTRIBUCION",
  "version": 2,
  "nombreArchivo": "plano_distribucion_subsanado_v2.pdf",
  "tamanioBytes": 204850,
  "hashSha256": "4a5e1e8db92...64caracteres",
  "fechaCarga": "2026-10-14T10:15:30",
  "estadoRevision": "PENDIENTE",
  "versionAnteriorId": "b11a9801-54a2-4a11-89ce-342aef912001",
  "esVersionActual": true
}
```

---

### 5.2 Observaciones y Subsanaciones (Art. 136 LPAG)

| Método | Endpoint | Roles Autorizados | Descripción |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/expedientes/{id}/observaciones` | `ROLE_EVALUADOR`, `ROLE_ADMIN` | Formula observaciones técnicas. Transiciona el expediente a `OBSERVADO`. |
| `GET` | `/api/expedientes/{id}/observaciones` | Ciudadano Titular / Evaluador / Admin | Consulta el pliego de observaciones, fundamentación legal y fecha límite. |
| `POST` | `/api/expedientes/{id}/observaciones/subsanar` | Ciudadano Titular / Operador con permiso | Presentación formal de subsanación. Transiciona a `SUBSANADO`. |

#### Contrato: Formular Observaciones Técnicas
- **Request (`POST /api/expedientes/{id}/observaciones`):**
```json
[
  {
    "documentoId": "b11a9801-54a2-4a11-89ce-342aef912001",
    "requisitoAfectado": "Plano de arquitectura y distribución",
    "motivo": "El ancho de pasadizo principal declarado es de 0.90 m, incumpliendo el mínimo de 1.20 m para locales comerciales.",
    "fundamentoLegal": "Reglamento Nacional de Edificaciones (Norma A.070, Art. 14) y TUPA MPH Requisito 02",
    "plazoDiasHabiles": 15
  }
]
```
- **Response (`201 Created`):**
```json
[
  {
    "id": "4a737f9e-324c-4e89-9801-b3bcf7cb0248",
    "expedienteId": "e200c776-df0d-4519-99ab-6527b818b832",
    "requisitoAfectado": "Plano de arquitectura y distribución",
    "motivo": "El ancho de pasadizo principal declarado es de 0.90 m, incumpliendo el mínimo de 1.20 m para locales comerciales.",
    "fundamentoLegal": "Reglamento Nacional de Edificaciones (Norma A.070, Art. 14) y TUPA MPH Requisito 02",
    "fechaObservacion": "2026-10-14T10:20:00",
    "observadorUsername": "evaluador_carlos",
    "plazoDiasHabiles": 15,
    "fechaLimiteSubsanacion": "2026-11-04T23:59:59",
    "subsanada": false
  }
]
```

#### Contrato: Subsanación por el Administrado
- **Request (`POST /api/expedientes/{id}/observaciones/subsanar`):**
```json
{
  "detalleSubsanacion": "Se adjuntó plano de distribución v2 con pasadizo rectificado a 1.25 m conforme a la Norma A.070 RNE.",
  "observacionesIds": ["4a737f9e-324c-4e89-9801-b3bcf7cb0248"]
}
```
- **Response (`204 No Content`)**

---

### 5.3 Generación de Anexo 1 y Anexos ITSE desde Borrador y Expediente

Se implementó el mapeo unificado de información que reutiliza los datos maestros de cuenta y establecimiento para alimentar los motores nativos OpenPDF sin alterar los diseños oficiales acordados:
- `GET /api/borradores/{id}/formularios/anexo-1/pdf`: Declaración Jurada oficial (2 páginas).
- `GET /api/borradores/{id}/formularios/anexo-3/pdf`: Matriz de Riesgo ITSE oficial (2 páginas).
- `GET /api/borradores/{id}/formularios/anexo-4/pdf`: Declaración Jurada de Cumplimiento de Condiciones de Seguridad (4 páginas).

---

## 6. Seguridad, Integridad y Auditoría

1. **Protección BOLA/IDOR Contextual:**
   - Evaluador SpEL `@expedienteSecurity.puedeDescargarDocumento(authentication, #id)` y `@expedienteSecurity.puedeSubsanar(authentication, #id)`.
   - Validación cruzada de identidad del administrado (`numeroDocumento` o cuenta de usuario). Un ciudadano autenticado recibe `403 Forbidden` al intentar consultar o descargar documentos de expedientes ajenos.
2. **Integridad Criptográfica Inmutable:**
   - Cada archivo cargado es sometido a cálculo de hash SHA-256 en memoria antes de persistir metadatos.
   - En la descarga, se verifica que el archivo físico o binario coincida exactamente con el hash registrado; de lo contrario, se aborta la entrega y se lanza alerta de alteración no autorizada.
3. **Mitigación de Vulnerabilidades de Almacenamiento:**
   - Inspección binaria de *magic numbers* (%PDF-, PNG, JPG) impidiendo archivos ejecutables enmascarados como PDFs.
   - Mitigación rigurosa de *path traversal* contra secuencias como `../`, `..\`, `/` o `\`.
   - Ocultamiento de rutas internas del servidor; los clientes únicamente interactúan a través de identificadores `UUID`.
4. **Privacidad de Datos Personales (Ley N° 29733):**
   - Enmascaramiento obligatorio en trazas de log mediante `DocumentoSecurityValidator.ofuscarDocumento(documentoIdentidad)`. Ejemplo: DNI `43777701` se registra como `43****01`.
5. **Auditoría Antimalware:**
   - Todo documento registrado queda clasificado con auditoría preventiva `ESCANEO_DELEGADO_PENDIENTE_CONFIGURACION_CLAMAV`, cumpliendo la prohibición expresa de marcar documentos como libres de malware sin un motor antivirus activo.

---

## 7. Resultados de Pruebas y Validación

La suite completa de pruebas unitarias, de integración, de seguridad y de concurrencia se ejecutó mediante Maven:
- **Comando:** `mvn clean test` y `mvn verify -DskipTests`
- **Resultado:** **193 pruebas ejecutadas, 0 fallos, 0 errores, 0 omitidas**
- **Estado de Compilación:** **BUILD SUCCESS**

### 7.1 Pruebas Específicas del Incremento I05

1. **`DocumentoVersionadoServiceTest` (11 pruebas):**
   - Versionado inmutable y cálculo de correlativo v1, v2, v3.
   - Rechazo de carga documental en expedientes con estado no habilitado para ciudadanos (`PRESENTADO`).
   - Verificación de hash SHA-256 y detección de archivos corruptos.
   - Bloqueo de acceso y descarga para usuarios no autorizados (BOLA/IDOR).
   - Rechazo de archivos sin cabeceras binarias válidas (*magic numbers*).
   - Rechazo de nombres de archivo maliciosos (*path traversal*).
2. **`ObservacionSubsanacionIntegrationTest` (4 pruebas):**
   - Registro de observación técnica por evaluador municipal y transición a `OBSERVADO`.
   - Denegación de formulación de observaciones a ciudadanos (`403 Forbidden`).
   - Consulta de observaciones por ciudadano titular.
   - Subsanación formal por el administrado con nueva versión documental y transición a `SUBSANADO`.
3. **`AnexosItseGeneracionTest` (2 pruebas):**
   - Reutilización de datos de borrador, cuenta y establecimiento en DTO formal.
   - Generación vectorial de PDFs oficiales legítimos (%PDF-) para Anexo 1, Anexo 3 y Anexo 4.

---

## 8. Riesgos Residuales y Limitaciones Técnicas

1. **Disponibilidad de Motor PostgreSQL 16 en Entorno Local:**
   - *Condición observada:* El motor Docker Desktop Linux Engine no se encontraba en ejecución en la estación local durante la validación automatizada.
   - *Declaración de limitación:* Las pruebas se ejecutaron exitosamente sobre la base de datos en memoria H2 configurada con dialecto PostgreSQL y compatibilidad de tipos (`UUID`, `TIMESTAMP`, etc.). Si bien los scripts DDL SQL aditivos (`04-v2-documentos-versionados-subsanaciones.sql`) cumplen estrictamente la sintaxis estándar de PostgreSQL 16, **no se afirma compatibilidad certificada sobre un contenedor PostgreSQL 16 físico activo** hasta el levantamiento del demonio Docker.
2. **Servicio Antimalware en Producción:**
   - Los archivos se inspeccionan por magic numbers y tamaño (<10 MB), pero el escaneo heurístico de virus requiere la configuración del socket Unix/TCP de ClamAV en el despliegue final de infraestructura.
3. **Emisión Oficial de Licencias y Código QR:**
   - Se mantiene el bloqueo estricto que impide la activación de licencias definitivas y QR oficiales sin la integración del servicio de firma digital institucional acreditado ante INDECOPI (Ley N° 27269).

---

## 9. Conclusiones y Estado del Incremento

El **Incremento I05** se encuentra **COMPLETO Y CERTIFICADO**:
- Ciclo documental inmutable implementado y protegido.
- Formulación de observaciones y subsanación en el mismo expediente (Art. 136 LPAG) operativo.
- Reutilización de datos para formatos oficiales y anexos ITSE verificada.
- 100% de pruebas en verde (193/193).
- No se iniciaron I06 ni I07 conforme a las restricciones mandatarias.
