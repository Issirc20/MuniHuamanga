package pe.gob.munihuamanga.licencias.expedientes.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import pe.gob.munihuamanga.licencias.common.dto.Anexo4CondicionesDto;
import pe.gob.munihuamanga.licencias.common.dto.ExpedienteResponseDto;
import pe.gob.munihuamanga.licencias.common.dto.VoucherDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Pruebas Exhaustivas de Generación de Formatos Oficiales PDF en Monolito Modular (US-05, US-07, US-09, US-10)")
class DocumentoPdfServiceCompletoTest {

    private DocumentoPdfService documentoPdfService;

    @BeforeEach
    void setUp() {
        Anexo1PdfGenerator anexo1 = new Anexo1PdfGenerator();
        Anexo3PdfGenerator anexo3 = new Anexo3PdfGenerator();
        Anexo4PdfGenerator anexo4 = new Anexo4PdfGenerator();
        LicenciaPdfGenerator licencia = new LicenciaPdfGenerator();
        documentoPdfService = new DocumentoPdfService(anexo1, anexo3, anexo4, licencia);
        ReflectionTestUtils.setField(documentoPdfService, "portalVerificacionUrl", "http://localhost:8081/verificar-licencia.html?codigo=");
    }

    @Test
    @DisplayName("Debe generar PDF oficial del Anexo 4 (Condiciones de Seguridad - 4 páginas) con desglose por pisos y checklist")
    void testGenerarAnexo4CondicionesSeguridadOficial() {
        Anexo4CondicionesDto condiciones = Anexo4CondicionesDto.builder()
                .areaPiso1(new BigDecimal("80.00"))
                .areaPiso2(new BigDecimal("40.00"))
                .areaTechadaTotal(new BigDecimal("120.00"))
                .areaOcupadaTotal(new BigDecimal("120.00"))
                .aforoPersonas(45)
                .antiguedadEdificacionAnios(8)
                .antiguedadGiroAnios(3)
                .extintoresOperativos(true)
                .lucesEmergenciaOperativas(true)
                .pozoTierraVigente(true)
                .tableroElectricoProtegido(true)
                .build();

        ExpedienteResponseDto exp = ExpedienteResponseDto.builder()
                .id(UUID.randomUUID())
                .numeroTramite("EXP-2026-00003")
                .nombreTitular("Rosa Benítez Torres")
                .documentoIdentidad("41258963")
                .nombreComercial("Farmacia y Bazar Ayacucho")
                .giroNegocio("Botica y bazar")
                .direccionEstablecimiento("Jr. Callao N° 215, Huamanga")
                .areaMetrosCuadrados(new BigDecimal("120.00"))
                .aforoPersonas(45)
                .estado(EstadoExpediente.FORMATOS_GENERADOS)
                .nivelRiesgo(NivelRiesgo.MEDIO)
                .tipoItse("ITSE_POSTERIOR")
                .anexo4Condiciones(condiciones)
                .fechaCreacion(LocalDateTime.now())
                .build();

        byte[] pdfBytes = documentoPdfService.generarAnexo4CondicionesSeguridad(exp);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 8000, "El PDF oficial del Anexo 4 de 4 páginas debe superar 8KB");
        String pdfHeader = new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII);
        assertTrue(pdfHeader.startsWith("%PDF-"));
    }

    @Test
    @DisplayName("Debe generar PDF oficial del Anexo 1 de 2 páginas con estructura reglamentaria")
    void testGenerarAnexo1DeclaracionJuradaOficial() {
        ExpedienteResponseDto exp = ExpedienteResponseDto.builder()
                .id(UUID.randomUUID())
                .numeroTramite("EXP-2026-00001")
                .nombreTitular("María Quispe Huamán")
                .documentoIdentidad("42567891")
                .nombreComercial("Boutique Huamanga")
                .giroNegocio("Venta de artesanías y textiles ayacuchanos")
                .direccionEstablecimiento("Jr. 9 de Diciembre 142, Huamanga")
                .areaMetrosCuadrados(new BigDecimal("45.50"))
                .estado(EstadoExpediente.FORMATOS_GENERADOS)
                .nivelRiesgo(NivelRiesgo.BAJO)
                .tipoItse("ITSE_POSTERIOR")
                .fechaCreacion(LocalDateTime.now())
                .fechaLimite(LocalDateTime.now().plusDays(21))
                .build();

        byte[] pdfBytes = documentoPdfService.generarAnexo1DeclaracionJurada(exp);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 5000, "El PDF oficial de 2 páginas debe contener tablas completas y superar 5KB");
        String pdfHeader = new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII);
        assertTrue(pdfHeader.startsWith("%PDF-"), "El archivo debe iniciar con %PDF-");
    }

    @Test
    @DisplayName("Debe generar PDF oficial del Anexo 3 (Matriz ITSE - 2 páginas) con funciones y factores agravantes")
    void testGenerarAnexo3MatrizRiesgoItseOficial() {
        ExpedienteResponseDto exp = ExpedienteResponseDto.builder()
                .id(UUID.randomUUID())
                .numeroTramite("EXP-2026-00002")
                .nombreTitular("Carlos Huamán Mendoza")
                .documentoIdentidad("28549632")
                .nombreComercial("Pollería y Parrillas El Portal")
                .giroNegocio("Restaurante / Expendio de comidas")
                .direccionEstablecimiento("Portal Constitución 45, Huamanga")
                .areaMetrosCuadrados(new BigDecimal("180.00"))
                .aforoPersonas(65)
                .estado(EstadoExpediente.FORMATOS_GENERADOS)
                .nivelRiesgo(NivelRiesgo.ALTO)
                .tipoItse("ITSE_PREVIA")
                .numeroInformeItse("ITSE-2026-DC-00124")
                .fechaCreacion(LocalDateTime.now())
                .build();

        byte[] pdfBytes = documentoPdfService.generarAnexo3MatrizRiesgoItse(exp);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 5000, "El PDF del Anexo 3 de 2 páginas debe superar 5KB");
        String pdfHeader = new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII);
        assertTrue(pdfHeader.startsWith("%PDF-"));
    }

    @Test
    @DisplayName("US-05: Debe generar PDF válido de Declaración Jurada con encabezado oficial")
    void testGenerarDeclaracionJurada() {
        ExpedienteResponseDto exp = ExpedienteResponseDto.builder()
                .id(UUID.randomUUID())
                .numeroTramite("EXP-2026-00001")
                .nombreTitular("María Quispe Huamán")
                .documentoIdentidad("42567891")
                .nombreComercial("Boutique Huamanga")
                .giroNegocio("Venta de artesanías")
                .direccionEstablecimiento("Jr. 9 de Diciembre 142")
                .areaMetrosCuadrados(new BigDecimal("35.50"))
                .estado(EstadoExpediente.FORMATOS_GENERADOS)
                .nivelRiesgo(NivelRiesgo.BAJO)
                .tipoItse("ITSE_POSTERIOR")
                .fechaCreacion(LocalDateTime.now())
                .fechaLimite(LocalDateTime.now().plusDays(21))
                .build();

        byte[] pdfBytes = documentoPdfService.generarDeclaracionJurada(exp);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 1000, "El PDF debe tener contenido estructurado");
        String pdfHeader = new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII);
        assertTrue(pdfHeader.startsWith("%PDF-"), "El archivo debe iniciar con el identificador estándar %PDF-");
    }

    @Test
    @DisplayName("US-05: Debe generar PDF válido de Solicitud ITSE para Defensa Civil")
    void testGenerarSolicitudItse() {
        ExpedienteResponseDto exp = ExpedienteResponseDto.builder()
                .numeroTramite("EXP-2026-00002")
                .nombreTitular("Carlos Huamán")
                .nombreComercial("Pollería El Portal")
                .giroNegocio("Restaurante")
                .direccionEstablecimiento("Portal Constitución 45")
                .areaMetrosCuadrados(new BigDecimal("80.00"))
                .nivelRiesgo(NivelRiesgo.MEDIO)
                .tipoItse("ITSE_POSTERIOR")
                .fechaCreacion(LocalDateTime.now())
                .build();

        byte[] pdfBytes = documentoPdfService.generarAnexo3MatrizRiesgoItse(exp);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 1000);
        String pdfHeader = new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII);
        assertTrue(pdfHeader.startsWith("%PDF-"));
    }

    @Test
    @DisplayName("US-07: Debe generar PDF válido de Voucher SAT con Código de Barras Code 128")
    void testGenerarVoucherSat() {
        VoucherDto voucher = VoucherDto.builder()
                .voucherId("VCH-2026-998877")
                .expedienteId(UUID.randomUUID())
                .numeroTramite("EXP-2026-00001")
                .titular("María Quispe Huamán")
                .documentoIdentidad("42567891")
                .monto(new BigDecimal("218.00"))
                .concepto("TASA LICENCIA FUNCIONAMIENTO - RIESGO MEDIO")
                .fechaEmision(LocalDateTime.now())
                .fechaVencimiento(LocalDateTime.now().plusDays(5))
                .codigoBarrasSat("0107VCH2026998877")
                .build();

        byte[] pdfBytes = documentoPdfService.generarVoucherSatPdf(voucher);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 1000);
        String pdfHeader = new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII);
        assertTrue(pdfHeader.startsWith("%PDF-"));
    }

    @Test
    @DisplayName("US-09: Debe generar PDF válido de Licencia Oficial con QR y Sello Digital")
    void testGenerarLicenciaPdf() {
        ExpedienteResponseDto exp = ExpedienteResponseDto.builder()
                .numeroTramite("EXP-2026-00001")
                .licenciaQrCode("LIC-2026-A1B2C3D4")
                .nombreTitular("María Quispe Huamán")
                .documentoIdentidad("42567891")
                .razonSocial("INVERSIONES AYACUCHO S.A.C.")
                .nombreComercial("Boutique Huamanga")
                .giroNegocio("Venta de artesanías")
                .direccionEstablecimiento("Jr. 9 de Diciembre 142")
                .areaMetrosCuadrados(new BigDecimal("35.50"))
                .nivelRiesgo(NivelRiesgo.BAJO)
                .tipoItse("ITSE_POSTERIOR")
                .fechaCreacion(LocalDateTime.now())
                .build();

        byte[] pdfBytes = documentoPdfService.generarLicenciaPdf(exp);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 1000, "El PDF de la licencia debe ser mayor a 1KB");
        String pdfHeader = new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII);
        assertTrue(pdfHeader.startsWith("%PDF-"));
    }

    @Test
    @DisplayName("US-10: Debe generar imagen PNG válida del Código QR")
    void testGenerarImagenQr() {
        byte[] qrBytes = documentoPdfService.generarImagenQr("LIC-2026-A1B2C3D4", 150, 150);

        assertNotNull(qrBytes);
        assertTrue(qrBytes.length > 200);
        assertEquals((byte) 0x89, qrBytes[0]);
        assertEquals((byte) 'P', qrBytes[1]);
        assertEquals((byte) 'N', qrBytes[2]);
        assertEquals((byte) 'G', qrBytes[3]);
    }
}
