package pe.gob.munihuamanga.licencias.expedientes.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculadoraDeTasaDesgloseTest {

    private CalculadoraDeTasa calculadora;

    @BeforeEach
    void setUp() {
        calculadora = new CalculadoraDeTasa(
                new BigDecimal("154.50"),
                new BigDecimal("218.00"),
                new BigDecimal("345.20"),
                new BigDecimal("480.00"),
                new BigDecimal("45.00")
        );
    }

    @Test
    @DisplayName("US-06: Debe desglosar correctamente la tasa para riesgo BAJO")
    void testDesgloseRiesgoBajo() {
        Map<String, BigDecimal> desglose = calculadora.calcularDesglose(NivelRiesgo.BAJO);

        assertNotNull(desglose);
        assertEquals(new BigDecimal("45.00"), desglose.get("Derecho de Trámite Administrativo"));
        assertEquals(new BigDecimal("109.50"), desglose.get("Derecho de Inspección Técnica ITSE (BAJO)"));
        assertEquals(new BigDecimal("154.50"), desglose.get("Total Tasa Liquidada"));
    }

    @Test
    @DisplayName("US-06: Debe desglosar correctamente la tasa para riesgo ALTO")
    void testDesgloseRiesgoAlto() {
        Map<String, BigDecimal> desglose = calculadora.calcularDesglose(NivelRiesgo.ALTO);

        assertNotNull(desglose);
        assertEquals(new BigDecimal("45.00"), desglose.get("Derecho de Trámite Administrativo"));
        assertEquals(new BigDecimal("300.20"), desglose.get("Derecho de Inspección Técnica ITSE (ALTO)"));
        assertEquals(new BigDecimal("345.20"), desglose.get("Total Tasa Liquidada"));
    }

    @Test
    @DisplayName("US-06: La suma de conceptos debe ser igual al total liquidado")
    void testConsistenciaSumaConceptos() {
        for (NivelRiesgo riesgo : NivelRiesgo.values()) {
            Map<String, BigDecimal> desglose = calculadora.calcularDesglose(riesgo);
            BigDecimal total = desglose.get("Total Tasa Liquidada");
            BigDecimal tramite = desglose.get("Derecho de Trámite Administrativo");
            BigDecimal itse = desglose.get("Derecho de Inspección Técnica ITSE (" + riesgo + ")");

            assertEquals(total, tramite.add(itse), "La suma de los conceptos debe coincidir con el total liquidado");
        }
    }
}
