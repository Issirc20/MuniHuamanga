package pe.gob.munihuamanga.licencias.expedientes.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.gob.munihuamanga.licencias.expedientes.security.sesion.GestorSesiones;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Autenticación por token de acceso. Además de la firma y la expiración del token, verifica en cada
 * petición que la sesión siga vigente y que la persona esté habilitada y no bloqueada; así, la
 * desactivación de un usuario o el cierre de sesión surten efecto inmediato.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final GestorSesiones gestorSesiones;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String jwt = obtenerJwtDeSolicitud(request);
        if (StringUtils.hasText(jwt) && tokenProvider.validarToken(jwt)) {
            autenticar(jwt, request);
        }
        filterChain.doFilter(request, response);
    }

    private void autenticar(String jwt, HttpServletRequest request) {
        Optional<UUID> sesionId = tokenProvider.obtenerSesionId(jwt);
        if (sesionId.isEmpty() || !gestorSesiones.estaVigente(sesionId.get())) {
            log.debug("[SEGURIDAD] Token de acceso asociado a una sesión inexistente o cerrada.");
            return;
        }
        try {
            CustomUserDetails persona = userDetailsService.cargarParaSesion(tokenProvider.obtenerUsernameDelToken(jwt), sesionId.get());
            if (!persona.isEnabled() || !persona.isAccountNonLocked()) {
                log.debug("[SEGURIDAD] Token de acceso de una persona deshabilitada o bloqueada.");
                return;
            }
            UsernamePasswordAuthenticationToken autenticacion =
                    new UsernamePasswordAuthenticationToken(persona, null, persona.getAuthorities());
            autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(autenticacion);
        } catch (UsernameNotFoundException ex) {
            log.debug("[SEGURIDAD] Token de acceso de una persona inexistente.");
        }
    }

    private static String obtenerJwtDeSolicitud(HttpServletRequest request) {
        String cabecera = request.getHeader("Authorization");
        if (StringUtils.hasText(cabecera) && cabecera.startsWith(PREFIJO_BEARER)) {
            return cabecera.substring(PREFIJO_BEARER.length());
        }
        return null;
    }
}
