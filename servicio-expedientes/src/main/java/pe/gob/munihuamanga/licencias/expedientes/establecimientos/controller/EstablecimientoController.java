package pe.gob.munihuamanga.licencias.expedientes.establecimientos.controller;

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
import pe.gob.munihuamanga.licencias.common.dto.ActualizarEstablecimientoDto;
import pe.gob.munihuamanga.licencias.common.dto.CrearEstablecimientoDto;
import pe.gob.munihuamanga.licencias.common.dto.EstablecimientoDto;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.service.EstablecimientoService;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para el Módulo M02: Establecimientos.
 */
@Slf4j
@RestController
@RequestMapping("/api/establecimientos")
@RequiredArgsConstructor
@Tag(name = "M02: Establecimientos", description = "Gestión de locales físicos comerciales vinculados a cuentas")
public class EstablecimientoController {

    private final EstablecimientoService establecimientoService;

    @PostMapping("/cuenta/{cuentaId}")
    @PreAuthorize("@establecimientoSecurity.puedeGestionarEstablecimientosDeCuenta(authentication, #cuentaId)")
    @Operation(summary = "Registrar un nuevo establecimiento físico vinculado a una cuenta")
    public ResponseEntity<EstablecimientoDto> registrarEstablecimiento(
            @PathVariable UUID cuentaId,
            @Valid @RequestBody CrearEstablecimientoDto dto,
            Authentication authentication
    ) {
        String responsable = authentication != null ? authentication.getName() : "ADMIN_SISTEMA";
        EstablecimientoDto response = establecimientoService.registrarEstablecimiento(cuentaId, dto, responsable);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@establecimientoSecurity.puedeVerEstablecimientoPorId(authentication, #id)")
    @Operation(summary = "Consultar los datos de un establecimiento por ID")
    public ResponseEntity<EstablecimientoDto> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(establecimientoService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@establecimientoSecurity.puedeModificarEstablecimientoPorId(authentication, #id)")
    @Operation(summary = "Actualizar los datos físicos o de giro de un establecimiento")
    public ResponseEntity<EstablecimientoDto> actualizarEstablecimiento(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarEstablecimientoDto dto,
            Authentication authentication
    ) {
        String responsable = authentication != null ? authentication.getName() : "ADMIN_SISTEMA";
        return ResponseEntity.ok(establecimientoService.actualizarEstablecimiento(id, dto, responsable));
    }

    @GetMapping("/cuenta/{cuentaId}")
    @PreAuthorize("@establecimientoSecurity.puedeVerEstablecimientosDeCuenta(authentication, #cuentaId)")
    @Operation(summary = "Listar los establecimientos de una cuenta")
    public ResponseEntity<List<EstablecimientoDto>> listarPorCuenta(@PathVariable UUID cuentaId) {
        return ResponseEntity.ok(establecimientoService.listarPorCuenta(cuentaId));
    }
}
