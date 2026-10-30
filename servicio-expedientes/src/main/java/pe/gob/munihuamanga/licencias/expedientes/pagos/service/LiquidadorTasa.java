package pe.gob.munihuamanga.licencias.expedientes.pagos.service;

import pe.gob.munihuamanga.licencias.common.dto.LiquidacionDto;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

/**
 * Puerto de liquidación de la tasa del procedimiento según el nivel de riesgo.
 */
public interface LiquidadorTasa {

    /**
     * @param definitiva {@code true} cuando el nivel de riesgo fue confirmado por la oficina de licencias.
     */
    LiquidacionDto liquidar(NivelRiesgo nivelRiesgo, boolean definitiva);
}
