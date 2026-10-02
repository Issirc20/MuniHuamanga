package pe.gob.munihuamanga.licencias.expedientes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.expedientes.model.TarifaTupa;
import pe.gob.munihuamanga.licencias.expedientes.repository.TarifaTupaRepository;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Componente que calcula la tasa administrativa según el nivel de riesgo ITSE
 * de acuerdo con el TUPA municipal y la Ley N° 28976.
 * Cumple con los requerimientos RNF-17 (tasas configurables), US-06 (liquidación desglosada)
 * y Fase 04 Sprint 4-D (tarifario dinámico persistido en base de datos).
 */
@Component
public class CalculadoraDeTasa {

    private final Map<NivelRiesgo, BigDecimal> tablaTasas = new EnumMap<>(NivelRiesgo.class);
    private final BigDecimal derechoTramiteBase;

    @Autowired(required = false)
    private TarifaTupaRepository tarifaRepository;

    public CalculadoraDeTasa() {
        this(new BigDecimal("154.50"), new BigDecimal("218.00"), new BigDecimal("345.20"), new BigDecimal("480.00"), new BigDecimal("45.00"));
    }

    @Autowired
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

    public CalculadoraDeTasa(
            TarifaTupaRepository tarifaRepository,
            BigDecimal tasaBajo,
            BigDecimal tasaMedio,
            BigDecimal tasaAlto,
            BigDecimal tasaMuyAlto,
            BigDecimal derechoTramiteBase
    ) {
        this(tasaBajo, tasaMedio, tasaAlto, tasaMuyAlto, derechoTramiteBase);
        this.tarifaRepository = tarifaRepository;
    }

    /**
     * Obtiene el monto total de la tasa administrativa correspondiente al nivel de riesgo.
     * Prioriza la tarifa configurada dinámicamente en la base de datos (Sprint 4-D),
     * con fallback a los valores por defecto en application.yml.
     */
    public BigDecimal calcularTasa(NivelRiesgo nivelRiesgo) {
        if (nivelRiesgo == null) {
            throw new IllegalArgumentException("El nivel de riesgo no puede ser nulo para calcular la tasa administrativa.");
        }

        if (tarifaRepository != null) {
            Optional<TarifaTupa> tarifaDb = tarifaRepository.findByNivelRiesgo(nivelRiesgo);
            if (tarifaDb.isPresent() && tarifaDb.get().isActivo()) {
                return tarifaDb.get().getMontoTotal();
            }
        }

        return tablaTasas.getOrDefault(nivelRiesgo, BigDecimal.ZERO);
    }

    /**
     * US-06: Proporciona la liquidación desglosada de los conceptos tributarios municipales (TUPA).
     */
    public Map<String, BigDecimal> calcularDesglose(NivelRiesgo nivelRiesgo) {
        if (nivelRiesgo == null) {
            throw new IllegalArgumentException("El nivel de riesgo no puede ser nulo para calcular el desglose.");
        }

        BigDecimal derechoTramite = this.derechoTramiteBase;
        BigDecimal costoItse = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;

        if (tarifaRepository != null) {
            Optional<TarifaTupa> tarifaDb = tarifaRepository.findByNivelRiesgo(nivelRiesgo);
            if (tarifaDb.isPresent() && tarifaDb.get().isActivo()) {
                TarifaTupa t = tarifaDb.get();
                total = t.getMontoTotal();
                derechoTramite = t.getDerechoTramite();
                costoItse = t.getCostoItse();
            }
        }

        if (total.compareTo(BigDecimal.ZERO) == 0) {
            total = calcularTasa(nivelRiesgo);
            costoItse = total.subtract(derechoTramite).max(BigDecimal.ZERO);
        }

        Map<String, BigDecimal> desglose = new LinkedHashMap<>();
        desglose.put("Derecho de Trámite Administrativo", derechoTramite);
        desglose.put("Derecho de Inspección Técnica ITSE (" + nivelRiesgo + ")", costoItse);
        desglose.put("Total Tasa Liquidada", total);

        return desglose;
    }
}
