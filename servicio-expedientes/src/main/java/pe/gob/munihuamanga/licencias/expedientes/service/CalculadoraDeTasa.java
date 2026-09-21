package pe.gob.munihuamanga.licencias.expedientes.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Componente que calcula la tasa administrativa según el nivel de riesgo ITSE
 * de acuerdo con el TUPA municipal y la Ley N° 28976.
 * Cumple con los requerimientos RNF-17 (tasas configurables) y US-06 (liquidación desglosada).
 */
@Component
public class CalculadoraDeTasa {

    private final Map<NivelRiesgo, BigDecimal> tablaTasas = new EnumMap<>(NivelRiesgo.class);
    private final BigDecimal derechoTramiteBase;

    public CalculadoraDeTasa() {
        this(new BigDecimal("154.50"), new BigDecimal("218.00"), new BigDecimal("345.20"), new BigDecimal("480.00"), new BigDecimal("45.00"));
    }

    @org.springframework.beans.factory.annotation.Autowired
    public CalculadoraDeTasa(
            @Value("${tupa.tasas.bajo:154.50}") BigDecimal tasaBajo,
            @Value("${tupa.tasas.medio:218.00}") BigDecimal tasaMedio,
            @Value("${tupa.tasas.alto:345.20}") BigDecimal tasaAlto,
            @Value("${tupa.tasas.muy-alto:480.00}") BigDecimal tasaMuyAlto,
            @Value("${tupa.tasas.derecho-tramite:45.00}") BigDecimal derechoTramiteBase
    ) {
        this.tablaTasas.put(NivelRiesgo.BAJO, tasaBajo);
        this.tablaTasas.put(NivelRiesgo.MEDIO, tasaMedio);
        this.tablaTasas.put(NivelRiesgo.ALTO, tasaAlto);
        this.tablaTasas.put(NivelRiesgo.MUY_ALTO, tasaMuyAlto);
        this.derechoTramiteBase = derechoTramiteBase;
    }

    public CalculadoraDeTasa(
            BigDecimal tasaBajo,
            BigDecimal tasaMedio,
            BigDecimal tasaAlto,
            BigDecimal tasaMuyAlto
    ) {
        this(tasaBajo, tasaMedio, tasaAlto, tasaMuyAlto, new BigDecimal("45.00"));
    }

    /**
     * Obtiene el monto total de la tasa administrativa correspondiente al nivel de riesgo.
     */
    public BigDecimal calcularTasa(NivelRiesgo nivelRiesgo) {
        if (nivelRiesgo == null) {
            throw new IllegalArgumentException("El nivel de riesgo no puede ser nulo para calcular la tasa administrativa.");
        }
        return tablaTasas.getOrDefault(nivelRiesgo, BigDecimal.ZERO);
    }

    /**
     * US-06: Proporciona la liquidación desglosada de los conceptos tributarios municipales (TUPA).
     */
    public Map<String, BigDecimal> calcularDesglose(NivelRiesgo nivelRiesgo) {
        BigDecimal total = calcularTasa(nivelRiesgo);
        BigDecimal costoItse = total.subtract(derechoTramiteBase).max(BigDecimal.ZERO);

        Map<String, BigDecimal> desglose = new LinkedHashMap<>();
        desglose.put("Derecho de Trámite Administrativo", derechoTramiteBase);
        desglose.put("Derecho de Inspección Técnica ITSE (" + nivelRiesgo + ")", costoItse);
        desglose.put("Total Tasa Liquidada", total);

        return desglose;
    }
}
