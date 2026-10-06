package pe.gob.munihuamanga.licencias.expedientes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pe.gob.munihuamanga.licencias.common.dto.LoginRequestDto;
import pe.gob.munihuamanga.licencias.common.dto.LoginResponseDto;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Debe autenticar al evaluador exitosamente y devolver token JWT")
    void testLoginExitoso() throws Exception {
        LoginRequestDto request = LoginRequestDto.builder()
                .username("evaluador")
                .password("eval123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.username").value("evaluador"))
                .andExpect(jsonPath("$.rol").value("ROLE_EVALUADOR"));
    }

    @Test
    @DisplayName("Debe rechazar credenciales incorrectas con 401/Unauthorized o BadCredentials")
    void testLoginInvalido() throws Exception {
        LoginRequestDto request = LoginRequestDto.builder()
                .username("evaluador")
                .password("incorrecta")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Debe denegar acceso (HTTP 401) a operaciones administrativas protegidas si no se envía token")
    void testOperacionProtegidaSinToken_Retorna401() throws Exception {
        mockMvc.perform(post("/api/expedientes/" + UUID.randomUUID() + "/aprobar")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Debe permitir acceso a la bandeja /api/expedientes con token Bearer válido")
    void testBandejaConToken_Retorna200() throws Exception {
        // 1. Iniciar sesión para obtener token
        LoginRequestDto request = LoginRequestDto.builder()
                .username("admin")
                .password("admin123")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponseDto loginResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                LoginResponseDto.class
        );

        // 2. Consultar bandeja con el token
        mockMvc.perform(get("/api/expedientes")
                        .header("Authorization", "Bearer " + loginResponse.getToken())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Debe permitir acceso al endpoint de consulta por trámite sin token (público para el ciudadano)")
    void testSeguimientoCiudadanoPublico_Permitido() throws Exception {
        // Consultar trámite inexistente responde 404 pero NO 401
        mockMvc.perform(get("/api/expedientes/tramite/EXP-2026-99999"))
                .andExpect(status().isNotFound());
    }
}
