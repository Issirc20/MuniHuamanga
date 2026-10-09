-- Script DDL de Inicialización y Migración de Base de Datos
-- Proyecto: Sistema de Gestión de Licencias de Funcionamiento - MuniHuamanga
-- Marco Legal: Ley N° 28976 / TUO D.S. N° 046-2017-PCM / D.S. N° 002-2018-PCM / D.S. N° 163-2020-PCM

CREATE TABLE IF NOT EXISTS expedientes (
    id UUID PRIMARY KEY,
    numero_tramite VARCHAR(30) NOT NULL UNIQUE,
    solicitante_id UUID NOT NULL,

    -- Sección I: Modalidad del Trámite (Anexo 1)
    modalidad_tramite VARCHAR(40) DEFAULT 'LICENCIA_INDETERMINADA',
    plazo_temporal_meses INTEGER,
    tipo_anuncio VARCHAR(100),
    numero_licencia_principal VARCHAR(50),

    -- Sección II: Datos del Solicitante
    tipo_persona VARCHAR(20) DEFAULT 'NATURAL',
    tipo_documento VARCHAR(20) DEFAULT 'DNI',
    nombre_titular VARCHAR(150) NOT NULL,
    documento_identidad VARCHAR(20) NOT NULL,
    razon_social VARCHAR(150),
    correo_electronico VARCHAR(120),
    telefono VARCHAR(20),
    autoriza_notificacion BOOLEAN DEFAULT TRUE,

    -- Sección III: Representante Legal o Apoderado (SUNARP)
    partida_sunarp VARCHAR(50),
    asiento_sunarp VARCHAR(50),
    dni_representante VARCHAR(20),
    nombre_representante VARCHAR(150),
    poder_sunarp VARCHAR(100),

    -- Sección IV: Datos del Establecimiento
    nombre_comercial VARCHAR(150) NOT NULL,
    ciiu_codigo VARCHAR(20),
    giro_negocio VARCHAR(150) NOT NULL,
    actividad_detallada VARCHAR(255),
    zonificacion VARCHAR(50),
    funcion_edificacion VARCHAR(40) DEFAULT 'COMERCIO',
    direccion_establecimiento VARCHAR(255) NOT NULL,

    -- Dirección desglosada del Anexo 1
    tipo_via VARCHAR(30),
    nombre_via VARCHAR(150),
    numero_vivienda VARCHAR(30),
    interior VARCHAR(30),
    manzana VARCHAR(30),
    lote VARCHAR(30),
    urbanizacion VARCHAR(100),
    distrito VARCHAR(100) DEFAULT 'Ayacucho',
    provincia VARCHAR(100) DEFAULT 'Huamanga',
    departamento VARCHAR(100) DEFAULT 'Ayacucho',
    referencia_ubicacion VARCHAR(255),

    -- Dimensionamiento y aforo
    area_metros_cuadrados NUMERIC(10, 2),
    area_terreno NUMERIC(10, 2),
    area_techada_total NUMERIC(10, 2),
    area_ocupada_total NUMERIC(10, 2),
    aforo_personas INTEGER,
    numero_pisos INTEGER,
    antiguedad_edificacion INTEGER,
    antiguedad_giro INTEGER,

    -- Autorización Sectorial
    requiere_autorizacion_sectorial BOOLEAN DEFAULT FALSE,
    sector_entidad VARCHAR(150),
    sector_denominacion VARCHAR(200),
    sector_fecha VARCHAR(30),
    sector_numero VARCHAR(50),

    -- Flujo Transaccional e ITSE
    estado VARCHAR(30) NOT NULL,
    nivel_riesgo VARCHAR(20),
    monto_tasa NUMERIC(10, 2),
    voucher_id VARCHAR(50),
    licencia_qr_code VARCHAR(255),

    -- Evidencias Externas / Fase 1
    numero_informe_itse VARCHAR(50),
    fecha_informe_itse TIMESTAMP,
    numero_operacion_sat VARCHAR(50),
    fecha_pago_sat TIMESTAMP,

    -- Observaciones y Subsanaciones (Ley N° 27444 LPAG / Ley N° 28976)
    motivo_observacion VARCHAR(500),
    fecha_observacion TIMESTAMP,
    fecha_subsanacion TIMESTAMP,
    detalle_subsanacion VARCHAR(500),

    -- Anexo 4: Condiciones de Seguridad (Embebido)
    a4_area_terreno NUMERIC(10, 2),
    a4_area_piso_1 NUMERIC(10, 2),
    a4_area_piso_2 NUMERIC(10, 2),
    a4_area_piso_3 NUMERIC(10, 2),
    a4_area_piso_4 NUMERIC(10, 2),
    a4_area_otros_pisos NUMERIC(10, 2),
    a4_area_techada_total NUMERIC(10, 2),
    a4_area_ocupada_total NUMERIC(10, 2),
    a4_aforo_personas INTEGER,
    a4_antiguedad_edificacion INTEGER,
    a4_antiguedad_giro INTEGER,
    a4_no_proceso_construccion BOOLEAN DEFAULT TRUE,
    a4_cuenta_servicios_basicos BOOLEAN DEFAULT TRUE,
    a4_cuenta_mobiliario_basico BOOLEAN DEFAULT TRUE,
    a4_tiene_equipos_instalados BOOLEAN DEFAULT TRUE,
    a4_medios_evacuacion_libres BOOLEAN DEFAULT TRUE,
    a4_senalizacion_seguridad BOOLEAN DEFAULT TRUE,
    a4_luces_emergencia BOOLEAN DEFAULT TRUE,
    a4_tablero_electrico_protegido BOOLEAN DEFAULT TRUE,
    a4_interruptores_diferenciales BOOLEAN DEFAULT TRUE,
    a4_pozo_tierra_vigente BOOLEAN DEFAULT TRUE,
    a4_extintores_operativos BOOLEAN DEFAULT TRUE,
    a4_estructuras_sin_colapso BOOLEAN DEFAULT TRUE,
    a4_cables_protegidos_pvc BOOLEAN DEFAULT TRUE,

    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_limite TIMESTAMP NOT NULL
);

