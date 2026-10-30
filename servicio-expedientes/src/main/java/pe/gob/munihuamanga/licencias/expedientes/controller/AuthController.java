package pe.gob.munihuamanga.licencias.expedientes.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.munihuamanga.licencias.common.dto.LoginRequestDto;
import pe.gob.munihuamanga.licencias.common.dto.LoginResponseDto;
import pe.gob.munihuamanga.licencias.common.dto.UsuarioDto;
import pe.gob.munihuamanga.licencias.expedientes.service.AuthService;

import java.time.Duration;

/**
 * Inicio, renovación y cierre de sesión. El token de acceso se entrega en el cuerpo de la respuesta;
 * el token de renovación, en una cookie HttpOnly restringida a las rutas de autenticación.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Inicio, renovación y cierre de sesión")
public class AuthController {

    static final String COOKIE_RENOVACION = "MH_SESION";

    private final AuthService authService;

    @Value("${munihuamanga.seguridad.cookie-segura:false}")
    private boolean cookieSegura;

    @Value("${munihuamanga.seguridad.duracion-sesion-horas:8}")
    private long duracionSesionHoras;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request, HttpServletRequest http) {
        AuthService.SesionIniciada sesion = authService.autenticar(request, http.getRemoteAddr(), http.getHeader(HttpHeaders.USER_AGENT));
        return conCookie(sesion);
    }

    @PostMapping("/renovar")
    @Operation(summary = "Renovar el token de acceso con la sesión vigente")
    public ResponseEntity<LoginResponseDto> renovar(@CookieValue(name = COOKIE_RENOVACION, required = false) String tokenRenovacion) {
        return conCookie(authService.renovar(tokenRenovacion));
    }

    @PostMapping("/cerrar-sesion")
    @Operation(summary = "Cerrar la sesión y revocar sus tokens")
    public ResponseEntity<Void> cerrarSesion(@CookieValue(name = COOKIE_RENOVACION, required = false) String tokenRenovacion) {
        authService.cerrarSesion(tokenRenovacion);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString()).build();
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Consultar el perfil de la persona autenticada y las cuentas que representa")
    public ResponseEntity<UsuarioDto> perfilActual(Authentication authentication) {
        return ResponseEntity.ok(authService.obtenerPerfilActual(authentication.getName()));
    }

    private ResponseEntity<LoginResponseDto> conCookie(AuthService.SesionIniciada sesion) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie(sesion.tokenRenovacion(), Duration.ofHours(duracionSesionHoras)).toString())
                .body(sesion.respuesta());
    }

    private ResponseCookie cookie(String valor, Duration duracion) {
        return ResponseCookie.from(COOKIE_RENOVACION, valor)
                .httpOnly(true)
                .secure(cookieSegura)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(duracion)
                .build();
    }
}
