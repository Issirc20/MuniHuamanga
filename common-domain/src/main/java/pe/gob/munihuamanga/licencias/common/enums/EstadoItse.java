package pe.gob.munihuamanga.licencias.common.enums;

/**
 * Estado de la etapa de Inspección Técnica de Seguridad en Edificaciones (ITSE) del expediente.
 */
public enum EstadoItse {
    /** A la espera de la carga de los cuatro anexos firmados por Defensa Civil. */
    PENDIENTE_CARGA,
    /** Anexos cargados y en revisión por la oficina de licencias. */
    EN_REVISION,
    /** Anexos observados; el administrado debe volver a cargarlos. */
    OBSERVADO,
    /** Anexos conformes; nivel de riesgo oficial determinado y liquidación emitida. */
    VALIDADO
}
