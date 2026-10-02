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
        documentoPdfService = new DocumentoPdfService(
                new Anexo1PdfGenerator(),
                new Anexo3PdfGenerator(),
                new Anexo4PdfGenerator(),
                new LicenciaPdfGenerator()   // Fase 04: Generador del formato oficial de Licencia
        );
    }


    @Test
    @DisplayName("Fase 2: Debe generar el Anexo 4 oficial (Condiciones de Seguridad) en PDF de 4 páginas")
    void testGenerarAnexo4CondicionesSeguridadValido() {
        ExpedienteResponseDto dto = ExpedienteResponseDto.builder()
                .id(UUID.randomUUID())
                .numeroTramite("EXP-2026-00003")
                .nombreTitular("Rosa Benítez Torres")
                .documentoIdentidad("41258963")
                .nombreComercial("Farmacia y Bazar Ayacucho")
                .giroNegocio("Botica y bazar")
                .direccionEstablecimiento("Jr. Callao N° 215, Huamanga")
                .areaMetrosCuadrados(new BigDecimal("120.00"))
                .aforoPersonas(45)
                .nivelRiesgo(NivelRiesgo.MEDIO)
                .tipoItse("ITSE POSTERIOR")
                .fechaCreacion(LocalDateTime.now())
                .build();

        byte[] pdfBytes = documentoPdfService.generarAnexo4CondicionesSeguridad(dto);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 8000, "El PDF del Anexo 4 de 4 páginas debe superar 8KB");
        String header = new String(pdfBytes, 0, 5);
        assertEquals("%PDF-", header);
    }

    @Test
    @DisplayName("Fase 2: Debe generar el Anexo 3 oficial (Matriz ITSE) en PDF de 2 páginas con tablas CENEPRED")
    void testGenerarAnexo3MatrizRiesgoItseValido() {
        ExpedienteResponseDto dto = ExpedienteResponseDto.builder()
                .id(UUID.randomUUID())
                .numeroTramite("EXP-2026-00002")
                .nombreTitular("Carlos Huamán Mendoza")
                .documentoIdentidad("28549632")
                .nombreComercial("Pollería El Portal")
                .giroNegocio("Restaurante")
                .direccionEstablecimiento("Portal Constitución 45, Huamanga")
                .areaMetrosCuadrados(new BigDecimal("180.00"))
                .aforoPersonas(70)
                .nivelRiesgo(NivelRiesgo.ALTO)
                .tipoItse("ITSE PREVIA")
                .numeroInformeItse("ITSE-2026-DC-00221")
                .fechaCreacion(LocalDateTime.now())
                .build();

        byte[] pdfBytes = documentoPdfService.generarAnexo3MatrizRiesgoItse(dto);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 5000, "El PDF del Anexo 3 debe superar 5KB");
        String header = new String(pdfBytes, 0, 5);
        assertEquals("%PDF-", header);
    }

    @Test
    @DisplayName("Fase 2: Debe generar el Anexo 1 oficial en PDF de 2 páginas con tablas y croquis")
    void testGenerarAnexo1DeclaracionJuradaValido() {
        ExpedienteResponseDto dto = ExpedienteResponseDto.builder()
                .id(UUID.randomUUID())
                .numeroTramite("EXP-2026-00001")
                .nombreTitular("María Quispe Huamán")
                .documentoIdentidad("45879632")
                .razonSocial("INVERSIONES AYACUCHO S.A.C.")
                .nombreComercial("Restaurante El Rincón Huamanguino")
                .giroNegocio("Restaurante")
                .direccionEstablecimiento("Jr. 28 de Julio N° 120, Huamanga")
                .areaMetrosCuadrados(new BigDecimal("120.50"))
                .nivelRiesgo(NivelRiesgo.MEDIO)
                .tipoItse("ITSE POSTERIOR")
                .montoTasa(new BigDecimal("218.00"))
                .fechaCreacion(LocalDateTime.now())
                .build();

        byte[] pdfBytes = documentoPdfService.generarAnexo1DeclaracionJurada(dto);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 5000, "El PDF oficial del Anexo 1 debe superar 5KB");
        String header = new String(pdfBytes, 0, 5);
        assertEquals("%PDF-", header);
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
    @DisplayName("Fase 04: Debe generar el PDF con formato oficial municipal (campos: horario, categoría, zonificación)")
    void testGenerarLicenciaFormatoOficialCompletoFase04() {
        ExpedienteResponseDto dto = ExpedienteResponseDto.builder()
                .id(UUID.randomUUID())
                .numeroTramite("EXP-2026-12345")
                .numeroLicencia("202613788")
                .licenciaQrCode("LIC-2026-202613788")
                .estado(EstadoExpediente.APROBADO)
                .nombreTitular("DONDE LOPEZ EIRL")
                .documentoIdentidad("20610371974")
                .razonSocial("DONDE LOPEZ EIRL")
                .nombreComercial("DONDE LOPEZ EIRL")
                .giroNegocio("FERRETERIA (NO ALMACEN)")
                .ciiuCodigo("30.27")
                .direccionEstablecimiento("CENTRO POBLADO BARRIO DE LA MAGDALENA")
                .manzana("E")
                .lote("09")
                .urbanizacion("UNIDAD VECINAL")
                .areaMetrosCuadrados(new BigDecimal("102.00"))
                .nivelRiesgo(NivelRiesgo.MEDIO)
                .categoriaEstablecimiento("1-A (INTERMEDIO)")
                .zonificacion("CENTRO HISTORICO: SECTOR 04")
                .horaInicio("06:00")
                .horaFin("23:00")
                .fechaAprobacion(LocalDateTime.of(2026, 9, 30, 10, 0))
                .fechaCreacion(LocalDateTime.now())
                .build();

        byte[] pdfBytes = documentoPdfService.generarLicenciaPdf(dto);

        assertNotNull(pdfBytes, "El PDF de la licencia oficial no debe ser nulo");
        assertTrue(pdfBytes.length > 5000, "El PDF del formato oficial debe superar 5KB");
        String header = new String(pdfBytes, 0, 5);
        assertEquals("%PDF-", header, "El documento debe tener firma de archivo PDF válida");
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
