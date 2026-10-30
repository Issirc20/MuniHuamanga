package pe.gob.munihuamanga.licencias.expedientes.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.gob.munihuamanga.licencias.common.enums.RolUsuario;
import pe.gob.munihuamanga.licencias.expedientes.model.Usuario;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String SECRETO = "ClaveSecretaParaPruebasUnitariasDeTokensJWTHuamanga2026!DebeTenerMasDe256Bits";

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(SECRETO, 3_600_000);
    }

    @Test
    @DisplayName("El token de acceso contiene usuario, sesión y roles")
    void generarYValidarToken() {
        UUID sesion = UUID.randomUUID();
        String token = tokenProvider.generarTokenAcceso(persona(Set.of(RolUsuario.ROLE_EVALUADOR, RolUsuario.ROLE_SUPERVISOR_LICENCIAS)), sesion);

        assertThat(tokenProvider.validarToken(token)).isTrue();
        assertThat(tokenProvider.obtenerUsernameDelToken(token)).isEqualTo("evaluador");
        assertThat(tokenProvider.obtenerSesionId(token)).contains(sesion);
        assertThat(tokenProvider.obtenerRolesDelToken(token)).containsExactlyInAnyOrder("ROLE_EVALUADOR", "ROLE_SUPERVISOR_LICENCIAS");
    }

    @Test
    @DisplayName("No se emite token para una persona sin roles (no se asigna un rol por defecto)")
    void sinRolesNoSeEmiteToken() {
        assertThatThrownBy(() -> tokenProvider.generarTokenAcceso(persona(Set.of()), UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Se rechaza un token adulterado")
    void tokenInvalido() {
        assertThat(tokenProvider.validarToken("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiJ9.firmainvalida")).isFalse();
    }

    @Test
    @DisplayName("Se rechaza un token expirado")
    void tokenExpirado() {
        JwtTokenProvider expirado = new JwtTokenProvider(SECRETO, -1000);
        String token = expirado.generarTokenAcceso(persona(Set.of(RolUsuario.ROLE_CAJERO)), UUID.randomUUID());
        assertThat(expirado.validarToken(token)).isFalse();
    }

    @Test
    @DisplayName("En producción se rechaza la clave de firma de desarrollo")
    void produccionRechazaClaveDeDesarrollo() {
        assertThatThrownBy(() -> new JwtTokenProvider(JwtTokenProvider.DEFAULT_DEV_SECRET, 1000, "prod,docker"))
                .isInstanceOf(IllegalStateException.class);
    }

    private static CustomUserDetails persona(Set<RolUsuario> roles) {
        return new CustomUserDetails(Usuario.builder()
                .id(UUID.randomUUID())
                .username("evaluador")
                .password("x")
                .nombreCompleto("Evaluador de Licencias")
                .email("evaluador@munihuamanga.gob.pe")
                .roles(new HashSet<>(roles))
                .build());
    }
}
