package pe.gob.munihuamanga.licencias.common.enums;

/**
 * Tipo de vínculo de representación entre una persona y una cuenta de administrado.
 * Cumple con el principio de menor privilegio (OWASP API5:2023).
 */
public enum RolOperador {
    /** Persona natural que actúa en nombre propio sobre su propia cuenta. */
    TITULAR,
    /** Representante legal acreditado de una persona jurídica. */
    REPRESENTANTE_LEGAL,
    ADMINISTRADOR_CUENTA,
    TRAMITADOR,
    CONSULTOR
}
