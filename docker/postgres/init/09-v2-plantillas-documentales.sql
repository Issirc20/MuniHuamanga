-- ============================================================================
-- SCRIPT DE MIGRACIÓN: 09-v2-plantillas-documentales.sql
-- MÓDULO: Administración, Versionado Inmutable y Auditoría de Plantillas Documentales Oficiales
-- 4 ANEXOS ITSE + 1 DECLARACIÓN JURADA DE LICENCIA
-- Municipalidad Provincial de Huamanga — Licencias de Funcionamiento V2
-- ============================================================================

-- 1. Tabla de cabecera de plantillas documentales vigentes
CREATE TABLE IF NOT EXISTS plantillas_documentales (
    id UUID PRIMARY KEY,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    nombre VARCHAR(200) NOT NULL,
    descripcion TEXT,
    base_legal VARCHAR(255),
    grupo VARCHAR(30) NOT NULL, -- 'ITSE' o 'LICENCIA'
    version_actual INTEGER NOT NULL DEFAULT 1,
    version_texto VARCHAR(30) NOT NULL,
    nombre_archivo VARCHAR(255) NOT NULL,
    hash_sha256 VARCHAR(64) NOT NULL,
    tamanio_bytes BIGINT,
    ruta_almacenamiento TEXT,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_por VARCHAR(100),
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

-- 2. Tabla de historial inmutable de versiones de plantillas
CREATE TABLE IF NOT EXISTS plantillas_documentales_versiones (
    id UUID PRIMARY KEY,
    plantilla_id UUID NOT NULL REFERENCES plantillas_documentales(id) ON DELETE RESTRICT,
    codigo_plantilla VARCHAR(50) NOT NULL,
    version INTEGER NOT NULL,
    version_texto VARCHAR(30) NOT NULL,
    nombre_archivo VARCHAR(255) NOT NULL,
    hash_sha256 VARCHAR(64) NOT NULL,
    tamanio_bytes BIGINT,
    ruta_almacenamiento TEXT,
    motivo_cambio TEXT,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    creado_por VARCHAR(100),
    es_version_vigente BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_plantilla_version UNIQUE (plantilla_id, version)
);

-- 3. Índices de optimización y trazabilidad
CREATE INDEX IF NOT EXISTS idx_plantillas_codigo ON plantillas_documentales(codigo);
CREATE INDEX IF NOT EXISTS idx_plantillas_grupo ON plantillas_documentales(grupo);
CREATE INDEX IF NOT EXISTS idx_plantillas_versiones_plantilla ON plantillas_documentales_versiones(plantilla_id);
CREATE INDEX IF NOT EXISTS idx_plantillas_versiones_codigo ON plantillas_documentales_versiones(codigo_plantilla);
CREATE INDEX IF NOT EXISTS idx_plantillas_versiones_fecha ON plantillas_documentales_versiones(fecha_creacion);

COMMENT ON TABLE plantillas_documentales IS 'Catálogo oficial de plantillas documentales vigentes para ITSE y Licencias con control de integridad SHA-256';
COMMENT ON TABLE plantillas_documentales_versiones IS 'Historial inmutable de versiones de plantillas documentales para auditoría y no retroactividad documental';
