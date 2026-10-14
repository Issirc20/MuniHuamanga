-- ============================================================================
-- SCRIPT DE MIGRACIÓN: 08-v2-verificacion-qr-inspeccion.sql
-- MÓDULO M10: Verificación QR Diferenciada y Consulta Móvil para Inspectores (Incremento I09)
-- Municipalidad Provincial de Huamanga — Licencias de Funcionamiento V2
-- ============================================================================

-- 1. Índice para optimización de consultas por token QR criptográfico no enumerable
CREATE UNIQUE INDEX IF NOT EXISTS idx_licencia_codigo_qr 
    ON licencias_emitidas(codigo_verificacion_qr);

-- 2. Tabla de auditoría de consultas móviles de fiscalización e inspección
CREATE TABLE IF NOT EXISTS auditoria_consultas_inspeccion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    licencia_id UUID NOT NULL REFERENCES licencias_emitidas(id) ON DELETE RESTRICT,
    token_qr VARCHAR(100) NOT NULL,
    inspector_username VARCHAR(100) NOT NULL,
    fecha_consulta TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_origen VARCHAR(45),
    user_agent TEXT,
    resultado VARCHAR(50) NOT NULL
);

-- 3. Índices para auditoría y trazabilidad de fiscalización
CREATE INDEX IF NOT EXISTS idx_audit_insp_licencia 
    ON auditoria_consultas_inspeccion(licencia_id);

CREATE INDEX IF NOT EXISTS idx_audit_insp_fecha 
    ON auditoria_consultas_inspeccion(fecha_consulta);

CREATE INDEX IF NOT EXISTS idx_audit_insp_usuario 
    ON auditoria_consultas_inspeccion(inspector_username);

COMMENT ON TABLE auditoria_consultas_inspeccion IS 'Registro inmutable de auditoría para consultas de fiscalización en campo por inspectores municipales (M10 / I09)';
