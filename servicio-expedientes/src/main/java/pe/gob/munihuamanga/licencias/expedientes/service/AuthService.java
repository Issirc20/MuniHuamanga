package pe.gob.munihuamanga.licencias.expedientes.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.dto.LoginRequestDto;
import pe.gob.munihuamanga.licencias.common.dto.LoginResponseDto;
import pe.gob.munihuamanga.licencias.common.dto.UsuarioDto;
import pe.gob.munihuamanga.licencias.expedientes.model.Usuario;
import pe.gob.munihuamanga.licencias.expedientes.repository.UsuarioRepository;
import pe.gob.munihuamanga.licencias.expedientes.security.CustomUserDetails;
import pe.gob.munihuamanga.licencias.expedientes.security.JwtTokenProvider;
import pe.gob.munihuamanga.licencias.expedientes.security.ProteccionIntentosAcceso;
import pe.gob.munihuamanga.licencias.expedientes.security.ServicioRepresentacion;
import pe.gob.munihuamanga.licencias.expedientes.security.sesion.GestorSesiones;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Inicio, renovación y cierre de sesión. Los mensajes de error son neutros para no revelar
 * si un usuario existe, está bloqueado o pendiente de activación.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String MENSAJE_CREDENCIALES_INVALIDAS = "Credenciales no válidas o acceso temporalmente restringido.";

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UsuarioRepository usuarioRepository;
    private final ProteccionIntentosAcceso proteccionIntentos;
    private final GestorSesiones gestorSesiones;
    private final ServicioRepresentacion servicioRepresentacion;

    /** Resultado del inicio o renovación de sesión: la respuesta pública y el token de renovación, que viaja en cookie. */
    public record SesionIniciada(LoginResponseDto respuesta, String tokenRenovacion) {
    }

    public SesionIniciada autenticar(LoginRequestDto solicitud, String direccionIp, String agenteUsuario) {
        String username = solicitud.getUsername() == null ? "" : solicitud.getUsername().trim();
        if (proteccionIntentos.estaBloqueado(username)) {
            throw new BadCredentialsException(MENSAJE_CREDENCIALES_INVALIDAS);
        }
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, solicitud.getPassword()));
        } catch (AuthenticationException ex) {
            proteccionIntentos.registrarFallo(username);
            throw new BadCredentialsException(MENSAJE_CREDENCIALES_INVALIDAS);
        }
        proteccionIntentos.registrarExito(username);

        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException(MENSAJE_CREDENCIALES_INVALIDAS));
        GestorSesiones.SesionEmitida sesion = gestorSesiones.abrir(usuario.getId(), direccionIp, agenteUsuario);
        log.info("[AUTENTICACION] Sesión iniciada para el usuario {}.", usuario.getId());
        return new SesionIniciada(construirRespuesta(usuario, sesion.sesion().getId()), sesion.tokenRenovacion());
    }

    @Transactional
    public SesionIniciada renovar(String tokenRenovacion) {
        if (tokenRenovacion == null || tokenRenovacion.isBlank()) {
            throw new BadCredentialsException("No existe una sesión activa.");
        }
        GestorSesiones.SesionEmitida sesion = gestorSesiones.renovar(tokenRenovacion);
        Usuario usuario = usuarioRepository.findById(sesion.sesion().getUsuarioId())
                .filter(persona -> persona.puedeIniciarSesion(LocalDateTime.now()))
                .orElseThrow(() -> {
                    gestorSesiones.revocar(sesion.sesion().getId(), "Persona deshabilitada");
                    return new BadCredentialsException(MENSAJE_CREDENCIALES_INVALIDAS);
                });
        return new SesionIniciada(construirRespuesta(usuario, sesion.sesion().getId()), sesion.tokenRenovacion());
    }

    public void cerrarSesion(String tokenRenovacion) {
        if (tokenRenovacion != null && !tokenRenovacion.isBlank()) {
            gestorSesiones.revocarPorTokenRenovacion(tokenRenovacion, "Cierre de sesión");
        }
    }

    @Transactional(readOnly = true)
    public UsuarioDto obtenerPerfilActual(String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado."));
        return UsuarioDto.builder()
                .id(usuario.getId())
                .username(usuario.getUsername())
                .nombreCompleto(usuario.getNombreCompleto())
                .email(usuario.getEmail())
                .roles(usuario.rolesInmutables())
                .activo(usuario.isActivo())
                .pendienteActivacion(usuario.isPendienteActivacion())
                .fechaCreacion(usuario.getFechaCreacion())
                .representaciones(servicioRepresentacion.listar(usuario.getId()))
                .build();
    }

    private LoginResponseDto construirRespuesta(Usuario usuario, UUID sesionId) {
        String token = tokenProvider.generarTokenAcceso(new CustomUserDetails(usuario, sesionId), sesionId);
        return LoginResponseDto.builder()
                .token(token)
                .tipo("Bearer")
                .id(usuario.getId())
                .username(usuario.getUsername())
                .nombreCompleto(usuario.getNombreCompleto())
                .email(usuario.getEmail())
                .rol(usuario.rolPrincipal().name())
                .roles(usuario.rolesInmutables().stream().map(Enum::name).toList())
                .representaciones(servicioRepresentacion.listar(usuario.getId()))
                .expiracionMs(tokenProvider.getExpirationMs())
                .build();
    }
}
