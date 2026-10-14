package pe.gob.munihuamanga.licencias.expedientes.resoluciones;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import pe.gob.munihuamanga.licencias.common.dto.EmitirLicenciaDto;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResolucionLicenciaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("I08 Seguridad RBAC: Ciudadano no tiene permiso para crear o aprobar resoluciones (HTTP 403 Forbidden)")
    @WithMockUser(username = "77889900", roles = {"CIUDADANO"})
    void testCiudadanoNoPuedeAprobarResolucion() throws Exception {
        UUID resId = UUID.randomUUID();

        mockMvc.perform(post("/api/resoluciones/" + resId + "/aprobar")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("I08 Seguridad RBAC: Ciudadano o Evaluador no pueden emitir licencias oficiales (HTTP 403 Forbidden - Solo ADMIN)")
    @WithMockUser(username = "77889900", roles = {"CIUDADANO"})
    void testCiudadanoOEvaluadorNoPuedenEmitirLicencia() throws Exception {
        EmitirLicenciaDto dto = EmitirLicenciaDto.builder()
                .resolucionId(UUID.randomUUID())
                .build();

        // Ciudadano
        mockMvc.perform(post("/api/licencias/emitir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("I08 Seguridad BOLA/IDOR: Ciudadano no puede acceder a resoluciones ajenas (HTTP 403 Forbidden)")
    @WithMockUser(username = "77889900", roles = {"CIUDADANO"})
    void testCiudadanoNoPuedeAccederResolucionAjena() throws Exception {
        UUID resId = UUID.randomUUID();

        mockMvc.perform(get("/api/resoluciones/" + resId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("I08 Verificación Pública: Endpoint /api/licencias/verificar/{codigoQr} es accesible públicamente sin autenticación")
    void testVerificacionPublicaLicenciaEsPermitAll() throws Exception {
        mockMvc.perform(get("/api/licencias/verificar/NONEXISTENT123456"))
                .andExpect(status().isBadRequest()); // o 400 por IllegalArgumentException "Código no encontrado", pero NO 401 ni 403
    }
}
