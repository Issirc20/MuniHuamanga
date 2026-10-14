package pe.gob.munihuamanga.licencias.expedientes.resoluciones.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.munihuamanga.licencias.common.dto.ActualizarResolucionDto;
import pe.gob.munihuamanga.licencias.common.dto.AprobarResolucionDto;
import pe.gob.munihuamanga.licencias.common.dto.CrearResolucionDto;
import pe.gob.munihuamanga.licencias.common.dto.EstadoFirmaDto;
import pe.gob.munihuamanga.licencias.common.dto.ResolucionAdministrativaDto;
import pe.gob.munihuamanga.licencias.common.dto.SolicitarFirmaDto;
import pe.gob.munihuamanga.licencias.expedientes.resoluciones.service.ResolucionAdministrativaService;

import java.util.UUID;

/**
 * Controlador REST para la gestión de Resoluciones Administrativas Gerenciales (M09 - Incremento I08).
 */
@Slf4j
@RestController
@RequestMapping("/api/resoluciones")
@RequiredArgsConstructor
public class ResolucionAdministrativaController {

    private final ResolucionAdministrativaService resolucionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUADOR')")
    public ResponseEntity<ResolucionAdministrativaDto> crearBorrador(
            @Valid @RequestBody CrearResolucionDto dto,
            Authentication auth) {
        log.info("[API-RESOLUCIONES] Solicitud de creación de borrador para expediente: {}", dto.getExpedienteId());
        ResolucionAdministrativaDto res = resolucionService.crearBorrador(dto, auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUADOR')")
    public ResponseEntity<ResolucionAdministrativaDto> actualizarBorrador(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarResolucionDto dto,
            Authentication auth) {
        return ResponseEntity.ok(resolucionService.actualizarBorrador(id, dto, auth));
    }

    @PostMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResolucionAdministrativaDto> aprobarResolucion(
            @PathVariable UUID id,
            @RequestBody(required = false) AprobarResolucionDto dto,
            Authentication auth) {
        log.info("[API-RESOLUCIONES] Acto formal de aprobación de resolución: {}", id);
        return ResponseEntity.ok(resolucionService.aprobarResolucion(id, dto, auth));
    }

    @PostMapping("/{id}/firmar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResolucionAdministrativaDto> solicitarFirmaDigital(
            @PathVariable UUID id,
            @RequestBody(required = false) SolicitarFirmaDto dto,
            Authentication auth) {
        log.info("[API-RESOLUCIONES] Solicitud de firma digital para resolución: {}", id);
        return ResponseEntity.ok(resolucionService.solicitarFirmaDigital(id, dto, auth));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUADOR', 'INSPECTOR') or @resolucionSecurity.puedeVerResolucion(authentication, #id)")
    public ResponseEntity<ResolucionAdministrativaDto> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(resolucionService.obtenerPorId(id));
    }

    @GetMapping("/expediente/{expedienteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUADOR', 'INSPECTOR') or @expedienteSecurity.puedeVerExpediente(authentication, #expedienteId)")
    public ResponseEntity<ResolucionAdministrativaDto> obtenerPorExpediente(@PathVariable UUID expedienteId) {
        return ResponseEntity.ok(resolucionService.obtenerActualPorExpediente(expedienteId));
    }

    @GetMapping("/{id}/estado-firma")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUADOR', 'INSPECTOR') or @resolucionSecurity.puedeVerResolucion(authentication, #id)")
    public ResponseEntity<EstadoFirmaDto> consultarEstadoFirma(@PathVariable UUID id) {
        return ResponseEntity.ok(resolucionService.consultarEstadoFirma(id));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUADOR', 'INSPECTOR') or @resolucionSecurity.puedeVerResolucion(authentication, #id)")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable UUID id) {
        byte[] pdfBytes = resolucionService.generarPdfResolucion(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "Resolucion-" + id + ".pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
