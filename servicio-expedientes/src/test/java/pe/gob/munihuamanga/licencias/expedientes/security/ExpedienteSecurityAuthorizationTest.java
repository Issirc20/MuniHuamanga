package pe.gob.munihuamanga.licencias.expedientes.security;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import pe.gob.munihuamanga.licencias.common.dto.LoginRequestDto;
import pe.gob.munihuamanga.licencias.common.dto.LoginResponseDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Suite de Pruebas de Seguridad y Autorización a Nivel de Objeto (BOLA / IDOR / RBAC)
 * Incremento I02 — MuniHuamanga V2
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class ExpedienteSecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExpedienteRepository expedienteRepository;

    private String tokenAdmin;
    private String tokenEvaluador;
    private String tokenCajero;
    private String tokenInspector;
    private String tokenCiudadanoPropietario; // 42567891 (titular de EXP-2026-00001)
    private String tokenCiudadanoTercero;     // 78999999 (tercero sin relación)

    private Expediente expedienteTitular;      // EXP-2026-00001 (documento: 42567891)
    private Expediente expedienteAprobado;     // EXP-2026-00002 (con licencia emitida)

    @BeforeEach
    void setUp() throws Exception {
        tokenAdmin = obtenerToken("admin", "admin123");
        tokenEvaluador = obtenerToken("evaluador", "eval123");
        tokenCajero = obtenerToken("cajero", "caja123");
        tokenInspector = obtenerToken("inspector", "insp123");
        tokenCiudadanoPropietario = obtenerToken("42567891", "ciud123");
        tokenCiudadanoTercero = obtenerToken("78999999", "ciud123");

        expedienteTitular = expedienteRepository.findByNumeroTramite("EXP-2026-00001").orElseThrow();
        expedienteAprobado = expedienteRepository.findByNumeroTramite("EXP-2026-00002").orElseThrow();
    }

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

    // ──────────────────────────────────────────────────────────────────────────
    // REQUISITO 1: Usuario anónimo no puede consultar expedientes ni documentos
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("REQ-1: Usuario anónimo es rechazado con 401 al intentar consultar expediente por ID")
    void anonimoNoPuedeConsultarExpedientePorId() throws Exception {
        mockMvc.perform(get("/api/expedientes/" + expedienteTitular.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("REQ-1: Usuario anónimo es rechazado con 401 al intentar consultar expediente por Detalle")
    void anonimoNoPuedeConsultarDetalleExpediente() throws Exception {
        mockMvc.perform(get("/api/expedientes/detalle/" + expedienteTitular.getNumeroTramite())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("REQ-1: Usuario anónimo es rechazado con 401 al intentar descargar documentos internos")
    void anonimoNoPuedeDescargarDocumentos() throws Exception {
        mockMvc.perform(get("/api/expedientes/" + expedienteTitular.getId() + "/documentos/anexo1"))
                .andExpect(status().isUnauthorized());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // REQUISITO 2: Ciudadano no puede consultar expedientes de otro ciudadano (IDOR)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("REQ-2: Ciudadano ajeno (78999999) recibe 403 Forbidden al intentar ver expediente de 42567891 por ID")
    void ciudadanoNoPuedeConsultarExpedienteAjenoPorId() throws Exception {
        mockMvc.perform(get("/api/expedientes/" + expedienteTitular.getId())
                        .header("Authorization", "Bearer " + tokenCiudadanoTercero)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("REQ-2: Ciudadano ajeno (78999999) recibe 403 Forbidden al intentar ver detalle de expediente ajeno")
    void ciudadanoNoPuedeConsultarDetalleExpedienteAjeno() throws Exception {
        mockMvc.perform(get("/api/expedientes/detalle/" + expedienteTitular.getNumeroTramite())
                        .header("Authorization", "Bearer " + tokenCiudadanoTercero)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("REQ-2: Ciudadano titular (42567891) SÍ puede consultar su propio expediente por ID")
    void ciudadanoTitularPuedeConsultarSuExpedientePorId() throws Exception {
        mockMvc.perform(get("/api/expedientes/" + expedienteTitular.getId())
                        .header("Authorization", "Bearer " + tokenCiudadanoPropietario)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(expedienteTitular.getId().toString()))
                .andExpect(jsonPath("$.numeroTramite").value("EXP-2026-00001"));
    }

    @Test
    @DisplayName("REQ-2: Ciudadano titular (42567891) SÍ puede consultar su propio expediente por detalle")
    void ciudadanoTitularPuedeConsultarSuExpedientePorDetalle() throws Exception {
        mockMvc.perform(get("/api/expedientes/detalle/" + expedienteTitular.getNumeroTramite())
                        .header("Authorization", "Bearer " + tokenCiudadanoPropietario)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroTramite").value("EXP-2026-00001"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // REQUISITO 3: Ciudadano no puede descargar documentos ajenos
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("REQ-3: Ciudadano ajeno recibe 403 Forbidden al intentar descargar Anexo 1 ajeno")
    void ciudadanoNoPuedeDescargarDocumentoAjeno() throws Exception {
        mockMvc.perform(get("/api/expedientes/" + expedienteTitular.getId() + "/documentos/anexo1")
                        .header("Authorization", "Bearer " + tokenCiudadanoTercero))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("REQ-3: Ciudadano titular SÍ puede descargar el Anexo 1 de su propio expediente")
    void ciudadanoTitularPuedeDescargarSuAnexo1() throws Exception {
        mockMvc.perform(get("/api/expedientes/" + expedienteTitular.getId() + "/documentos/anexo1")
                        .header("Authorization", "Bearer " + tokenCiudadanoPropietario))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // REQUISITO 4: Tercero no puede subsanar solicitudes ajenas
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("REQ-4: Ciudadano ajeno recibe 403 Forbidden al intentar subsanar expediente ajeno")
    void ciudadanoTerceroNoPuedeSubsanarExpedienteAjeno() throws Exception {
        Map<String, String> payload = Map.of("detalleSubsanacion", "Certificado de pozo a tierra actualizado");

        mockMvc.perform(post("/api/expedientes/" + expedienteTitular.getId() + "/subsanar")
                        .header("Authorization", "Bearer " + tokenCiudadanoTercero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("REQ-4: Intento de subsanación cuando el expediente NO está en OBSERVADO es rechazado con 403")
    void subsanacionRechazadaSiNoEstaObservado() throws Exception {
        // EXP-2026-00001 está en FORMATOS_GENERADOS (no OBSERVADO)
        Map<String, String> payload = Map.of("detalleSubsanacion", "Corrección de requisitos");

        mockMvc.perform(post("/api/expedientes/" + expedienteTitular.getId() + "/subsanar")
                        .header("Authorization", "Bearer " + tokenCiudadanoPropietario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // REQUISITO 5: Funcionarios autorizados acceden conforme a sus competencias
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("REQ-5: EVALUADOR y ADMIN pueden consultar cualquier expediente")
    void evaluadorYAdminPuedenConsultarExpedientes() throws Exception {
        mockMvc.perform(get("/api/expedientes/" + expedienteTitular.getId())
                        .header("Authorization", "Bearer " + tokenEvaluador))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/expedientes/" + expedienteTitular.getId())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("REQ-5: CAJERO no puede aprobar expedientes de licencia (403 Forbidden)")
    void cajeroNoPuedeEvaluarExpediente() throws Exception {
        mockMvc.perform(post("/api/expedientes/" + expedienteTitular.getId() + "/aprobar")
                        .header("Authorization", "Bearer " + tokenCajero))
                .andExpect(status().isForbidden());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // REQUISITO 6: El INSPECTOR no puede modificar licencias ni expedientes
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("REQ-6: INSPECTOR no puede evaluar ni aprobar expedientes (403 Forbidden)")
    void inspectorNoPuedeEvaluarExpediente() throws Exception {
        mockMvc.perform(post("/api/expedientes/" + expedienteTitular.getId() + "/aprobar")
                        .header("Authorization", "Bearer " + tokenInspector))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("REQ-6: INSPECTOR no puede validar pagos en ventanilla (403 Forbidden)")
    void inspectorNoPuedeValidarPago() throws Exception {
        Map<String, Object> pagoPayload = Map.of(
                "voucherId", "VCH-2026-123456",
                "numeroOperacionSat", "OP-123456"
        );

        mockMvc.perform(post("/api/expedientes/" + expedienteTitular.getId() + "/pago")
                        .header("Authorization", "Bearer " + tokenInspector)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pagoPayload)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("REQ-6: INSPECTOR SÍ puede consultar verificación ampliada de licencia para fiscalización en campo")
    void inspectorPuedeConsultarVerificacionAmpliada() throws Exception {
        String codigoLicencia = expedienteAprobado.getLicenciaQrCode();

        mockMvc.perform(get("/api/public/licencias/" + codigoLicencia + "/ampliada")
                        .header("Authorization", "Bearer " + tokenInspector)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroLicencia").value(codigoLicencia))
                .andExpect(jsonPath("$.direccion").exists())
                .andExpect(jsonPath("$.nivelRiesgo").exists());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // REQUISITO 7: El QR público no expone datos personales restringidos
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("REQ-7: Consulta de QR pública minimiza y ofusca datos personales (Ley N° 29733)")
    void qrPublicoMinimizaDatosPersonales() throws Exception {
        String codigoLicencia = expedienteAprobado.getLicenciaQrCode();

        mockMvc.perform(get("/api/public/licencias/" + codigoLicencia)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroLicencia").value(codigoLicencia))
                .andExpect(jsonPath("$.documentoIdentidad").value(containsString("***")))
                .andExpect(jsonPath("$.correoElectronico").doesNotExist())
                .andExpect(jsonPath("$.telefono").doesNotExist());
    }

    @Test
    @DisplayName("REQ-7: Verificación ampliada sin credenciales es rechazada con 401 Unauthorized")
    void qrAmpliadoAnonimoRetorna401() throws Exception {
        String codigoLicencia = expedienteAprobado.getLicenciaQrCode();

        mockMvc.perform(get("/api/public/licencias/" + codigoLicencia + "/ampliada")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // REQUISITO 8: Endpoints públicos permitidos siguen funcionando
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("REQ-8: Seguimiento público por número de trámite funciona anónimamente con datos protegidos")
    void seguimientoTramitePublicoFuncionaAnonimamente() throws Exception {
        mockMvc.perform(get("/api/expedientes/tramite/EXP-2026-00001")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroTramite").value("EXP-2026-00001"))
                .andExpect(jsonPath("$.estado").exists())
                .andExpect(jsonPath("$.titularOfuscado").value(containsString("***")));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // REQUISITO 9: Respuestas seguras ante accesos no autorizados (401/403)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("REQ-9: Intento no autorizado a recurso inexistente con usuario ciudadano no fuga información")
    void accesoNoAutorizadoARecursoInexistenteProduce404O403Seguro() throws Exception {
        UUID randomId = UUID.randomUUID();

        // Debe retornar 404 (si busca y no existe) o 403 (rechazado por seguridad) sin stacktrace interno
        mockMvc.perform(get("/api/expedientes/" + randomId)
                        .header("Authorization", "Bearer " + tokenCiudadanoTercero)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
