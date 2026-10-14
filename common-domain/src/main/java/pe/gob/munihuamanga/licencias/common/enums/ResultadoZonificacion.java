package pe.gob.munihuamanga.licencias.common.enums;

import lombok.Getter;

/**
 * Estados del flujo administrativo de evaluación de zonificación y compatibilidad de uso (M07 - I06).
 * Regla técnica: Registra el dictamen emitido por el evaluador municipal competente
 * conforme al PDU y ordenanzas vigentes. No se infiere por coordenadas ni por código CIIU de forma automática.
 */
@Getter
public enum ResultadoZonificacion {

    PENDIENTE_EVALUACION("Ubicación y giro pendientes de revisión conforme al Plan de Desarrollo Urbano (PDU)"),
    EN_EVALUACION("En análisis técnico por el evaluador municipal de Edificaciones/Zonificación"),
    COMPATIBLE("Uso de suelo y actividad comercial dictaminados como compatibles con la zonificación"),
    NO_COMPATIBLE("Actividad comercial no permitida en la zonificación del establecimiento"),
    REQUIERE_SUBSANACION("Requiere aclaración técnica o subsanación de dirección, numeración o coordenadas");

    private final String descripcion;

    ResultadoZonificacion(String descripcion) {
        this.descripcion = descripcion;
    }
}
