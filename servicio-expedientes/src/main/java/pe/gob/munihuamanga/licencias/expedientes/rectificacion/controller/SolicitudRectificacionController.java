package pe.gob.munihuamanga.licencias.expedientes.rectificacion.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.munihuamanga.licencias.common.dto.EvaluarRectificacionUbicacionDto;
import pe.gob.munihuamanga.licencias.common.dto.SolicitarRectificacionUbicacionDto;
import pe.gob.munihuamanga.licencias.common.dto.SolicitudRectificacionUbicacionDto;
import pe.gob.munihuamanga.licencias.expedientes.rectificacion.service.SolicitudRectificacionUbicacionService;

import java.util.List;
import java.util.UUID;

@Tag(name = "Rectificación de Coordenadas Geográficas", description = "Endpoints para solicitudes ciudadanas y evaluación municipal de corrección de ubicación (Criterio CA-08)")
@RestController
@RequestMapping("/api/rectificaciones-ubicacion")
@RequiredArgsConstructor
public class SolicitudRectificacionController {

    private final SolicitudRectificacionUbicacionService rectificacionService;

    @Operation(summary = "Registrar solicitud ciudadana de rectificación de coordenadas geográficas")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @establecimientoSecurity.puedeModificarEstablecimientoPorId(authentication, #dto.establecimientoId)")
    public ResponseEntity<SolicitudRectificacionUbicacionDto> solicitarRectificacion(
            @Valid @RequestBody SolicitarRectificacionUbicacionDto dto,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rectificacionService.solicitarRectificacion(dto, auth));
    }

    @Operation(summary = "Evaluar solicitud de rectificación (aprobación, rechazo o detección de traslado físico)")
    @PostMapping("/{id}/evaluar")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUADOR')")
    public ResponseEntity<SolicitudRectificacionUbicacionDto> evaluarRectificacion(
            @PathVariable UUID id,
            @Valid @RequestBody EvaluarRectificacionUbicacionDto dto,
            Authentication auth) {
        return ResponseEntity.ok(rectificacionService.evaluarRectificacion(id, dto, auth));
    }

    @Operation(summary = "Listar solicitudes de rectificación registradas para un establecimiento")
    @GetMapping("/establecimientos/{establecimientoId}")
    @PreAuthorize("hasRole('ADMIN') or @establecimientoSecurity.puedeVerEstablecimientoPorId(authentication, #establecimientoId)")
    public ResponseEntity<List<SolicitudRectificacionUbicacionDto>> listarPorEstablecimiento(
            @PathVariable UUID establecimientoId) {
        return ResponseEntity.ok(rectificacionService.listarPorEstablecimiento(establecimientoId));
    }

    @Operation(summary = "Listar solicitudes de rectificación en estado PENDIENTE para inspección del evaluador")
    @GetMapping("/pendientes")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUADOR')")
    public ResponseEntity<List<SolicitudRectificacionUbicacionDto>> listarPendientes() {
        return ResponseEntity.ok(rectificacionService.listarPendientes());
    }
}
