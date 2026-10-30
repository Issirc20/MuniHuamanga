package pe.gob.munihuamanga.licencias.common.enums;

/**
 * Anexos ITSE que el administrado debe presentar firmados por Defensa Civil.
 */
public enum TipoAnexoItse {
    ANEXO_1(TipoDocumentoAdjunto.ANEXO_ITSE_1_SOLICITUD, "Anexo 1 - Solicitud de ITSE"),
    ANEXO_2(TipoDocumentoAdjunto.ANEXO_ITSE_2_RIESGO, "Anexo 2 - Determinación del nivel de riesgo"),
    ANEXO_3(TipoDocumentoAdjunto.ANEXO_ITSE_3_REPORTE, "Anexo 3 - Reporte de nivel de riesgo"),
    ANEXO_4(TipoDocumentoAdjunto.ANEXO_ITSE_4_CONDICIONES, "Anexo 4 - Declaración jurada de condiciones de seguridad");

    private final TipoDocumentoAdjunto tipoDocumento;
    private final String descripcion;

    TipoAnexoItse(TipoDocumentoAdjunto tipoDocumento, String descripcion) {
        this.tipoDocumento = tipoDocumento;
        this.descripcion = descripcion;
    }

    public TipoDocumentoAdjunto getTipoDocumento() {
        return tipoDocumento;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
