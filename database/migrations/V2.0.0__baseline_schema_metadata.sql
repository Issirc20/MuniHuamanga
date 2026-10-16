-- ============================================================================
-- MIGRACIÓN VERSIONADA: V2.0.0__baseline_schema_metadata.sql
-- Propósito: Tabla de control de versiones y registro de línea base V2.0
-- Compatible con estándares Flyway / DBA Enterprise
-- ============================================================================

CREATE TABLE IF NOT EXISTS schema_version (
    installed_rank INTEGER NOT NULL,
    version VARCHAR(50),
    description VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    script VARCHAR(1000) NOT NULL,
    checksum INTEGER,
    installed_by VARCHAR(100) NOT NULL,
    installed_on TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    execution_time INTEGER NOT NULL,
    success BOOLEAN NOT NULL,
    CONSTRAINT pk_schema_version PRIMARY KEY (installed_rank)
);

CREATE INDEX IF NOT EXISTS idx_schema_version_s_idx ON schema_version (success);

-- Registro de línea base si no existe
INSERT INTO schema_version (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success)
SELECT 1, '2.0.0', 'Linea Base DDL Esquema V2 (Scripts 01-08)', 'SQL', 'V2.0.0__baseline_schema_metadata.sql', 20261015, CURRENT_USER, 15, true
WHERE NOT EXISTS (SELECT 1 FROM schema_version WHERE version = '2.0.0');
