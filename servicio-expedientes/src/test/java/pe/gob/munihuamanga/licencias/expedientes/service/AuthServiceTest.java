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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import pe.gob.munihuamanga.licencias.common.dto.LoginRequestDto;
import pe.gob.munihuamanga.licencias.common.dto.LoginResponseDto;
import pe.gob.munihuamanga.licencias.common.dto.UsuarioDto;
import pe.gob.munihuamanga.licencias.common.enums.RolUsuario;
import pe.gob.munihuamanga.licencias.expedientes.model.Usuario;
import pe.gob.munihuamanga.licencias.expedientes.repository.UsuarioRepository;
import pe.gob.munihuamanga.licencias.expedientes.security.JwtTokenProvider;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private AuthService authService;

    private Usuario usuarioPrueba;

    @BeforeEach
    void setUp() {
        usuarioPrueba = Usuario.builder()
                .id(UUID.randomUUID())
                .username("evaluador")
                .password("$2a$10$encryptedPassword")
                .nombreCompleto("Ing. Carlos Mendoza")
                .email("cmendoza@munihuamanga.gob.pe")
                .rol(RolUsuario.ROLE_EVALUADOR)
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Debe autenticar correctamente con credenciales válidas y devolver token")
    void testAutenticar_Exitoso() {
        LoginRequestDto request = LoginRequestDto.builder()
                .username("evaluador")
                .password("eval123")
                .build();

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "evaluador",
                "eval123",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_EVALUADOR"))
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(usuarioRepository.findByUsername("evaluador")).thenReturn(Optional.of(usuarioPrueba));
        when(tokenProvider.generarToken(auth, usuarioPrueba.getId(), usuarioPrueba.getNombreCompleto()))
                .thenReturn("mocked.jwt.token");
        when(tokenProvider.getExpirationMs()).thenReturn(86400000L);

        LoginResponseDto response = authService.autenticar(request);

        assertNotNull(response);
        assertEquals("mocked.jwt.token", response.getToken());
        assertEquals("Bearer", response.getTipo());
        assertEquals("evaluador", response.getUsername());
        assertEquals("ROLE_EVALUADOR", response.getRol());
        assertEquals("Ing. Carlos Mendoza", response.getNombreCompleto());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción si las credenciales son incorrectas")
    void testAutenticar_CredencialesInvalidas() {
        LoginRequestDto request = LoginRequestDto.builder()
                .username("evaluador")
                .password("claveIncorrecta")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Credenciales incorrectas"));

        assertThrows(BadCredentialsException.class, () -> authService.autenticar(request));
    }

    @Test
    @DisplayName("Debe obtener el perfil actual del usuario autenticado")
    void testObtenerPerfilActual_Exitoso() {
        when(usuarioRepository.findByUsername("evaluador")).thenReturn(Optional.of(usuarioPrueba));

        UsuarioDto perfil = authService.obtenerPerfilActual("evaluador");

        assertNotNull(perfil);
        assertEquals("evaluador", perfil.getUsername());
        assertEquals(RolUsuario.ROLE_EVALUADOR, perfil.getRol());
        assertEquals("Ing. Carlos Mendoza", perfil.getNombreCompleto());
    }

    @Test
    @DisplayName("Debe fallar al obtener perfil si el usuario no existe")
    void testObtenerPerfilActual_UsuarioNoExiste() {
        when(usuarioRepository.findByUsername("desconocido")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> authService.obtenerPerfilActual("desconocido"));
    }
}
