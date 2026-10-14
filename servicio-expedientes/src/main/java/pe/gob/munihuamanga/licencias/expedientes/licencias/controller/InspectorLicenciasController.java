package pe.gob.munihuamanga.licencias.expedientes.licencias.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.munihuamanga.licencias.common.dto.VerificacionInspectorDto;
import pe.gob.munihuamanga.licencias.expedientes.licencias.service.LicenciaService;

/**
 * Controlador REST para la consulta móvil y fiscalización de inspectores municipales (M10 - Incremento I09).
 * Permite verificar licencias en campo mediante token QR y consultar datos georreferenciados oficiales.
 * Acceso de solo lectura reservado para ROLE_INSPECTOR y ROLE_ADMIN.
 */
@Slf4j
@RestController
@RequestMapping("/api/inspector/licencias")
@RequiredArgsConstructor
@Tag(name = "Consulta de Fiscalización Móvil", description = "Endpoints para fiscalización en campo por inspectores municipales (RF-VER-007 / CA-10)")
public class InspectorLicenciasController {

    private final LicenciaService licenciaService;

    @GetMapping("/verificar/{token}")
    @PreAuthorize("hasAnyRole('INSPECTOR', 'ADMIN')")
    @Operation(summary = "RF-VER-007 / CA-10: Consulta móvil autenticada de licencia con georreferenciación oficial y auditoría")
    public ResponseEntity<VerificacionInspectorDto> verificarParaInspector(
            @PathVariable String token,
            Authentication auth,
            HttpServletRequest request) {
        String inspectorUsername = (auth != null) ? auth.getName() : "INSPECTOR_ANONIMO";
        String ipOrigen = (request != null) ? request.getRemoteAddr() : "127.0.0.1";
        String userAgent = (request != null) ? request.getHeader("User-Agent") : "UNKNOWN";

        log.info("[INSPECCION-MOVIL] Inspector {} consulta licencia con token QR {}", inspectorUsername, token);

        VerificacionInspectorDto dto = licenciaService.verificarLicenciaInspector(token, inspectorUsername, ipOrigen, userAgent);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().mustRevalidate())
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(dto);
    }
}
