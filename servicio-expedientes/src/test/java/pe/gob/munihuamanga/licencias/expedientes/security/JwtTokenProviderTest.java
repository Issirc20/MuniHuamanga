package pe.gob.munihuamanga.licencias.expedientes.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private final String secretKey = "ClaveSecretaParaPruebasUnitariasDeTokensJWTHuamanga2026!DebeTenerMasDe256Bits";
    private final long expirationMs = 3600000; // 1 hora

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(secretKey, expirationMs);
    }

    @Test
    @DisplayName("Debe generar token JWT válido y extraer username y rol correctamente")
    void testGenerarYValidarToken() {
        UUID userId = UUID.randomUUID();
        String username = "evaluador";
        String rol = "ROLE_EVALUADOR";
        String nombre = "Ing. Carlos Mendoza";

        String token = tokenProvider.generarToken(username, rol, nombre, userId);

        assertNotNull(token);
        assertTrue(tokenProvider.validarToken(token));
        assertEquals(username, tokenProvider.obtenerUsernameDelToken(token));
        assertEquals(rol, tokenProvider.obtenerRolDelToken(token));
    }

    @Test
    @DisplayName("Debe rechazar un token malformado o adulterado")
    void testTokenInvalido() {
        String tokenAdulterado = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiJ9.invalidsignature";
        assertFalse(tokenProvider.validarToken(tokenAdulterado));
    }

    @Test
    @DisplayName("Debe rechazar un token expirado")
    void testTokenExpirado() {
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(secretKey, -1000); // Ya expirado
        String tokenExpirado = shortLivedProvider.generarToken("cajero", "ROLE_CAJERO", "Rosa Flores", UUID.randomUUID());

        assertFalse(shortLivedProvider.validarToken(tokenExpirado));
    }
}
