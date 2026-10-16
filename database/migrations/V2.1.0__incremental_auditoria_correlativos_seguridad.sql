-- ============================================================================
-- MIGRACIÓN INCREMENTAL VERSIONADA: V2.1.0__incremental_auditoria_correlativos_seguridad.sql
-- Propósito: Actualización incremental sobre base existente
-- - Índices de alta concurrencia para expedientes, pagos, resoluciones y licencias
-- - Índice de auditoría de inspecciones QR para fiscalización en tiempo real
-- - Registro oficial en tabla de versiones versionada
-- ============================================================================

DO $$
BEGIN
    -- 1. Índices de concurrencia y búsqueda rápida
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_expedientes_numero_tramite_btree') THEN
        CREATE INDEX idx_expedientes_numero_tramite_btree ON expedientes(numero_tramite);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_expedientes_estado_fecha') THEN
        CREATE INDEX idx_expedientes_estado_fecha ON expedientes(estado, fecha_creacion DESC);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_licencias_numero_estado') THEN
        CREATE INDEX idx_licencias_numero_estado ON licencias_emitidas(numero_licencia, estado);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_pagos_operacion_comprobante') THEN
        CREATE INDEX idx_pagos_operacion_comprobante ON pagos_expediente(numero_operacion_sat, codigo_comprobante);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_inspeccion_licencia_fecha') THEN
        CREATE INDEX idx_inspeccion_licencia_fecha ON auditoria_consultas_inspeccion(licencia_id, fecha_consulta DESC);
    END IF;

    -- 2. Aseguramiento de integridad en secuencias atómicas
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_secuencias_expediente_anio') THEN
        ALTER TABLE secuencias_expediente ADD CONSTRAINT uq_secuencias_expediente_anio UNIQUE (anio);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_secuencias_resolucion_anio') THEN
        ALTER TABLE secuencias_resolucion ADD CONSTRAINT uq_secuencias_resolucion_anio UNIQUE (anio);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_secuencias_licencia_anio') THEN
        ALTER TABLE secuencias_licencia ADD CONSTRAINT uq_secuencias_licencia_anio UNIQUE (anio);
    END IF;

END $$;

-- 3. Registro de migración aplicada en schema_version
INSERT INTO schema_version (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success)
SELECT 2, '2.1.0', 'Optimizacion indices concurrencia, restricciones integridad secuencias y auditoria QR', 'SQL', 'V2.1.0__incremental_auditoria_correlativos_seguridad.sql', 20261016, CURRENT_USER, 22, true
WHERE NOT EXISTS (SELECT 1 FROM schema_version WHERE version = '2.1.0');
