package pe.gob.munihuamanga.licencias.common.enums;

/**
 * Roles de usuario para el control de acceso basado en roles (RBAC)
 * en el Sistema de Gestión de Licencias de Funcionamiento — MuniHuamanga V2.
 */
public enum RolUsuario {
    /**
     * Administrador general con acceso a catálogos TUPA, parámetros y auditoría.
     */
    ROLE_ADMIN,

    /**
     * Funcionario de la Gerencia de Licencias: revisión documental, zonificación,
     * formulación de observaciones, emisión de informes y aprobación de licencias.
     */
    ROLE_EVALUADOR,

    /**
     * Personal de caja/recaudación: emisión de vouchers y validación manual de pagos SAT.
     */
    ROLE_CAJERO,

    /**
     * Administrado / Ciudadano solicitante: acceso restringido únicamente a sus propios
     * trámites, expedientes y documentos.
     */
    ROLE_CIUDADANO,

    /**
     * Inspector municipal autorizado: consulta de solo lectura para verificación de
     * autenticidad y ubicación en campo. Sin facultades de edición, actas ni sanciones.
     */
    ROLE_INSPECTOR
}

