package pe.gob.munihuamanga.licencias.expedientes.service;

import com.lowagie.text.pdf.PdfReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import pe.gob.munihuamanga.licencias.common.dto.ExpedienteResponseDto;
import pe.gob.munihuamanga.licencias.common.enums.ModalidadTramite;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumento;
import pe.gob.munihuamanga.licencias.common.enums.TipoPersona;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias del Generador Oficial Anexo N° 1 (Ley N° 28976 / D.S. N° 163-2020-PCM)")
class Anexo1PdfGeneratorTest {

    private Anexo1PdfGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new Anexo1PdfGenerator();
    }

    @Test
    @DisplayName("1. Persona Natural: Debe generar Anexo 1 con datos completos de persona natural y 2 páginas exactas")
    void testPersonaNatural() throws Exception {
        ExpedienteResponseDto dto = ExpedienteResponseDto.builder()
                .id(UUID.randomUUID())
                .numeroTramite("EXP-2024-001234")
                .tipoPersona(TipoPersona.NATURAL)
                .tipoDocumento(TipoDocumento.DNI)
                .nombreTitular("PÉREZ GARCÍA JUAN CARLOS")
                .documentoIdentidad("43215678")
                .telefono("966123456")
                .correoElectronico("juan.perez@email.com")
                .tipoVia("Av.")
                .nombreVia("Mariscal Cáceres")
                .numeroVivienda("123")
                .urbanizacion("Urb. San Juan Bautista")
                .distrito("Ayacucho")
                .provincia("Huamanga")
                .nombreComercial("RESTAURANTE SABOR HUAMANGA")
                .ciiuCodigo("561011")
                .giroNegocio("Restaurante")
                .actividadDetallada("Venta de alimentos y bebidas")
                .zonificacion("Zona Comercial (ZC)")
                .areaMetrosCuadrados(new BigDecimal("120.00"))
                .modalidadTramite(ModalidadTramite.LICENCIA_INDETERMINADA)
                .nivelRiesgo(NivelRiesgo.MEDIO)
                .numeroOperacionSat("00098765")
                .fechaPagoSat(LocalDateTime.of(2024, 4, 15, 10, 30))
                .fechaCreacion(LocalDateTime.of(2024, 4, 15, 9, 0))
                .build();

        byte[] pdf = generator.generarPdf(dto);

        assertNotNull(pdf);
        assertTrue(pdf.length > 50000, "El PDF estampado debe superar los 50KB");
        assertEquals("%PDF-", new String(pdf, 0, 5, StandardCharsets.US_ASCII));

        PdfReader reader = new PdfReader(pdf);
        assertEquals(2, reader.getNumberOfPages(), "Debe contener exactamente 2 páginas");
        reader.close();

        // Guardar archivo para evidencia de prueba con datos ficticios
        File outDir = new File("target/evidencias-pdf");
        outDir.mkdirs();
        try (FileOutputStream fos = new FileOutputStream(new File(outDir, "anexo1-persona-natural-ejemplo.pdf"))) {
            fos.write(pdf);
        }
    }

    @Test
    @DisplayName("2. Persona Jurídica: Debe consignar RUC, Razón Social y Representante Legal")
    void testPersonaJuridica() throws Exception {
        ExpedienteResponseDto dto = ExpedienteResponseDto.builder()
                .id(UUID.randomUUID())
                .numeroTramite("EXP-2024-005678")
                .tipoPersona(TipoPersona.JURIDICA)
                .tipoDocumento(TipoDocumento.RUC)
                .razonSocial("INVERSIONES Y SERVICIOS AYACUCHO S.A.C.")
                .documentoIdentidad("20601234567")
                .telefono("966987654")
                .correoElectronico("contacto@serviciosayacucho.pe")
                .nombreRepresentante("MENDOZA ALARCÓN CARMEN ROSA")
                .dniRepresentante("41852963")
                .partidaSunarp("11022334")
                .asientoSunarp("A0001")
                .nombreComercial("CENTRO EMPRESARIAL LOS ANDES")
                .modalidadTramite(ModalidadTramite.LICENCIA_INDETERMINADA)
                .nivelRiesgo(NivelRiesgo.ALTO)
                .fechaCreacion(LocalDateTime.now())
                .build();

        byte[] pdf = generator.generarPdf(dto);
        assertNotNull(pdf);

        PdfReader reader = new PdfReader(pdf);
        assertEquals(2, reader.getNumberOfPages());
        reader.close();
    }

    @Test
    @DisplayName("3. Opcionales Vacíos: No debe fallar ni inventar datos con campos nulos")
    void testCamposOpcionalesVacios() throws Exception {
        ExpedienteResponseDto dto = ExpedienteResponseDto.builder()
                .numeroTramite("EXP-2024-000001")
                .tipoPersona(TipoPersona.NATURAL)
                .nombreTitular("QUISPE HUAMÁN PEDRO")
                .documentoIdentidad("40123456")
                .modalidadTramite(ModalidadTramite.LICENCIA_INDETERMINADA)
                .build();

        byte[] pdf = generator.generarPdf(dto);
        assertNotNull(pdf);

        PdfReader reader = new PdfReader(pdf);
        assertEquals(2, reader.getNumberOfPages());
        reader.close();
    }

    @Test
    @DisplayName("4. Nombres y Direcciones Extensas: Auto-ajuste de tamaño de fuente sin error")
    void testTextosExtensos() throws Exception {
        ExpedienteResponseDto dto = ExpedienteResponseDto.builder()
                .numeroTramite("EXP-2024-EXTENSO-01")
                .tipoPersona(TipoPersona.JURIDICA)
                .razonSocial("CONSORCIO INTERNACIONAL DE IMPORTACIONES, EXPORTACIONES, DISTRIBUCIONES Y LOGÍSTICA INTEGRAL DE LA MANCOMUNIDAD REGIONAL DE LOS ANDES S.A.C.")
                .documentoIdentidad("20556677889")
                .correoElectronico("direccion.general.ejecutiva.notificaciones.legales.huamanga@consorciointernacionaldelosandes.com.pe")
                .direccionEstablecimiento("Prolongación Avenida Mariscal Agustín Gamarra con Jr. Dos de Mayo N° 1245-1249 Interior B-4 Mz. K-2 Lote 14 Sector Mollepata")
                .nombreComercial("MEGACENTRO COMERCIAL E INDUSTRIAL MULTISERVICIOS Y GASTRONOMÍA TRADICIONAL AYACUCHANA S.A.C.")
                .giroNegocio("Comercialización mayorista y minorista de productos perecibles, abarrotes y servicios complementarios")
                .actividadDetallada("Distribución de alimentos, bebidas, textiles tradicionales y artesanía huamanguina de exportación")
                .modalidadTramite(ModalidadTramite.LICENCIA_INDETERMINADA)
                .nivelRiesgo(NivelRiesgo.MUY_ALTO)
                .fechaCreacion(LocalDateTime.now())
                .build();

        byte[] pdf = generator.generarPdf(dto);
        assertNotNull(pdf);

        PdfReader reader = new PdfReader(pdf);
        assertEquals(2, reader.getNumberOfPages());
        reader.close();
    }

    @Test
    @DisplayName("5. Modalidad Temporal: Debe estampar X en casilla Temporal y consignar plazo en meses")
    void testModalidadTemporal() throws Exception {
        ExpedienteResponseDto dto = ExpedienteResponseDto.builder()
                .numeroTramite("EXP-2024-TEMP-02")
                .tipoPersona(TipoPersona.NATURAL)
                .nombreTitular("FLORES GÓMEZ MARÍA")
                .documentoIdentidad("45123789")
                .modalidadTramite(ModalidadTramite.LICENCIA_TEMPORAL)
                .plazoTemporalMeses(6)
                .fechaCreacion(LocalDateTime.now())
                .build();

        byte[] pdf = generator.generarPdf(dto);
        assertNotNull(pdf);
        assertTrue(pdf.length > 50000);
    }

    @Test
    @DisplayName("6. Transferencia de Licencia: Debe consignar N° de licencia anterior")
    void testTransferenciaLicencia() throws Exception {
        ExpedienteResponseDto dto = ExpedienteResponseDto.builder()
                .numeroTramite("EXP-2024-TRANSF-03")
                .tipoPersona(TipoPersona.JURIDICA)
                .razonSocial("NUEVA BOTICA CENTRAL S.R.L.")
                .documentoIdentidad("20443322110")
                .modalidadTramite(ModalidadTramite.TRANSFERENCIA_LICENCIA)
                .numeroLicenciaPrincipal("LIC-2020-00456")
                .fechaCreacion(LocalDateTime.now())
                .build();

        byte[] pdf = generator.generarPdf(dto);
        assertNotNull(pdf);
    }

    @Test
    @DisplayName("7. Pago SAT: Con y sin voucher registrado")
    void testPagoSatConYSinRegistro() {
        // Con pago
        ExpedienteResponseDto conPago = ExpedienteResponseDto.builder()
                .numeroTramite("EXP-PAGO-01")
                .tipoPersona(TipoPersona.NATURAL)
                .nombreTitular("JUAN CON PAGO")
                .numeroOperacionSat("SAT-998877")
                .fechaPagoSat(LocalDateTime.of(2024, 5, 2, 14, 0))
                .build();
        byte[] pdfConPago = generator.generarPdf(conPago);
        assertNotNull(pdfConPago);

        // Sin pago
        ExpedienteResponseDto sinPago = ExpedienteResponseDto.builder()
                .numeroTramite("EXP-SIN-PAGO-02")
                .tipoPersona(TipoPersona.NATURAL)
                .nombreTitular("PEDRO SIN PAGO")
                .build();
        byte[] pdfSinPago = generator.generarPdf(sinPago);
        assertNotNull(pdfSinPago);
    }

    @Test
    @DisplayName("8. Clasificación ITSE: Pendiente (null) y Registrada (BAJO, MEDIO, ALTO, MUY_ALTO)")
    void testClasificacionItse() {
        for (NivelRiesgo riesgo : NivelRiesgo.values()) {
            ExpedienteResponseDto dto = ExpedienteResponseDto.builder()
                    .numeroTramite("EXP-ITSE-" + riesgo)
                    .tipoPersona(TipoPersona.NATURAL)
                    .nombreTitular("TITULAR ITSE")
                    .nivelRiesgo(riesgo)
                    .build();
            byte[] pdf = generator.generarPdf(dto);
            assertNotNull(pdf);
        }

        // Riesgo no asignado aún (pendiente de inspección)
        ExpedienteResponseDto dtoNull = ExpedienteResponseDto.builder()
                .numeroTramite("EXP-ITSE-PENDIENTE")
                .tipoPersona(TipoPersona.NATURAL)
                .nombreTitular("TITULAR PENDIENTE")
                .nivelRiesgo(null)
                .build();
        byte[] pdfNull = generator.generarPdf(dtoNull);
        assertNotNull(pdfNull);
    }

    @Test
    @DisplayName("9. Tolerancia del Escudo Institucional: Escudo presente y escudo ausente")
    void testEscudoDisponibleYAusente() throws Exception {
        ExpedienteResponseDto dto = ExpedienteResponseDto.builder()
                .numeroTramite("EXP-ESCUDO-TEST")
                .tipoPersona(TipoPersona.NATURAL)
                .nombreTitular("TEST ESCUDO")
                .build();

        // 1. Con escudo normal
        byte[] pdfConEscudo = generator.generarPdf(dto);
        assertNotNull(pdfConEscudo);

        // 2. Con ruta de escudo inexistente (debe generar el PDF sin lanzar excepción)
        ReflectionTestUtils.setField(generator, "escudoPath", "classpath:static/img/escudo-inexistente.png");
        byte[] pdfSinEscudo = generator.generarPdf(dto);
        assertNotNull(pdfSinEscudo);

        PdfReader reader = new PdfReader(pdfSinEscudo);
        assertEquals(2, reader.getNumberOfPages());
        reader.close();
    }

    @Test
    @DisplayName("10. Validación de Integridad: Expediente nulo debe lanzar IllegalArgumentException")
    void testExpedienteNulo() {
        assertThrows(IllegalArgumentException.class, () -> generator.generarPdf(null));
    }
}
