package pe.gob.munihuamanga.licencias.expedientes.security;

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
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class SeguridadEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExpedienteRepository expedienteRepository;

    private String obtenerToken(String username, String password) throws Exception {
        LoginRequestDto request = LoginRequestDto.builder()
                .username(username)
                .password(password)
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
        return loginResponse.getToken();
    }

    @Test
    @DisplayName("H01/H05: GET /api/expedientes sin autenticación debe retornar 401 Unauthorized")
    void testListarExpedientesAnonimo_Retorna401() throws Exception {
        mockMvc.perform(get("/api/expedientes")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("H01: CAJERO no puede acceder a endpoints de emisión de licencia de EVALUADOR/ADMIN (403 Forbidden)")
    void testRolCajeroNoPuedeEmitirLicencia_Retorna403() throws Exception {
        String tokenCajero = obtenerToken("cajero", "caja123");

        Expediente exp = expedienteRepository.findAll().stream().findFirst().orElseThrow();

        mockMvc.perform(get("/api/formularios/licencia/" + exp.getId())
                        .header("Authorization", "Bearer " + tokenCajero)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("H07: Consulta pública por trámite retorna DTO de seguimiento con datos personales ofuscados (Ley N° 29733)")
    void testSeguimientoCiudadanoOfuscado_Retorna200ConDatosProtegidos() throws Exception {
        // EXP-2026-00001 tiene titular 'María Quispe Huamán' y documento '42567891'
        mockMvc.perform(get("/api/expedientes/tramite/EXP-2026-00001")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroTramite").value("EXP-2026-00001"))
                .andExpect(jsonPath("$.titularOfuscado").value(containsString("***")))
                .andExpect(jsonPath("$.documentoIdentidadOfuscado").value("42***91"))
                .andExpect(jsonPath("$.correoElectronico").doesNotExist())
                .andExpect(jsonPath("$.telefono").doesNotExist());
    }

    @Test
    @DisplayName("H08: Descarga de Licencia PDF para expediente NO aprobado debe ser rechazada con 400 Bad Request")
    void testDescargarLicenciaNoAprobado_Retorna400BadRequest() throws Exception {
        // EXP-2026-00001 está en FORMATOS_GENERADOS (no APROBADO)
        Expediente exp = expedienteRepository.findByNumeroTramite("EXP-2026-00001").orElseThrow();

        mockMvc.perform(get("/api/expedientes/" + exp.getId() + "/documentos/licencia"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Petición Inválida o Regla de Negocio Incumplida"))
                .andExpect(jsonPath("$.message").value(containsString("APROBADO")));
    }

    @Test
    @DisplayName("H08: Descarga de Licencia PDF para expediente APROBADO se emite correctamente (200 OK)")
    void testDescargarLicenciaAprobado_Retorna200Pdf() throws Exception {
        // EXP-2026-00002 está APROBADO
        Expediente exp = expedienteRepository.findByNumeroTramite("EXP-2026-00002").orElseThrow();

        mockMvc.perform(get("/api/expedientes/" + exp.getId() + "/documentos/licencia"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE));
    }

    @Test
    @DisplayName("H05: Actuator Health es público (200 OK) pero endpoints administrativos requieren ADMIN (401/403)")
    void testActuatorSeguridad() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/beans"))
                .andExpect(status().isUnauthorized());
    }
}
