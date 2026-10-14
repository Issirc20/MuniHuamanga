package pe.gob.munihuamanga.licencias.common.enums;

/**
 * Representa los estados del expediente según el modelo C4 y máquina de estados del trámite de licencia.
 */
public enum EstadoExpediente {
    PRESENTADO,
    FORMATOS_GENERADOS,
    DOCUMENTOS_VALIDADOS,
    EN_EVALUACION_FINAL,
    OBSERVADO,
    APTO_PARA_RESOLUCION,
    APROBADO,
    RECHAZADO
}
