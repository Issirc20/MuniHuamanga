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
import pe.gob.munihuamanga.licencias.common.dto.ActualizarTarifaDto;
import pe.gob.munihuamanga.licencias.common.dto.LoginRequestDto;
import pe.gob.munihuamanga.licencias.common.dto.LoginResponseDto;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.expedientes.model.TarifaTupa;
import pe.gob.munihuamanga.licencias.expedientes.repository.TarifaTupaRepository;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class TarifaTupaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TarifaTupaRepository tarifaRepository;

    @Test
    @DisplayName("Debe listar tarifas TUPA públicamente sin requerir autenticación previa")
    void testListarTarifasPublico_Retorna200() throws Exception {
        mockMvc.perform(get("/api/tupa/tarifas")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    @DisplayName("Debe permitir al ADMIN actualizar montos del TUPA y reflejarse inmediatamente")
    void testActualizarTarifa_ComoAdmin_Exitoso() throws Exception {
        // 1. Obtener token de ADMIN
        String tokenAdmin = loginYObtenerToken("admin", "admin123");

        // 2. Obtener ID de la tarifa de Riesgo Bajo
        TarifaTupa tarifaBajo = tarifaRepository.findByNivelRiesgo(NivelRiesgo.BAJO).orElseThrow();

        ActualizarTarifaDto updateDto = ActualizarTarifaDto.builder()
                .montoTotal(new BigDecimal("160.00"))
                .derechoTramite(new BigDecimal("45.00"))
                .costoItse(new BigDecimal("115.00"))
                .concepto("Tarifa Actualizada 2026")
                .baseLegal("Decreto de Alcaldía N° 005-2026-MPH")
                .activo(true)
                .build();

        // 3. Ejecutar PUT con token de Admin
        mockMvc.perform(put("/api/tupa/tarifas/" + tarifaBajo.getId())
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montoTotal").value(160.00))
                .andExpect(jsonPath("$.costoItse").value(115.00))
                .andExpect(jsonPath("$.usuarioModificacion").value("admin"));
    }

    @Test
    @DisplayName("Debe rechazar (HTTP 403 Forbidden) cuando un EVALUADOR intenta modificar el tarifario")
    void testActualizarTarifa_ComoEvaluador_Retorna403() throws Exception {
        String tokenEvaluador = loginYObtenerToken("evaluador", "eval123");
        TarifaTupa tarifaMedio = tarifaRepository.findByNivelRiesgo(NivelRiesgo.MEDIO).orElseThrow();

        ActualizarTarifaDto updateDto = ActualizarTarifaDto.builder()
                .montoTotal(new BigDecimal("250.00"))
                .build();

        mockMvc.perform(put("/api/tupa/tarifas/" + tarifaMedio.getId())
                        .header("Authorization", "Bearer " + tokenEvaluador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Debe denegar acceso (HTTP 401 Unauthorized) si se intenta modificar sin token")
    void testActualizarTarifa_SinToken_Retorna401() throws Exception {
        TarifaTupa tarifaAlto = tarifaRepository.findByNivelRiesgo(NivelRiesgo.ALTO).orElseThrow();

        ActualizarTarifaDto updateDto = ActualizarTarifaDto.builder()
                .montoTotal(new BigDecimal("400.00"))
                .build();

        mockMvc.perform(put("/api/tupa/tarifas/" + tarifaAlto.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isUnauthorized());
    }

    private String loginYObtenerToken(String username, String password) throws Exception {
        LoginRequestDto req = LoginRequestDto.builder()
                .username(username)
                .password(password)
                .build();

        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponseDto resp = objectMapper.readValue(res.getResponse().getContentAsString(), LoginResponseDto.class);
        return resp.getToken();
    }
}
