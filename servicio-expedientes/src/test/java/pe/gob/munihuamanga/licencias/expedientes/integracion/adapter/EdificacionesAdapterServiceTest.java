package pe.gob.munihuamanga.licencias.expedientes.integracion.adapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EdificacionesAdapterServiceTest {

    private EdificacionesAdapterService edificacionesAdapterService;

    @BeforeEach
    void setUp() {
        edificacionesAdapterService = new EdificacionesAdapterService();
    }

    @Test
    @DisplayName("I02.1: El adaptador de Edificaciones no debe emitir certificados de zonificación automáticos simulados")
    void consultarCompatibilidadZonificacion_NoEmiteCertificadosSimulados() {
        String direccion = "Jr. 28 de Julio 123";
        String giro = "Restaurante";

        Map<String, Object> resultado = edificacionesAdapterService.consultarCompatibilidadZonificacion(direccion, giro);

        assertThat(resultado).isNotNull();
        assertThat(resultado.get("direccion")).isEqualTo(direccion);
        assertThat(resultado.get("giroSolicitado")).isEqualTo(giro);
        assertThat(resultado.get("clasificacionZonificacion")).isEqualTo("REFERENCIAL_PDU");
        assertThat(resultado.get("compatibilidadUso")).isEqualTo("PENDIENTE_DICTAMEN_TECNICO_EVALUADOR");
        assertThat(resultado.get("certificadoNumero")).isNull();
        assertThat(resultado.get("simulacionOficial")).isEqualTo(false);
        assertThat(resultado.get("requiereInformeTecnico")).isEqualTo(true);
        assertThat(resultado.get("mensaje").toString()).contains("dictamen técnico motivado");
    }
}
