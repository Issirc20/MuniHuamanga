package pe.gob.munihuamanga.licencias.expedientes.integracion.adapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FirmaDigitalAdapterServiceTest {

    private FirmaDigitalAdapterService firmaDigitalAdapterService;

    @BeforeEach
    void setUp() {
        firmaDigitalAdapterService = new FirmaDigitalAdapterService();
    }

    @Test
    @DisplayName("I02.1: Firma digital no debe considerarse disponible si no hay integración institucional real (Ley N° 27269)")
    void estaDisponible_RetornaFalse() {
        assertThat(firmaDigitalAdapterService.estaDisponible()).isFalse();
    }

    @Test
    @DisplayName("I02.1: Intento de firmar documento sin servicio institucional lanza UnsupportedOperationException")
    void firmarDocumento_LanzaExcepcion() {
        byte[] pdfVacio = new byte[]{1, 2, 3};
        assertThatThrownBy(() -> firmaDigitalAdapterService.firmarDocumento(pdfVacio, "EXP-2026-00001"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("No está permitido simular firmas institucionales");
    }
}
