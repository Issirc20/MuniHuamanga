package pe.gob.munihuamanga.licencias.common.enums;

/**
 * Catálogo legal de tipos de observaciones según el procedimiento administrativo y normativa aplicable.
 * - RECEPCION_DOCUMENTAL: Art. 136.1 TUO Ley N° 27444 LPAG (Plazo: 2 días hábiles improrrogables).
 * - EVALUACION_TECNICA_ITSE: D.S. N° 002-2018-PCM y TUPA MPH (Plazo: 5 días hábiles).
 * - EVALUACION_ADMINISTRATIVA: Art. 143 TUO Ley N° 27444 LPAG (Plazo: hasta 10 días hábiles).
 */
public enum TipoObservacion {

    RECEPCION_DOCUMENTAL("Art. 136.1 del TUO de la Ley N° 27444 (LPAG) - Subsanación de requisitos de admisibilidad formal", 2),
    EVALUACION_TECNICA_ITSE("D.S. N° 002-2018-PCM (Reglamento de ITSE) y TUPA MPH - Subsanación de condiciones de seguridad", 5),
    EVALUACION_ADMINISTRATIVA("Art. 143 del TUO de la Ley N° 27444 (LPAG) - Trámite e instrucción de evaluación municipal", 10);

    private final String baseLegalDefault;
    private final int plazoDiasHabilesDefault;

    TipoObservacion(String baseLegalDefault, int plazoDiasHabilesDefault) {
        this.baseLegalDefault = baseLegalDefault;
        this.plazoDiasHabilesDefault = plazoDiasHabilesDefault;
    }

    public String getBaseLegalDefault() {
        return baseLegalDefault;
    }

    public int getPlazoDiasHabilesDefault() {
        return plazoDiasHabilesDefault;
    }
}
