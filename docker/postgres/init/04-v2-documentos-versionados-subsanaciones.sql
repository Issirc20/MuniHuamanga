-- ==============================================================================
-- MUNIHUAMANGA V2 — MIGRACIÓN ADITIVA I05 / I05.1 (AUDITORÍA DE INMUTABILIDAD)
-- Módulo: Gestión Documental, Versionado Inmutable y Subsanación de Observaciones
-- Marco Legal: Ley N° 27444 LPAG (Art. 136) / Ley N° 28976 / D.S. N° 002-2018-PCM
-- Política de Retención: Archivo General de la Nación (AGN) - Conservación Inmutable
-- ==============================================================================

-- 1. Tabla de Documentos Versionados (Expedientes y Borradores)
-- REGLA DE INMUTABILIDAD I05.1: Se elimina ON DELETE CASCADE para expedientes formales.
-- Se aplica ON DELETE RESTRICT para garantizar la conservación histórica del acervo documentario.
CREATE TABLE IF NOT EXISTS documentos_versionados (
    id UUID PRIMARY KEY,
    expediente_id UUID REFERENCES expedientes(id) ON DELETE RESTRICT,
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
    version_anterior_id UUID REFERENCES documentos_versionados(id) ON DELETE RESTRICT,
    motivo_actualizacion TEXT,
    es_version_actual BOOLEAN NOT NULL DEFAULT TRUE,
    observacion_id UUID,
    CONSTRAINT uk_doc_expediente_version UNIQUE (expediente_id, tipo_documento, version),
    CONSTRAINT uk_doc_borrador_version UNIQUE (borrador_id, tipo_documento, version)
);

CREATE INDEX IF NOT EXISTS idx_doc_versionado_expediente ON documentos_versionados(expediente_id);
CREATE INDEX IF NOT EXISTS idx_doc_versionado_borrador ON documentos_versionados(borrador_id);

-- REGLA I05.1: Índice único parcial para garantizar que solo exista UNA versión activa (es_version_actual = TRUE)
-- por tipo documental en cada expediente y borrador
CREATE UNIQUE INDEX IF NOT EXISTS uk_doc_expediente_actual 
    ON documentos_versionados (expediente_id, tipo_documento) 
    WHERE (es_version_actual = TRUE);

CREATE UNIQUE INDEX IF NOT EXISTS uk_doc_borrador_actual 
    ON documentos_versionados (borrador_id, tipo_documento) 
    WHERE (es_version_actual = TRUE);

-- 2. Tabla de Observaciones Técnicas y Documentales
-- REGLA DE INMUTABILIDAD I05.1: ON DELETE RESTRICT para garantizar el historial de fiscalización y evaluación.
CREATE TABLE IF NOT EXISTS observaciones_expediente (
    id UUID PRIMARY KEY,
    expediente_id UUID NOT NULL REFERENCES expedientes(id) ON DELETE RESTRICT,
    documento_id UUID REFERENCES documentos_versionados(id) ON DELETE RESTRICT,
    requisito_afectado VARCHAR(100) NOT NULL,
    motivo TEXT NOT NULL,
    fundamento_legal VARCHAR(255),
    tipo_observacion VARCHAR(50) NOT NULL DEFAULT 'EVALUACION_TECNICA',
    fecha_observacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    observador_id UUID,
    observador_username VARCHAR(100),
    plazo_dias_habiles INTEGER NOT NULL DEFAULT 5,
    fecha_limite_subsanacion TIMESTAMP NOT NULL,
    subsanada BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_subsanacion TIMESTAMP,
    documento_subsanacion_id UUID REFERENCES documentos_versionados(id) ON DELETE RESTRICT,
    detalle_subsanacion TEXT
);

CREATE INDEX IF NOT EXISTS idx_obs_expediente ON observaciones_expediente(expediente_id);
CREATE INDEX IF NOT EXISTS idx_obs_subsanada ON observaciones_expediente(expediente_id, subsanada);
