package pe.gob.munihuamanga.licencias.common.enums;

/**
 * Estados del ciclo de vida de la Licencia de Funcionamiento emitida (M09 - Incremento I08).
 */
public enum EstadoLicencia {
    /**
     * Licencia oficialmente emitida y plenamente vigente.
     */
    EMITIDA,

    /**
     * Suspendida temporalmente por medida de seguridad o sanción administrativa.
     */
    SUSPENDIDA,

    /**
     * Revocada definitivamente por clausura definitiva o cambio no autorizado de condiciones.
     */
    REVOCADA,

    /**
     * Vencida por cumplimiento del plazo de vigencia temporal otorgado.
     */
    VENCIDA
}
