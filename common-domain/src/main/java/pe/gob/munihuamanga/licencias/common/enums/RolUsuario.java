package pe.gob.munihuamanga.licencias.common.enums;

/**
 * Roles institucionales para el control de acceso basado en roles (RBAC).
 * Un usuario puede tener varios roles. El modelo reconoce 4 actores reales:
 * el administrado, el funcionario de licencias, el administrador del sistema
 * y el inspector municipal. La trazabilidad de quién ejecuta cada actuación
 * se preserva en la auditoría inmutable por nombre de actor, no por rol.
 */
public enum RolUsuario {

    /** Administrador del sistema: usuarios, parámetros, catálogos y tarifario. Sin facultades sobre expedientes. */
    ROLE_ADMIN,

    /**
     * Funcionario de la Subgerencia de Licencias: admisibilidad, validación de pagos SAT,
     * evaluación técnica y de zonificación, observaciones, informes administrativos,
     * resoluciones y emisión de licencias con firma digital.
     */
    ROLE_FUNCIONARIO_LICENCIAS,

    /** Inspector municipal: consulta ampliada de licencias mediante código de verificación. */
    ROLE_INSPECTOR,

    /** Administrado: actúa sobre los trámites de las cuentas que representa. */
    ROLE_CIUDADANO;

    public boolean esFuncionario() {
        return this != ROLE_CIUDADANO;
    }

    /** Nombre sin el prefijo de Spring Security, para su uso en {@code hasRole}. */
    public String nombreCorto() {
        return name().substring("ROLE_".length());
    }
}
