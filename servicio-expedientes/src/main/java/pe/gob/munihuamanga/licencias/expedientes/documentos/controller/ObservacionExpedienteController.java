package pe.gob.munihuamanga.licencias.expedientes.documentos.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.munihuamanga.licencias.common.dto.CrearObservacionDto;
import pe.gob.munihuamanga.licencias.common.dto.ObservacionExpedienteDto;
import pe.gob.munihuamanga.licencias.common.dto.SubsanarObservacionesDto;
import pe.gob.munihuamanga.licencias.expedientes.documentos.service.ObservacionExpedienteService;
import pe.gob.munihuamanga.licencias.expedientes.security.CustomUserDetails;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controlador REST para formulación y subsanación de observaciones (Incremento I05).
 */
@RestController
@RequestMapping("/api/expedientes/{id}/observaciones")
@Tag(name = "Observaciones y Subsanaciones", description = "Flujo normativo de observaciones y subsanaciones según Ley N° 27444 LPAG")
public class ObservacionExpedienteController {

    private final ObservacionExpedienteService observacionService;

    public ObservacionExpedienteController(ObservacionExpedienteService observacionService) {
        this.observacionService = observacionService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EVALUADOR', 'ADMIN')")
    @Operation(summary = "Formular observaciones técnicas o documentales sobre un expediente (EVALUADOR / ADMIN)")
    public ResponseEntity<List<ObservacionExpedienteDto>> formularObservaciones(
            @PathVariable UUID id,
            @Valid @RequestBody List<CrearObservacionDto> dtos,
            Authentication authentication
    ) {
        UUID evaluadorId = extraerUsuarioId(authentication);
        String username = authentication.getName();

        List<ObservacionExpedienteDto> res = observacionService.formularObservaciones(id, dtos, evaluadorId, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @GetMapping
    @PreAuthorize("@expedienteSecurity.puedeVerExpediente(authentication, #id)")
    @Operation(summary = "Consultar el listado de observaciones y plazos de subsanación de un expediente")
    public ResponseEntity<List<ObservacionExpedienteDto>> listarObservaciones(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID usuarioId = extraerUsuarioId(authentication);
        String username = authentication.getName();
        String roles = extraerRoles(authentication);

        return ResponseEntity.ok(observacionService.listarObservaciones(id, usuarioId, username, roles));
    }

    @PostMapping("/subsanar")
    @PreAuthorize("@expedienteSecurity.puedeSubsanar(authentication, #id)")
    @Operation(summary = "Presentación formal de subsanación de observaciones por el administrado (Art. 136 Ley N° 27444)")
    public ResponseEntity<Void> subsanarObservaciones(
            @PathVariable UUID id,
            @Valid @RequestBody SubsanarObservacionesDto dto,
            Authentication authentication
    ) {
        UUID usuarioId = extraerUsuarioId(authentication);
        String username = authentication.getName();
        String roles = extraerRoles(authentication);

        observacionService.subsanarObservaciones(id, dto, usuarioId, username, roles);
        return ResponseEntity.noContent().build();
    }

    private UUID extraerUsuarioId(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails cud) {
            return cud.getId();
        }
        return null;
    }

    private String extraerRoles(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) return "";
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(","));
    }
}
