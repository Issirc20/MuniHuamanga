package pe.gob.munihuamanga.licencias.expedientes.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculadoraDeTasaTest {

    private CalculadoraDeTasa calculadora;

    @BeforeEach
    void setUp() {
        calculadora = new CalculadoraDeTasa(
                new BigDecimal("154.50"),
                new BigDecimal("218.00"),
                new BigDecimal("345.20"),
                new BigDecimal("480.00")
        );
    }

    @Test
    @DisplayName("Debe calcular tasa correcta para nivel de riesgo BAJO")
    void calcularTasaBajo() {
        BigDecimal tasa = calculadora.calcularTasa(NivelRiesgo.BAJO);
        assertEquals(new BigDecimal("154.50"), tasa);
    }

    @Test
    @DisplayName("Debe calcular tasa correcta para nivel de riesgo MEDIO")
    void calcularTasaMedio() {
        BigDecimal tasa = calculadora.calcularTasa(NivelRiesgo.MEDIO);
        assertEquals(new BigDecimal("218.00"), tasa);
    }

    @Test
    @DisplayName("Debe calcular tasa correcta para nivel de riesgo ALTO")
    void calcularTasaAlto() {
        BigDecimal tasa = calculadora.calcularTasa(NivelRiesgo.ALTO);
        assertEquals(new BigDecimal("345.20"), tasa);
    }

    @Test
    @DisplayName("Debe calcular tasa correcta para nivel de riesgo MUY ALTO")
    void calcularTasaMuyAlto() {
        BigDecimal tasa = calculadora.calcularTasa(NivelRiesgo.MUY_ALTO);
        assertEquals(new BigDecimal("480.00"), tasa);
    }

    @Test
    @DisplayName("Debe lanzar excepción si el nivel de riesgo es nulo")
    void errorSiNivelRiesgoEsNulo() {
        assertThrows(IllegalArgumentException.class, () -> calculadora.calcularTasa(null));
    }
}
