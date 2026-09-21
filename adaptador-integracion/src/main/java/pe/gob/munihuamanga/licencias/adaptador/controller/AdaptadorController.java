package pe.gob.munihuamanga.licencias.adaptador.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Adaptador de Integración (Fase 2 / Patrón Adapter-Stub).
 * En Fase 1 permite simular y registrar respuestas manuales de entidades externas.
 * En Fase 2 se conectará directamente a los APIs del SAT, Defensa Civil y Edificaciones sin cambiar los microservicios centrales.
 */
@Slf4j
@RestController
@RequestMapping("/api/integraciones")
@Tag(name = "Adaptador de Integración", description = "Puntos de extensión para SAT, Defensa Civil y Edificaciones")
public class AdaptadorController {

    @GetMapping("/sat/validar-pago/{voucherId}")
    @Operation(summary = "SAT: Consultar validación de pago de tasa administrativa")
    public ResponseEntity<Map<String, Object>> validarPagoSat(@PathVariable String voucherId) {
        log.info("Consultando estado de pago en SAT para voucher: {}", voucherId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("voucherId", voucherId);
        resp.put("estadoPago", "PAGADO");
        resp.put("fechaPago", LocalDateTime.now().minusHours(2));
        resp.put("entidadRecaudadora", "SAT - Municipalidad Provincial de Huamanga");
        resp.put("canal", "Ventanilla Caja Central / Banca por Internet");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/defensa-civil/simular-dictamen")
    @Operation(summary = "Defensa Civil: Simular dictamen técnico de inspección ITSE")
    public ResponseEntity<Map<String, Object>> simularDictamenDefensaCivil(
            @RequestParam String numeroTramite,
            @RequestParam NivelRiesgo nivelRiesgo
    ) {
        log.info("Simulando dictamen ITSE de Defensa Civil para {}: {}", numeroTramite, nivelRiesgo);
        Map<String, Object> resp = new HashMap<>();
        resp.put("numeroTramite", numeroTramite);
        resp.put("nivelRiesgo", nivelRiesgo);
        resp.put("informeItse", "INF-ITSE-2026-" + (int)(Math.random() * 9000 + 1000));
        resp.put("resultado", "FAVORABLE");
        resp.put("inspector", "Ing. Carlos Mendoza (CIP 45879)");
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/edificaciones/zonificacion")
    @Operation(summary = "Edificaciones: Consulta de compatibilidad de uso y zonificación")
    public ResponseEntity<Map<String, Object>> consultarZonificacion(
            @RequestParam String direccion,
            @RequestParam String giro
    ) {
        log.info("Consultando zonificación para: {} con giro {}", direccion, giro);
        Map<String, Object> resp = new HashMap<>();
        resp.put("direccion", direccion);
        resp.put("giro", giro);
        resp.put("zona", "ZRE-CH (Zona de Reglamentación Especial - Centro Histórico)");
        resp.put("compatible", true);
        resp.put("resolucionCompatibilidad", "RES-ZONIF-2026-0341");
        return ResponseEntity.ok(resp);
    }
}
