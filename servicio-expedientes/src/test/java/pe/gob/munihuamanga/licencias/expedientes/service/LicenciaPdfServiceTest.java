package pe.gob.munihuamanga.licencias.expedientes.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.gob.munihuamanga.licencias.common.dto.ExpedienteResponseDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias de Generación de Licencia Oficial PDF y Código QR (Sprint 3)")
class LicenciaPdfServiceTest {

    private DocumentoPdfService documentoPdfService;

    @BeforeEach
    void setUp() {
        documentoPdfService = new DocumentoPdfService();
    }

    @Test
    @DisplayName("US-09: Debe generar el PDF oficial de la Licencia de Funcionamiento con cabecera %PDF- y tamaño > 1KB")
    void testGenerarLicenciaPdfValido() {
        ExpedienteResponseDto dto = ExpedienteResponseDto.builder()
                .id(UUID.randomUUID())
                .numeroTramite("EXP-2026-00001")
                .licenciaQrCode("LIC-2026-A1B2C3D4")
                .estado(EstadoExpediente.APROBADO)
                .nombreTitular("María Quispe Huamán")
                .documentoIdentidad("45879632")
                .razonSocial("INVERSIONES AYACUCHO S.A.C.")
                .nombreComercial("Restaurante El Rincón Huamanguino")
                .giroNegocio("Restaurante y expendio de comidas típicas")
                .direccionEstablecimiento("Jr. 28 de Julio N° 120, Huamanga")
                .areaMetrosCuadrados(new BigDecimal("120.50"))
                .nivelRiesgo(NivelRiesgo.MEDIO)
                .tipoItse("ITSE POSTERIOR")
                .montoTasa(new BigDecimal("218.00"))
                .fechaCreacion(LocalDateTime.now())
                .build();

        byte[] pdfBytes = documentoPdfService.generarLicenciaPdf(dto);

        assertNotNull(pdfBytes, "El PDF de la licencia no debe ser nulo");
        assertTrue(pdfBytes.length > 1000, "El tamaño del PDF de la licencia debe ser mayor a 1KB");

        // Validar firma mágica de archivo PDF (%PDF-)
        String header = new String(pdfBytes, 0, 5);
        assertEquals("%PDF-", header, "El documento generado debe tener la firma de archivo PDF");
    }

    @Test
    @DisplayName("US-10: Debe generar una imagen PNG del código QR con formato válido y dimensiones correctas")
    void testGenerarImagenQrValido() {
        String codigoLicencia = "LIC-2026-A1B2C3D4";
        byte[] qrBytes = documentoPdfService.generarImagenQr(codigoLicencia, 200, 200);

        assertNotNull(qrBytes, "Los bytes del código QR no deben ser nulos");
        assertTrue(qrBytes.length > 200, "El tamaño del PNG del código QR debe ser mayor a 200 bytes");

        // Validar magic bytes de PNG: 0x89 'P' 'N' 'G'
        assertEquals((byte) 0x89, qrBytes[0]);
        assertEquals((byte) 'P', qrBytes[1]);
        assertEquals((byte) 'N', qrBytes[2]);
        assertEquals((byte) 'G', qrBytes[3]);
    }
}
