package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Liquidación de la tasa del procedimiento según el nivel de riesgo y el tarifario TUPA vigente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiquidacionDto {

    private NivelRiesgo nivelRiesgo;
    private BigDecimal monto;
    private String concepto;
    private LocalDateTime fechaLiquidacion;
    /** Falso mientras el nivel de riesgo no haya sido confirmado por la oficina de licencias. */
    private boolean definitiva;
}
