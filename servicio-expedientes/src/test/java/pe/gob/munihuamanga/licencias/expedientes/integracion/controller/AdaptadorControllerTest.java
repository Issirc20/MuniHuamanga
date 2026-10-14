package pe.gob.munihuamanga.licencias.expedientes.integracion.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import pe.gob.munihuamanga.licencias.expedientes.integracion.port.EdificacionesPort;
import pe.gob.munihuamanga.licencias.expedientes.integracion.port.SatPort;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias del Adaptador de Integración (US-13, RNF-19 - Clean Architecture)")
class AdaptadorControllerTest {

    @Mock
    private SatPort satPort;

    @Mock
    private EdificacionesPort edificacionesPort;

    @InjectMocks
    private AdaptadorController adaptadorController;

    @Test
    @DisplayName("US-13: Debe consultar validación de pago ante el puerto SAT")
    void testValidarPagoSat() {
        String voucherId = "VCH-2026-123456";
        when(satPort.validarEstadoPago(voucherId)).thenReturn(Map.of(
                "estadoPago", "REQUIERE_VALIDACION_MANUAL_VENTANILLA",
                "valido", false,
                "voucherId", voucherId
        ));

        ResponseEntity<Map<String, Object>> resp = adaptadorController.validarPagoSat(voucherId);

        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertEquals("REQUIERE_VALIDACION_MANUAL_VENTANILLA", resp.getBody().get("estadoPago"));
        assertEquals(false, resp.getBody().get("valido"));
    }

    @Test
    @DisplayName("US-13: Debe consultar compatibilidad de zonificación ante el puerto Edificaciones")
    void testConsultarZonificacion() {
        String direccion = "Jr. 28 de Julio N° 120";
        String giro = "Restaurante";
        when(edificacionesPort.consultarCompatibilidadZonificacion(direccion, giro))
                .thenReturn(Map.of(
                        "clasificacionZonificacion", "REFERENCIAL_PDU",
                        "compatibilidadUso", "PENDIENTE_DICTAMEN_TECNICO_EVALUADOR"
                ));

        ResponseEntity<Map<String, Object>> resp = adaptadorController.consultarZonificacion(direccion, giro);

        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertEquals("PENDIENTE_DICTAMEN_TECNICO_EVALUADOR", resp.getBody().get("compatibilidadUso"));
        assertEquals("REFERENCIAL_PDU", resp.getBody().get("clasificacionZonificacion"));
    }
}
