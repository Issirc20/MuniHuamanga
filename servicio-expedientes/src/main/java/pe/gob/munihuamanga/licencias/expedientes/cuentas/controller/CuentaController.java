package pe.gob.munihuamanga.licencias.expedientes.cuentas.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.munihuamanga.licencias.common.dto.ActualizarPermisosOperadorDto;
import pe.gob.munihuamanga.licencias.common.dto.CrearOperadorDto;
import pe.gob.munihuamanga.licencias.common.dto.CuentaResponseDto;
import pe.gob.munihuamanga.licencias.common.dto.OperadorCuentaDto;
import pe.gob.munihuamanga.licencias.common.dto.RegistroCuentaDto;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.service.CuentaService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador REST para el Módulo M01: Cuentas y Operadores.
 */
@Slf4j
@RestController
@RequestMapping("/api/cuentas")
@RequiredArgsConstructor
@Tag(name = "M01: Cuentas y Operadores", description = "Gestión de identidades de administrados y delegación de poderes nominativos")
public class CuentaController {

    private final CuentaService cuentaService;

    @PostMapping("/registro")
    @Operation(summary = "Registrar nueva cuenta personal (DNI) o institucional (RUC)")
    public ResponseEntity<CuentaResponseDto> registrarCuenta(@Valid @RequestBody RegistroCuentaDto dto) {
        log.info("[API-CUENTAS] Solicitud de registro de cuenta para documento: {}", dto.getNumeroDocumento());
        CuentaResponseDto response = cuentaService.registrarCuenta(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/verificar-correo")
    @Operation(summary = "Verificar correo electrónico mediante token seguro de activación")
    public ResponseEntity<Map<String, Object>> verificarCorreo(@RequestParam("token") String token) {
        boolean verificado = cuentaService.verificarEmail(token);
        return ResponseEntity.ok(Map.of(
                "mensaje", "Correo electrónico verificado exitosamente. La cuenta ha sido activada.",
                "verificado", verificado
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@cuentaSecurity.puedeVerCuenta(authentication, #id)")
    @Operation(summary = "Consultar datos y operadores de una cuenta por ID")
    public ResponseEntity<CuentaResponseDto> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(cuentaService.obtenerPorId(id));
    }

    @GetMapping("/documento/{numeroDocumento}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUADOR', 'INSPECTOR') or authentication.name == #numeroDocumento")
    @Operation(summary = "Consultar cuenta por número de documento de identidad")
    public ResponseEntity<CuentaResponseDto> obtenerPorDocumento(@PathVariable String numeroDocumento) {
        return ResponseEntity.ok(cuentaService.obtenerPorDocumento(numeroDocumento));
    }

    @PostMapping("/{id}/operadores")
    @PreAuthorize("@cuentaSecurity.puedeGestionarOperadores(authentication, #id)")
    @Operation(summary = "Designar un nuevo operador nominativo con permisos explícitos")
    public ResponseEntity<OperadorCuentaDto> agregarOperador(
            @PathVariable UUID id,
            @Valid @RequestBody CrearOperadorDto dto,
            Authentication authentication
    ) {
        String responsable = authentication != null ? authentication.getName() : "ADMIN_SISTEMA";
        OperadorCuentaDto response = cuentaService.agregarOperador(id, dto, responsable);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}/operadores/{operadorId}")
    @PreAuthorize("@cuentaSecurity.puedeGestionarOperadores(authentication, #id)")
    @Operation(summary = "Actualizar permisos de un operador nominativo con pista de auditoría")
    public ResponseEntity<OperadorCuentaDto> actualizarPermisosOperador(
            @PathVariable UUID id,
            @PathVariable UUID operadorId,
            @Valid @RequestBody ActualizarPermisosOperadorDto dto,
            Authentication authentication
    ) {
        String responsable = authentication != null ? authentication.getName() : "ADMIN_SISTEMA";
        OperadorCuentaDto response = cuentaService.actualizarPermisosOperador(id, operadorId, dto, responsable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/operadores")
    @PreAuthorize("@cuentaSecurity.puedeVerCuenta(authentication, #id)")
    @Operation(summary = "Listar los operadores nominados de una cuenta")
    public ResponseEntity<List<OperadorCuentaDto>> listarOperadores(@PathVariable UUID id) {
        return ResponseEntity.ok(cuentaService.listarOperadores(id));
    }
}
