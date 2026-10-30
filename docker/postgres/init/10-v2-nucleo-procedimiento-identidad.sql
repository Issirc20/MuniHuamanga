-- ============================================================================
-- Núcleo del procedimiento (Módulo 1) e identidad y sesiones (Módulo 2)
-- Máquina de estados por eventos, etapa ITSE, plazos, notificación de licencias,
-- roles múltiples, bloqueo de acceso, activación por enlace y sesiones revocables.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. Expediente: control de concurrencia, retorno tras observación y archivo
-- ----------------------------------------------------------------------------
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS estado_previo_observacion VARCHAR(30);
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS motivo_archivo VARCHAR(30);
ALTER TABLE expedientes ALTER COLUMN fecha_limite DROP NOT NULL;

ALTER TABLE expedientes DROP CONSTRAINT IF EXISTS ck_expedientes_estado;
ALTER TABLE expedientes ADD CONSTRAINT ck_expedientes_estado CHECK (estado IN (
    'PRESENTADO', 'ITSE_EN_TRAMITE', 'ITSE_EN_REVISION', 'PENDIENTE_PAGO', 'EN_EVALUACION',
    'OBSERVADO', 'SUBSANADO', 'APTO_PARA_RESOLUCION', 'APROBADO', 'DENEGADO',
    'EMITIDO', 'CONCLUIDO', 'ARCHIVADO'));

-- Historial: hecho del procedimiento y persona que lo registró
ALTER TABLE historial_estados ADD COLUMN IF NOT EXISTS evento VARCHAR(50);
ALTER TABLE historial_estados ADD COLUMN IF NOT EXISTS usuario_id UUID;
CREATE INDEX IF NOT EXISTS idx_historial_expediente_estado ON historial_estados(expediente_id, estado_nuevo, fecha);

-- ----------------------------------------------------------------------------
-- 2. Etapa ITSE: anexos firmados por Defensa Civil, verificación y liquidación
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS itse_expedientes (
    id UUID PRIMARY KEY,
    expediente_id UUID NOT NULL UNIQUE REFERENCES expedientes(id) ON DELETE CASCADE,
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('PENDIENTE_CARGA', 'EN_REVISION', 'OBSERVADO', 'VALIDADO')),
    canal VARCHAR(20) CHECK (canal IN ('VIRTUAL', 'PRESENCIAL')),
    nivel_declarado VARCHAR(20),
    nivel_oficial VARCHAR(20),
    monto_liquidado NUMERIC(10, 2),
    concepto_liquidacion VARCHAR(200),
    fecha_liquidacion TIMESTAMP,
    liquidacion_definitiva BOOLEAN NOT NULL DEFAULT FALSE,
    cargado_por_id UUID,
    cargado_por VARCHAR(150),
    fecha_carga TIMESTAMP,
    comentario_carga VARCHAR(500),
    validado_por_id UUID,
    validado_por VARCHAR(150),
    fecha_validacion TIMESTAMP,
    comentario_validacion VARCHAR(500),
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_itse_estado ON itse_expedientes(estado);

