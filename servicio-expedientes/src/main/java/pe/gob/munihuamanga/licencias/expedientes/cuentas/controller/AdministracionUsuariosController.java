package pe.gob.munihuamanga.licencias.expedientes.cuentas.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.munihuamanga.licencias.common.dto.ActualizarRolesDto;
import pe.gob.munihuamanga.licencias.common.dto.CrearFuncionarioDto;
import pe.gob.munihuamanga.licencias.common.dto.UsuarioDto;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.service.AdministracionUsuariosService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/administracion/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administración de usuarios", description = "Alta, roles y habilitación de funcionarios")
public class AdministracionUsuariosController {

    private final AdministracionUsuariosService administracionService;

    @GetMapping
    @Operation(summary = "Listar los funcionarios con acceso al sistema")
    public ResponseEntity<List<UsuarioDto>> listar() {
        return ResponseEntity.ok(administracionService.listarFuncionarios());
    }

    @PostMapping
    @Operation(summary = "Dar de alta a un funcionario; recibirá un enlace para definir su contraseña")
    public ResponseEntity<UsuarioDto> crear(@Valid @RequestBody CrearFuncionarioDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(administracionService.crearFuncionario(dto));
    }

    @PutMapping("/{id}/roles")
    @Operation(summary = "Actualizar los roles institucionales de un funcionario")
    public ResponseEntity<UsuarioDto> actualizarRoles(@PathVariable UUID id, @Valid @RequestBody ActualizarRolesDto dto) {
        return ResponseEntity.ok(administracionService.actualizarRoles(id, dto.getRoles()));
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Habilitar o inhabilitar el acceso de un funcionario")
    public ResponseEntity<UsuarioDto> cambiarEstado(@PathVariable UUID id, @RequestParam boolean activo) {
        return ResponseEntity.ok(administracionService.cambiarEstado(id, activo));
    }

    @PostMapping("/{id}/activacion")
    @Operation(summary = "Reenviar el enlace de activación de acceso")
    public ResponseEntity<Void> reenviarActivacion(@PathVariable UUID id) {
        administracionService.reenviarActivacion(id);
        return ResponseEntity.noContent().build();
    }
}
