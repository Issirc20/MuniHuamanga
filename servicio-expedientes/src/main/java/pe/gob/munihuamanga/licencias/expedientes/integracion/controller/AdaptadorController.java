package pe.gob.munihuamanga.licencias.expedientes.integracion.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.munihuamanga.licencias.expedientes.integracion.port.EdificacionesPort;
import pe.gob.munihuamanga.licencias.expedientes.integracion.port.SatPort;

import java.util.Map;

/**
 * Adaptador de Integración (Patrón Ports & Adapters / Clean Architecture).
 * En MuniHuamanga V2 (Corrección I02.1), se eliminaron y deshabilitaron todas las simulaciones
 * que puedan interpretarse como validaciones oficiales o actos administrativos (ERS-1.3 / RF-EVA-003).
 * Las consultas al SAT y a Edificaciones son de carácter referencial y no sustituyen
 * la conciliación manual en ventanilla ni el informe técnico motivado del evaluador.
 */
@Slf4j
@RestController
@RequestMapping("/api/integraciones")
@RequiredArgsConstructor
@Tag(name = "Adaptador de Integración", description = "Puertos y adaptadores para interoperabilidad externa institucional")
public class AdaptadorController {

    private final SatPort satPort;
    private final EdificacionesPort edificacionesPort;

    @GetMapping("/sat/validar-pago/{voucherId}")
    @PreAuthorize("hasRole('CAJERO')")
    @Operation(summary = "SAT: Consultar validación y conciliación de pago de tasa administrativa")
    public ResponseEntity<Map<String, Object>> validarPagoSat(@PathVariable String voucherId) {
        return ResponseEntity.ok(satPort.validarEstadoPago(voucherId));
    }

    @GetMapping("/edificaciones/zonificacion")
    @PreAuthorize("hasRole('EVALUADOR')")
    @Operation(summary = "Edificaciones: Consulta de compatibilidad de uso y zonificación PDU de referencia")
    public ResponseEntity<Map<String, Object>> consultarZonificacion(
            @RequestParam String direccion,
            @RequestParam String giro
    ) {
        return ResponseEntity.ok(edificacionesPort.consultarCompatibilidadZonificacion(direccion, giro));
    }
}

