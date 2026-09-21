package pe.gob.munihuamanga.licencias.formularios.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.gob.munihuamanga.licencias.common.dto.ExpedienteResponseDto;
import pe.gob.munihuamanga.licencias.common.dto.VoucherDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneradorDocumentoServiceTest {

    private GeneradorDocumentoService generador;

    @BeforeEach
    void setUp() {
        generador = new GeneradorDocumentoService();
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

        byte[] pdfBytes = generador.generarDeclaracionJurada(exp);

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

        byte[] pdfBytes = generador.generarSolicitudItsePdf(exp);

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

        byte[] pdfBytes = generador.generarVoucherSatPdf(voucher);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 1000);
        String pdfHeader = new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII);
        assertTrue(pdfHeader.startsWith("%PDF-"));
    }
}
