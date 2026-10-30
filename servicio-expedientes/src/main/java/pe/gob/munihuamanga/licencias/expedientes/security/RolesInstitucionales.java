package pe.gob.munihuamanga.licencias.expedientes.security;

import pe.gob.munihuamanga.licencias.common.enums.RolUsuario;

/**
 * Agrupaciones de roles institucionales reutilizadas por las políticas de acceso.
 */
public final class RolesInstitucionales {

    /** Funcionarios que intervienen en la tramitación o la fiscalizan y pueden consultar expedientes completos. */
    public static final RolUsuario[] CONSULTA_EXPEDIENTES = {
            RolUsuario.ROLE_MESA_DE_PARTES,
            RolUsuario.ROLE_EVALUADOR,
            RolUsuario.ROLE_SUPERVISOR_LICENCIAS,
            RolUsuario.ROLE_AUTORIDAD_RESOLUTIVA,
            RolUsuario.ROLE_CAJERO,
            RolUsuario.ROLE_AUDITOR
    };

    /** Funcionarios que pueden registrar actuaciones en nombre del administrado en ventanilla. */
    public static final RolUsuario[] ATENCION_PRESENCIAL = {
            RolUsuario.ROLE_MESA_DE_PARTES
    };

    private RolesInstitucionales() {
    }
}
