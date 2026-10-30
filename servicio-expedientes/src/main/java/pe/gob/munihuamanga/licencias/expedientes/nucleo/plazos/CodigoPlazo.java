package pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos;

/**
 * Plazos del procedimiento, expresados en días hábiles. Los valores son los predeterminados;
 * la administración puede ajustarlos sin redesplegar el sistema.
 * Las bases legales deben ser confirmadas por la Oficina de Asesoría Jurídica de la Municipalidad.
 */
public enum CodigoPlazo {

    PLAZO_LEGAL_EVALUACION(15,
            "Plazo máximo para resolver la solicitud de licencia desde que el expediente está completo.",
            "Ley N° 28976, art. 8 (silencio administrativo positivo)"),
    ABANDONO_ITSE(30,
            "Paralización máxima a la espera de la carga de los anexos ITSE firmados.",
            "TUO de la Ley N° 27444, art. 202 (abandono)"),
    ABANDONO_PAGO(30,
            "Paralización máxima a la espera del pago de la tasa liquidada.",
            "TUO de la Ley N° 27444, art. 202 (abandono)"),
    SUBSANACION_ADMISIBILIDAD(2,
            "Plazo para subsanar observaciones formuladas en la etapa de admisibilidad.",
            "TUO de la Ley N° 27444, art. 136"),
    SUBSANACION_EVALUACION(10,
            "Plazo para subsanar observaciones formuladas durante la evaluación administrativa.",
            "Plazo fijado por la autoridad en la notificación"),
    RECURSOS_ADMINISTRATIVOS(15,
            "Plazo para interponer recursos administrativos contra la resolución notificada.",
            "TUO de la Ley N° 27444, art. 218.2"),
    ATENCION_ADMISIBILIDAD(2,
            "Meta interna de atención de la verificación de admisibilidad.",
            "Indicador de gestión"),
    ATENCION_REVISION_ITSE(2,
            "Meta interna de atención de la revisión de anexos ITSE.",
            "Indicador de gestión"),
    ATENCION_VALIDACION_PAGO(1,
            "Meta interna de atención de la validación del comprobante de pago.",
            "Indicador de gestión");

    private final int diasPredeterminados;
    private final String descripcion;
    private final String baseLegal;

    CodigoPlazo(int diasPredeterminados, String descripcion, String baseLegal) {
        this.diasPredeterminados = diasPredeterminados;
        this.descripcion = descripcion;
        this.baseLegal = baseLegal;
    }

    public int getDiasPredeterminados() {
        return diasPredeterminados;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getBaseLegal() {
        return baseLegal;
    }
}