-- Script de Migración idempotente para bases existentes
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS modalidad_tramite VARCHAR(40) DEFAULT 'LICENCIA_INDETERMINADA';
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS plazo_temporal_meses INTEGER;
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS tipo_anuncio VARCHAR(100);
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS numero_licencia_principal VARCHAR(50);

ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS tipo_persona VARCHAR(20) DEFAULT 'NATURAL';
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS tipo_documento VARCHAR(20) DEFAULT 'DNI';
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS autoriza_notificacion BOOLEAN DEFAULT TRUE;

ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS partida_sunarp VARCHAR(50);
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS asiento_sunarp VARCHAR(50);
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS dni_representante VARCHAR(20);
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS nombre_representante VARCHAR(150);
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS poder_sunarp VARCHAR(100);

ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS ciiu_codigo VARCHAR(20);
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS actividad_detallada VARCHAR(255);
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS zonificacion VARCHAR(50);
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS funcion_edificacion VARCHAR(40) DEFAULT 'COMERCIO';

ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS tipo_via VARCHAR(30);
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS nombre_via VARCHAR(150);
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS numero_vivienda VARCHAR(30);
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS interior VARCHAR(30);
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS manzana VARCHAR(30);
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS lote VARCHAR(30);
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS urbanizacion VARCHAR(100);
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS distrito VARCHAR(100) DEFAULT 'Ayacucho';
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS provincia VARCHAR(100) DEFAULT 'Huamanga';
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS departamento VARCHAR(100) DEFAULT 'Ayacucho';
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS referencia_ubicacion VARCHAR(255);

ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS area_terreno NUMERIC(10, 2);
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS area_techada_total NUMERIC(10, 2);
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS area_ocupada_total NUMERIC(10, 2);
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS aforo_personas INTEGER;
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS numero_pisos INTEGER;
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS antiguedad_edificacion INTEGER;
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS antiguedad_giro INTEGER;

ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS requiere_autorizacion_sectorial BOOLEAN DEFAULT FALSE;
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS sector_entidad VARCHAR(150);
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS sector_denominacion VARCHAR(200);
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS sector_fecha VARCHAR(30);
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS sector_numero VARCHAR(50);

ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS numero_informe_itse VARCHAR(50);
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS fecha_informe_itse TIMESTAMP;
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS numero_operacion_sat VARCHAR(50);
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS fecha_pago_sat TIMESTAMP;

ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS motivo_observacion VARCHAR(500);
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS fecha_observacion TIMESTAMP;
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS fecha_subsanacion TIMESTAMP;
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS detalle_subsanacion VARCHAR(500);

ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS a4_area_terreno NUMERIC(10, 2);
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS a4_area_piso_1 NUMERIC(10, 2);
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS a4_area_piso_2 NUMERIC(10, 2);
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS a4_area_piso_3 NUMERIC(10, 2);
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS a4_area_piso_4 NUMERIC(10, 2);
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS a4_area_otros_pisos NUMERIC(10, 2);
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS a4_area_techada_total NUMERIC(10, 2);
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS a4_area_ocupada_total NUMERIC(10, 2);
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS a4_aforo_personas INTEGER;
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS a4_antiguedad_edificacion INTEGER;
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS a4_antiguedad_giro INTEGER;
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS a4_no_proceso_construccion BOOLEAN DEFAULT TRUE;
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS a4_cuenta_servicios_basicos BOOLEAN DEFAULT TRUE;
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS a4_cuenta_mobiliario_basico BOOLEAN DEFAULT TRUE;
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS a4_tiene_equipos_instalados BOOLEAN DEFAULT TRUE;
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS a4_medios_evacuacion_libres BOOLEAN DEFAULT TRUE;
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS a4_senalizacion_seguridad BOOLEAN DEFAULT TRUE;
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS a4_luces_emergencia BOOLEAN DEFAULT TRUE;
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS a4_tablero_electrico_protegido BOOLEAN DEFAULT TRUE;
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS a4_interruptores_diferenciales BOOLEAN DEFAULT TRUE;
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS a4_pozo_tierra_vigente BOOLEAN DEFAULT TRUE;
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS a4_extintores_operativos BOOLEAN DEFAULT TRUE;
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS a4_estructuras_sin_colapso BOOLEAN DEFAULT TRUE;
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS a4_cables_protegidos_pvc BOOLEAN DEFAULT TRUE;

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

