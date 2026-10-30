package pe.gob.munihuamanga.licencias.common.enums;

/**
 * Medio por el cual mesa de partes validó la identidad declarada del administrado.
 */
public enum MedioValidacionIdentidad {
    /** Exhibición presencial del documento de identidad original en ventanilla. */
    PRESENCIAL_DOCUMENTO_ORIGINAL,
    /** Cotejo del documento de identidad digitalizado adjunto al expediente. */
    DOCUMENTO_DIGITALIZADO,
    /** Consulta en línea a RENIEC o SUNARP (requiere convenio institucional). */
    CONSULTA_EN_LINEA
}
