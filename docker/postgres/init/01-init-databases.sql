-- Script DDL de Inicialización de Base de Datos
-- Proyecto: Sistema de Gestión de Licencias de Funcionamiento - MuniHuamanga
-- Marco Legal: Ley N° 28976 / TUO D.S. N° 046-2017-PCM

CREATE TABLE IF NOT EXISTS expedientes (
    id UUID PRIMARY KEY,
    numero_tramite VARCHAR(30) NOT NULL UNIQUE,
    solicitante_id UUID NOT NULL,
    nombre_titular VARCHAR(150) NOT NULL,
    documento_identidad VARCHAR(20) NOT NULL,
    razon_social VARCHAR(150),
    nombre_comercial VARCHAR(150) NOT NULL,
    giro_negocio VARCHAR(150) NOT NULL,
    direccion_establecimiento VARCHAR(255) NOT NULL,
    area_metros_cuadrados NUMERIC(10, 2),
    estado VARCHAR(30) NOT NULL,
    nivel_riesgo VARCHAR(20),
    monto_tasa NUMERIC(10, 2),
    voucher_id VARCHAR(50),
    licencia_qr_code VARCHAR(255),
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_limite TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS historial_estados (
    id UUID PRIMARY KEY,
    expediente_id UUID NOT NULL,
    estado_anterior VARCHAR(30),
    estado_nuevo VARCHAR(30) NOT NULL,
    usuario VARCHAR(100) NOT NULL,
    motivo VARCHAR(500),
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_historial_expediente FOREIGN KEY (expediente_id) REFERENCES expedientes(id) ON DELETE CASCADE
);

-- Índices para optimización de consultas concurrentes (RNF-01 y RNF-02: <3s de respuesta)
CREATE INDEX IF NOT EXISTS idx_expedientes_numero_tramite ON expedientes(numero_tramite);
CREATE INDEX IF NOT EXISTS idx_expedientes_solicitante_id ON expedientes(solicitante_id);
CREATE INDEX IF NOT EXISTS idx_expedientes_estado ON expedientes(estado);
CREATE INDEX IF NOT EXISTS idx_historial_expediente_id ON historial_estados(expediente_id);
CREATE INDEX IF NOT EXISTS idx_historial_fecha ON historial_estados(fecha);

-- Datos semilla iniciales para pruebas del entorno
INSERT INTO expedientes (
    id, numero_tramite, solicitante_id, nombre_titular, documento_identidad,
    razon_social, nombre_comercial, giro_negocio, direccion_establecimiento,
    area_metros_cuadrados, estado, nivel_riesgo, monto_tasa, voucher_id,
    licencia_qr_code, fecha_creacion, fecha_limite
) VALUES (
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    'EXP-2026-00001',
    'f47ac10b-58cc-4372-a567-0e02b2c3d479',
    'María Quispe Huamán',
    '42567891',
    'INVERSIONES LOS RETABLOS S.A.C.',
    'Boutique Artesanal Huamanga',
    'Venta de artesanías, retablos y souvenirs',
    'Jr. 9 de Diciembre N° 142, Centro Histórico, Ayacucho',
    35.50,
    'FORMATOS_GENERADOS',
    NULL,
    NULL,
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '21 days'
) ON CONFLICT (numero_tramite) DO NOTHING;

INSERT INTO historial_estados (
    id, expediente_id, estado_anterior, estado_nuevo, usuario, motivo, fecha
) VALUES (
    'b2c3d4e5-f6a7-8901-bcde-f12345678901',
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    NULL,
    'FORMATOS_GENERADOS',
    'María Quispe Huamán',
    'Ingreso virtual de solicitud y generación de anexos preliminares',
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;
