package pe.gob.munihuamanga.licencias.expedientes.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Pruebas Unitarias del Servicio de Verificación y Generación de QR (RNF-13, RNF-20)")
class QrGeneratorServiceTest {

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
    @DisplayName("US-10: Debe generar imagen PNG válida del código QR con dimensiones especificadas")
    void testGenerarImagenQr() {
        String codigoLicencia = "LIC-2026-B9C8D7E6";
        byte[] qrBytes = documentoPdfService.generarImagenQr(codigoLicencia, 250, 250);

        assertNotNull(qrBytes);
        assertTrue(qrBytes.length > 200, "El PNG generado debe tener un tamaño superior a 200 bytes");

        // Validar magic bytes de PNG
        assertEquals((byte) 0x89, qrBytes[0]);
        assertEquals((byte) 'P', qrBytes[1]);
        assertEquals((byte) 'N', qrBytes[2]);
        assertEquals((byte) 'G', qrBytes[3]);
    }
}
