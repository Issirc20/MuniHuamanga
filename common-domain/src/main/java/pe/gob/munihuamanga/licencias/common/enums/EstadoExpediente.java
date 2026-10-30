package pe.gob.munihuamanga.licencias.common.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estados del procedimiento administrativo de Licencia de Funcionamiento.
 * El orden refleja la secuencia normal del trámite: presentación, admisibilidad,
 * inspección técnica de seguridad (ITSE) gestionada ante Defensa Civil, pago de la tasa
 * liquidada según el nivel de riesgo, evaluación administrativa, resolución y emisión.
 */
public enum EstadoExpediente {

    /** Solicitud formal recibida y en verificación de admisibilidad. */
    PRESENTADO,

    /** Solicitud admitida; el administrado gestiona la firma de los anexos ITSE ante Defensa Civil. */
    ITSE_EN_TRAMITE,

    /** Los cuatro anexos ITSE firmados fueron cargados y se encuentran en revisión. */
    ITSE_EN_REVISION,

    /** Nivel de riesgo oficial determinado y liquidación de la tasa emitida. */
    PENDIENTE_PAGO,

    /** Expediente completo en evaluación administrativa; corre el plazo legal. */
    EN_EVALUACION,

    /** Observación formal notificada al administrado; el plazo legal se suspende. */
    OBSERVADO,

    /** Subsanación presentada por el administrado, pendiente de verificación. */
    SUBSANADO,

    /** Informe administrativo aprobado; pendiente de resolución. */
    APTO_PARA_RESOLUCION,

    /** Resolución favorable firmada. */
    APROBADO,

    /** Resolución denegatoria firmada. Estado terminal. */
    DENEGADO,

    /** Licencia de funcionamiento generada, firmada y con código de verificación. */
    EMITIDO,

    /** Procedimiento concluido tras cumplirse todas las condiciones de cierre. Estado terminal. */
    CONCLUIDO,

    /** Procedimiento archivado por abandono, desistimiento o solicitud no presentada. Estado terminal. */
    ARCHIVADO;

    private static final Set<EstadoExpediente> TERMINALES = EnumSet.of(DENEGADO, CONCLUIDO, ARCHIVADO);

    public boolean esTerminal() {
        return TERMINALES.contains(this);
    }

    /**
     * Indica si el expediente ya cuenta con un pronunciamiento de fondo que impide modificar su contenido.
     */
    public boolean estaResuelto() {
        return this == APROBADO || this == EMITIDO || esTerminal();
    }

    public static Set<EstadoExpediente> noTerminales() {
        return EnumSet.complementOf(EnumSet.copyOf(TERMINALES));
    }
}
