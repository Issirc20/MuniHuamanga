package pe.gob.munihuamanga.licencias.expedientes.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

/**
 * Componente que calcula la tasa administrativa según el nivel de riesgo ITSE
 * de acuerdo con el TUPA municipal y la Ley N° 28976.
 * Cumple con el requerimiento RNF-17 (tasas configurables vía properties sin redespacho).
 */
@Component
public class CalculadoraDeTasa {

    private final Map<NivelRiesgo, BigDecimal> tablaTasas = new EnumMap<>(NivelRiesgo.class);

    public CalculadoraDeTasa(
            @Value("${tupa.tasas.bajo:154.50}") BigDecimal tasaBajo,
            @Value("${tupa.tasas.medio:218.00}") BigDecimal tasaMedio,
            @Value("${tupa.tasas.alto:345.20}") BigDecimal tasaAlto,
            @Value("${tupa.tasas.muy-alto:480.00}") BigDecimal tasaMuyAlto
    ) {
        tablaTasas.put(NivelRiesgo.BAJO, tasaBajo);
        tablaTasas.put(NivelRiesgo.MEDIO, tasaMedio);
        tablaTasas.put(NivelRiesgo.ALTO, tasaAlto);
        tablaTasas.put(NivelRiesgo.MUY_ALTO, tasaMuyAlto);
    }

    /**
     * Obtiene el monto de la tasa administrativa correspondiente al nivel de riesgo.
     *
     * @param nivelRiesgo nivel de riesgo determinado por ITSE (Defensa Civil).
     * @return monto en Soles (PEN) con 2 decimales.
     */
    public BigDecimal calcularTasa(NivelRiesgo nivelRiesgo) {
        if (nivelRiesgo == null) {
            throw new IllegalArgumentException("El nivel de riesgo no puede ser nulo para calcular la tasa administrativa.");
        }
        return tablaTasas.getOrDefault(nivelRiesgo, BigDecimal.ZERO);
    }
}
