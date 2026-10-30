package pe.gob.munihuamanga.licencias.expedientes.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pe.gob.munihuamanga.licencias.common.dto.AdmitirSolicitudDto;
import pe.gob.munihuamanga.licencias.common.dto.LoginRequestDto;
import pe.gob.munihuamanga.licencias.common.dto.SubsanarObservacionesDto;
import pe.gob.munihuamanga.licencias.common.dto.ValidarItseDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.expedientes.config.UsuarioDataInitializer;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.repository.HistorialRepository;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pe.gob.munihuamanga.licencias.expedientes.soporte.DatosDePrueba.*;

/**
 * Autorización a nivel de objeto y por rol institucional sobre expedientes (OWASP API1 y API5).
 * Los tokens se obtienen con el flujo real de inicio de sesión.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class ExpedienteSecurityAuthorizationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ExpedienteRepository expedienteRepository;
    @Autowired private HistorialRepository historialRepository;

    private Expediente expediente;

    @BeforeEach
    void setUp() {
        expediente = expedienteRepository.save(Expediente.builder()
                .id(UUID.randomUUID())
                .numeroTramite("EXP-SEG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .solicitanteId(UsuarioDataInitializer.CUENTA_MARIA_QUISPE)
                .cuentaId(UsuarioDataInitializer.CUENTA_MARIA_QUISPE)
                .nombreTitular("María Quispe Huamán")
                .documentoIdentidad(CIUDADANA_TITULAR)
                .nombreComercial("Boutique Artesanal Huamanga")
                .giroNegocio("Venta de artesanías")
                .direccionEstablecimiento("Jr. 9 de Diciembre 142")
                .motivoObservacion("Observación interna que no debe publicarse")
                .estado(EstadoExpediente.PRESENTADO)
                .fechaCreacion(LocalDateTime.now())
                .build());
    }

    @AfterEach
    void limpiar() {
        historialRepository.deleteAll(historialRepository.findByExpedienteIdOrderByFechaAsc(expediente.getId()));
        expedienteRepository.deleteById(expediente.getId());
    }

    @Test
    @DisplayName("Un usuario anónimo no accede al expediente, a su detalle ni a sus documentos")
    void anonimoRechazado() throws Exception {
        mockMvc.perform(get("/api/expedientes/" + expediente.getId())).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/expedientes/detalle/" + expediente.getNumeroTramite())).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/expedientes/" + expediente.getId() + "/documentos/anexo1")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("La titular accede a su expediente por la representación registrada")
    void titularAccede() throws Exception {
        String token = obtenerToken(mockMvc, objectMapper, CIUDADANA_TITULAR, CLAVE_CIUDADANO);
        mockMvc.perform(get("/api/expedientes/" + expediente.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroTramite").value(expediente.getNumeroTramite()));
        mockMvc.perform(get("/api/expedientes/mis-expedientes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Un tercero sin representación no accede al expediente ajeno")
    void terceroRechazado() throws Exception {
        String token = obtenerToken(mockMvc, objectMapper, CIUDADANO_TERCERO, CLAVE_CIUDADANO);
        mockMvc.perform(get("/api/expedientes/" + expediente.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/expedientes/detalle/" + expediente.getNumeroTramite()).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/expedientes/" + expediente.getId() + "/documentos/anexo1").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Los funcionarios con rol de gestión consultan la bandeja; el administrado y el inspector no")
    void listadoSegunRol() throws Exception {
        for (String[] credencial : new String[][]{{MESA_DE_PARTES, CLAVE_MESA_DE_PARTES}, {EVALUADOR, CLAVE_EVALUADOR}, {AUDITOR, CLAVE_AUDITOR}}) {
            String token = obtenerToken(mockMvc, objectMapper, credencial[0], credencial[1]);
            mockMvc.perform(get("/api/expedientes").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        }
        String tokenCiudadano = obtenerToken(mockMvc, objectMapper, CIUDADANA_TITULAR, CLAVE_CIUDADANO);
        mockMvc.perform(get("/api/expedientes").header("Authorization", "Bearer " + tokenCiudadano)).andExpect(status().isForbidden());
        String tokenInspector = obtenerToken(mockMvc, objectMapper, INSPECTOR, CLAVE_INSPECTOR);
        mockMvc.perform(get("/api/expedientes/" + expediente.getId()).header("Authorization", "Bearer " + tokenInspector))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("El administrador del sistema no ejerce facultades sobre expedientes")
    void administradorSinFacultadesDeFondo() throws Exception {
        String token = obtenerToken(mockMvc, objectMapper, ADMIN, CLAVE_ADMIN);
        mockMvc.perform(post("/api/expedientes/" + expediente.getId() + "/admision")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AdmitirSolicitudDto.builder().sustento("Admisión de la solicitud presentada").build())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/expedientes").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Mesa de partes admite la solicitud y el expediente pasa a la etapa ITSE")
    void mesaDePartesAdmite() throws Exception {
        String token = obtenerToken(mockMvc, objectMapper, MESA_DE_PARTES, CLAVE_MESA_DE_PARTES);
        mockMvc.perform(post("/api/expedientes/" + expediente.getId() + "/admision")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AdmitirSolicitudDto.builder().sustento("Solicitud completa y conforme").build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoNuevo").value("ITSE_EN_TRAMITE"));

        assertThat(expedienteRepository.findById(expediente.getId()).orElseThrow().getEstado()).isEqualTo(EstadoExpediente.ITSE_EN_TRAMITE);
        assertThat(historialRepository.findByExpedienteIdOrderByFechaAsc(expediente.getId()))
                .anySatisfy(registro -> assertThat(registro.getEvento()).isEqualTo("SOLICITUD_ADMITIDA"));
    }

    @Test
    @DisplayName("El cajero no puede validar el ITSE ni el evaluador aprobar resoluciones")
    void segregacionDeFunciones() throws Exception {
        String tokenCajero = obtenerToken(mockMvc, objectMapper, CAJERO, CLAVE_CAJERO);
        mockMvc.perform(post("/api/expedientes/" + expediente.getId() + "/itse/validacion")
                        .header("Authorization", "Bearer " + tokenCajero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ValidarItseDto.builder().nivelOficial(NivelRiesgo.BAJO).build())))
                .andExpect(status().isForbidden());

        String tokenEvaluador = obtenerToken(mockMvc, objectMapper, EVALUADOR, CLAVE_EVALUADOR);
        mockMvc.perform(post("/api/resoluciones/" + UUID.randomUUID() + "/aprobar")
                        .header("Authorization", "Bearer " + tokenEvaluador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Solo se subsana un expediente observado")
    void subsanacionSoloEnObservado() throws Exception {
        String token = obtenerToken(mockMvc, objectMapper, CIUDADANA_TITULAR, CLAVE_CIUDADANO);
        mockMvc.perform(post("/api/expedientes/" + expediente.getId() + "/observaciones/subsanar")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SubsanarObservacionesDto.builder().detalleSubsanacion("Documento corregido adjunto").build())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Las rutas de transición de la versión anterior ya no existen")
    void rutasV1Retiradas() throws Exception {
        String token = obtenerToken(mockMvc, objectMapper, EVALUADOR, CLAVE_EVALUADOR);
        for (String ruta : new String[]{"/aprobar", "/rechazar", "/observar", "/pago", "/clasificar-riesgo", "/subsanar"}) {
            int codigo = mockMvc.perform(post("/api/expedientes/" + expediente.getId() + ruta)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andReturn().getResponse().getStatus();
            assertThat(codigo).as("Ruta %s", ruta).isIn(404, 405);
        }
    }

    @Test
    @DisplayName("El seguimiento público exige los cuatro últimos dígitos del documento y no expone observaciones")
    void seguimientoPublico() throws Exception {
        mockMvc.perform(get("/api/expedientes/tramite/" + expediente.getNumeroTramite()).param("documento", "7891"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PRESENTADO"))
                .andExpect(jsonPath("$.motivoObservacion").doesNotExist());
        mockMvc.perform(get("/api/expedientes/tramite/" + expediente.getNumeroTramite()).param("documento", "0000"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Al cerrar la sesión, el token de acceso emitido deja de ser válido")
    void cierreDeSesionRevocaToken() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(LoginRequestDto.builder().username(EVALUADOR).password(CLAVE_EVALUADOR).build())))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(login.getResponse().getContentAsString()).get("token").asText();
        Cookie sesion = login.getResponse().getCookie("MH_SESION");
        assertThat(sesion).isNotNull();
        assertThat(sesion.isHttpOnly()).isTrue();

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/cerrar-sesion").cookie(sesion)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }
}
