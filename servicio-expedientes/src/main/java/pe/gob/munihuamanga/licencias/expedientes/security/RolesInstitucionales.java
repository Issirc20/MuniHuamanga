package pe.gob.munihuamanga.licencias.expedientes.security;

import pe.gob.munihuamanga.licencias.common.enums.RolUsuario;

/**
 * Agrupaciones de roles institucionales reutilizadas por las políticas de acceso.
 */
public final class RolesInstitucionales {

    /** Funcionarios que intervienen en la tramitación y pueden consultar expedientes completos. */
    public static final RolUsuario[] CONSULTA_EXPEDIENTES = {
            RolUsuario.ROLE_FUNCIONARIO_LICENCIAS
    };

    /** Funcionarios que pueden registrar actuaciones en nombre del administrado en ventanilla. */
    public static final RolUsuario[] ATENCION_PRESENCIAL = {
            RolUsuario.ROLE_FUNCIONARIO_LICENCIAS
    };

    /** Funcionarios que pueden registrar la declaración de un pago cuando el administrado paga presencialmente en caja. */
    public static final RolUsuario[] RECEPCION_PAGOS = {
            RolUsuario.ROLE_FUNCIONARIO_LICENCIAS
    };

    private RolesInstitucionales() {
    }
}