CREATE TABLE IF NOT EXISTS itse_observaciones (
    id UUID PRIMARY KEY,
    expediente_itse_id UUID NOT NULL REFERENCES itse_expedientes(id) ON DELETE CASCADE,
    anexo VARCHAR(20) NOT NULL CHECK (anexo IN ('ANEXO_1', 'ANEXO_2', 'ANEXO_3', 'ANEXO_4')),
    tipo VARCHAR(40) NOT NULL CHECK (tipo IN ('SUBSANABLE_POR_ADMINISTRADO', 'REQUIERE_DEFENSA_CIVIL')),
    motivo VARCHAR(500) NOT NULL,
    observado_por_id UUID,
    observado_por VARCHAR(150) NOT NULL,
    fecha_observacion TIMESTAMP NOT NULL,
    atendida BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_atencion TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_itse_observaciones_etapa ON itse_observaciones(expediente_itse_id);

-- ----------------------------------------------------------------------------
-- 3. Plazos: calendario de días no laborables y parámetros configurables
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dias_no_laborables (
    fecha DATE PRIMARY KEY,
    descripcion VARCHAR(150) NOT NULL,
    base_legal VARCHAR(200),
    registrado_por VARCHAR(100) NOT NULL,
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS parametros_plazo (
    codigo VARCHAR(40) PRIMARY KEY,
    dias INTEGER NOT NULL CHECK (dias BETWEEN 1 AND 365),
    actualizado_por VARCHAR(100) NOT NULL,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- 4. Notificación de la licencia (inicio del plazo de recursos y condición de cierre)
-- ----------------------------------------------------------------------------
ALTER TABLE licencias_emitidas ADD COLUMN IF NOT EXISTS fecha_notificacion TIMESTAMP;
ALTER TABLE licencias_emitidas ADD COLUMN IF NOT EXISTS medio_notificacion VARCHAR(30);
ALTER TABLE licencias_emitidas ADD COLUMN IF NOT EXISTS constancia_notificacion VARCHAR(200);

-- ----------------------------------------------------------------------------
-- 5. Identidad: roles múltiples, bloqueo temporal y activación por enlace
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS usuarios_roles (
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    rol VARCHAR(40) NOT NULL CHECK (rol IN (
        'ROLE_ADMIN', 'ROLE_MESA_DE_PARTES', 'ROLE_EVALUADOR', 'ROLE_SUPERVISOR_LICENCIAS',
        'ROLE_AUTORIDAD_RESOLUTIVA', 'ROLE_CAJERO', 'ROLE_INSPECTOR', 'ROLE_AUDITOR', 'ROLE_CIUDADANO')),
    PRIMARY KEY (usuario_id, rol)
);

-- Migración del rol único anterior al modelo de roles múltiples
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'usuarios' AND column_name = 'rol') THEN
        INSERT INTO usuarios_roles (usuario_id, rol)
        SELECT id, rol FROM usuarios
        WHERE rol IN ('ROLE_ADMIN', 'ROLE_EVALUADOR', 'ROLE_CAJERO', 'ROLE_INSPECTOR', 'ROLE_CIUDADANO')
        ON CONFLICT DO NOTHING;
        ALTER TABLE usuarios DROP COLUMN rol;
    END IF;
END $$;

ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS pendiente_activacion BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS token_activacion_hash VARCHAR(64);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS token_activacion_expira TIMESTAMP;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS intentos_fallidos INTEGER NOT NULL DEFAULT 0;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS bloqueado_hasta TIMESTAMP;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS fecha_ultimo_acceso TIMESTAMP;
CREATE UNIQUE INDEX IF NOT EXISTS uq_usuarios_token_activacion ON usuarios(token_activacion_hash) WHERE token_activacion_hash IS NOT NULL;

-- Validación presencial de la identidad declarada del administrado
ALTER TABLE cuentas ADD COLUMN IF NOT EXISTS medio_validacion_identidad VARCHAR(40);
ALTER TABLE cuentas ADD COLUMN IF NOT EXISTS referencia_validacion_identidad VARCHAR(200);
ALTER TABLE cuentas ADD COLUMN IF NOT EXISTS identidad_validada_por VARCHAR(150);
ALTER TABLE cuentas ADD COLUMN IF NOT EXISTS fecha_validacion_identidad TIMESTAMP;

-- ----------------------------------------------------------------------------
-- 6. Sesiones con token de renovación rotativo y revocable
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sesiones_usuario (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    token_renovacion_hash VARCHAR(64) NOT NULL UNIQUE,
    fecha_creacion TIMESTAMP NOT NULL,
    fecha_ultima_renovacion TIMESTAMP,
    fecha_expiracion TIMESTAMP NOT NULL,
    revocada BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_revocacion TIMESTAMP,
    motivo_revocacion VARCHAR(100),
    direccion_ip VARCHAR(45),
    agente_usuario VARCHAR(255)
);
CREATE INDEX IF NOT EXISTS idx_sesiones_usuario ON sesiones_usuario(usuario_id, revocada);
