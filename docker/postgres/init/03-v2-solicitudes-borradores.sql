
-- ==============================================================================
-- MUNIHUAMANGA V2 — MIGRACIÓN ADITIVA I04
-- Módulo M03: Solicitudes y Borradores
-- Módulo M06: Máquina de Estados y Presentación Formal
-- Procedimiento de Reclamación de Titularidad Histórica
-- ==============================================================================

-- 1. Secuencia segura ante concurrencia para numeración formal de expedientes
CREATE TABLE IF NOT EXISTS secuencias_expediente (
    anio INTEGER PRIMARY KEY,
    ultimo_correlativo INTEGER NOT NULL DEFAULT 0
);

INSERT INTO secuencias_expediente (anio, ultimo_correlativo)
VALUES (2026, 100)
ON CONFLICT (anio) DO NOTHING;

-- 2. Entidad SolicitudBorrador
CREATE TABLE IF NOT EXISTS solicitudes_borrador (
    id UUID PRIMARY KEY,
    cuenta_id UUID NOT NULL REFERENCES cuentas(id),
    establecimiento_id UUID NOT NULL REFERENCES establecimientos(id),
    creador_id UUID NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(30) NOT NULL DEFAULT 'EN_EDICION',
    version BIGINT NOT NULL DEFAULT 0,
    modalidad_tramite VARCHAR(50),
    nombre_comercial_declarado VARCHAR(255),
    giro_negocio_declarado VARCHAR(255),
    actividad_detallada TEXT,
    area_metros_cuadrados NUMERIC(10,2),
    aforo_personas INTEGER,
    numero_pisos INTEGER,
    horario_inicio VARCHAR(10),
    horario_fin VARCHAR(10),
    telefono_contacto VARCHAR(20),
    correo_notificacion VARCHAR(150),
    autoriza_notificacion BOOLEAN DEFAULT FALSE,
    nivel_riesgo_declarado VARCHAR(20),
    numero_operacion_sat VARCHAR(50),
    fecha_pago_sat DATE,
    monto_tasa NUMERIC(10,2),
    voucher_id VARCHAR(50),
    expediente_id UUID REFERENCES expedientes(id),
    motivo_descarte TEXT,
    fecha_descarte TIMESTAMP,
    usuario_descarte UUID
);

CREATE INDEX IF NOT EXISTS idx_borradores_cuenta ON solicitudes_borrador(cuenta_id);
CREATE INDEX IF NOT EXISTS idx_borradores_establecimiento ON solicitudes_borrador(establecimiento_id);
CREATE INDEX IF NOT EXISTS idx_borradores_estado ON solicitudes_borrador(estado);

-- 3. Documentos adjuntos al borrador
CREATE TABLE IF NOT EXISTS documentos_borrador (
    id UUID PRIMARY KEY,
    borrador_id UUID NOT NULL REFERENCES solicitudes_borrador(id) ON DELETE CASCADE,
    tipo_documento VARCHAR(50) NOT NULL,
    nombre_archivo VARCHAR(255) NOT NULL,
    tipo_contenido VARCHAR(100),
    tamanio_bytes BIGINT,
    hash_sha256 VARCHAR(64) NOT NULL,
    ruta_almacenamiento TEXT,
    fecha_carga TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cargado_por UUID NOT NULL,
    descripcion TEXT
);

CREATE INDEX IF NOT EXISTS idx_docs_borrador ON documentos_borrador(borrador_id);

-- 4. Reclamación y validación de titularidad de expedientes históricos
CREATE TABLE IF NOT EXISTS reclamaciones_expediente (
    id UUID PRIMARY KEY,
    expediente_id UUID NOT NULL REFERENCES expedientes(id),
    cuenta_id UUID NOT NULL REFERENCES cuentas(id),
    solicitante_username VARCHAR(100) NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    evidencia_titularidad TEXT NOT NULL,
    motivo TEXT,
    fecha_solicitud TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_resolucion TIMESTAMP,
    evaluador_id UUID,
    motivo_resolucion TEXT
);

CREATE INDEX IF NOT EXISTS idx_reclamaciones_expediente ON reclamaciones_expediente(expediente_id);
CREATE INDEX IF NOT EXISTS idx_reclamaciones_cuenta ON reclamaciones_expediente(cuenta_id);
