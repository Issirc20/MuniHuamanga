-- Script DDL de Migración Versionada para Incremento I03 - MuniHuamanga V2
-- Módulo M01: Cuentas y Operadores (Personas Naturales DNI y Personas Jurídicas RUC)
-- Módulo M02: Establecimientos y Catálogo CIIU Administrable
-- Marco Legal: Ley N° 28976 / TUO D.S. N° 046-2017-PCM / Ley N° 29733 / Ley N° 27444

-- 1. Catálogo CIIU administrable (Rev. 4 Adaptada para Licencias de Funcionamiento MPH)
CREATE TABLE IF NOT EXISTS catalogo_ciiu (
    codigo VARCHAR(20) PRIMARY KEY,
    descripcion VARCHAR(255) NOT NULL,
    giro_principal VARCHAR(255) NOT NULL,
    nivel_riesgo_sugerido VARCHAR(20) NOT NULL DEFAULT 'BAJO',
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

-- 2. Cuentas Ciudadanas e Institucionales (Personas Naturales y Personas Jurídicas)
CREATE TABLE IF NOT EXISTS cuentas (
    id UUID PRIMARY KEY,
    tipo_persona VARCHAR(20) NOT NULL,
    tipo_documento VARCHAR(20) NOT NULL,
    numero_documento VARCHAR(20) NOT NULL,
    razon_social_nombre VARCHAR(200) NOT NULL,
    email VARCHAR(120) NOT NULL,
    telefono VARCHAR(20),
    direccion_fiscal VARCHAR(255),
    email_verificado BOOLEAN NOT NULL DEFAULT FALSE,
    token_verificacion_email VARCHAR(100),
    fecha_expiracion_token TIMESTAMP,
    identidad_validada_oficialmente BOOLEAN NOT NULL DEFAULT FALSE,
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE_VERIFICACION',
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP,
    CONSTRAINT uq_cuenta_documento UNIQUE (tipo_documento, numero_documento),
    CONSTRAINT uq_cuenta_email UNIQUE (email)
);

-- 3. Operadores Nominativos de Cuenta (Delegación de Poderes para Personas Jurídicas)
CREATE TABLE IF NOT EXISTS operadores_cuenta (
    id UUID PRIMARY KEY,
    cuenta_id UUID NOT NULL,
    usuario_id UUID,
    dni_operador VARCHAR(20) NOT NULL,
    nombre_completo VARCHAR(150) NOT NULL,
    email VARCHAR(120) NOT NULL,
    rol_operador VARCHAR(40) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_asignacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    asignado_por VARCHAR(100) NOT NULL,
    CONSTRAINT fk_operador_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas(id) ON DELETE CASCADE,
    CONSTRAINT fk_operador_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL,
    CONSTRAINT uq_operador_cuenta_dni UNIQUE (cuenta_id, dni_operador)
);

-- 4. Permisos Específicos por Operador (Principio de Menor Privilegio)
CREATE TABLE IF NOT EXISTS operadores_cuenta_permisos (
    operador_id UUID NOT NULL,
    permiso VARCHAR(50) NOT NULL,
    PRIMARY KEY (operador_id, permiso),
    CONSTRAINT fk_permiso_operador FOREIGN KEY (operador_id) REFERENCES operadores_cuenta(id) ON DELETE CASCADE
);

-- 5. Pista de Auditoría para Modificaciones de Permisos y Delegaciones
CREATE TABLE IF NOT EXISTS auditoria_cuenta_permisos (
    id UUID PRIMARY KEY,
    cuenta_id UUID NOT NULL,
    operador_id UUID NOT NULL,
    accion VARCHAR(50) NOT NULL,
    permisos_anteriores VARCHAR(255),
    permisos_nuevos VARCHAR(255),
    usuario_responsable VARCHAR(100) NOT NULL,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_origen VARCHAR(45),
    CONSTRAINT fk_auditoria_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas(id) ON DELETE CASCADE
);

-- 6. Establecimientos Físicos Asociados a la Cuenta
CREATE TABLE IF NOT EXISTS establecimientos (
    id UUID PRIMARY KEY,
    cuenta_id UUID NOT NULL,
    codigo_establecimiento VARCHAR(30) NOT NULL,
    nombre_comercial VARCHAR(150) NOT NULL,
    direccion VARCHAR(255) NOT NULL,
    distrito VARCHAR(100) NOT NULL DEFAULT 'Ayacucho',
    provincia VARCHAR(100) NOT NULL DEFAULT 'Huamanga',
    departamento VARCHAR(100) NOT NULL DEFAULT 'Ayacucho',
    referencia VARCHAR(255),
    latitud NUMERIC(10, 7) NOT NULL,
    longitud NUMERIC(10, 7) NOT NULL,
    ciiu_codigo VARCHAR(20),
    giro_principal VARCHAR(255) NOT NULL,
    area_metros_cuadrados NUMERIC(10, 2) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP,
    CONSTRAINT fk_establecimiento_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas(id) ON DELETE CASCADE,
    CONSTRAINT fk_establecimiento_ciiu FOREIGN KEY (ciiu_codigo) REFERENCES catalogo_ciiu(codigo),
    CONSTRAINT uq_establecimiento_codigo_cuenta UNIQUE (cuenta_id, codigo_establecimiento)
);

-- 7. Migración Aditiva y No Destructiva para la futura relación con Expedientes
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS cuenta_id UUID;
ALTER TABLE expedientes ADD COLUMN IF NOT EXISTS establecimiento_id UUID;

-- 8. Índices de Búsqueda y Unicidad para Alta Eficiencia
CREATE INDEX IF NOT EXISTS idx_cuentas_documento ON cuentas(numero_documento);
CREATE INDEX IF NOT EXISTS idx_cuentas_tipo ON cuentas(tipo_persona);
CREATE INDEX IF NOT EXISTS idx_operadores_dni ON operadores_cuenta(dni_operador);
CREATE INDEX IF NOT EXISTS idx_operadores_cuenta_id ON operadores_cuenta(cuenta_id);
CREATE INDEX IF NOT EXISTS idx_establecimientos_cuenta_id ON establecimientos(cuenta_id);
CREATE INDEX IF NOT EXISTS idx_establecimientos_ciiu ON establecimientos(ciiu_codigo);
CREATE INDEX IF NOT EXISTS idx_expedientes_cuenta_id ON expedientes(cuenta_id);
CREATE INDEX IF NOT EXISTS idx_expedientes_establecimiento_id ON expedientes(establecimiento_id);

-- 9. Semillas de Catálogo CIIU Oficial de Huamanga
INSERT INTO catalogo_ciiu (codigo, descripcion, giro_principal, nivel_riesgo_sugerido, activo) VALUES
('4711', 'Venta al por menor en comercios no especializados con surtido compuesto principalmente por alimentos, bebidas o tabaco', 'Bodega, Minimarket, Tienda de Abarrotes', 'BAJO', TRUE),
('4773', 'Venta al por menor de otros productos nuevos en comercios especializados', 'Boutique Artesanal, Venta de Retablos y Souvenirs', 'BAJO', TRUE),
('5610', 'Actividades de restaurantes y de servicio móvil de comidas', 'Restaurante, Cafetería, Picantería Tradicional', 'MEDIO', TRUE),
('5630', 'Actividades de servicio de bebidas', 'Bar, Discoteca, Pub', 'ALTO', TRUE),
('4721', 'Venta al por menor de alimentos en comercios especializados', 'Panadería, Pastelería, Verdulería', 'BAJO', TRUE),
('9602', 'Peluquería y otros tratamientos de belleza', 'Salón de Belleza, Barbería, Spa', 'BAJO', TRUE),
('6810', 'Actividades inmobiliarias realizadas con bienes propios o arrendados', 'Galería Comercial, Alquiler de Oficinas', 'ALTO', TRUE),
('8510', 'Enseñanza preescolar y primaria', 'Colegio, Centro Educativo, Nido', 'ALTO', TRUE)
ON CONFLICT (codigo) DO NOTHING;
