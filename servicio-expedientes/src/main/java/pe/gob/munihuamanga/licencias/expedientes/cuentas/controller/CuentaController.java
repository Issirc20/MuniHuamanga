package pe.gob.munihuamanga.licencias.expedientes.cuentas.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import pe.gob.munihuamanga.licencias.common.dto.EstablecerContrasenaDto;
import pe.gob.munihuamanga.licencias.common.dto.OperadorCuentaDto;
import pe.gob.munihuamanga.licencias.common.dto.ReenviarVerificacionDto;
import pe.gob.munihuamanga.licencias.common.dto.RegistroCuentaDto;
import pe.gob.munihuamanga.licencias.common.dto.ValidarIdentidadDto;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.service.ActivacionAccesoService;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.service.CuentaService;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.service.ValidacionIdentidadService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/cuentas")
@RequiredArgsConstructor
@Tag(name = "Cuentas", description = "Registro de administrados, verificación de correo, representantes y validación de identidad")
public class CuentaController {

    private final CuentaService cuentaService;
    private final ActivacionAccesoService activacionAccesoService;
    private final ValidacionIdentidadService validacionIdentidadService;

    @PostMapping("/registro")
    @Operation(summary = "Registrar una cuenta de persona natural o jurídica")
    public ResponseEntity<CuentaResponseDto> registrarCuenta(@Valid @RequestBody RegistroCuentaDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cuentaService.registrarCuenta(dto));
    }

    @PostMapping("/verificar-correo")
    @Operation(summary = "Verificar el correo electrónico mediante el enlace de un solo uso")
    public ResponseEntity<Map<String, Object>> verificarCorreo(@RequestParam("token") String token) {
        boolean verificado = cuentaService.verificarEmail(token);
        return ResponseEntity.ok(Map.of(
                "mensaje", "Correo electrónico verificado. La cuenta se encuentra activa.",
                "verificado", verificado));
    }

    @PostMapping("/verificacion/reenviar")
    @Operation(summary = "Solicitar un nuevo enlace de verificación de correo")
    public ResponseEntity<Map<String, String>> reenviarVerificacion(@Valid @RequestBody ReenviarVerificacionDto dto) {
        cuentaService.reenviarVerificacion(dto.getEmail());
        return ResponseEntity.accepted().body(Map.of("mensaje",
                "Si el correo corresponde a una cuenta pendiente de verificación, recibirá un nuevo enlace."));
    }

    @PostMapping("/accesos/activar")
    @Operation(summary = "Definir la contraseña mediante el enlace de activación de acceso")
    public ResponseEntity<Map<String, String>> activarAcceso(@Valid @RequestBody EstablecerContrasenaDto dto) {
        activacionAccesoService.activar(dto);
        return ResponseEntity.ok(Map.of("mensaje", "Su acceso ha sido activado. Puede iniciar sesión."));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@cuentaSecurity.puedeVerCuenta(authentication, #id)")
    @Operation(summary = "Consultar una cuenta y sus representantes")
    public ResponseEntity<CuentaResponseDto> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(cuentaService.obtenerPorId(id));
    }

    @GetMapping("/documento/{numeroDocumento}")
    @PreAuthorize("hasAnyRole('MESA_DE_PARTES', 'EVALUADOR', 'SUPERVISOR_LICENCIAS', 'AUDITOR')")
    @Operation(summary = "Consultar una cuenta por número de documento (funcionarios)")
    public ResponseEntity<CuentaResponseDto> obtenerPorDocumento(@PathVariable String numeroDocumento) {
        return ResponseEntity.ok(cuentaService.obtenerPorDocumento(numeroDocumento));
    }

    @PostMapping("/{id}/validacion-identidad")
    @PreAuthorize("hasRole('MESA_DE_PARTES')")
    @Operation(summary = "Registrar la validación de la identidad declarada del administrado")
    public ResponseEntity<Void> validarIdentidad(@PathVariable UUID id, @Valid @RequestBody ValidarIdentidadDto dto) {
        validacionIdentidadService.validar(id, dto.getMedio(), dto.getReferencia());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/operadores")
    @PreAuthorize("@cuentaSecurity.puedeGestionarOperadores(authentication, #id)")
    @Operation(summary = "Designar un representante con facultades explícitas")
    public ResponseEntity<OperadorCuentaDto> agregarOperador(@PathVariable UUID id, @Valid @RequestBody CrearOperadorDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cuentaService.agregarOperador(id, dto));
    }

    @PutMapping("/{id}/operadores/{operadorId}")
    @PreAuthorize("@cuentaSecurity.puedeGestionarOperadores(authentication, #id)")
    @Operation(summary = "Actualizar las facultades de un representante")
    public ResponseEntity<OperadorCuentaDto> actualizarPermisosOperador(@PathVariable UUID id, @PathVariable UUID operadorId,
                                                                        @Valid @RequestBody ActualizarPermisosOperadorDto dto) {
        return ResponseEntity.ok(cuentaService.actualizarPermisosOperador(id, operadorId, dto));
    }

    @GetMapping("/{id}/operadores")
    @PreAuthorize("@cuentaSecurity.puedeVerCuenta(authentication, #id)")
    @Operation(summary = "Listar los representantes de una cuenta")
    public ResponseEntity<List<OperadorCuentaDto>> listarOperadores(@PathVariable UUID id) {
        return ResponseEntity.ok(cuentaService.listarOperadores(id));
    }
}
