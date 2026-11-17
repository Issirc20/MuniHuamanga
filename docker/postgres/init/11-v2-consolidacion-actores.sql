-- ============================================================================
-- Consolidación del modelo de actores (RBAC) a 4 roles reales
-- Fusiona MESA_DE_PARTES, EVALUADOR, SUPERVISOR_LICENCIAS, AUTORIDAD_RESOLUTIVA,
-- CAJERO y AUDITOR en un único ROLE_FUNCIONARIO_LICENCIAS. ROLE_ADMIN, ROLE_INSPECTOR
-- y ROLE_CIUDADANO se mantienen sin cambios.
--
-- Nota: los scripts de docker/postgres/init/ son de inicialización de entornos
-- limpios, no un mecanismo de migración sobre bases de datos en producción con
-- datos preexistentes (ver docs/analisis-de-sistema/05-restricciones.md, punto 4).
-- Este script documenta la migración de datos para quien instancie un entorno
-- limpio a partir de los scripts 01..10 y necesite reconciliar roles existentes.
-- ============================================================================

-- 1. Migrar filas existentes de usuarios_roles al rol único consolidado.
-- Se inserta primero el rol consolidado (evitando duplicados vía ON CONFLICT,
-- ya que la clave primaria es (usuario_id, rol)) y luego se retiran los roles
-- antiguos; no se usa UPDATE directo porque un usuario podía tener más de uno
-- de los roles fusionados a la vez, lo que violaría la clave primaria.
INSERT INTO usuarios_roles (usuario_id, rol)
SELECT DISTINCT usuario_id, 'ROLE_FUNCIONARIO_LICENCIAS'
FROM usuarios_roles
WHERE rol IN (
    'ROLE_MESA_DE_PARTES', 'ROLE_EVALUADOR', 'ROLE_SUPERVISOR_LICENCIAS',
    'ROLE_AUTORIDAD_RESOLUTIVA', 'ROLE_CAJERO', 'ROLE_AUDITOR'
)
ON CONFLICT (usuario_id, rol) DO NOTHING;

DELETE FROM usuarios_roles
WHERE rol IN (
    'ROLE_MESA_DE_PARTES', 'ROLE_EVALUADOR', 'ROLE_SUPERVISOR_LICENCIAS',
    'ROLE_AUTORIDAD_RESOLUTIVA', 'ROLE_CAJERO', 'ROLE_AUDITOR'
);

-- 2. Redefinir el CHECK de roles permitidos al nuevo catálogo de 4 actores
ALTER TABLE usuarios_roles DROP CONSTRAINT IF EXISTS usuarios_roles_rol_check;
ALTER TABLE usuarios_roles ADD CONSTRAINT usuarios_roles_rol_check CHECK (rol IN (
    'ROLE_ADMIN', 'ROLE_FUNCIONARIO_LICENCIAS', 'ROLE_INSPECTOR', 'ROLE_CIUDADANO'
));
