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
import pe.gob.munihuamanga.licencias.common.dto.Anexo2ItseDto;
import pe.gob.munihuamanga.licencias.common.dto.ExpedienteResponseDto;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generador PDF Oficial del Anexo 2 ITSE:
 * INFORMACIÓN PROPORCIONADA POR EL SOLICITANTE PARA LA DETERMINACIÓN DEL
 * NIVEL DE RIESGO DEL ESTABLECIMIENTO OBJETO DE INSPECCIÓN
 * Conforme al Reglamento de ITSE (D.S. N° 002-2018-PCM) y R.J. N° 016-2018-CENEPRED/J.
 * Genera exactamente dos (2) páginas A4 idénticas a la matriz CENEPRED.
 */
@Slf4j
@Component
public class Anexo2ItsePdfGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Color COLOR_PRIMARY = new Color(0, 43, 92);
    private static final Color COLOR_SEC_BAR = new Color(30, 58, 138);
    private static final Color COLOR_BORDER = new Color(148, 163, 184);
    private static final Color COLOR_BG_CELL = new Color(248, 250, 252);
    private static final Color COLOR_YELLOW_TINT = new Color(255, 253, 231);

    private static final Font F_INST = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.8f, COLOR_PRIMARY);
    private static final Font F_DOC_CODE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.0f, COLOR_PRIMARY);
    private static final Font F_DOC_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.8f, Color.BLACK);
    private static final Font F_DOC_SUB = FontFactory.getFont(FontFactory.HELVETICA, 5.8f, new Color(51, 65, 85));

    private static final Font F_SEC_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.5f, Color.WHITE);
    private static final Font F_CAT_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.0f, COLOR_PRIMARY);
    private static final Font F_ITEM = FontFactory.getFont(FontFactory.HELVETICA, 5.2f, Color.BLACK);
    private static final Font F_ITEM_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 5.4f, Color.BLACK);
    private static final Font F_LBL = FontFactory.getFont(FontFactory.HELVETICA, 5.2f, Color.DARK_GRAY);
    private static final Font F_VAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 5.8f, Color.BLACK);
    private static final Font F_CHECK = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.0f, COLOR_PRIMARY);

    private final ResourceLoader resourceLoader;

    @Value("${muni.documentos.escudo-path:classpath:static/img/escudo-huamanga.png}")
    private String escudoPath;

    public Anexo2ItsePdfGenerator(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public Anexo2ItsePdfGenerator() {
        this.resourceLoader = null;
        this.escudoPath = "classpath:static/img/escudo-huamanga.png";
    }

    public byte[] generarPdf(ExpedienteResponseDto exp) {
        Anexo2ItseDto dto = convertirExpedienteADto(exp);
        return generarPdf(dto, exp != null ? exp.getNumeroTramite() : "EXP-BORRADOR", exp);
    }

    public byte[] generarPdf(Anexo2ItseDto dto, String numeroTramite, ExpedienteResponseDto exp) {
        Document document = new Document(PageSize.A4, 15f, 15f, 12f, 12f);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // ==================== PÁGINA 1 ====================
            agregarEncabezado(document, numeroTramite, 1);
            agregarDatosGenerales(document, exp);
            agregarMatrizPagina1(document, dto);

            // ==================== PÁGINA 2 ====================
            document.newPage();
            agregarEncabezado(document, numeroTramite, 2);
            agregarMatrizPagina2(document, dto);
            agregarFirmaYFactores(document, dto, exp);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error al generar PDF de Anexo 2 ITSE: {}", e.getMessage(), e);
            throw new RuntimeException("Error en renderizado de Anexo 2 ITSE PDF: " + e.getMessage(), e);
        }
    }

    private void agregarEncabezado(Document document, String numeroTramite, int page) throws Exception {
        PdfPTable header = new PdfPTable(new float[]{1.2f, 3.8f, 1.6f});
        header.setWidthPercentage(100);
        header.setSpacingAfter(4f);

        PdfPCell cLogo = new PdfPCell();
        cLogo.setBorder(Rectangle.BOX);
        cLogo.setBorderColor(COLOR_BORDER);
        cLogo.setPadding(3f);
        cLogo.setHorizontalAlignment(Element.ALIGN_CENTER);
        cLogo.setVerticalAlignment(Element.ALIGN_MIDDLE);

        try {
            Image logo = obtenerEscudoImage();
            if (logo != null) {
                logo.scaleToFit(36, 36);
                cLogo.addElement(logo);
            }
        } catch (Exception ignored) {}
        header.addCell(cLogo);

        PdfPCell cTit = new PdfPCell();
        cTit.setBorder(Rectangle.BOX);
        cTit.setBorderColor(COLOR_BORDER);
        cTit.setPadding(3f);
        cTit.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph pInst = new Paragraph("MUNICIPALIDAD PROVINCIAL DE HUAMANGA — SUBGERENCIA DE DEFENSA CIVIL", F_INST);
        pInst.setAlignment(Element.ALIGN_CENTER);
        cTit.addElement(pInst);

        Paragraph pCod = new Paragraph("ANEXO 2 (Página " + page + " de 2)", F_DOC_CODE);
        pCod.setAlignment(Element.ALIGN_CENTER);
        cTit.addElement(pCod);

        Paragraph pMain = new Paragraph("INFORMACIÓN PROPORCIONADA POR EL SOLICITANTE PARA LA DETERMINACIÓN\nDEL NIVEL DE RIESGO DEL ESTABLECIMIENTO OBJETO DE INSPECCIÓN", F_DOC_TITLE);
        pMain.setAlignment(Element.ALIGN_CENTER);
        cTit.addElement(pMain);

        header.addCell(cTit);

        PdfPCell cMeta = new PdfPCell();
        cMeta.setBorder(Rectangle.BOX);
        cMeta.setBorderColor(COLOR_BORDER);
        cMeta.setPadding(3f);
        cMeta.setBackgroundColor(COLOR_BG_CELL);

        Paragraph pOrg = new Paragraph("ÓRGANO EJECUTANTE:", F_LBL);
        Paragraph pOrgVal = new Paragraph("Defensa Civil MPH", F_VAL);
        Paragraph pExp = new Paragraph("EXPEDIENTE:", F_LBL);
        Paragraph pExpVal = new Paragraph(numeroTramite != null ? numeroTramite : "EN TRÁMITE", F_VAL);

        cMeta.addElement(pOrg);
        cMeta.addElement(pOrgVal);
        cMeta.addElement(pExp);
        cMeta.addElement(pExpVal);

        header.addCell(cMeta);
        document.add(header);
    }

    private void agregarDatosGenerales(Document document, ExpedienteResponseDto exp) throws Exception {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingAfter(4f);

        PdfPCell cBar = new PdfPCell(new Phrase("DATOS DEL SOLICITANTE Y DEL ESTABLECIMIENTO", F_SEC_HEADER));
        cBar.setBackgroundColor(COLOR_SEC_BAR);
        cBar.setPadding(2.5f);
        t.addCell(cBar);

        PdfPTable sub = new PdfPTable(new float[]{3f, 2f, 3f, 2f});
        sub.setWidthPercentage(100);

        String tit = exp != null && exp.getNombreTitular() != null ? exp.getNombreTitular() : "ADMINISTRADO";
        String doc = exp != null && exp.getDocumentoIdentidad() != null ? exp.getDocumentoIdentidad() : "-";
        String ruc = exp != null && exp.getRazonSocial() != null ? exp.getDocumentoIdentidad() : "-";
        String giro = exp != null && exp.getGiroNegocio() != null ? exp.getGiroNegocio() : "Comercio / Venta de Bienes";
        String dir = exp != null && exp.getDireccionEstablecimiento() != null ? exp.getDireccionEstablecimiento() : "Huamanga";

        sub.addCell(celda("SOLICITANTE:", tit));
        sub.addCell(celda("DNI / CE:", doc));
        sub.addCell(celda("RAZÓN SOCIAL:", exp != null && exp.getRazonSocial() != null ? exp.getRazonSocial() : tit));
        sub.addCell(celda("RUC:", ruc));

        sub.addCell(celdaColspan("DIRECCIÓN DEL ESTABLECIMIENTO:", dir, 2));
        sub.addCell(celdaColspan("ACTIVIDAD O GIRO:", giro, 2));

        PdfPCell cSub = new PdfPCell(sub);
        cSub.setPadding(0);
        cSub.setBorder(Rectangle.NO_BORDER);
        t.addCell(cSub);

        document.add(t);
    }

    private void agregarMatrizPagina1(Document document, Anexo2ItseDto dto) throws Exception {
        PdfPTable t = new PdfPTable(new float[]{1.8f, 7.2f, 1.0f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(4f);

        PdfPCell h1 = celdaHeader("FUNCIÓN");
        PdfPCell h2 = celdaHeader("CARACTERÍSTICAS DECLARADAS POR EL SOLICITANTE (D.S. N° 002-2018-PCM)");
        PdfPCell h3 = celdaHeader("MARCAR (X)");
        t.addCell(h1);
        t.addCell(h2);
        t.addCell(h3);

        // 1. SALUD
        agregarFilaMatriz(t, "1. SALUD", "1.1 Primer Nivel: Puesto de salud, consultorio profesional (no médico) sin internamiento", "1.1", dto.getOpcionSalud());
        agregarFilaMatriz(t, "", "1.2 Primer Nivel: Posta de salud, consultorio médico sin internamiento", "1.2", dto.getOpcionSalud());
        agregarFilaMatriz(t, "", "1.3 Primer Nivel: Centro de salud, policlínico, centro médico especializado", "1.3", dto.getOpcionSalud());
        agregarFilaMatriz(t, "", "1.4 Segundo Nivel: Hospitales y clínicas de atención general", "1.4", dto.getOpcionSalud());
        agregarFilaMatriz(t, "", "1.5 Tercer Nivel: Hospitales e institutos especializados de alta complejidad", "1.5", dto.getOpcionSalud());

        // 2. ENCUENTRO
        agregarFilaMatriz(t, "2. ENCUENTRO", "2.1 Edificación con carga de ocupantes de hasta 50 personas", "2.1", dto.getOpcionEncuentro());
        agregarFilaMatriz(t, "", "2.2 Edificación con carga de ocupantes mayor a 50 personas", "2.2", dto.getOpcionEncuentro());
        agregarFilaMatriz(t, "", "2.3 La actividad de encuentro se realiza en sótano", "2.3", dto.getOpcionEncuentro());
        agregarFilaMatriz(t, "", "2.4 Discotecas, casinos, tragamonedas, cines, teatros, salas de concierto, templos, estadios", "2.4", dto.getOpcionEncuentro());

        // 3. HOSPEDAJE
        agregarFilaMatriz(t, "3. HOSPEDAJE", "3.1 Hasta 3 estrellas y hasta 4 pisos, albergue o ecolodge sin sótano", "3.1", dto.getOpcionHospedaje());
        agregarFilaMatriz(t, "", "3.2 Hasta 3 estrellas y hasta 4 pisos, con sótano", "3.2", dto.getOpcionHospedaje());
        agregarFilaMatriz(t, "", "3.3 Hospedaje con más de 4 pisos, o ubicado en piso superior al cuarto", "3.3", dto.getOpcionHospedaje());
        agregarFilaMatriz(t, "", "3.4 Hospedaje con sótano de estacionamiento > 500m² o 250m² de depósitos", "3.4", dto.getOpcionHospedaje());

        // 4. EDUCACIÓN
        agregarFilaMatriz(t, "4. EDUCACIÓN", "4.1 Inicial, primaria y secundaria hasta tres (3) pisos", "4.1", dto.getOpcionEducacion());
        agregarFilaMatriz(t, "", "4.2 Toda edificación educativa mayor a tres (3) pisos", "4.2", dto.getOpcionEducacion());
        agregarFilaMatriz(t, "", "4.3 Superior: Universidades, institutos, centros y escuelas superiores", "4.3", dto.getOpcionEducacion());
        agregarFilaMatriz(t, "", "4.4 Toda edificación remodelada o acondicionada para uso educativo", "4.4", dto.getOpcionEducacion());

        // 5. INDUSTRIAL
        agregarFilaMatriz(t, "5. INDUSTRIAL", "5.1 Taller artesanal con herramientas manuales (puede incluir área comercial)", "5.1", dto.getOpcionIndustrial());
        agregarFilaMatriz(t, "", "5.2 Industria en general con maquinaria y procesos productivos", "5.2", dto.getOpcionIndustrial());
        agregarFilaMatriz(t, "", "5.3 Fábricas de productos explosivos, pirotécnicos o materiales relacionados", "5.3", dto.getOpcionIndustrial());

        document.add(t);
    }

    private void agregarMatrizPagina2(Document document, Anexo2ItseDto dto) throws Exception {
        PdfPTable t = new PdfPTable(new float[]{1.8f, 7.2f, 1.0f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(4f);

        PdfPCell h1 = celdaHeader("FUNCIÓN");
        PdfPCell h2 = celdaHeader("CARACTERÍSTICAS DECLARADAS POR EL SOLICITANTE (D.S. N° 002-2018-PCM)");
        PdfPCell h3 = celdaHeader("MARCAR (X)");
        t.addCell(h1);
        t.addCell(h2);
        t.addCell(h3);

        // 6. OFICINAS ADMINISTRATIVAS
        agregarFilaMatriz(t, "6. OFICINAS", "6.1 Hasta 4 pisos y/o planta techada por piso igual o menor a 560 m²", "6.1", dto.getOpcionOficinas());
        agregarFilaMatriz(t, "", "6.2 Con conformidad de obra ≤ 5 años para el giro de diseño", "6.2", dto.getOpcionOficinas());
        agregarFilaMatriz(t, "", "6.3 En edificio cuyas áreas comunes cuentan con Certificado ITSE vigente", "6.3", dto.getOpcionOficinas());
        agregarFilaMatriz(t, "", "6.4 En edificio cuyas áreas comunes NO cuentan con Certificado ITSE vigente", "6.4", dto.getOpcionOficinas());
        agregarFilaMatriz(t, "", "6.5 Edificación con planta techada por piso mayor a 560 m²", "6.5", dto.getOpcionOficinas());

        // 7. COMERCIO
        String opCom = dto.getOpcionComercio() != null ? dto.getOpcionComercio() : "7.1";
        agregarFilaMatriz(t, "7. COMERCIO", "7.1 Edificación hasta tres (3) pisos y/o área techada total hasta 750 m²", "7.1", opCom);
        agregarFilaMatriz(t, "", "7.2 Módulos, stands o puestos en mercado/galería con licencia corporativa", "7.2", opCom);
        agregarFilaMatriz(t, "", "7.3 Edificación mayor a tres (3) pisos y/o área techada mayor a 750 m²", "7.3", opCom);
        agregarFilaMatriz(t, "", "7.4 Áreas comunes de galerías comerciales, mercados o centros comerciales", "7.4", opCom);
        agregarFilaMatriz(t, "", "7.5 Mercado mayorista/minorista, supermercados, tiendas por departamentos, malls", "7.5", opCom);
        agregarFilaMatriz(t, "", "7.6 Comercialización de productos pirotécnicos, explosivos o relacionados", "7.6", opCom);

        // 8. ALMACÉN
        agregarFilaMatriz(t, "8. ALMACÉN", "8.1 Almacén o estacionamiento no techado (puede incluir área administrativa techada)", "8.1", dto.getOpcionAlmacen());
        agregarFilaMatriz(t, "", "8.2 Almacén o estacionamiento techado", "8.2", dto.getOpcionAlmacen());
        agregarFilaMatriz(t, "", "8.3 Almacén de productos pirotécnicos, explosivos o relacionados", "8.3", dto.getOpcionAlmacen());

        document.add(t);
    }

    private void agregarFirmaYFactores(Document document, Anexo2ItseDto dto, ExpedienteResponseDto exp) throws Exception {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingAfter(4f);

        PdfPCell cBar = new PdfPCell(new Phrase("FACTORES ADICIONALES QUE INCREMENTAN EL RIESGO PARA TODAS LAS FUNCIONES", F_SEC_HEADER));
        cBar.setBackgroundColor(COLOR_SEC_BAR);
        cBar.setPadding(2.5f);
        t.addCell(cBar);

        PdfPTable sub = new PdfPTable(new float[]{8.5f, 1.5f});
        sub.setWidthPercentage(100);

        boolean fGlp = Boolean.TRUE.equals(dto.getFactorGlpCombustible());
        boolean fCal = Boolean.TRUE.equals(dto.getFactorCaldero());

        sub.addCell(celda("FACTOR A: Tanque de GLP (>0.45 m³ / 118.18 gal) o combustible líquido (>1 m³ / 264.17 gal)", "D.S. N° 002-2018-PCM"));
        sub.addCell(celdaCenter(fGlp ? "[ X ]" : "[   ]"));

        sub.addCell(celda("FACTOR B: El establecimiento utiliza caldero o generador de vapor industrial", "D.S. N° 002-2018-PCM"));
        sub.addCell(celdaCenter(fCal ? "[ X ]" : "[   ]"));

        PdfPCell cSub = new PdfPCell(sub);
        cSub.setPadding(0);
        cSub.setBorder(Rectangle.NO_BORDER);
        t.addCell(cSub);

        document.add(t);

        // Bloque de Firma
        PdfPTable tFirma = new PdfPTable(new float[]{5f, 5f});
        tFirma.setWidthPercentage(100);

        PdfPCell cRes = new PdfPCell();
        cRes.setBorder(Rectangle.BOX);
        cRes.setBorderColor(COLOR_BORDER);
        cRes.setPadding(4f);
        cRes.setBackgroundColor(COLOR_BG_CELL);

        Paragraph pResTit = new Paragraph("NIVEL DE RIESGO DECLARADO / PRELIMINAR:", F_CAT_HEADER);
        NivelRiesgo nr = dto.getNivelRiesgoCalculado() != null ? dto.getNivelRiesgoCalculado() : NivelRiesgo.MEDIO;
        Paragraph pResVal = new Paragraph("RIESGO " + nr.name() + " (Matriz ITSE)", F_VAL);
        Paragraph pNota = new Paragraph("Nota: Sujeto a verificación y clasificación definitiva por el Inspector de Defensa Civil.", F_LBL);

        cRes.addElement(pResTit);
        cRes.addElement(pResVal);
        cRes.addElement(pNota);
        tFirma.addCell(cRes);

        PdfPCell cSol = new PdfPCell();
        cSol.setBorder(Rectangle.BOX);
        cSol.setBorderColor(COLOR_BORDER);
        cSol.setPadding(4f);
        cSol.setBackgroundColor(COLOR_BG_CELL);

        String nom = exp != null && exp.getNombreTitular() != null ? exp.getNombreTitular() : "ADMINISTRADO DECLARANTE";
        String dni = exp != null && exp.getDocumentoIdentidad() != null ? exp.getDocumentoIdentidad() : "DNI";

        Paragraph pSol = new Paragraph(
                "Declaración bajo fe de juramento:\n" +
                "Nombres: " + nom + " | DNI: " + dni + "\n" +
                "Fecha: " + LocalDate.now().format(DATE_FMT) + "\n\n" +
                "_________________________________________\n" +
                "Firma del Solicitante / Administrado", F_LBL);

        cSol.addElement(pSol);
        tFirma.addCell(cSol);

        document.add(tFirma);
    }

    private void agregarFilaMatriz(PdfPTable t, String funcion, String desc, String codigoOpcion, String opcionMarcada) {
        PdfPCell c1 = new PdfPCell(new Phrase(funcion, F_CAT_HEADER));
        c1.setBorder(Rectangle.BOX);
        c1.setBorderColor(COLOR_BORDER);
        c1.setPadding(2f);
        c1.setBackgroundColor(COLOR_BG_CELL);

        PdfPCell c2 = new PdfPCell(new Phrase(desc, F_ITEM));
        c2.setBorder(Rectangle.BOX);
        c2.setBorderColor(COLOR_BORDER);
        c2.setPadding(2f);

        boolean marcada = codigoOpcion != null && codigoOpcion.equalsIgnoreCase(opcionMarcada);
        PdfPCell c3 = new PdfPCell(new Phrase(marcada ? "X" : "", F_CHECK));
        c3.setBorder(Rectangle.BOX);
        c3.setBorderColor(COLOR_BORDER);
        c3.setPadding(2f);
        c3.setHorizontalAlignment(Element.ALIGN_CENTER);
        c3.setVerticalAlignment(Element.ALIGN_MIDDLE);
        if (marcada) c3.setBackgroundColor(COLOR_YELLOW_TINT);

        t.addCell(c1);
        t.addCell(c2);
        t.addCell(c3);
    }

    private PdfPCell celdaHeader(String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto, F_SEC_HEADER));
        c.setBackgroundColor(COLOR_SEC_BAR);
        c.setBorder(Rectangle.BOX);
        c.setBorderColor(COLOR_BORDER);
        c.setPadding(2.5f);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        return c;
    }

    private PdfPCell celda(String label, String valor) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.BOX);
        c.setBorderColor(COLOR_BORDER);
        c.setPadding(2.5f);
        c.addElement(new Paragraph(label, F_LBL));
        c.addElement(new Paragraph(valor != null ? valor : "-", F_VAL));
        return c;
    }

    private PdfPCell celdaColspan(String label, String valor, int col) {
        PdfPCell c = celda(label, valor);
        c.setColspan(col);
        return c;
    }

    private PdfPCell celdaCenter(String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto, F_VAL));
        c.setBorder(Rectangle.BOX);
        c.setBorderColor(COLOR_BORDER);
        c.setPadding(2.5f);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
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

    private Anexo2ItseDto convertirExpedienteADto(ExpedienteResponseDto exp) {
        if (exp == null) return new Anexo2ItseDto();
        return Anexo2ItseDto.builder()
                .funcionPrincipal(exp.getFuncionEdificacion() != null ? exp.getFuncionEdificacion().name() : "COMERCIO")
                .opcionComercio("7.1")
                .factorGlpCombustible(false)
                .factorCaldero(false)
                .nivelRiesgoCalculado(exp.getNivelRiesgo() != null ? exp.getNivelRiesgo() : NivelRiesgo.MEDIO)
                .build();
    }
}
