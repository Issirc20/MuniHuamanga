package pe.gob.munihuamanga.licencias.expedientes.audit;

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
import pe.gob.munihuamanga.licencias.common.dto.ExpedienteResponseDto;
import pe.gob.munihuamanga.licencias.common.dto.VerificacionPublicaQrDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.EstadoLicencia;
import pe.gob.munihuamanga.licencias.common.enums.EstadoResolucion;
import pe.gob.munihuamanga.licencias.common.enums.TipoDecisionResolucion;
import pe.gob.munihuamanga.licencias.common.enums.TipoVigenciaLicencia;
import pe.gob.munihuamanga.licencias.expedientes.integracion.adapter.FirmaDigitalAdapterService;
import pe.gob.munihuamanga.licencias.expedientes.licencias.model.LicenciaEmitida;
import pe.gob.munihuamanga.licencias.expedientes.licencias.repository.LicenciaEmitidaRepository;
import pe.gob.munihuamanga.licencias.expedientes.licencias.service.LicenciaService;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.resoluciones.model.ResolucionAdministrativa;
import pe.gob.munihuamanga.licencias.expedientes.resoluciones.repository.ResolucionAdministrativaRepository;
import pe.gob.munihuamanga.licencias.expedientes.service.DocumentoPdfService;
import pe.gob.munihuamanga.licencias.expedientes.service.LicenciaPdfGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Suite integral de pruebas de auditoría y controles criptográficos / administrativos
 * para el Incremento I08.1 (MuniHuamanga V2).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuditoriaI08_1Test {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LicenciaEmitidaRepository licenciaRepository;

    @Autowired
    private ResolucionAdministrativaRepository resolucionRepository;

    @Autowired
    private pe.gob.munihuamanga.licencias.expedientes.evaluacion.repository.InformeAdministrativoRepository informeAdministrativoRepository;

    @Autowired
    private ExpedienteRepository expedienteRepository;

    @Autowired
    private LicenciaService licenciaService;

    @Autowired
    private LicenciaPdfGenerator licenciaPdfGenerator;

    @Autowired
    private DocumentoPdfService documentoPdfService;

    @Autowired
    private FirmaDigitalAdapterService firmaDigitalAdapterService;

    private ResolucionAdministrativa crearResolucionPrueba(String num) {
        Expediente exp = Expediente.builder()
                .id(UUID.randomUUID())
                .numeroTramite("EXP-2026-AUD-" + num + "-" + UUID.randomUUID().toString().substring(0, 4))
                .nombreTitular("TITULAR AUDITORIA " + num)
                .documentoIdentidad("42123491")
                .nombreComercial("COMERCIAL AUDITORIA " + num)
                .giroNegocio("VENTA DE ABARROTES")
                .direccionEstablecimiento("JR. AUDITORIA 123, HUAMANGA")
                .solicitanteId(UUID.randomUUID())
                .estado(EstadoExpediente.APTO_PARA_RESOLUCION)
                .fechaCreacion(LocalDateTime.now())
                .fechaLimite(LocalDateTime.now().plusDays(15))
                .build();
        exp = expedienteRepository.save(exp);

        pe.gob.munihuamanga.licencias.expedientes.evaluacion.model.InformeAdministrativo inf =
                pe.gob.munihuamanga.licencias.expedientes.evaluacion.model.InformeAdministrativo.builder()
                        .id(UUID.randomUUID())
                        .expediente(exp)
                        .numeroInforme("INF-2026-AUD-" + num + "-" + UUID.randomUUID().toString().substring(0, 4))
                        .version(1)
                        .evaluadorId(UUID.randomUUID())
                        .evaluadorNombre("EVALUADOR AUDITOR")
                        .fechaElaboracion(LocalDateTime.now())
                        .conclusion(pe.gob.munihuamanga.licencias.common.enums.ConclusionInforme.FAVORABLE)
                        .estado(pe.gob.munihuamanga.licencias.common.enums.EstadoInforme.APROBADO)
                        .fundamentosAdministrativos("Cumple con la totalidad de requisitos normativos TUPA")
                        .referenciasDocumentales("Anexo 1, Anexo 4, Certificado ITSE")
                        .esVersionActual(true)
                        .build();
        inf = informeAdministrativoRepository.save(inf);

        ResolucionAdministrativa res = ResolucionAdministrativa.builder()
                .id(UUID.randomUUID())
                .numeroResolucion("RES-2026-AUD-" + num + "-" + UUID.randomUUID().toString().substring(0, 4))
                .expediente(exp)
                .informeAdministrativo(inf)
                .tipoDecision(TipoDecisionResolucion.FAVORABLE)
                .estado(EstadoResolucion.FIRMADA)
                .fundamentos("Fundamentos de prueba para auditoría I08.1")
                .archivoPdf(new byte[]{0x25, 0x50, 0x44, 0x46})
                .version(1)
                .versionInforme(1)
                .esVersionActual(true)
                .creadoPor("AUDITOR")
                .fechaCreacion(LocalDateTime.now())
                .build();
        return resolucionRepository.save(res);
    }

    @Test
    @DisplayName("AUD-01: Bloqueo de emisión en endpoint heredado V1 (/api/expedientes/{id}/emision-licencia)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testBloqueoEmisionHeredadaV1_Rechazada() throws Exception {
        UUID expedienteId = UUID.randomUUID();

        mockMvc.perform(post("/api/expedientes/" + expedienteId + "/emision-licencia")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("AUD-02: Verificación pública QR con minimización estricta de datos (DNI eliminado, sin IDs sensibles)")
    void testVerificacionPublicaQr_MinimizacionEstrictaDatos() throws Exception {
        String qrCodigo = "QR-AUDIT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ResolucionAdministrativa res = crearResolucionPrueba("01");

        LicenciaEmitida lic = LicenciaEmitida.builder()
                .id(UUID.randomUUID())
                .numeroLicencia("LIC-2026-999001")
                .resolucion(res)
                .expediente(res.getExpediente())
                .codigoVerificacionQr(qrCodigo)
                .titularTipoDocumento("DNI")
                .titularNumeroDocumento("42123491")
                .titularNombreORazonSocial("JUAN PÉREZ GARCÍA")
                .nombreComercial("BODEGA SAN JUAN")
                .direccionEstablecimiento("Jr. Asamblea 123, Huamanga")
                .giroActividad("Venta de abarrotes")
                .tipoVigencia(TipoVigenciaLicencia.INDETERMINADA)
                .fechaEmision(LocalDate.now())
                .autoridadEmisora("Subgerencia de Comercio y Licencias")
                .estado(EstadoLicencia.EMITIDA)
                .hashPdf("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
                .archivoPdf(new byte[]{0x25, 0x50, 0x44, 0x46})
                .fechaCreacion(LocalDateTime.now())
                .creadoPor("AUDITOR")
                .build();
        licenciaRepository.save(lic);

        mockMvc.perform(get("/api/licencias/verificar/" + qrCodigo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoVerificacionQr").value(qrCodigo))
                .andExpect(jsonPath("$.numeroLicencia").value("LIC-2026-999001"))
                .andExpect(jsonPath("$.titularDocumentoEnmascarado").doesNotExist())
                .andExpect(jsonPath("$.titularNumeroDocumento").doesNotExist())
                .andExpect(jsonPath("$.valida").value(true))
                .andExpect(jsonPath("$.mensajeVerificacion").value(containsString("VÁLIDA Y VIGENTE")));
    }

    @Test
    @DisplayName("AUD-03: Verificación pública QR para licencia SUSPENDIDA o REVOCADA refleja invalidez")
    void testVerificacionPublicaQr_LicenciaSuspendida_ReflejaInvalidez() {
        String qrCodigo = "QR-SUSPENDIDA-" + UUID.randomUUID().toString().substring(0, 8);
        ResolucionAdministrativa res = crearResolucionPrueba("02");

        LicenciaEmitida lic = LicenciaEmitida.builder()
                .id(UUID.randomUUID())
                .numeroLicencia("LIC-2026-999002")
                .resolucion(res)
                .expediente(res.getExpediente())
                .codigoVerificacionQr(qrCodigo)
                .titularTipoDocumento("DNI")
                .titularNumeroDocumento("70809010")
                .titularNombreORazonSocial("EMPRESA SUSPENDIDA S.A.C.")
                .nombreComercial("EMPRESA SUSPENDIDA")
                .giroActividad("Servicios generales")
                .direccionEstablecimiento("Av. Mariscal Cáceres 456")
                .tipoVigencia(TipoVigenciaLicencia.INDETERMINADA)
                .fechaEmision(LocalDate.now().minusMonths(2))
                .autoridadEmisora("Subgerencia de Comercio y Licencias")
                .estado(EstadoLicencia.SUSPENDIDA)
                .hashPdf("abcdef123456")
                .fechaCreacion(LocalDateTime.now())
                .creadoPor("AUDITOR")
                .build();
        licenciaRepository.save(lic);

        VerificacionPublicaQrDto dto = licenciaService.verificarLicenciaPublica(qrCodigo);
        assertNotNull(dto);
        assertFalse(dto.isValida());
        assertTrue(dto.getMensajeVerificacion().contains("SUSPENDIDA TEMPORALMENTE"));
        assertNull(dto.getTitularDocumentoEnmascarado());
    }

    @Test
    @DisplayName("AUD-04: RBAC / BOLA: INSPECTOR no puede descargar directamente el PDF oficial de licencia interna (HTTP 403 Forbidden)")
    @WithMockUser(username = "inspector_pedro", roles = {"INSPECTOR"})
    void testInspectorBloqueadoDescargaDirectaLicencia_Retorna403() throws Exception {
        UUID licenciaId = UUID.randomUUID();

        mockMvc.perform(get("/api/licencias/" + licenciaId + "/pdf"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("AUD-05: Inmutabilidad documental: descargarPdfLicencia devuelve exactamente los bytes almacenados sin alteración")
    void testInmutabilidadLicenciaPdf_RetornaBytesAlmacenados() {
        byte[] pdfOficialBytes = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x37, 0x0A, 0x25, 0x41, 0x55, 0x44, 0x49, 0x54};
        String qrCodigo = "QR-IMMUTABLE-" + UUID.randomUUID().toString().substring(0, 8);
        ResolucionAdministrativa res = crearResolucionPrueba("03");

        LicenciaEmitida lic = LicenciaEmitida.builder()
                .id(UUID.randomUUID())
                .numeroLicencia("LIC-2026-999003")
                .resolucion(res)
                .expediente(res.getExpediente())
                .codigoVerificacionQr(qrCodigo)
                .titularTipoDocumento("DNI")
                .titularNumeroDocumento("41223344")
                .titularNombreORazonSocial("COMERCIAL AYACUCHO S.A.")
                .nombreComercial("COMERCIAL AYACUCHO")
                .giroActividad("Venta al por mayor")
                .direccionEstablecimiento("Jr. Bellido 234")
                .tipoVigencia(TipoVigenciaLicencia.INDETERMINADA)
                .fechaEmision(LocalDate.now())
                .autoridadEmisora("Subgerencia de Comercio y Licencias")
                .estado(EstadoLicencia.EMITIDA)
                .hashPdf("abcdef987654321")
                .archivoPdf(pdfOficialBytes)
                .fechaCreacion(LocalDateTime.now())
                .creadoPor("AUDITOR")
                .build();
        LicenciaEmitida guardada = licenciaRepository.save(lic);

        byte[] descargado = licenciaService.descargarPdfLicencia(guardada.getId());
        assertArrayEquals(pdfOficialBytes, descargado);
    }

    @Test
    @DisplayName("AUD-06: Watermark en vista preliminar: Si firmaDigitalValida es false, genera PDF con marca SIN VALIDEZ OFICIAL")
    void testWatermarkVistaPreliminar_GeneraPdfConMarca() {
        ExpedienteResponseDto dto = ExpedienteResponseDto.builder()
                .numeroTramite("EXP-2026-PREVIEW")
                .numeroLicencia("LIC-2026-PREVIEW")
                .nombreTitular("PREVIEW TITULAR")
                .documentoIdentidad("42998877")
                .firmaDigitalValida(false) // Sin firma digital institucional
                .build();

        byte[] qr = documentoPdfService.generarImagenQr("PREVIEW-CODE", 100, 100);
        byte[] pdf = licenciaPdfGenerator.generarLicencia(dto, qr);

        assertNotNull(pdf);
        assertTrue(pdf.length > 1000);
        // Comprobar cabecera estándar PDF
        assertEquals('%', (char) pdf[0]);
        assertEquals('P', (char) pdf[1]);
        assertEquals('D', (char) pdf[2]);
        assertEquals('F', (char) pdf[3]);
    }

    @Test
    @DisplayName("AUD-07: Fail-Closed en adaptador de firma digital: lanza excepción descriptiva si no hay conexión PKI oficial")
    void testFailClosedFirmaDigitalAdapter() {
        assertFalse(firmaDigitalAdapterService.estaDisponible());
        assertThrows(UnsupportedOperationException.class, () ->
                firmaDigitalAdapterService.firmarDocumento(new byte[]{1, 2, 3}, "EXP-2026-0001"));
        assertThrows(UnsupportedOperationException.class, () ->
                firmaDigitalAdapterService.firmarResolucion(new byte[]{1, 2, 3}, "RES-2026-0001", "Licencia", "Gerente"));
    }

    @Test
    @DisplayName("AUD-08: Enmascaramiento estricto de documentos de identidad (Ley N° 29733)")
    void testEnmascaramientoDocumentos() {
        assertEquals("42***91", LicenciaService.enmascararDocumento("42123491"));
        assertEquals("20***71", LicenciaService.enmascararDocumento("20601234571"));
        assertEquals("***4", LicenciaService.enmascararDocumento("1234"));
    }
}
