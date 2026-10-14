package pe.gob.munihuamanga.licencias.expedientes.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import pe.gob.munihuamanga.licencias.common.enums.RolUsuario;
import pe.gob.munihuamanga.licencias.expedientes.model.Usuario;
import pe.gob.munihuamanga.licencias.expedientes.repository.UsuarioRepository;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@TestPropertySource(properties = {
        "JWT_SECRET=production-secret-test-key-32-bytes-minimum-length-required!",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "SPRING_DATASOURCE_URL=jdbc:h2:mem:testdb_prod;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "SPRING_DATASOURCE_USERNAME=sa",
        "SPRING_DATASOURCE_PASSWORD=sa",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SecurityConfigProdTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        if (usuarioRepository.findByUsername("admin").isEmpty()) {
            usuarioRepository.save(Usuario.builder()
                    .id(UUID.randomUUID())
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .nombreCompleto("Administrador Prod Test")
                    .email("admin@test.pe")
                    .rol(RolUsuario.ROLE_ADMIN)
                    .activo(true)
                    .build());
        }
    }

    @Test
    @DisplayName("I02.1: En perfil prod, /h2-console anónimo es rechazado sin autenticación (401)")
    void h2ConsoleAnonimoRechazadoEnProduccion() throws Exception {
        mockMvc.perform(get("/h2-console"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/h2-console/index.jsp"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("I02.1: En perfil prod, /h2-console está denegado absolutamente incluso con token de administrador (403 Forbidden)")
    void h2ConsoleDenegadoConAutenticacionEnProduccion() throws Exception {
        String tokenAdmin = jwtTokenProvider.generarToken("admin", "ROLE_ADMIN", "Administrador General", UUID.randomUUID());

        mockMvc.perform(get("/h2-console")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/h2-console/index.jsp")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("I02.1: En perfil prod, las cabeceras deben forzar X-Frame-Options: DENY")
    void frameOptionsDenyEnProduccion() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }
}
