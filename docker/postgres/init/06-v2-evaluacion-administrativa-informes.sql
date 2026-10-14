-- ============================================================================
-- MUNIHUAMANGA V2 — INCREMENTO I07: EVALUACIÓN ADMINISTRATIVA E INFORMES TÉCNICOS
-- Script: 06-v2-evaluacion-administrativa-informes.sql
-- Módulo: M08 (Evaluación Administrativa, Informes Técnicos y Rectificación)
-- ============================================================================

-- 1. Tabla de Informes Administrativos Técnicos
CREATE TABLE IF NOT EXISTS informes_administrativos (
    id UUID PRIMARY KEY,
    expediente_id UUID NOT NULL REFERENCES expedientes(id) ON DELETE RESTRICT,
    numero_informe VARCHAR(50) NOT NULL,
    version INT NOT NULL DEFAULT 1,
    evaluador_id UUID NOT NULL,
    evaluador_nombre VARCHAR(200) NOT NULL,
    revisor_id UUID,
    revisor_nombre VARCHAR(200),
    aprobador_id UUID,
    aprobador_nombre VARCHAR(200),
    fecha_elaboracion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_revision TIMESTAMP,
    fecha_aprobacion TIMESTAMP,
    conclusion VARCHAR(30) NOT NULL CHECK (conclusion IN ('FAVORABLE', 'DESFAVORABLE', 'REQUIERE_SUBSANACION')),
    estado VARCHAR(30) NOT NULL CHECK (estado IN ('BORRADOR', 'EN_REVISION', 'OBSERVADO', 'VISADO', 'APROBADO')),
    fundamentos_administrativos TEXT NOT NULL,
    referencias_documentales TEXT NOT NULL,
    resultados_checklist_json TEXT,
    observaciones_revision TEXT,
    es_version_actual BOOLEAN NOT NULL DEFAULT TRUE,
    version_anterior_id UUID REFERENCES informes_administrativos(id) ON DELETE RESTRICT
);

-- Índices de consulta y unicidad parcial
CREATE INDEX IF NOT EXISTS idx_inf_expediente ON informes_administrativos(expediente_id);
CREATE INDEX IF NOT EXISTS idx_inf_estado ON informes_administrativos(estado);
CREATE INDEX IF NOT EXISTS idx_inf_numero ON informes_administrativos(numero_informe);

-- Garantía de única versión activa por expediente
CREATE UNIQUE INDEX IF NOT EXISTS uk_informe_expediente_actual 
ON informes_administrativos (expediente_id) 
WHERE (es_version_actual = TRUE);

-- Unicidad de número de informe y versión
CREATE UNIQUE INDEX IF NOT EXISTS uk_informe_numero_version 
ON informes_administrativos (numero_informe, version);


-- 2. Historial Inmutable de Decisiones y Revisiones de Informes
CREATE TABLE IF NOT EXISTS historial_informes_administrativos (
    id UUID PRIMARY KEY,
    informe_id UUID NOT NULL REFERENCES informes_administrativos(id) ON DELETE RESTRICT,
    accion VARCHAR(50) NOT NULL,
    estado_anterior VARCHAR(30),
    estado_nuevo VARCHAR(30) NOT NULL,
    responsable VARCHAR(200) NOT NULL,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    motivo TEXT
);

CREATE INDEX IF NOT EXISTS idx_hist_inf_informe ON historial_informes_administrativos(informe_id);
CREATE INDEX IF NOT EXISTS idx_hist_inf_fecha ON historial_informes_administrativos(fecha);


-- 3. Solicitudes de Rectificación de Coordenadas Geográficas (Criterio CA-08)
CREATE TABLE IF NOT EXISTS solicitudes_rectificacion_ubicacion (
    id UUID PRIMARY KEY,
    establecimiento_id UUID NOT NULL REFERENCES establecimientos(id) ON DELETE RESTRICT,
    expediente_id UUID REFERENCES expedientes(id) ON DELETE RESTRICT,
    latitud_original NUMERIC(10, 7) NOT NULL,
    longitud_original NUMERIC(10, 7) NOT NULL,
    latitud_propuesta NUMERIC(10, 7) NOT NULL,
    longitud_propuesta NUMERIC(10, 7) NOT NULL,
    distancia_metros_aprox NUMERIC(10, 2),
    justificacion TEXT NOT NULL,
    documento_sustento_id UUID REFERENCES documentos_versionados(id) ON DELETE RESTRICT,
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE', 'APROBADA', 'RECHAZADA')),
    solicitante VARCHAR(200) NOT NULL,
    fecha_solicitud TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    evaluador VARCHAR(200),
    fecha_evaluacion TIMESTAMP,
    motivo_respuesta TEXT,
    es_traslado_fisico BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_rect_establecimiento ON solicitudes_rectificacion_ubicacion(establecimiento_id);
CREATE INDEX IF NOT EXISTS idx_rect_estado ON solicitudes_rectificacion_ubicacion(estado);

-- 4. Secuencia Anual Atómica para Numeración de Informes Técnicos (INF-YYYY-XXXXX)
CREATE TABLE IF NOT EXISTS secuencias_informe (
    anio INTEGER PRIMARY KEY,
    ultimo_correlativo INTEGER NOT NULL DEFAULT 0
);

INSERT INTO secuencias_informe (anio, ultimo_correlativo)
VALUES (2026, 0)
ON CONFLICT (anio) DO NOTHING;

