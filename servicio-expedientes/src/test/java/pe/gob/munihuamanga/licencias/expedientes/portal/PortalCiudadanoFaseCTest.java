package pe.gob.munihuamanga.licencias.expedientes.portal;

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
import org.springframework.test.web.servlet.MvcResult;
import pe.gob.munihuamanga.licencias.common.dto.ActualizarBorradorDto;
import pe.gob.munihuamanga.licencias.common.dto.AdjuntarDocumentoBorradorDto;
import pe.gob.munihuamanga.licencias.common.dto.CrearBorradorDto;
import pe.gob.munihuamanga.licencias.common.dto.CrearEstablecimientoDto;
import pe.gob.munihuamanga.licencias.common.dto.LoginRequestDto;
import pe.gob.munihuamanga.licencias.common.dto.PresentarBorradorDto;
import pe.gob.munihuamanga.licencias.common.dto.RegistroCuentaDto;
import pe.gob.munihuamanga.licencias.common.enums.ModalidadTramite;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumento;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumentoAdjunto;
import pe.gob.munihuamanga.licencias.common.enums.TipoPersona;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración automatizadas para la Fase C.1:
 * - Autenticación y registro de cuentas ciudadanas (M01)
 * - Protección RBAC y BOLA/IDOR entre ciudadanos (M01 / M03)
 * - Creación de establecimientos comerciales con geolocalización lat/lng (M02)
 * - Ciclo de vida de borradores: creación, guardado parcial, carga documental SHA-256 (M03 / M04)
 * - Presentación formal atómica con correlativo inmutable EXP-YYYY-XXXXX (M06)
 * - Idempotencia: control estricto contra doble presentación (M06)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PortalCiudadanoFaseCTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Fase C.1 - 01: Autenticación de ciudadano semilla devuelve token JWT y perfil correcto")
    void testAutenticacionCiudadano() throws Exception {
        LoginRequestDto loginDto = new LoginRequestDto("42567891", "ciud123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.username").value("42567891"))
                .andExpect(jsonPath("$.rol").value("ROLE_CIUDADANO"));
    }

    @Test
    @DisplayName("Fase C.1 - 02: Registro público de nueva cuenta ciudadana y posterior inicio de sesión")
    void testRegistroCuentaCiudadana() throws Exception {
        String docUnico = String.format("88%06d", Math.abs((int) (System.currentTimeMillis() % 1000000)));
        String emailUnico = "ciudadano." + docUnico + "@gmail.com";

        RegistroCuentaDto regDto = RegistroCuentaDto.builder()
                .tipoPersona(TipoPersona.NATURAL)
                .tipoDocumento(TipoDocumento.DNI)
                .numeroDocumento(docUnico)
                .razonSocialONombre("Juan Huamán Pérez")
                .email(emailUnico)
                .telefono("966998877")
                .direccionFiscal("Jr. Bellido 320, Ayacucho")
                .password("claveSegura2026")
                .build();

        mockMvc.perform(post("/api/cuentas/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.numeroDocumento").value(docUnico));

        // Iniciar sesión con la cuenta recién creada
        LoginRequestDto loginDto = new LoginRequestDto(docUnico, "claveSegura2026");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()));
    }

    @Test
    @DisplayName("Fase C.1 - 03: Control BOLA/IDOR: Ciudadano A no puede consultar ni registrar en cuenta de Ciudadano B")
    void testAislamientoBolaEntreCuentas() throws Exception {
        String tokenAjeno = obtenerToken("78999999", "ciud123");
        UUID cuentaMariaQuispe = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479");

        CrearEstablecimientoDto estDto = CrearEstablecimientoDto.builder()
                .nombreComercial("Intento no autorizado")
                .direccion("Jr. Cusco 100")
                .latitud(new BigDecimal("-13.16000"))
                .longitud(new BigDecimal("-74.22000"))
                .ciiuCodigo("G4711")
                .giroPrincipal("Abarrotes")
                .areaMetrosCuadrados(new BigDecimal("50.0"))
                .build();

        // Debe ser denegado con 403 Forbidden
        mockMvc.perform(post("/api/establecimientos/cuenta/" + cuentaMariaQuispe)
                        .header("Authorization", "Bearer " + tokenAjeno)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(estDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Fase C.1 - 04: Flujo integral del Wizard Ciudadano V2: Establecimiento con Leaflet GPS, Borrador, Carga SHA-256 y Presentación Formal")
    void testFlujoCompletoWizardCiudadanoV2() throws Exception {
        String tokenTitular = obtenerToken("42567891", "ciud123");
        UUID cuentaId = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479");

        // 1. Registrar establecimiento con coordenadas geográficas Leaflet
        CrearEstablecimientoDto estDto = CrearEstablecimientoDto.builder()
                .nombreComercial("Botica San Juan Bautista - Sucursal 2")
                .direccion("Jr. 28 de Julio 620, Ayacucho")
                .referencia("Cerca al Arco del Triunfo")
                .latitud(new BigDecimal("-13.16315"))
                .longitud(new BigDecimal("-74.22389"))
                .ciiuCodigo("G4772")
                .giroPrincipal("Venta minorista de productos farmacéuticos")
                .areaMetrosCuadrados(new BigDecimal("75.00"))
                .build();

        MvcResult resEst = mockMvc.perform(post("/api/establecimientos/cuenta/" + cuentaId)
                        .header("Authorization", "Bearer " + tokenTitular)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(estDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andReturn();

        UUID establecimientoId = UUID.fromString(
                objectMapper.readTree(resEst.getResponse().getContentAsString()).get("id").asText());

        // 2. Crear borrador V2
        CrearBorradorDto borradorDto = CrearBorradorDto.builder()
                .cuentaId(cuentaId)
                .establecimientoId(establecimientoId)
                .modalidadTramite(ModalidadTramite.LICENCIA_INDETERMINADA)
                .nombreComercialDeclarado("Botica San Juan Bautista - Sucursal 2")
                .giroNegocioDeclarado("Farmacia y botica")
                .actividadDetallada("Venta minorista de medicamentos")
                .areaMetrosCuadrados(new BigDecimal("75.00"))
                .aforoPersonas(25)
                .numeroPisos(1)
                .horarioInicio("08:00")
                .horarioFin("21:00")
                .telefonoContacto("966123456")
                .correoNotificacion("maria.quispe@gmail.com")
                .autorizaNotificacion(true)
                .nivelRiesgoDeclarado(NivelRiesgo.BAJO)
                .build();

        MvcResult resBorr = mockMvc.perform(post("/api/borradores")
                        .header("Authorization", "Bearer " + tokenTitular)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(borradorDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.estado").value("EN_EDICION"))
                .andReturn();

        UUID borradorId = UUID.fromString(
                objectMapper.readTree(resBorr.getResponse().getContentAsString()).get("id").asText());

        // 3. Guardar avance parcial
        ActualizarBorradorDto avanceDto = ActualizarBorradorDto.builder()
                .nombreComercialDeclarado("Botica San Juan Bautista - Sucursal 2 (Actualizado)")
                .numeroOperacionSat("SAT-OP-2026-9901")
                .build();

        mockMvc.perform(put("/api/borradores/" + borradorId)
                        .header("Authorization", "Bearer " + tokenTitular)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(avanceDto)))
                .andExpect(status().isOk());

        // 4. Adjuntar documentos obligatorios en Base64 con verificación de hash SHA-256
        String dummyPdfContent = Base64.getEncoder().encodeToString("%PDF-1.4 Declaracion Jurada Anexo 1 Oficial MPH".getBytes());
        AdjuntarDocumentoBorradorDto docAnexo1 = AdjuntarDocumentoBorradorDto.builder()
                .tipoDocumento(TipoDocumentoAdjunto.DECLARACION_JURADA_ANEXO1)
                .nombreArchivo("Anexo1-Declaracion-Jurada.pdf")
                .tipoContenido("application/pdf")
                .contenidoBase64(dummyPdfContent)
                .descripcion("Generado desde formulario oficial interactivo")
                .build();

        mockMvc.perform(post("/api/borradores/" + borradorId + "/documentos")
                        .header("Authorization", "Bearer " + tokenTitular)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(docAnexo1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hashSha256", notNullValue()));

        String dummySatContent = Base64.getEncoder().encodeToString("%PDF-1.4 Voucher SAT MPH".getBytes());
        AdjuntarDocumentoBorradorDto docSat = AdjuntarDocumentoBorradorDto.builder()
                .tipoDocumento(TipoDocumentoAdjunto.COMPROBANTE_PAGO_SAT)
                .nombreArchivo("Voucher-SAT-Tasa.pdf")
                .tipoContenido("application/pdf")
                .contenidoBase64(dummySatContent)
                .descripcion("Comprobante de pago SAT")
                .build();

        mockMvc.perform(post("/api/borradores/" + borradorId + "/documentos")
                        .header("Authorization", "Bearer " + tokenTitular)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(docSat)))
                .andExpect(status().isCreated());

        String dummyItseContent = Base64.getEncoder().encodeToString("%PDF-1.4 Formato ITSE CENEPRED".getBytes());
        AdjuntarDocumentoBorradorDto docItse = AdjuntarDocumentoBorradorDto.builder()
                .tipoDocumento(TipoDocumentoAdjunto.EVIDENCIA_RIESGO_ITSE)
                .nombreArchivo("Anexo4-Seguridad-ITSE.pdf")
                .tipoContenido("application/pdf")
                .contenidoBase64(dummyItseContent)
                .descripcion("Condiciones de seguridad firmadas")
                .build();

        mockMvc.perform(post("/api/borradores/" + borradorId + "/documentos")
                        .header("Authorization", "Bearer " + tokenTitular)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(docItse)))
                .andExpect(status().isCreated());

        // 5. Validar que la lista de documentos se consulte correctamente
        mockMvc.perform(get("/api/borradores/" + borradorId + "/documentos")
                        .header("Authorization", "Bearer " + tokenTitular))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        // 6. Verificar requisitos pendientes
        mockMvc.perform(get("/api/borradores/" + borradorId + "/requisitos-pendientes")
                        .header("Authorization", "Bearer " + tokenTitular))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aptoParaPresentacion").value(true));

        // 7. Formalizar presentación del borrador (M06) -> Genera correlativo EXP-YYYY-XXXXX
        PresentarBorradorDto presentarDto = PresentarBorradorDto.builder()
                .declaracionJuradaAceptada(true)
                .confirmacionPresentacion(true)
                .comentarios("Solicitud formalizada por María Quispe vía Portal Ciudadano")
                .build();

        MvcResult resPresentar = mockMvc.perform(post("/api/borradores/" + borradorId + "/presentar")
                        .header("Authorization", "Bearer " + tokenTitular)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(presentarDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroTramite", containsString("EXP-2026-")))
                .andExpect(jsonPath("$.estado").value("PRESENTADO"))
                .andReturn();

        String numeroTramiteOriginal = objectMapper.readTree(resPresentar.getResponse().getContentAsString()).get("numeroTramite").asText();
        String expedienteIdOriginal = objectMapper.readTree(resPresentar.getResponse().getContentAsString()).get("id").asText();

        // 8. Control de no duplicidad / idempotencia: Un reintento de presentación retorna el expediente original sin duplicar
        mockMvc.perform(post("/api/borradores/" + borradorId + "/presentar")
                        .header("Authorization", "Bearer " + tokenTitular)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(presentarDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(expedienteIdOriginal))
                .andExpect(jsonPath("$.numeroTramite").value(numeroTramiteOriginal));
    }

    private String obtenerToken(String username, String password) throws Exception {
        LoginRequestDto loginDto = new LoginRequestDto(username, password);
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }
}