-- Tabla de Usuarios del Sistema Municipal (RBAC - Spring Security)
CREATE TABLE IF NOT EXISTS usuarios (
    id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nombre_completo VARCHAR(150) NOT NULL,
    email VARCHAR(100) NOT NULL,
    rol VARCHAR(30) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tabla de Tarifas TUPA (Ley N° 28976 / Ordenanza N° 018-2024-MPH)
CREATE TABLE IF NOT EXISTS tarifas_tupa (
    id UUID PRIMARY KEY,
    codigo_tupa VARCHAR(50) NOT NULL UNIQUE,
    nivel_riesgo VARCHAR(20) NOT NULL UNIQUE,
    concepto VARCHAR(255) NOT NULL,
    monto_total NUMERIC(10, 2) NOT NULL,
    derecho_tramite NUMERIC(10, 2) NOT NULL,
    costo_itse NUMERIC(10, 2) NOT NULL,
    base_legal VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_modificacion VARCHAR(100)
);

-- Índices para optimización de consultas concurrentes (RNF-01 y RNF-02: <3s de respuesta)
CREATE INDEX IF NOT EXISTS idx_expedientes_numero_tramite ON expedientes(numero_tramite);
CREATE INDEX IF NOT EXISTS idx_expedientes_solicitante_id ON expedientes(solicitante_id);
CREATE INDEX IF NOT EXISTS idx_expedientes_estado ON expedientes(estado);
CREATE INDEX IF NOT EXISTS idx_expedientes_modalidad ON expedientes(modalidad_tramite);
CREATE INDEX IF NOT EXISTS idx_expedientes_tipo_persona ON expedientes(tipo_persona);
CREATE INDEX IF NOT EXISTS idx_historial_expediente_id ON historial_estados(expediente_id);
CREATE INDEX IF NOT EXISTS idx_historial_fecha ON historial_estados(fecha);
CREATE INDEX IF NOT EXISTS idx_usuarios_username ON usuarios(username);
CREATE INDEX IF NOT EXISTS idx_tarifas_tupa_nivel ON tarifas_tupa(nivel_riesgo);

-- Datos semilla iniciales para pruebas del entorno
INSERT INTO expedientes (
    id, numero_tramite, solicitante_id, modalidad_tramite, tipo_persona, tipo_documento,
    nombre_titular, documento_identidad, razon_social, partida_sunarp, asiento_sunarp,
    dni_representante, nombre_representante, poder_sunarp,
    nombre_comercial, ciiu_codigo, giro_negocio, actividad_detallada, zonificacion, funcion_edificacion,
    direccion_establecimiento, tipo_via, nombre_via, numero_vivienda, urbanizacion, distrito, provincia, departamento,
    area_metros_cuadrados, area_terreno, aforo_personas, estado, nivel_riesgo, monto_tasa, voucher_id,
    licencia_qr_code, fecha_creacion, fecha_limite
) VALUES (
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    'EXP-2026-00001',
    'f47ac10b-58cc-4372-a567-0e02b2c3d479',
    'LICENCIA_INDETERMINADA',
    'JURIDICA',
    'RUC',
    'María Quispe Huamán',
    '20601234567',
    'INVERSIONES LOS RETABLOS S.A.C.',
    '11029384',
    'A0001',
    '42567891',
    'María Quispe Huamán',
    'Gerente General con facultades inscritas',
    'Boutique Artesanal Huamanga',
    '4773',
    'Venta de artesanías, retablos y souvenirs',
    'Comercio al por menor de artesanías y recuerdos turísticos',
    'ZRE-CH (Zona de Reglamentación Especial Centro Histórico)',
    'COMERCIO',
    'Jr. 9 de Diciembre N° 142, Centro Histórico, Ayacucho',
    'Jr.',
    '9 de Diciembre',
    '142',
    'Centro Histórico',
    'Ayacucho',
    'Huamanga',
    'Ayacucho',
    35.50,
    40.00,
    15,
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
    'Ingreso virtual de solicitud y generación de anexos preliminares (Anexo 1 y Anexo 4)',
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;

-- Tasas Oficiales TUPA Huamanga (Ordenanza Municipal N° 018-2024-MPH)
INSERT INTO tarifas_tupa (
    id, codigo_tupa, nivel_riesgo, concepto, monto_total, derecho_tramite, costo_itse, base_legal, activo, fecha_actualizacion, usuario_modificacion
) VALUES
    ('c1d2e3f4-a5b6-7890-bcde-f12345678901', 'TUPA-ITSE-01', 'BAJO', 'Licencia de Funcionamiento con Inspección Técnica de Seguridad en Edificaciones Posterior (Riesgo Bajo)', 154.50, 45.00, 109.50, 'Ordenanza Municipal N° 018-2024-MPH / D.S. N° 046-2017-PCM', TRUE, CURRENT_TIMESTAMP, 'SISTEMA_INICIALIZADOR'),
    ('c2d3e4f5-a6b7-8901-bcde-f23456789012', 'TUPA-ITSE-02', 'MEDIO', 'Licencia de Funcionamiento con Inspección Técnica de Seguridad en Edificaciones Posterior (Riesgo Medio)', 218.00, 45.00, 173.00, 'Ordenanza Municipal N° 018-2024-MPH / D.S. N° 046-2017-PCM', TRUE, CURRENT_TIMESTAMP, 'SISTEMA_INICIALIZADOR'),
    ('c3d4e5f6-a7b8-8901-bcde-f34567890123', 'TUPA-ITSE-03', 'ALTO', 'Licencia de Funcionamiento con Inspección Técnica de Seguridad en Edificaciones Previa (Riesgo Alto)', 345.20, 45.00, 300.20, 'Ordenanza Municipal N° 018-2024-MPH / D.S. N° 046-2017-PCM', TRUE, CURRENT_TIMESTAMP, 'SISTEMA_INICIALIZADOR'),
    ('c4d5e6f7-a8b9-8901-bcde-f45678901234', 'TUPA-ITSE-04', 'MUY_ALTO', 'Licencia de Funcionamiento con Inspección Técnica de Seguridad en Edificaciones Previa (Riesgo Muy Alto)', 480.00, 45.00, 435.00, 'Ordenanza Municipal N° 018-2024-MPH / D.S. N° 046-2017-PCM', TRUE, CURRENT_TIMESTAMP, 'SISTEMA_INICIALIZADOR')
ON CONFLICT (codigo_tupa) DO NOTHING;
