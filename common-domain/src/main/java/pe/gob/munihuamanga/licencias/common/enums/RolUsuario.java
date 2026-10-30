package pe.gob.munihuamanga.licencias.common.enums;

/**
 * Roles institucionales para el control de acceso basado en roles (RBAC).
 * Un usuario puede tener varios roles. La segregación de funciones se garantiza
 * asignando a cargos distintos la elaboración, la aprobación y la firma de los actos.
 */
public enum RolUsuario {

    /** Administrador del sistema: usuarios, parámetros, catálogos y tarifario. Sin facultades sobre expedientes. */
    ROLE_ADMIN,

    /** Mesa de partes: admisibilidad, recepción presencial y registro de notificaciones. */
    ROLE_MESA_DE_PARTES,

    /** Evaluador de licencias: validación del ITSE, observaciones, zonificación y elaboración de informes. */
    ROLE_EVALUADOR,

    /** Supervisor de licencias: revisión y aprobación de informes administrativos. */
    ROLE_SUPERVISOR_LICENCIAS,

    /** Autoridad resolutiva: aprobación y firma de resoluciones y emisión de licencias. */
    ROLE_AUTORIDAD_RESOLUTIVA,

    /** Cajero del SAT: validación de comprobantes de pago. */
    ROLE_CAJERO,

    /** Inspector municipal: consulta ampliada de licencias mediante código de verificación. */
    ROLE_INSPECTOR,

    /** Auditor: consulta de solo lectura de expedientes y de la trazabilidad del sistema. */
    ROLE_AUDITOR,

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
