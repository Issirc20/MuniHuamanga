package pe.gob.munihuamanga.licencias.expedientes.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.munihuamanga.licencias.common.dto.ActualizarTarifaDto;
import pe.gob.munihuamanga.licencias.common.dto.TarifaTupaDto;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.expedientes.service.TarifaTupaService;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para el mantenimiento del Tarifario TUPA (Fase 04 Sprint 4-D).
 */
@RestController
@RequestMapping("/api/tupa/tarifas")
@RequiredArgsConstructor
@Tag(name = "Tarifario TUPA", description = "Endpoints para la consulta y administración dinámica de tasas municipales")
public class TarifaTupaController {

    private final TarifaTupaService tarifaService;

    @GetMapping
    @Operation(summary = "Listar tarifas TUPA (todas o solo activas)")
    public ResponseEntity<List<TarifaTupaDto>> listarTarifas(
            @RequestParam(required = false, defaultValue = "false") boolean soloActivas
    ) {
        List<TarifaTupaDto> lista = soloActivas
                ? tarifaService.listarActivas()
                : tarifaService.listarTodas();
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de tarifa TUPA por identificador UUID")
    public ResponseEntity<TarifaTupaDto> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(tarifaService.obtenerPorId(id));
    }

    @GetMapping("/riesgo/{nivelRiesgo}")
    @Operation(summary = "Obtener tarifa TUPA por nivel de riesgo ITSE")
    public ResponseEntity<TarifaTupaDto> obtenerPorRiesgo(@PathVariable NivelRiesgo nivelRiesgo) {
        return ResponseEntity.ok(tarifaService.obtenerPorRiesgo(nivelRiesgo));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar montos, desglose y base legal de una tarifa TUPA (Solo ADMIN)")
    public ResponseEntity<TarifaTupaDto> actualizarTarifa(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarTarifaDto dto,
            Authentication authentication
    ) {
        String usuario = authentication != null ? authentication.getName() : "ADMIN_SISTEMA";
        TarifaTupaDto actualizada = tarifaService.actualizarTarifa(id, dto, usuario);
        return ResponseEntity.ok(actualizada);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activar o desactivar una tarifa TUPA (Solo ADMIN)")
    public ResponseEntity<TarifaTupaDto> cambiarEstado(
            @PathVariable UUID id,
            @RequestParam boolean activo,
            Authentication authentication
    ) {
        String usuario = authentication != null ? authentication.getName() : "ADMIN_SISTEMA";
        TarifaTupaDto actualizada = tarifaService.cambiarEstado(id, activo, usuario);
        return ResponseEntity.ok(actualizada);
    }
}
