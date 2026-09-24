package pe.gob.munihuamanga.licencias.adaptador.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.munihuamanga.licencias.adaptador.port.DefensaCivilPort;
import pe.gob.munihuamanga.licencias.adaptador.port.EdificacionesPort;
import pe.gob.munihuamanga.licencias.adaptador.port.FiscalizacionPort;
import pe.gob.munihuamanga.licencias.adaptador.port.SatPort;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

import java.util.Map;

/**
 * Adaptador de Integración (Fase 2 / Patrón Ports & Adapters).
 * En Fase 1 permite simular y desacoplar las respuestas de entidades externas.
 * En Fase 2 se conectará directamente a los APIs del SAT, Defensa Civil, Edificaciones y Fiscalización sin alterar los microservicios centrales (RNF-19).
 */
@Slf4j
@RestController
@RequestMapping("/api/integraciones")
@RequiredArgsConstructor
@Tag(name = "Adaptador de Integración", description = "Puertos y adaptadores para SAT, Defensa Civil, Edificaciones y Fiscalización")
public class AdaptadorController {

    private final SatPort satPort;
    private final DefensaCivilPort defensaCivilPort;
    private final EdificacionesPort edificacionesPort;
    private final FiscalizacionPort fiscalizacionPort;

    @GetMapping("/sat/validar-pago/{voucherId}")
    @Operation(summary = "SAT: Consultar validación y conciliación de pago de tasa administrativa")
    public ResponseEntity<Map<String, Object>> validarPagoSat(@PathVariable String voucherId) {
        return ResponseEntity.ok(satPort.validarEstadoPago(voucherId));
    }

    @PostMapping("/defensa-civil/simular-dictamen")
    @Operation(summary = "Defensa Civil: Simular dictamen técnico de inspección ITSE")
    public ResponseEntity<Map<String, Object>> simularDictamenDefensaCivil(
            @RequestParam String numeroTramite,
            @RequestParam NivelRiesgo nivelRiesgo
    ) {
        return ResponseEntity.ok(defensaCivilPort.simularDictamenItse(numeroTramite, nivelRiesgo));
    }

    @GetMapping("/edificaciones/zonificacion")
    @Operation(summary = "Edificaciones: Consulta de compatibilidad de uso y zonificación PDU")
    public ResponseEntity<Map<String, Object>> consultarZonificacion(
            @RequestParam String direccion,
            @RequestParam String giro
    ) {
        return ResponseEntity.ok(edificacionesPort.consultarCompatibilidadZonificacion(direccion, giro));
    }

    @PostMapping("/fiscalizacion/acta")
    @Operation(summary = "Fiscalización: Registrar acta de control e inspección posterior en comercio")
    public ResponseEntity<Map<String, Object>> registrarActaFiscalizacion(
            @RequestParam String codigoLicencia,
            @RequestParam(defaultValue = "CONFORME") String resultado,
            @RequestParam(required = false) String inspector
    ) {
        return ResponseEntity.ok(fiscalizacionPort.registrarActaFiscalizacion(codigoLicencia, resultado, inspector));
    }
}
