package pe.gob.munihuamanga.licencias.common.enums;

/**
 * Acción que el sistema propone al funcionario cuando una regla de plazo se cumple.
 */
public enum TipoAccionPropuesta {
    /** Recordatorio al administrado de que el plazo de su etapa está próximo a vencer. */
    AVISO_ADMINISTRADO,
    /** El plazo de paralización venció; corresponde evaluar la declaración de abandono. */
    DECLARAR_ABANDONO,
    /** La observación de admisibilidad no fue subsanada; corresponde tener la solicitud por no presentada. */
    DECLARAR_SOLICITUD_NO_PRESENTADA,
    /** El plazo legal de evaluación venció sin resolución; corresponde atender el silencio administrativo positivo. */
    ALERTA_SILENCIO_POSITIVO,
    /** Se cumplieron las condiciones de cierre; el procedimiento puede concluirse. */
    CONCLUIR_PROCEDIMIENTO
}
