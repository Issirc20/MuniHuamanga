package pe.gob.munihuamanga.licencias.expedientes.integracion.adapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SatAdapterServiceTest {

    private SatAdapterService satAdapterService;

    @BeforeEach
    void setUp() {
        satAdapterService = new SatAdapterService();
    }

    @Test
    @DisplayName("I02.1: El adaptador SAT no debe simular pagos como PAGADO ni números OP falsos")
    void validarEstadoPago_NoSimulaPagosOficiales() {
        String voucher = "VOUCHER-TEST-999";
        Map<String, Object> resultado = satAdapterService.validarEstadoPago(voucher);

        assertThat(resultado).isNotNull();
        assertThat(resultado.get("voucherId")).isEqualTo(voucher);
        assertThat(resultado.get("estadoPago")).isEqualTo("REQUIERE_VALIDACION_MANUAL_VENTANILLA");
        assertThat(resultado.get("valido")).isEqualTo(false);
        assertThat(resultado.get("simulacionOficial")).isEqualTo(false);
        assertThat(resultado.get("requiereValidacionManual")).isEqualTo(true);
        assertThat(resultado.get("mensaje").toString()).contains("homologación técnica");
    }
}
