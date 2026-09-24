package pe.gob.munihuamanga.licencias.adaptador.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import pe.gob.munihuamanga.licencias.adaptador.port.DefensaCivilPort;
import pe.gob.munihuamanga.licencias.adaptador.port.EdificacionesPort;
import pe.gob.munihuamanga.licencias.adaptador.port.FiscalizacionPort;
import pe.gob.munihuamanga.licencias.adaptador.port.SatPort;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias del Adaptador de Integración Fase 2 (US-13, RNF-19)")
class AdaptadorControllerTest {

    @Mock
    private SatPort satPort;

    @Mock
    private DefensaCivilPort defensaCivilPort;

    @Mock
    private EdificacionesPort edificacionesPort;

    @Mock
    private FiscalizacionPort fiscalizacionPort;

    @InjectMocks
    private AdaptadorController adaptadorController;

    @Test
    @DisplayName("US-13: Debe consultar validación de pago ante el puerto SAT")
    void testValidarPagoSat() {
        String voucherId = "VCH-2026-123456";
        when(satPort.validarEstadoPago(voucherId)).thenReturn(Map.of("estadoPago", "PAGADO", "voucherId", voucherId));

        ResponseEntity<Map<String, Object>> resp = adaptadorController.validarPagoSat(voucherId);

        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode().value());
        assertEquals("PAGADO", resp.getBody().get("estadoPago"));
    }

    @Test
    @DisplayName("US-13: Debe simular dictamen técnico ante el puerto Defensa Civil")
    void testSimularDictamenDefensaCivil() {
        String tramite = "EXP-2026-00001";
        NivelRiesgo riesgo = NivelRiesgo.MEDIO;
        when(defensaCivilPort.simularDictamenItse(tramite, riesgo))
                .thenReturn(Map.of("resultadoInspeccion", "FAVORABLE", "numeroTramite", tramite));

        ResponseEntity<Map<String, Object>> resp = adaptadorController.simularDictamenDefensaCivil(tramite, riesgo);

        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode().value());
        assertEquals("FAVORABLE", resp.getBody().get("resultadoInspeccion"));
    }

    @Test
    @DisplayName("US-13: Debe consultar compatibilidad de zonificación ante el puerto Edificaciones")
    void testConsultarZonificacion() {
        String direccion = "Jr. 28 de Julio N° 120";
        String giro = "Restaurante";
        when(edificacionesPort.consultarCompatibilidadZonificacion(direccion, giro))
                .thenReturn(Map.of("compatibilidadUso", "COMPATIBLE / PERMITIDO"));

        ResponseEntity<Map<String, Object>> resp = adaptadorController.consultarZonificacion(direccion, giro);

        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode().value());
        assertEquals("COMPATIBLE / PERMITIDO", resp.getBody().get("compatibilidadUso"));
    }

    @Test
    @DisplayName("US-13: Debe registrar acta de control ante el puerto Fiscalización")
    void testRegistrarActaFiscalizacion() {
        String codLicencia = "LIC-2026-00000002";
        when(fiscalizacionPort.registrarActaFiscalizacion(codLicencia, "CONFORME", "Inspector"))
                .thenReturn(Map.of("resultadoFiscalizacion", "CONFORME", "codigoLicencia", codLicencia));

        ResponseEntity<Map<String, Object>> resp = adaptadorController.registrarActaFiscalizacion(codLicencia, "CONFORME", "Inspector");

        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode().value());
        assertEquals("CONFORME", resp.getBody().get("resultadoFiscalizacion"));
    }
}
