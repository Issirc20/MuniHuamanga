package pe.gob.munihuamanga.licencias.expedientes.cuentas.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.dto.CrearFuncionarioDto;
import pe.gob.munihuamanga.licencias.common.dto.UsuarioDto;
import pe.gob.munihuamanga.licencias.common.enums.RolUsuario;
import pe.gob.munihuamanga.licencias.common.exception.RecursoNoEncontradoException;
import pe.gob.munihuamanga.licencias.expedientes.model.Usuario;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.aplicacion.ProveedorActor;
import pe.gob.munihuamanga.licencias.expedientes.repository.UsuarioRepository;
import pe.gob.munihuamanga.licencias.expedientes.security.GeneradorTokensSeguros;
import pe.gob.munihuamanga.licencias.expedientes.security.sesion.GestorSesiones;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Administración de los accesos de funcionarios: alta con enlace de activación, asignación de roles
 * y habilitación o inhabilitación. La inhabilitación revoca de inmediato todas las sesiones.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdministracionUsuariosService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final ActivacionAccesoService activacionAccesoService;
    private final GestorSesiones gestorSesiones;
    private final ProveedorActor proveedorActor;
    private final Clock reloj;

    @Transactional(readOnly = true)
    public List<UsuarioDto> listarFuncionarios() {
        return usuarioRepository.findAll().stream()
                .filter(Usuario::esFuncionario)
                .sorted(Comparator.comparing(Usuario::getUsername))
                .map(AdministracionUsuariosService::aDto)
                .toList();
    }

    @Transactional
    public UsuarioDto crearFuncionario(CrearFuncionarioDto dto) {
        validarRolesInstitucionales(dto.getRoles());
        if (usuarioRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalStateException("El nombre de usuario ya se encuentra registrado.");
        }
        Usuario funcionario = usuarioRepository.save(Usuario.builder()
                .id(UUID.randomUUID())
                .username(dto.getUsername())
                .password(passwordEncoder.encode(GeneradorTokensSeguros.nuevoToken()))
                .nombreCompleto(dto.getNombreCompleto().trim())
                .email(dto.getEmail().trim().toLowerCase())
                .roles(new HashSet<>(dto.getRoles()))
                .activo(true)
                .fechaCreacion(LocalDateTime.now(reloj))
                .build());
        activacionAccesoService.solicitarActivacion(funcionario);
        log.info("[ADMINISTRACION] Alta del funcionario {} con roles {} registrada por {}.",
                funcionario.getUsername(), dto.getRoles(), proveedorActor.actual().nombreParaRegistro());
        return aDto(funcionario);
    }

    @Transactional
    public UsuarioDto actualizarRoles(UUID usuarioId, Set<RolUsuario> roles) {
        validarRolesInstitucionales(roles);
        Usuario funcionario = obtenerFuncionario(usuarioId);
        funcionario.setRoles(new HashSet<>(roles));
        usuarioRepository.save(funcionario);
        gestorSesiones.revocarTodas(usuarioId, "Modificación de roles");
        log.info("[ADMINISTRACION] Roles del funcionario {} actualizados a {} por {}.",
                funcionario.getUsername(), roles, proveedorActor.actual().nombreParaRegistro());
        return aDto(funcionario);
    }

    @Transactional
    public UsuarioDto cambiarEstado(UUID usuarioId, boolean activo) {
        Usuario funcionario = obtenerFuncionario(usuarioId);
        if (!activo && funcionario.getId().equals(proveedorActor.actual().usuarioId())) {
            throw new IllegalStateException("No puede inhabilitar su propio acceso.");
        }
        funcionario.setActivo(activo);
        usuarioRepository.save(funcionario);
        if (!activo) {
            gestorSesiones.revocarTodas(usuarioId, "Acceso inhabilitado");
        }
        return aDto(funcionario);
    }

    @Transactional
    public void reenviarActivacion(UUID usuarioId) {
        activacionAccesoService.solicitarActivacion(obtenerFuncionario(usuarioId));
    }

    private Usuario obtenerFuncionario(UUID usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .filter(Usuario::esFuncionario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Funcionario", usuarioId));
    }

    private static void validarRolesInstitucionales(Set<RolUsuario> roles) {
        if (roles == null || roles.isEmpty() || roles.contains(RolUsuario.ROLE_CIUDADANO)) {
            throw new IllegalArgumentException("Debe asignar uno o más roles institucionales; el rol de administrado no corresponde a funcionarios.");
        }
    }

    private static UsuarioDto aDto(Usuario usuario) {
        return UsuarioDto.builder()
                .id(usuario.getId())
                .username(usuario.getUsername())
                .nombreCompleto(usuario.getNombreCompleto())
                .email(usuario.getEmail())
                .roles(usuario.rolesInmutables())
                .activo(usuario.isActivo())
                .pendienteActivacion(usuario.isPendienteActivacion())
                .fechaCreacion(usuario.getFechaCreacion())
                .build();
    }
}
