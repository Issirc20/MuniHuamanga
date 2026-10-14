package pe.gob.munihuamanga.licencias.common.enums;

/**
 * Roles nominativos de operadores delegados en Cuentas Institucionales (Personas Jurídicas).
 * Cumple con el Principio de Menor Privilegio (OWASP API5:2023).
 */
public enum RolOperador {
    REPRESENTANTE_LEGAL,
    ADMINISTRADOR_CUENTA,
    TRAMITADOR,
    CONSULTOR
}
