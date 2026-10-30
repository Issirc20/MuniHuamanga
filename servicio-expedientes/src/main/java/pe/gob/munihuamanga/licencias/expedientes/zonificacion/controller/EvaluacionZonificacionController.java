package pe.gob.munihuamanga.licencias.expedientes.zonificacion.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.munihuamanga.licencias.common.dto.EvaluacionZonificacionDto;
import pe.gob.munihuamanga.licencias.common.dto.RegistrarEvaluacionZonificacionDto;
import pe.gob.munihuamanga.licencias.common.enums.ResultadoZonificacion;
import pe.gob.munihuamanga.licencias.expedientes.security.CustomUserDetails;
import pe.gob.munihuamanga.licencias.expedientes.zonificacion.service.EvaluacionZonificacionService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Tag(name = "Zonificación y Compatibilidad de Uso", description = "Evaluación técnica de compatibilidad de uso y zonificación PDU (M07 - I06)")
public class EvaluacionZonificacionController {

    private final EvaluacionZonificacionService zonificacionService;

    @PostMapping("/api/expedientes/{id}/zonificacion/evaluar")
    @PreAuthorize("hasRole('EVALUADOR')")
    @Operation(summary = "Registrar evaluación técnica de zonificación y compatibilidad (EVALUADOR / ADMIN)")
    public ResponseEntity<EvaluacionZonificacionDto> registrarEvaluacion(
            @PathVariable UUID id,
            @Valid @RequestBody RegistrarEvaluacionZonificacionDto dto,
            Authentication authentication
    ) {
        UUID evaluadorId = extraerUsuarioId(authentication);
        String evaluadorNombre = authentication != null ? authentication.getName() : "EVALUADOR";

        EvaluacionZonificacionDto evaluacion = zonificacionService.registrarEvaluacion(id, dto, evaluadorId, evaluadorNombre);
        return ResponseEntity.status(HttpStatus.CREATED).body(evaluacion);
    }

    @GetMapping("/api/expedientes/{id}/zonificacion")
    @Operation(summary = "Consultar el historial de evaluaciones de zonificación de un expediente")
    public ResponseEntity<List<EvaluacionZonificacionDto>> listarEvaluaciones(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID usuarioId = extraerUsuarioId(authentication);
        String username = authentication != null ? authentication.getName() : null;
        String roles = extraerRoles(authentication);

        return ResponseEntity.ok(zonificacionService.listarEvaluacionesPorExpediente(id, usuarioId, username, roles));
    }

    @GetMapping("/api/expedientes/{id}/zonificacion/ultima")
    @Operation(summary = "Consultar la última evaluación de zonificación dictaminada para el expediente")
    public ResponseEntity<EvaluacionZonificacionDto> obtenerUltimaEvaluacion(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID usuarioId = extraerUsuarioId(authentication);
        String username = authentication != null ? authentication.getName() : null;
        String roles = extraerRoles(authentication);

        return zonificacionService.obtenerUltimaEvaluacion(id, usuarioId, username, roles)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/api/zonificacion/bandeja")
    @PreAuthorize("hasRole('EVALUADOR')")
    @Operation(summary = "Bandeja de evaluaciones de zonificación por resultado (EVALUADOR / ADMIN)")
    public ResponseEntity<List<EvaluacionZonificacionDto>> bandejaZonificacion(
            @RequestParam(required = false, defaultValue = "PENDIENTE_EVALUACION") ResultadoZonificacion resultado
    ) {
        return ResponseEntity.ok(zonificacionService.listarPorResultado(resultado));
    }

    private UUID extraerUsuarioId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails cud) {
            return cud.getId();
        }
        return null;
    }

    private String extraerRoles(Authentication authentication) {
        if (authentication == null) return "";
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
    }
}
