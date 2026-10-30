package pe.gob.munihuamanga.licencias.expedientes.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.enums.RolUsuario;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.aplicacion.ProveedorActor;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio.Actor;
import pe.gob.munihuamanga.licencias.expedientes.repository.UsuarioRepository;

import java.util.EnumSet;
import java.util.Set;

/**
 * Resuelve, a partir del contexto de Spring Security, la persona que ejecuta la operación en curso.
 * Es la única fuente de identidad para la trazabilidad y las decisiones de autorización.
 */
@Component
@RequiredArgsConstructor
public class ProveedorContextoSeguridad implements ProveedorActor {

    private final UsuarioRepository usuarioRepository;

    @Override
    public Actor actual() {
        return desde(SecurityContextHolder.getContext().getAuthentication());
    }

    public Actor desde(Authentication authentication) {
        if (!estaAutenticado(authentication)) {
            return Actor.sistema();
        }
        if (authentication.getPrincipal() instanceof CustomUserDetails detalle) {
            return new Actor(detalle.getId(), detalle.getUsername(), detalle.getNombreCompleto(), detalle.getRoles());
        }
        // Otros mecanismos de autenticación: la identidad se resuelve por el nombre de usuario registrado.
        Set<RolUsuario> roles = rolesDe(authentication);
        return usuarioRepository.findByUsername(authentication.getName())
                .map(usuario -> new Actor(usuario.getId(), usuario.getUsername(), usuario.getNombreCompleto(), roles))
                .orElseGet(() -> new Actor(null, authentication.getName(), authentication.getName(), roles));
    }

    public static boolean estaAutenticado(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private static Set<RolUsuario> rolesDe(Authentication authentication) {
        Set<RolUsuario> roles = EnumSet.noneOf(RolUsuario.class);
        for (GrantedAuthority autoridad : authentication.getAuthorities()) {
            try {
                roles.add(RolUsuario.valueOf(autoridad.getAuthority()));
            } catch (IllegalArgumentException ignorada) {
                // Autoridades ajenas al catálogo institucional no otorgan roles.
            }
        }
        return roles;
    }
}
