package pe.gob.munihuamanga.licencias.expedientes.reclamaciones.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
import pe.gob.munihuamanga.licencias.common.dto.ReclamacionExpedienteDto;
import pe.gob.munihuamanga.licencias.common.dto.ReclamarExpedienteDto;
import pe.gob.munihuamanga.licencias.common.dto.ResolverReclamacionDto;
import pe.gob.munihuamanga.licencias.expedientes.reclamaciones.service.ReclamacionExpedienteService;
import pe.gob.munihuamanga.licencias.expedientes.security.CustomUserDetails;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/expedientes")
@RequiredArgsConstructor
@Tag(name = "Reclamaciones de Expedientes", description = "Procedimiento formal y verificable de reclamación de titularidad de expedientes históricos")
public class ReclamacionExpedienteController {

    private final ReclamacionExpedienteService reclamacionService;

    @PostMapping("/{id}/reclamar")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Solicitar reclamación de titularidad de un expediente histórico V1")
    public ResponseEntity<ReclamacionExpedienteDto> solicitarReclamacion(
            @PathVariable UUID id,
            @Valid @RequestBody ReclamarExpedienteDto dto,
            Authentication authentication
    ) {
        String username = authentication.getName();
        ReclamacionExpedienteDto creada = reclamacionService.solicitarReclamacion(id, dto, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping("/reclamaciones/cuenta/{cuentaId}")
    @PreAuthorize("@cuentaSecurity.puedeVerCuenta(authentication, #cuentaId)")
    @Operation(summary = "Listar reclamaciones de titularidad de una cuenta")
    public ResponseEntity<List<ReclamacionExpedienteDto>> listarPorCuenta(@PathVariable UUID cuentaId) {
        List<ReclamacionExpedienteDto> lista = reclamacionService.listarPorCuenta(cuentaId);
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/reclamaciones/pendientes")
    @PreAuthorize("hasAnyRole('EVALUADOR', 'ADMIN')")
    @Operation(summary = "Bandeja municipal de reclamaciones pendientes de validación")
    public ResponseEntity<List<ReclamacionExpedienteDto>> listarPendientes() {
        List<ReclamacionExpedienteDto> lista = reclamacionService.listarPendientes();
        return ResponseEntity.ok(lista);
    }

    @PutMapping("/reclamaciones/{reclamacionId}/resolver")
    @PreAuthorize("hasAnyRole('EVALUADOR', 'ADMIN')")
    @Operation(summary = "Resolver formalmente una reclamación de titularidad (Aprobar/Rechazar)")
    public ResponseEntity<ReclamacionExpedienteDto> resolverReclamacion(
            @PathVariable UUID reclamacionId,
            @Valid @RequestBody ResolverReclamacionDto dto,
            Authentication authentication
    ) {
        UUID evaluadorId = obtenerUsuarioId(authentication);
        String evaluadorUsername = authentication.getName();
        ReclamacionExpedienteDto resuelta = reclamacionService.resolverReclamacion(reclamacionId, dto, evaluadorId, evaluadorUsername);
        return ResponseEntity.ok(resuelta);
    }

    private UUID obtenerUsuarioId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof CustomUserDetails cud) {
            return cud.getId();
        }
        return null;
    }
}
