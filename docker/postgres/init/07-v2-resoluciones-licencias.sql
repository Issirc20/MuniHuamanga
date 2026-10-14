-- ============================================================================
-- MUNIHUAMANGA V2 — INCREMENTO I08
-- M09: Resoluciones Administrativas, Firma Digital Institucional y Licencias
-- ============================================================================

-- 1. Tabla de secuencias anuales para numeración atómica de resoluciones (RES-YYYY-XXXXX)
CREATE TABLE IF NOT EXISTS secuencias_resolucion (
    anio INT PRIMARY KEY,
    ultimo_numero INT NOT NULL DEFAULT 0
);

INSERT INTO secuencias_resolucion (anio, ultimo_numero)
VALUES (EXTRACT(YEAR FROM CURRENT_DATE)::INT, 0)
ON CONFLICT (anio) DO NOTHING;

-- 2. Tabla de secuencias anuales para numeración atómica de licencias oficiales (LIC-YYYY-XXXXX)
CREATE TABLE IF NOT EXISTS secuencias_licencia (
    anio INT PRIMARY KEY,
    ultimo_numero INT NOT NULL DEFAULT 0
);

INSERT INTO secuencias_licencia (anio, ultimo_numero)
VALUES (EXTRACT(YEAR FROM CURRENT_DATE)::INT, 0)
ON CONFLICT (anio) DO NOTHING;

