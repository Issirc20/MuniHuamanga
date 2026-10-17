package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

/**
 * DTO que encapsula los datos del Anexo 2 ITSE:
 * Información proporcionada por el solicitante para la determinación del nivel de riesgo del establecimiento (D.S. N° 002-2018-PCM).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Anexo2ItseDto {

    // Función principal declarada
    @Builder.Default
    private String funcionPrincipal = "COMERCIO"; // SALUD, ENCUENTRO, HOSPEDAJE, EDUCACION, INDUSTRIAL, OFICINAS, COMERCIO, ALMACEN

    // Subcategorías seleccionadas en la matriz
    private String opcionSalud;       // 1.1, 1.2, 1.3, 1.4, 1.5
    private String opcionEncuentro;   // 2.1, 2.2, 2.3, 2.4
    private String opcionHospedaje;   // 3.1, 3.2, 3.3, 3.4
    private String opcionEducacion;   // 4.1, 4.2, 4.3, 4.4
    private String opcionIndustrial;  // 5.1, 5.2, 5.3
    private String opcionOficinas;    // 6.1, 6.2, 6.3, 6.4, 6.5
    @Builder.Default
    private String opcionComercio = "7.1"; // 7.1 (hasta 3 pisos/750m2), 7.2, 7.3, 7.4, 7.5, 7.6
    private String opcionAlmacen;     // 8.1, 8.2, 8.3

    // Factores adicionales que incrementan nivel de riesgo
    @Builder.Default
    private Boolean factorGlpCombustible = false; // Tanque GLP > 0.45m3 o combustible > 1m3
    @Builder.Default
    private Boolean factorCaldero = false;        // Uso de caldero

    // Resultado preliminar de la matriz
    @Builder.Default
    private NivelRiesgo nivelRiesgoCalculado = NivelRiesgo.MEDIO;
    private String observacionRiesgo;
}
