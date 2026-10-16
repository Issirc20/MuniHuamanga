package pe.gob.munihuamanga.licencias.common.enums;

/**
 * Representa los estados del ciclo de vida del expediente de licencias de funcionamiento
 * según la especificación ERS-MH-V2-001 v1.0 (Sección 3.1) y el modelo de arquitectura DAS.
 */
public enum EstadoExpediente {
    // 10 estados oficiales institucionales (ERS 3.1 / Ley N° 28976 / Ley N° 27444 LPAG)
    BORRADOR,
    PRESENTADO,
    EN_EVALUACION,
    OBSERVADO,
    SUBSANADO,
    APTO_PARA_RESOLUCION,
    APROBADO,
    EMITIDO,
    DENEGADO,
    CONCLUIDO,

    // Estados de compatibilidad operativa y transicional (C.1 / C.2)
    FORMATOS_GENERADOS,
    DOCUMENTOS_VALIDADOS,
    EN_EVALUACION_FINAL,
    RECHAZADO
}
