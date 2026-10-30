package pe.gob.munihuamanga.licencias.common.enums;

/**
 * Naturaleza de una observación formulada a los anexos ITSE.
 * La oficina de licencias verifica la forma y la coherencia de los anexos;
 * no reevalúa el contenido técnico emitido por Defensa Civil.
 */
public enum TipoObservacionItse {
    /** Defecto del archivo que el administrado corrige volviendo a cargarlo (ilegible, incompleto, equivocado). */
    SUBSANABLE_POR_ADMINISTRADO,
    /** Defecto que exige volver a Defensa Civil (falta de firma o sello, datos o nivel de riesgo incoherentes). */
    REQUIERE_DEFENSA_CIVIL
}
