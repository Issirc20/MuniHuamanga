package pe.gob.munihuamanga.licencias.expedientes.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.dto.Anexo1ItseDto;
import pe.gob.munihuamanga.licencias.common.dto.ExpedienteResponseDto;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generador PDF Oficial del Anexo 1 ITSE:
 * SOLICITUD DE INSPECCIÓN TÉCNICA DE SEGURIDAD EN EDIFICACIONES (ITSE) Y DE
 * EVALUACIÓN DE CONDICIONES DE SEGURIDAD EN ESPECTÁCULOS PÚBLICOS DEPORTIVOS Y NO DEPORTIVOS (ECSE)
 * Conforme al D.S. N° 002-2018-PCM y la R.J. N° 016-2018-CENEPRED/J.
 * Genera fielmente una (1) página A4 idéntica a la plantilla de Defensa Civil.
 */
@Slf4j
@Component
public class Anexo1ItsePdfGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Paleta institucional Defensa Civil / Municipalidad de Huamanga
    private static final Color COLOR_PRIMARY = new Color(0, 43, 92);       // #002b5c
    private static final Color COLOR_SEC_BAR = new Color(30, 58, 138);     // Azul institucional
    private static final Color COLOR_BORDER = new Color(100, 116, 139);    // #64748b
    private static final Color COLOR_BG_CELL = new Color(248, 250, 252);
    private static final Color COLOR_YELLOW_TINT = new Color(255, 253, 231);

    // Tipografías compactas para formato oficial de 1 sola página
    private static final Font F_INST = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.5f, COLOR_PRIMARY);
    private static final Font F_DOC_CODE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, COLOR_PRIMARY);
    private static final Font F_DOC_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.2f, Color.BLACK);
    private static final Font F_DOC_SUB = FontFactory.getFont(FontFactory.HELVETICA, 5.5f, new Color(51, 65, 85));

    private static final Font F_SEC_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.2f, Color.WHITE);
    private static final Font F_SUB_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 5.5f, COLOR_PRIMARY);
    private static final Font F_LBL = FontFactory.getFont(FontFactory.HELVETICA, 5.2f, Color.DARK_GRAY);
    private static final Font F_VAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 5.8f, Color.BLACK);
    private static final Font F_CHECK = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.5f, COLOR_PRIMARY);

    private final ResourceLoader resourceLoader;

    @Value("${muni.documentos.escudo-path:classpath:static/img/escudo-huamanga.png}")
    private String escudoPath;

    public Anexo1ItsePdfGenerator(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public Anexo1ItsePdfGenerator() {
        this.resourceLoader = null;
        this.escudoPath = "classpath:static/img/escudo-huamanga.png";
    }

    public byte[] generarPdf(ExpedienteResponseDto exp) {
        Anexo1ItseDto dto = convertirExpedienteADto(exp);
        return generarPdf(dto, exp != null ? exp.getNumeroTramite() : "EXP-BORRADOR");
    }

    public byte[] generarPdf(Anexo1ItseDto dto, String numeroTramite) {
        Document document = new Document(PageSize.A4, 15f, 15f, 12f, 12f);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // 1. Encabezado Oficial
            agregarEncabezado(document, numeroTramite);

            // 2. Sección I: Información General
            agregarSeccionI(document, dto);

            // 3. Sección II: Datos del Solicitante
            agregarSeccionII(document, dto);

            // 4. Sección III: Datos del Establecimiento Objeto de Inspección
            agregarSeccionIII(document, dto);

            // 5. Sección IV: Documentos Presentados
            agregarSeccionIV(document, dto);

            // 6. Firmas y Cargo de Recepción
            agregarFirmas(document, dto);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error al generar PDF de Anexo 1 ITSE: {}", e.getMessage(), e);
            throw new RuntimeException("Error en renderizado de Anexo 1 ITSE PDF: " + e.getMessage(), e);
        }
    }

    private void agregarEncabezado(Document document, String numeroTramite) throws Exception {
        PdfPTable header = new PdfPTable(new float[]{1.2f, 3.8f, 1.6f});
        header.setWidthPercentage(100);
        header.setSpacingAfter(4f);

        // Logo
        PdfPCell cLogo = new PdfPCell();
        cLogo.setBorder(Rectangle.BOX);
        cLogo.setBorderColor(COLOR_BORDER);
        cLogo.setPadding(3f);
        cLogo.setHorizontalAlignment(Element.ALIGN_CENTER);
        cLogo.setVerticalAlignment(Element.ALIGN_MIDDLE);

        try {
            Image logo = obtenerEscudoImage();
            if (logo != null) {
                logo.scaleToFit(38, 38);
                cLogo.addElement(logo);
            }
        } catch (Exception ignored) {}
        header.addCell(cLogo);

        // Título Central
        PdfPCell cTit = new PdfPCell();
        cTit.setBorder(Rectangle.BOX);
        cTit.setBorderColor(COLOR_BORDER);
        cTit.setPadding(3f);
        cTit.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph pInst = new Paragraph("MUNICIPALIDAD PROVINCIAL DE HUAMANGA\nSUBGERENCIA DE DEFENSA CIVIL", F_INST);
        pInst.setAlignment(Element.ALIGN_CENTER);
        cTit.addElement(pInst);

        Paragraph pCod = new Paragraph("ANEXO 1", F_DOC_CODE);
        pCod.setAlignment(Element.ALIGN_CENTER);
        cTit.addElement(pCod);

        Paragraph pMain = new Paragraph("SOLICITUD DE INSPECCIÓN TÉCNICA DE SEGURIDAD EN EDIFICACIONES (ITSE)\nY DE EVALUACIÓN DE CONDICIONES DE SEGURIDAD EN ESPECTÁCULOS (ECSE)", F_DOC_TITLE);
        pMain.setAlignment(Element.ALIGN_CENTER);
        cTit.addElement(pMain);

        Paragraph pSub = new Paragraph("Conforme al D.S. N° 002-2018-PCM y la Resolución Jefatural N° 016-2018-CENEPRED/J", F_DOC_SUB);
        pSub.setAlignment(Element.ALIGN_CENTER);
        cTit.addElement(pSub);

        header.addCell(cTit);

        // Meta Box
        PdfPCell cMeta = new PdfPCell();
        cMeta.setBorder(Rectangle.BOX);
        cMeta.setBorderColor(COLOR_BORDER);
        cMeta.setPadding(3f);
        cMeta.setBackgroundColor(COLOR_BG_CELL);

        Paragraph pOrg = new Paragraph("ÓRGANO EJECUTANTE:", F_LBL);
        Paragraph pOrgVal = new Paragraph("Defensa Civil - MPH", F_VAL);
        Paragraph pExp = new Paragraph("N° EXPEDIENTE:", F_LBL);
        Paragraph pExpVal = new Paragraph(numeroTramite != null ? numeroTramite : "EN TRÁMITE", F_VAL);

        cMeta.addElement(pOrg);
        cMeta.addElement(pOrgVal);
        cMeta.addElement(pExp);
        cMeta.addElement(pExpVal);

        header.addCell(cMeta);

        document.add(header);
    }

    private void agregarSeccionI(Document document, Anexo1ItseDto dto) throws Exception {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingAfter(4f);

        PdfPCell cBar = new PdfPCell(new Phrase("I. INFORMACIÓN GENERAL", F_SEC_HEADER));
        cBar.setBackgroundColor(COLOR_SEC_BAR);
        cBar.setPadding(2.5f);
        t.addCell(cBar);

        // Subtabla interna
        PdfPTable sub = new PdfPTable(new float[]{2.5f, 2.5f, 2.5f, 2.5f});
        sub.setWidthPercentage(100);

        // I.1 Tipo de ITSE
        boolean esPosterior = dto.getTipoItse() == null || dto.getTipoItse().equalsIgnoreCase("POSTERIOR");
        boolean esPrevia = dto.getTipoItse() != null && dto.getTipoItse().equalsIgnoreCase("PREVIA");
        boolean esReno = dto.getTipoItse() != null && dto.getTipoItse().equalsIgnoreCase("RENOVACION");

        PdfPCell c1 = celdaCampo("I.1 TIPO DE ITSE:",
                String.format("[%s] Posterior al inicio\n[%s] Previa al inicio\n[%s] Renovación Certificado",
                        esPosterior ? "X" : " ", esPrevia ? "X" : " ", esReno ? "X" : " "));
        sub.addCell(c1);

        // I.2 ECSE
        boolean ecse3000 = "HASTA_3000".equalsIgnoreCase(dto.getTipoEcse());
        boolean ecseMayor = "MAYOR_3000".equalsIgnoreCase(dto.getTipoEcse());
        PdfPCell c2 = celdaCampo("I.2 ECSE (EVENTOS):",
                String.format("[%s] Hasta 3,000 personas\n[%s] Mayor a 3,000 personas\n[%s] No Aplica (Comercio)",
                        ecse3000 ? "X" : " ", ecseMayor ? "X" : " ", (!ecse3000 && !ecseMayor) ? "X" : " "));
        sub.addCell(c2);

        // I.3 Función
        String f = dto.getFuncionEdificacion() != null ? dto.getFuncionEdificacion().toUpperCase() : "COMERCIO";
        PdfPCell c3 = celdaCampo("I.3 FUNCIÓN:",
                String.format("[%s] Comercio  [%s] Encuentro\n[%s] Hospedaje [%s] Educación\n[%s] Industrial [%s] Oficinas",
                        f.contains("COMERCIO") ? "X" : " ", f.contains("ENCUENTRO") ? "X" : " ",
                        f.contains("HOSPEDAJE") ? "X" : " ", f.contains("EDUCACION") ? "X" : " ",
                        f.contains("IND") ? "X" : " ", f.contains("OFICINA") ? "X" : " "));
        sub.addCell(c3);

        // I.4 Nivel de Riesgo
        NivelRiesgo r = dto.getNivelRiesgo() != null ? dto.getNivelRiesgo() : NivelRiesgo.MEDIO;
        PdfPCell c4 = celdaCampo("I.4 CLASIFICACIÓN DEL RIESGO:",
                String.format("[%s] Riesgo Bajo\n[%s] Riesgo Medio\n[%s] Riesgo Alto\n[%s] Riesgo Muy Alto",
                        r == NivelRiesgo.BAJO ? "X" : " ", r == NivelRiesgo.MEDIO ? "X" : " ",
                        r == NivelRiesgo.ALTO ? "X" : " ", r == NivelRiesgo.MUY_ALTO ? "X" : " "));
        sub.addCell(c4);

        PdfPCell cSub = new PdfPCell(sub);
        cSub.setPadding(0);
        cSub.setBorder(Rectangle.NO_BORDER);
        t.addCell(cSub);

        document.add(t);
    }

    private void agregarSeccionII(Document document, Anexo1ItseDto dto) throws Exception {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingAfter(4f);

        PdfPCell cBar = new PdfPCell(new Phrase("II. DATOS DEL SOLICITANTE", F_SEC_HEADER));
        cBar.setBackgroundColor(COLOR_SEC_BAR);
        cBar.setPadding(2.5f);
        t.addCell(cBar);

        PdfPTable sub = new PdfPTable(new float[]{2.5f, 2f, 2.5f, 3f});
        sub.setWidthPercentage(100);

        String cond = dto.getCondicionSolicitante() != null ? dto.getCondicionSolicitante() : "CONDUCTOR";
        sub.addCell(celdaCampo("CONDICIÓN DEL SOLICITANTE:", cond.replace("_", " ")));
        sub.addCell(celdaCampo("DNI / C.E. N°:", dto.getDniCeSolicitante() != null ? dto.getDniCeSolicitante() : "-"));
        sub.addCell(celdaCampo("NOMBRES Y APELLIDOS:", dto.getNombresSolicitante() != null ? dto.getNombresSolicitante() : "-"));
        sub.addCell(celdaCampo("CORREO / TELÉFONO:", (dto.getCorreoSolicitante() != null ? dto.getCorreoSolicitante() : "-") + " | " + (dto.getTelefonoSolicitante() != null ? dto.getTelefonoSolicitante() : "-")));

        sub.addCell(celdaCampoColspan("DOMICILIO DEL SOLICITANTE:", dto.getDomicilioSolicitante() != null ? dto.getDomicilioSolicitante() : "Huamanga, Ayacucho", 4));

        PdfPCell cSub = new PdfPCell(sub);
        cSub.setPadding(0);
        cSub.setBorder(Rectangle.NO_BORDER);
        t.addCell(cSub);

        document.add(t);
    }

    private void agregarSeccionIII(Document document, Anexo1ItseDto dto) throws Exception {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingAfter(4f);

        PdfPCell cBar = new PdfPCell(new Phrase("III. DATOS ADMINISTRATIVOS DEL ESTABLECIMIENTO OBJETO DE INSPECCIÓN", F_SEC_HEADER));
        cBar.setBackgroundColor(COLOR_SEC_BAR);
        cBar.setPadding(2.5f);
        t.addCell(cBar);

        PdfPTable sub = new PdfPTable(new float[]{3f, 2f, 3f, 2f});
        sub.setWidthPercentage(100);

        sub.addCell(celdaCampo("RAZÓN SOCIAL:", dto.getRazonSocial() != null ? dto.getRazonSocial() : dto.getNombresSolicitante()));
        sub.addCell(celdaCampo("RUC N°:", dto.getRuc() != null ? dto.getRuc() : "-"));
        sub.addCell(celdaCampo("NOMBRE COMERCIAL:", dto.getNombreComercial() != null ? dto.getNombreComercial() : "-"));
        sub.addCell(celdaCampo("GIRO / ACTIVIDAD:", dto.getGiroActividades() != null ? dto.getGiroActividades() : "-"));

        sub.addCell(celdaCampoColspan("DIRECCIÓN / UBICACIÓN:", dto.getDireccionEstablecimiento() != null ? dto.getDireccionEstablecimiento() : "-", 2));
        sub.addCell(celdaCampo("REFERENCIA:", dto.getReferenciaDireccion() != null ? dto.getReferenciaDireccion() : "-"));
        sub.addCell(celdaCampo("DISTRITO / PROVINCIA:", (dto.getDistrito() != null ? dto.getDistrito() : "Ayacucho") + " / " + (dto.getProvincia() != null ? dto.getProvincia() : "Huamanga")));

        sub.addCell(celdaCampo("N° PISOS EDIFICACIÓN:", dto.getNumeroPisos() != null ? dto.getNumeroPisos() + " pisos" : "1 piso"));
        sub.addCell(celdaCampo("PISO OBJETO INSPECCIÓN:", dto.getPisoUbicacion() != null ? dto.getPisoUbicacion() : "1er Piso"));
        sub.addCell(celdaCampo("ÁREA OCUPADA TOTAL (M²):", dto.getAreaOcupadaTotal() != null ? dto.getAreaOcupadaTotal() + " m²" : "85.00 m²"));
        sub.addCell(celdaCampo("HORARIO DE ATENCIÓN:", dto.getHorarioAtencion() != null ? dto.getHorarioAtencion() : "08:00 - 20:00"));

        PdfPCell cSub = new PdfPCell(sub);
        cSub.setPadding(0);
        cSub.setBorder(Rectangle.NO_BORDER);
        t.addCell(cSub);

        document.add(t);
    }

    private void agregarSeccionIV(Document document, Anexo1ItseDto dto) throws Exception {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingAfter(4f);

        PdfPCell cBar = new PdfPCell(new Phrase("IV. DOCUMENTOS PRESENTADOS (CONFORME AL D.S. N° 002-2018-PCM)", F_SEC_HEADER));
        cBar.setBackgroundColor(COLOR_SEC_BAR);
        cBar.setPadding(2.5f);
        t.addCell(cBar);

        PdfPTable sub = new PdfPTable(new float[]{5f, 5f});
        sub.setWidthPercentage(100);

        String colA = String.format(
                "[%s] a) Declaración Jurada de Cumplimiento de Condiciones (Anexo 4)\n" +
                "[%s] b) Croquis de ubicación del establecimiento\n" +
                "[%s] c) Plano de arquitectura de la distribución y cálculo de aforo\n" +
                "[%s] d) Plano de tableros eléctricos, diagramas unifilares y cuadro de cargas",
                Boolean.TRUE.equals(dto.getDocDeclaracionJurada()) ? "X" : " ",
                Boolean.TRUE.equals(dto.getDocCroquisUbicacion()) ? "X" : " ",
                Boolean.TRUE.equals(dto.getDocPlanoArquitecturaAforo()) ? "X" : " ",
                Boolean.TRUE.equals(dto.getDocPlanoTableros()) ? "X" : " "
        );

        String colB = String.format(
                "[%s] e) Plan de Seguridad del Objeto de Inspección\n" +
                "[%s] f) Certificado vigente de medición de resistencia de pozo a tierra\n" +
                "[%s] g) Protocolos de prueba de equipos de seguridad contra incendios\n" +
                "[%s] h) Recibo de pago de derecho de tramitación TUPA",
                Boolean.TRUE.equals(dto.getDocPlanSeguridad()) ? "X" : " ",
                Boolean.TRUE.equals(dto.getDocCertificadoPozoTierra()) ? "X" : " ",
                Boolean.TRUE.equals(dto.getDocPruebasOperatividad()) ? "X" : " ",
                Boolean.TRUE.equals(dto.getDocReciboPago()) ? "X" : " "
        );

        sub.addCell(celdaCampo("REQUISITOS GENERALES (A):", colA));
        sub.addCell(celdaCampo("REQUISITOS TÉCNICOS (B):", colB));

        PdfPCell cSub = new PdfPCell(sub);
        cSub.setPadding(0);
        cSub.setBorder(Rectangle.NO_BORDER);
        t.addCell(cSub);

        document.add(t);
    }

    private void agregarFirmas(Document document, Anexo1ItseDto dto) throws Exception {
        PdfPTable t = new PdfPTable(new float[]{5f, 5f});
        t.setWidthPercentage(100);

        // Cargo de recepción municipal
        PdfPCell cCargo = new PdfPCell();
        cCargo.setBorder(Rectangle.BOX);
        cCargo.setBorderColor(COLOR_BORDER);
        cCargo.setPadding(4f);
        cCargo.setBackgroundColor(COLOR_BG_CELL);

        Paragraph pTitCargo = new Paragraph("CARGO DE RECEPCIÓN (MUNICIPALIDAD):", F_SUB_HEADER);
        Paragraph pBodyCargo = new Paragraph(
                "Persona autorizada: Mesa de Partes / Defensa Civil MPH\n" +
                "Cargo: Calificador Técnico ITSE\n" +
                "Fecha y hora: " + LocalDate.now().format(DATE_FMT) + " [SISTEMA DIGITAL V2]\n\n" +
                "_________________________________________\n" +
                "Sello y Firma - Persona Autorizada Gobierno Local", F_LBL);

        cCargo.addElement(pTitCargo);
        cCargo.addElement(pBodyCargo);
        t.addCell(cCargo);

        // Firma del administrado
        PdfPCell cSol = new PdfPCell();
        cSol.setBorder(Rectangle.BOX);
        cSol.setBorderColor(COLOR_BORDER);
        cSol.setPadding(4f);
        cSol.setBackgroundColor(COLOR_BG_CELL);

        Paragraph pTitSol = new Paragraph("ADMINISTRADO / SOLICITANTE:", F_SUB_HEADER);
        String nomSol = dto.getNombresSolicitante() != null ? dto.getNombresSolicitante() : "ADMINISTRADO DECLARANTE";
        String dniSol = dto.getDniCeSolicitante() != null ? dto.getDniCeSolicitante() : "DNI REGISTRADO";

        Paragraph pBodySol = new Paragraph(
                "Declaración bajo fe de juramento según Ley N° 27444\n" +
                "Nombres: " + nomSol + "\n" +
                "DNI / C.E.: " + dniSol + "\n\n" +
                "_________________________________________\n" +
                "Firma del Solicitante / Representante Legal", F_LBL);

        cSol.addElement(pTitSol);
        cSol.addElement(pBodySol);
        t.addCell(cSol);

        document.add(t);
    }

    private PdfPCell celdaCampo(String label, String valor) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.BOX);
        c.setBorderColor(COLOR_BORDER);
        c.setPadding(2.5f);
        c.setBackgroundColor(COLOR_YELLOW_TINT);

        Paragraph pLbl = new Paragraph(label, F_LBL);
        Paragraph pVal = new Paragraph(valor != null ? valor : "-", F_VAL);
        c.addElement(pLbl);
        c.addElement(pVal);
        return c;
    }

    private PdfPCell celdaCampoColspan(String label, String valor, int colspan) {
        PdfPCell c = celdaCampo(label, valor);
        c.setColspan(colspan);
        return c;
    }

    private Image obtenerEscudoImage() {
        try {
            if (resourceLoader != null) {
                Resource resource = resourceLoader.getResource(escudoPath);
                if (resource.exists()) {
                    try (InputStream is = resource.getInputStream()) {
                        return Image.getInstance(is.readAllBytes());
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Anexo1ItseDto convertirExpedienteADto(ExpedienteResponseDto exp) {
        if (exp == null) return new Anexo1ItseDto();
        return Anexo1ItseDto.builder()
                .tipoItse(exp.getNivelRiesgo() == NivelRiesgo.ALTO || exp.getNivelRiesgo() == NivelRiesgo.MUY_ALTO ? "PREVIA" : "POSTERIOR")
                .funcionEdificacion(exp.getFuncionEdificacion() != null ? exp.getFuncionEdificacion().name() : "COMERCIO")
                .nivelRiesgo(exp.getNivelRiesgo() != null ? exp.getNivelRiesgo() : NivelRiesgo.MEDIO)
                .condicionSolicitante("CONDUCTOR_ADMINISTRADOR")
                .nombresSolicitante(exp.getNombreTitular())
                .dniCeSolicitante(exp.getDocumentoIdentidad())
                .domicilioSolicitante(exp.getDireccionEstablecimiento())
                .telefonoSolicitante(exp.getTelefono())
                .correoSolicitante(exp.getCorreoElectronico())
                .razonSocial(exp.getRazonSocial())
                .ruc(exp.getDocumentoIdentidad())
                .nombreComercial(exp.getNombreComercial())
                .direccionEstablecimiento(exp.getDireccionEstablecimiento())
                .localidad("Ayacucho")
                .distrito(exp.getDistrito() != null ? exp.getDistrito() : "Ayacucho")
                .provincia(exp.getProvincia() != null ? exp.getProvincia() : "Huamanga")
                .departamento(exp.getDepartamento() != null ? exp.getDepartamento() : "Ayacucho")
                .referenciaDireccion(exp.getReferenciaUbicacion())
                .giroActividades(exp.getGiroNegocio())
                .numeroPisos(exp.getNumeroPisos() != null ? exp.getNumeroPisos() : 1)
                .pisoUbicacion("1er Piso")
                .areaOcupadaTotal(exp.getAreaOcupadaTotal() != null ? exp.getAreaOcupadaTotal() : exp.getAreaMetrosCuadrados())
                .horarioAtencion("08:00 - 20:00")
                .docDeclaracionJurada(true)
                .docReciboPago(true)
                .docCroquisUbicacion(true)
                .docPlanoArquitecturaAforo(exp.getNivelRiesgo() == NivelRiesgo.ALTO || exp.getNivelRiesgo() == NivelRiesgo.MUY_ALTO)
                .docPlanoTableros(exp.getNivelRiesgo() == NivelRiesgo.ALTO || exp.getNivelRiesgo() == NivelRiesgo.MUY_ALTO)
                .docPlanSeguridad(exp.getNumeroPisos() != null && exp.getNumeroPisos() >= 2)
                .docCertificadoPozoTierra(true)
                .build();
    }
}