-- 3. Tabla de resoluciones administrativas gerenciales
CREATE TABLE IF NOT EXISTS resoluciones_administrativas (
    id UUID PRIMARY KEY,
    numero_resolucion VARCHAR(30) NOT NULL UNIQUE,
    expediente_id UUID NOT NULL,
    informe_administrativo_id UUID NOT NULL,
    version_informe INT NOT NULL,
    tipo_decision VARCHAR(20) NOT NULL,
    estado VARCHAR(30) NOT NULL,
    fundamentos TEXT NOT NULL,
    referencias_legales TEXT,
    parte_resolutiva TEXT,
    autoridad_competente_id UUID,
    autoridad_competente_nombre VARCHAR(150),
    autoridad_competente_cargo VARCHAR(150),
    fecha_aprobacion TIMESTAMP,
    fecha_firma TIMESTAMP,
    firmante_nombre VARCHAR(150),
    firmante_cargo VARCHAR(150),
    firmante_entidad VARCHAR(150),
    hash_documento VARCHAR(64),
    archivo_pdf BYTEA,
    observaciones_firma TEXT,
    es_version_actual BOOLEAN NOT NULL DEFAULT TRUE,
    version INT NOT NULL DEFAULT 1,
    creado_por VARCHAR(100) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modificado_por VARCHAR(100),
    fecha_modificacion TIMESTAMP,

    CONSTRAINT fk_resolucion_expediente
        FOREIGN KEY (expediente_id) REFERENCES expedientes(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_resolucion_informe
        FOREIGN KEY (informe_administrativo_id) REFERENCES informes_administrativos(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_resolucion_decision
        CHECK (tipo_decision IN ('FAVORABLE', 'DENEGATORIA')),

    CONSTRAINT chk_resolucion_estado
        CHECK (estado IN ('BORRADOR', 'APROBADA_PENDIENTE_FIRMA', 'EN_PROCESO_FIRMA', 'FIRMADA', 'ERROR_FIRMA', 'ANULADA'))
);

CREATE INDEX IF NOT EXISTS idx_resolucion_expediente ON resoluciones_administrativas (expediente_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_resolucion_expediente_actual ON resoluciones_administrativas (expediente_id) WHERE es_version_actual = true;

-- 4. Historial inmutable de trazabilidad de resoluciones
CREATE TABLE IF NOT EXISTS historial_resoluciones (
    id UUID PRIMARY KEY,
    resolucion_id UUID NOT NULL,
    accion VARCHAR(50) NOT NULL,
    estado_anterior VARCHAR(30),
    estado_nuevo VARCHAR(30) NOT NULL,
    actor VARCHAR(100) NOT NULL,
    motivo_observaciones TEXT,
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_historial_resolucion
        FOREIGN KEY (resolucion_id) REFERENCES resoluciones_administrativas(id)
        ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_historial_resolucion_id ON historial_resoluciones (resolucion_id);

-- 5. Tabla de licencias oficiales emitidas
CREATE TABLE IF NOT EXISTS licencias_emitidas (
    id UUID PRIMARY KEY,
    numero_licencia VARCHAR(30) NOT NULL UNIQUE,
    resolucion_id UUID NOT NULL,
    expediente_id UUID NOT NULL,
    establecimiento_id UUID,
    codigo_verificacion_qr VARCHAR(64) NOT NULL UNIQUE,
    titular_tipo_documento VARCHAR(20) NOT NULL,
    titular_numero_documento VARCHAR(20) NOT NULL,
    titular_nombre_o_razon_social VARCHAR(200) NOT NULL,
    nombre_comercial VARCHAR(200) NOT NULL,
    direccion_establecimiento TEXT NOT NULL,
    giro_actividad TEXT NOT NULL,
    ciiu_codigo VARCHAR(10),
    area_metros_cuadrados NUMERIC(10, 2),
    aforo_personas INT,
    zonificacion VARCHAR(50),
    nivel_riesgo_itse VARCHAR(30),
    tipo_vigencia VARCHAR(20) NOT NULL,
    fecha_emision DATE NOT NULL,
    fecha_caducidad DATE,
    autoridad_emisora VARCHAR(150) NOT NULL,
    hash_pdf VARCHAR(64) NOT NULL,
    archivo_pdf BYTEA,
    estado VARCHAR(20) NOT NULL,
    observaciones_emision TEXT,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    creado_por VARCHAR(100) NOT NULL,

    CONSTRAINT fk_licencia_resolucion
        FOREIGN KEY (resolucion_id) REFERENCES resoluciones_administrativas(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_licencia_expediente
        FOREIGN KEY (expediente_id) REFERENCES expedientes(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_licencia_establecimiento
        FOREIGN KEY (establecimiento_id) REFERENCES establecimientos(id)
        ON DELETE RESTRICT,

    CONSTRAINT uk_licencia_expediente
        UNIQUE (expediente_id),

    CONSTRAINT uk_licencia_resolucion
        UNIQUE (resolucion_id),

    CONSTRAINT chk_licencia_vigencia
        CHECK (tipo_vigencia IN ('INDETERMINADA', 'TEMPORAL')),

    CONSTRAINT chk_licencia_estado
        CHECK (estado IN ('EMITIDA', 'SUSPENDIDA', 'REVOCADA', 'VENCIDA'))
);

CREATE INDEX IF NOT EXISTS idx_licencia_expediente ON licencias_emitidas (expediente_id);
CREATE INDEX IF NOT EXISTS idx_licencia_establecimiento ON licencias_emitidas (establecimiento_id);
CREATE INDEX IF NOT EXISTS idx_licencia_resolucion ON licencias_emitidas (resolucion_id);

-- 6. Historial inmutable de emisión y actos sobre la licencia
CREATE TABLE IF NOT EXISTS historial_emision_licencias (
    id UUID PRIMARY KEY,
    licencia_id UUID NOT NULL,
    accion VARCHAR(50) NOT NULL,
    estado_anterior VARCHAR(20),
    estado_nuevo VARCHAR(20) NOT NULL,
    actor VARCHAR(100) NOT NULL,
    detalle TEXT,
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_historial_licencia
        FOREIGN KEY (licencia_id) REFERENCES licencias_emitidas(id)
        ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_historial_licencia_id ON historial_emision_licencias (licencia_id);

-- 7. Tabla de operaciones y transacciones de firma digital institucional
CREATE TABLE IF NOT EXISTS operaciones_firma (
    id UUID PRIMARY KEY,
    resolucion_id UUID NOT NULL,
    id_transaccion_remota VARCHAR(100) NOT NULL,
    estado VARCHAR(30) NOT NULL,
    hash_pre_firma VARCHAR(64) NOT NULL,
    hash_post_firma VARCHAR(64),
    intentos INT NOT NULL DEFAULT 1,
    fecha_inicio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_fin TIMESTAMP,
    codigo_error VARCHAR(50),
    mensaje_error TEXT,
    creado_por VARCHAR(100) NOT NULL,

    CONSTRAINT fk_operacion_firma_resolucion
        FOREIGN KEY (resolucion_id) REFERENCES resoluciones_administrativas(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_operacion_firma_estado
        CHECK (estado IN ('INICIADA', 'ENVIADA', 'COMPLETADA', 'FALLIDA', 'TIMEOUT'))
);

CREATE INDEX IF NOT EXISTS idx_operacion_firma_res ON operaciones_firma (resolucion_id);
CREATE INDEX IF NOT EXISTS idx_operacion_firma_tx ON operaciones_firma (id_transaccion_remota);

