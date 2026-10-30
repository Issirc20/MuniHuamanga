package pe.gob.munihuamanga.licencias.expedientes.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import pe.gob.munihuamanga.licencias.common.dto.LoginRequestDto;
import pe.gob.munihuamanga.licencias.common.dto.UsuarioDto;
import pe.gob.munihuamanga.licencias.common.enums.RolUsuario;
import pe.gob.munihuamanga.licencias.expedientes.model.Usuario;
import pe.gob.munihuamanga.licencias.expedientes.repository.UsuarioRepository;
import pe.gob.munihuamanga.licencias.expedientes.security.JwtTokenProvider;
import pe.gob.munihuamanga.licencias.expedientes.security.ProteccionIntentosAcceso;
import pe.gob.munihuamanga.licencias.expedientes.security.ServicioRepresentacion;
import pe.gob.munihuamanga.licencias.expedientes.security.sesion.GestorSesiones;
import pe.gob.munihuamanga.licencias.expedientes.security.sesion.SesionUsuario;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider tokenProvider;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ProteccionIntentosAcceso proteccionIntentos;
    @Mock private GestorSesiones gestorSesiones;
    @Mock private ServicioRepresentacion servicioRepresentacion;

    @InjectMocks
    private AuthService authService;

    private Usuario evaluador;

    @BeforeEach
    void setUp() {
        evaluador = Usuario.builder()
                .id(UUID.randomUUID())
                .username("evaluador")
                .password("$2a$10$hash")
                .nombreCompleto("Evaluador de Licencias")
                .email("evaluador@munihuamanga.gob.pe")
                .roles(new HashSet<>(Set.of(RolUsuario.ROLE_EVALUADOR)))
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("El inicio de sesión exitoso abre una sesión y emite token de acceso y de renovación")
    void autenticarExitoso() {
        SesionUsuario sesion = SesionUsuario.builder().id(UUID.randomUUID()).usuarioId(evaluador.getId()).build();
        when(proteccionIntentos.estaBloqueado("evaluador")).thenReturn(false);
        when(usuarioRepository.findByUsername("evaluador")).thenReturn(Optional.of(evaluador));
        when(gestorSesiones.abrir(eq(evaluador.getId()), anyString(), anyString()))
                .thenReturn(new GestorSesiones.SesionEmitida(sesion, "token-renovacion"));
        when(tokenProvider.generarTokenAcceso(any(), eq(sesion.getId()))).thenReturn("token.acceso");
        when(servicioRepresentacion.listar(evaluador.getId())).thenReturn(List.of());

        AuthService.SesionIniciada resultado = authService.autenticar(solicitud("Licencias.Huamanga26"), "127.0.0.1", "navegador");

        assertThat(resultado.respuesta().getToken()).isEqualTo("token.acceso");
        assertThat(resultado.respuesta().getRoles()).containsExactly("ROLE_EVALUADOR");
        assertThat(resultado.tokenRenovacion()).isEqualTo("token-renovacion");
        verify(proteccionIntentos).registrarExito("evaluador");
    }

    @Test
    @DisplayName("Las credenciales incorrectas registran el intento fallido y devuelven un mensaje neutro")
    void credencialesInvalidas() {
        when(proteccionIntentos.estaBloqueado("evaluador")).thenReturn(false);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("x"));

        assertThatThrownBy(() -> authService.autenticar(solicitud("incorrecta"), "127.0.0.1", "navegador"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage(AuthService.MENSAJE_CREDENCIALES_INVALIDAS);
        verify(proteccionIntentos).registrarFallo("evaluador");
    }

    @Test
    @DisplayName("Un usuario bloqueado no llega a validar su contraseña")
    void usuarioBloqueado() {
        when(proteccionIntentos.estaBloqueado("evaluador")).thenReturn(true);

        assertThatThrownBy(() -> authService.autenticar(solicitud("Licencias.Huamanga26"), "127.0.0.1", "navegador"))
                .isInstanceOf(BadCredentialsException.class);
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("La renovación con una persona inhabilitada revoca la sesión")
    void renovacionDePersonaInhabilitada() {
        evaluador.setActivo(false);
        SesionUsuario sesion = SesionUsuario.builder().id(UUID.randomUUID()).usuarioId(evaluador.getId()).build();
        when(gestorSesiones.renovar("token")).thenReturn(new GestorSesiones.SesionEmitida(sesion, "nuevo"));
        when(usuarioRepository.findById(evaluador.getId())).thenReturn(Optional.of(evaluador));

        assertThatThrownBy(() -> authService.renovar("token")).isInstanceOf(BadCredentialsException.class);
        verify(gestorSesiones).revocar(eq(sesion.getId()), anyString());
    }

    @Test
    @DisplayName("El perfil incluye los roles y las representaciones de la persona")
    void perfilActual() {
        when(usuarioRepository.findByUsername("evaluador")).thenReturn(Optional.of(evaluador));
        when(servicioRepresentacion.listar(evaluador.getId())).thenReturn(List.of());

        UsuarioDto perfil = authService.obtenerPerfilActual("evaluador");

        assertThat(perfil.getRoles()).containsExactly(RolUsuario.ROLE_EVALUADOR);
        assertThat(perfil.getRepresentaciones()).isEmpty();
    }

    @Test
    @DisplayName("El perfil de un usuario inexistente no se obtiene")
    void perfilDeUsuarioInexistente() {
        when(usuarioRepository.findByUsername("desconocido")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.obtenerPerfilActual("desconocido")).isInstanceOf(UsernameNotFoundException.class);
    }

    private static LoginRequestDto solicitud(String contrasena) {
        return LoginRequestDto.builder().username("evaluador").password(contrasena).build();
    }
}
