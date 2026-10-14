-- ==============================================================================
-- MUNIHUAMANGA V2 — MIGRACIÓN ADITIVA I06
-- Módulo: Recepción, Revisión y Validación Manual de Pagos SAT y Evaluación de Zonificación
-- Marco Legal: Ley N° 28976 / TUO D.S. N° 046-2017-PCM / Ley N° 27444 LPAG / Ordenanza N° 018-2024-MPH
-- Regla de Negocio: No integraciones automáticas con SAT ni emisión de dictámenes de zonificación automáticos.
-- ==============================================================================

-- 1. Tabla de Registro y Validación Manual de Pagos SAT (M05)
CREATE TABLE IF NOT EXISTS pagos_expediente (
    id UUID PRIMARY KEY,
    expediente_id UUID NOT NULL REFERENCES expedientes(id) ON DELETE RESTRICT,
    codigo_comprobante VARCHAR(60) NOT NULL,
    fecha_declarada DATE NOT NULL,
    monto_declarado NUMERIC(10, 2) NOT NULL,
    documento_id UUID REFERENCES documentos_versionados(id) ON DELETE RESTRICT,
    observaciones_administrado TEXT,
    estado VARCHAR(40) NOT NULL DEFAULT 'PENDIENTE_VALIDACION',
    validador_id UUID,
    validador_nombre VARCHAR(150),
    fecha_validacion TIMESTAMP,
    motivo_observacion_rechazo TEXT,
    numero_operacion_sat VARCHAR(60),
    fecha_pago_real DATE,
    monto_validado NUMERIC(10, 2),
    tasa_tupa_esperada NUMERIC(10, 2),
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_pago_codigo_comprobante UNIQUE (codigo_comprobante)
);

CREATE INDEX IF NOT EXISTS idx_pago_expediente ON pagos_expediente(expediente_id);
CREATE INDEX IF NOT EXISTS idx_pago_estado ON pagos_expediente(estado);
CREATE INDEX IF NOT EXISTS idx_pago_fecha ON pagos_expediente(fecha_declarada);

-- 2. Historial de Decisiones y Trazabilidad de Pagos (Auditoría I06)
CREATE TABLE IF NOT EXISTS historial_decisiones_pago (
    id UUID PRIMARY KEY,
    pago_id UUID NOT NULL REFERENCES pagos_expediente(id) ON DELETE RESTRICT,
    estado_anterior VARCHAR(40) NOT NULL,
    estado_nuevo VARCHAR(40) NOT NULL,
    usuario_id UUID,
    usuario_nombre VARCHAR(150),
    motivo TEXT NOT NULL,
    fecha_decision TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_historial_pago ON historial_decisiones_pago(pago_id);

-- 3. Tabla de Evaluación Técnica de Zonificación y Compatibilidad de Uso (M07)
CREATE TABLE IF NOT EXISTS evaluaciones_zonificacion (
    id UUID PRIMARY KEY,
    expediente_id UUID NOT NULL REFERENCES expedientes(id) ON DELETE RESTRICT,
    establecimiento_id UUID REFERENCES establecimientos(id) ON DELETE RESTRICT,
    direccion_declarada VARCHAR(255) NOT NULL,
    latitud_declarada NUMERIC(10, 7),
    longitud_declarada NUMERIC(10, 7),
    codigo_ciiu VARCHAR(20),
    giro_comercial VARCHAR(150) NOT NULL,
    zonificacion_pdu VARCHAR(50),
    referencia_zonificacion VARCHAR(255) NOT NULL,
    fuente_sustento_tecnico VARCHAR(255) NOT NULL,
    resultado VARCHAR(40) NOT NULL DEFAULT 'PENDIENTE_EVALUACION',
    evaluador_id UUID,
    evaluador_nombre VARCHAR(150),
    fecha_evaluacion TIMESTAMP,
    observaciones TEXT,
    documento_sustentatorio_id UUID REFERENCES documentos_versionados(id) ON DELETE RESTRICT,
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_eval_zonif_expediente ON evaluaciones_zonificacion(expediente_id);
CREATE INDEX IF NOT EXISTS idx_eval_zonif_resultado ON evaluaciones_zonificacion(resultado);
