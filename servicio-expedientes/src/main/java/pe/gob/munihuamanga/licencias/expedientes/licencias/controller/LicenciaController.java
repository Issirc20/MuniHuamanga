package pe.gob.munihuamanga.licencias.expedientes.licencias.controller;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.munihuamanga.licencias.common.dto.EmitirLicenciaDto;
import pe.gob.munihuamanga.licencias.common.dto.LicenciaEmitidaDto;
import pe.gob.munihuamanga.licencias.common.dto.VerificacionPublicaQrDto;
import pe.gob.munihuamanga.licencias.expedientes.licencias.service.LicenciaService;

import java.util.UUID;

/**
 * Controlador REST para la emisión y consulta de Licencias de Funcionamiento oficiales (M09 - Incremento I08).
 */
@Slf4j
@RestController
@RequestMapping("/api/licencias")
@RequiredArgsConstructor
public class LicenciaController {

    private final LicenciaService licenciaService;
    private final pe.gob.munihuamanga.licencias.expedientes.licencias.service.RateLimiterService rateLimiterService;

    @PostMapping("/emitir")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LicenciaEmitidaDto> emitirLicencia(
            @Valid @RequestBody EmitirLicenciaDto dto,
            Authentication auth) {
        log.info("[API-LICENCIAS] Solicitud formal de emisión de licencia para resolución: {}", dto.getResolucionId());
        LicenciaEmitidaDto lic = licenciaService.emitirLicencia(dto, auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(lic);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUADOR') or @licenciaSecurity.puedeVerODescargarLicencia(authentication, #id)")
    public ResponseEntity<LicenciaEmitidaDto> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(licenciaService.obtenerPorId(id));
    }

    @GetMapping("/expediente/{expedienteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUADOR') or @expedienteSecurity.puedeVerExpediente(authentication, #expedienteId)")
    public ResponseEntity<LicenciaEmitidaDto> obtenerPorExpediente(@PathVariable UUID expedienteId) {
        return ResponseEntity.ok(licenciaService.obtenerPorExpediente(expedienteId));
    }

    @GetMapping("/verificar/{codigoQr}")
    public ResponseEntity<VerificacionPublicaQrDto> verificarPorCodigoQr(
            @PathVariable String codigoQr,
            jakarta.servlet.http.HttpServletRequest request) {
        String ip = request != null ? request.getRemoteAddr() : "127.0.0.1";
        if (!rateLimiterService.admitirSolicitud(ip)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", "60")
                    .build();
        }
        VerificacionPublicaQrDto dto = licenciaService.verificarLicenciaPublica(codigoQr);
        return ResponseEntity.ok()
                .cacheControl(org.springframework.http.CacheControl.noStore().mustRevalidate())
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(dto);
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUADOR') or @licenciaSecurity.puedeVerODescargarLicencia(authentication, #id)")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable UUID id) {
        byte[] pdfBytes = licenciaService.descargarPdfLicencia(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "Licencia-" + id + ".pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
