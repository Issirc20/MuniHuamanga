package pe.gob.munihuamanga.licencias.expedientes.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.dto.ExpedienteResponseDto;
import pe.gob.munihuamanga.licencias.common.enums.ModalidadTramite;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.common.enums.TipoPersona;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generador PDF Oficial del Anexo N° 1:
 * FORMATO DE DECLARACIÓN JURADA PARA LICENCIA DE FUNCIONAMIENTO (Versión 03)
 * Conforme a la Ley N° 28976, D.S. N° 046-2017-PCM y D.S. N° 163-2020-PCM.
 * Genera exactamente dos (2) páginas idénticas al formato estándar oficial de la Municipalidad de Huamanga.
 */
@Slf4j
@Component
public class Anexo1PdfGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Font F_TITLE_TAG = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, Color.BLACK);
    private static final Font F_TITLE_MAIN = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Color.BLACK);
    private static final Font F_TITLE_SUB = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 6.5f, Color.DARK_GRAY);

    private static final Font F_BOX_LABEL = FontFactory.getFont(FontFactory.HELVETICA, 6f, Color.BLACK);
    private static final Font F_BOX_VAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.5f, new Color(0, 51, 102));

    private static final Font F_SEC_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.5f, Color.BLACK);
    private static final Font F_COL_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6f, Color.BLACK);
    private static final Font F_ITEM = FontFactory.getFont(FontFactory.HELVETICA, 5.8f, Color.BLACK);
    private static final Font F_ITEM_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 5.8f, Color.BLACK);

    private static final Font F_LABEL = FontFactory.getFont(FontFactory.HELVETICA, 5.8f, Color.DARK_GRAY);
    private static final Font F_VAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.2f, Color.BLACK);

    private static final Font F_FOOT_NOTE = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 5.2f, Color.DARK_GRAY);
    private static final Font F_INST_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.5f, Color.BLACK);
    private static final Font F_INST_BODY = FontFactory.getFont(FontFactory.HELVETICA, 5.2f, new Color(40, 40, 40));

    private static final Color BG_HEADER = new Color(240, 240, 240);
    private static final Color BORDER_COLOR = new Color(160, 160, 160);

    public byte[] generarPdf(ExpedienteResponseDto exp) {
        Document document = new Document(PageSize.A4, 20f, 20f, 18f, 18f);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // ==================== PÁGINA 1 ====================
            agregarEncabezado(document, exp, 1);

            Paragraph pVerInst = new Paragraph("VER INSTRUCCIONES PARA EL LLENADO (Página 2)", F_FOOT_NOTE);
            pVerInst.setAlignment(Element.ALIGN_CENTER);
            pVerInst.setSpacingAfter(3f);
            document.add(pVerInst);

            agregarSeccionI(document, exp);
            agregarSeccionII(document, exp);
            agregarSeccionIII(document, exp);
            agregarSeccionIV(document, exp);

            Paragraph pFoot1 = new Paragraph("* Esta información es llenada por el representante de la municipalidad.", F_FOOT_NOTE);
            pFoot1.setSpacingBefore(2f);
            document.add(pFoot1);

            // ==================== PÁGINA 2 ====================
            document.newPage();
            agregarEncabezado(document, exp, 2);

            agregarSeccionV(document, exp);
            agregarSeccionVI(document, exp);
            agregarInstruccionesLlenado(document);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error al generar PDF del Anexo 1 oficial", e);
            throw new RuntimeException("Error al generar PDF del Anexo 1", e);
        }
    }

    private void agregarEncabezado(Document document, ExpedienteResponseDto exp, int pagina) throws Exception {
        PdfPTable t = new PdfPTable(3);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{20f, 55f, 25f});
        t.setSpacingAfter(2f);

        // Celda Izquierda: Logo institucional
        PdfPCell cLogo = new PdfPCell();
        cLogo.setBorder(PdfPCell.BOX);
        cLogo.setBorderColor(BORDER_COLOR);
        cLogo.setHorizontalAlignment(Element.ALIGN_CENTER);
        cLogo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cLogo.setPadding(3f);

        try {
            java.net.URL logoUrl = getClass().getResource("/static/img/escudo-huamanga.png");
            if (logoUrl != null) {
                Image imgLogo = Image.getInstance(logoUrl);
                imgLogo.scaleToFit(38f, 38f);
                imgLogo.setAlignment(Element.ALIGN_CENTER);
                cLogo.addElement(imgLogo);
            }
        } catch (Exception e) {
            log.warn("No se pudo cargar el escudo institucional: {}", e.getMessage());
        }

        Paragraph pLogo = new Paragraph("MUNICIPALIDAD PROVINCIAL\nDE HUAMANGA", F_TITLE_TAG);
        pLogo.setAlignment(Element.ALIGN_CENTER);
        cLogo.addElement(pLogo);
        t.addCell(cLogo);

        // Celda Centro: Título oficial
        PdfPCell cTitulo = new PdfPCell();
        cTitulo.setBorder(PdfPCell.BOX);
        cTitulo.setBorderColor(BORDER_COLOR);
        cTitulo.setHorizontalAlignment(Element.ALIGN_CENTER);
        cTitulo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cTitulo.setPadding(4f);

        Paragraph pAnx = new Paragraph("Anexo N° 1", F_TITLE_TAG);
        pAnx.setAlignment(Element.ALIGN_CENTER);
        cTitulo.addElement(pAnx);

        Paragraph pNom = new Paragraph("FORMATO DE DECLARACIÓN JURADA PARA LICENCIA DE\nFUNCIONAMIENTO", F_TITLE_MAIN);
        pNom.setAlignment(Element.ALIGN_CENTER);
        cTitulo.addElement(pNom);

        Paragraph pLey = new Paragraph("LEY Nº 28976 - Ley Marco de Licencia de Funcionamiento y modificatorias\nVersión 03", F_TITLE_SUB);
        pLey.setAlignment(Element.ALIGN_CENTER);
        cTitulo.addElement(pLey);
        t.addCell(cTitulo);

        // Celda Derecha: Recuadro de control municipal
        PdfPCell cBox = new PdfPCell();
        cBox.setBorder(PdfPCell.BOX);
        cBox.setBorderColor(BORDER_COLOR);
        cBox.setPadding(3f);

        String fechaRecep = exp.getFechaCreacion() != null ? exp.getFechaCreacion().format(DATE_FMT) : "-";
        String voucher = exp.getVoucherId() != null ? exp.getVoucherId() : "...................";
        String fechaPago = exp.getFechaPagoSat() != null ? exp.getFechaPagoSat().format(DATE_FMT) : "...................";

        Paragraph pB = new Paragraph();
        pB.add(new Phrase("N° de expediente: ", F_BOX_LABEL));
        pB.add(new Phrase(exp.getNumeroTramite() != null ? exp.getNumeroTramite() : "-", F_BOX_VAL));
        pB.add(new Phrase("\nPágina: " + pagina + " de 2", F_BOX_LABEL));
        pB.add(new Phrase("\nFecha de recepción: " + fechaRecep, F_BOX_LABEL));
        pB.add(new Phrase("\nN° de recibo de pago: " + voucher, F_BOX_LABEL));
        pB.add(new Phrase("\nFecha de pago: " + fechaPago, F_BOX_LABEL));
        cBox.addElement(pB);
        t.addCell(cBox);

        document.add(t);
    }

    private void agregarSeccionI(Document document, ExpedienteResponseDto exp) throws Exception {
        PdfPTable t = new PdfPTable(3);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{37f, 35f, 28f});
        t.setSpacingAfter(3f);

        // Título de la sección I
        PdfPCell cTit = new PdfPCell(new Phrase("I MODALIDAD DEL TRÁMITE QUE SOLICITA (marcar más de una alternativa si corresponde)", F_SEC_TITLE));
        cTit.setColspan(3);
        cTit.setBackgroundColor(BG_HEADER);
        cTit.setBorderColor(BORDER_COLOR);
        cTit.setPadding(2f);
        t.addCell(cTit);

        ModalidadTramite mod = exp.getModalidadTramite() != null ? exp.getModalidadTramite() : ModalidadTramite.LICENCIA_INDETERMINADA;

        // Col 1: Licencia de funcionamiento
        PdfPCell c1 = new PdfPCell();
        c1.setBorderColor(BORDER_COLOR);
        c1.setPadding(3f);
        c1.addElement(new Paragraph("Licencia de funcionamiento", F_COL_HEADER));
        c1.addElement(crearCheckItem("Indeterminada", mod == ModalidadTramite.LICENCIA_INDETERMINADA));
        String plazo = exp.getPlazoTemporalMeses() != null ? exp.getPlazoTemporalMeses() + " meses" : ".....................";
        c1.addElement(crearCheckItem("Temporal (Indicar plazo: " + plazo + ")", mod == ModalidadTramite.LICENCIA_TEMPORAL));
        String tipoAnuncio = exp.getTipoAnuncio() != null ? exp.getTipoAnuncio() : ".....................";
        c1.addElement(crearCheckItem("Licencia más autorización de anuncio publicitario\n(Tipo de anuncio: " + tipoAnuncio + ")", mod == ModalidadTramite.LICENCIA_CON_ANUNCIO));
        String licPrinc = exp.getNumeroLicenciaPrincipal() != null ? exp.getNumeroLicenciaPrincipal() : ".....................";
        c1.addElement(crearCheckItem("Licencia para cesionario\n(N° lic. principal: " + licPrinc + ")", mod == ModalidadTramite.LICENCIA_CESIONARIO));
        c1.addElement(crearCheckItem("Licencias para mercados de abastos, galerías y centros comerciales", mod == ModalidadTramite.LICENCIA_MERCADOS_GALERIAS));
        t.addCell(c1);

        // Col 2: Cambios o modificaciones
        PdfPCell c2 = new PdfPCell();
        c2.setBorderColor(BORDER_COLOR);
        c2.setPadding(3f);
        c2.addElement(new Paragraph("Cambios o modificaciones", F_COL_HEADER));
        c2.addElement(crearCheckItem("Cambio de denominación o nombre comercial de persona jurídica (Solo Secc. II, III y V)", mod == ModalidadTramite.CAMBIO_DENOMINACION));
        c2.addElement(new Paragraph("  N° lic: ..................... Nueva denom: .....................", F_ITEM));
        c2.addElement(crearCheckItem("Transferencia de Licencia de Funcionamiento (Solo Secc. II, III, V y adjuntar copia de contrato)", mod == ModalidadTramite.TRANSFERENCIA_LICENCIA));
        c2.addElement(new Paragraph("  N° licencia anterior: .......................................", F_ITEM));
        t.addCell(c2);

        // Col 3: Otros
        PdfPCell c3 = new PdfPCell();
        c3.setBorderColor(BORDER_COLOR);
        c3.setPadding(3f);
        c3.addElement(new Paragraph("Otros", F_COL_HEADER));
        c3.addElement(crearCheckItem("Cese de actividades (Solo Secc. II, III y V)\nN° licencia: .......................................", mod == ModalidadTramite.CESE_ACTIVIDADES));
        c3.addElement(crearCheckItem("Otros (especificar):\n...........................................................", mod == ModalidadTramite.OTROS));
        t.addCell(c3);

        document.add(t);
    }

    private void agregarSeccionII(Document document, ExpedienteResponseDto exp) throws Exception {
        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{25f, 25f, 25f, 25f});
        t.setSpacingAfter(3f);

        PdfPCell cTit = new PdfPCell(new Phrase("II DATOS DEL SOLICITANTE", F_SEC_TITLE));
        cTit.setColspan(4);
        cTit.setBackgroundColor(BG_HEADER);
        cTit.setBorderColor(BORDER_COLOR);
        cTit.setPadding(2f);
        t.addCell(cTit);

        // Fila 1: Apellidos y nombres / Razón social
        String nombreOrazon = (exp.getRazonSocial() != null && !exp.getRazonSocial().isBlank())
                ? exp.getRazonSocial()
                : (exp.getNombreTitular() != null ? exp.getNombreTitular() : "-");
        PdfPCell cNom = new PdfPCell();
        cNom.setColspan(4);
        cNom.setBorderColor(BORDER_COLOR);
        cNom.setPadding(2f);
        cNom.addElement(new Phrase("Apellidos y Nombres / Razón social:", F_LABEL));
        cNom.addElement(new Phrase(" " + nombreOrazon, F_VAL));
        t.addCell(cNom);

        // Fila 2: Documentos y contacto
        String dni = (exp.getTipoPersona() == TipoPersona.NATURAL) ? exp.getDocumentoIdentidad() : "-";
        String ruc = (exp.getTipoPersona() == TipoPersona.JURIDICA || (exp.getDocumentoIdentidad() != null && exp.getDocumentoIdentidad().length() == 11))
                ? exp.getDocumentoIdentidad() : (exp.getRazonSocial() != null ? exp.getDocumentoIdentidad() : "-");

        t.addCell(crearCeldaDoble("N° DNI / N° C.E.:", dni));
        t.addCell(crearCeldaDoble("N° RUC:", ruc));
        t.addCell(crearCeldaDoble("N° Teléfono:", exp.getTelefono() != null ? exp.getTelefono() : "-"));
        t.addCell(crearCeldaDoble("Correo electrónico:", exp.getCorreoElectronico() != null ? exp.getCorreoElectronico() : "-"));

        // Fila 3: Dirección desglosada del solicitante
        String via = (exp.getTipoVia() != null ? exp.getTipoVia() + " " : "") + (exp.getNombreVia() != null ? exp.getNombreVia() : exp.getDireccionEstablecimiento());
        String num = (exp.getNumeroVivienda() != null ? "N° " + exp.getNumeroVivienda() : "") + (exp.getInterior() != null ? " Int. " + exp.getInterior() : "");
        String urb = exp.getUrbanizacion() != null ? exp.getUrbanizacion() : "-";
        String distProv = (exp.getDistrito() != null ? exp.getDistrito() : "Ayacucho") + " / " + (exp.getProvincia() != null ? exp.getProvincia() : "Huamanga");

        t.addCell(crearCeldaDoble("Av./Jr./Ca./Pje./Otros:", via));
        t.addCell(crearCeldaDoble("N°/Int. /Mz/Lt./Otros:", num.isBlank() ? "-" : num));
        t.addCell(crearCeldaDoble("Urb./ AA.HH./Otros:", urb));
        t.addCell(crearCeldaDoble("Distrito y Provincia:", distProv));

        document.add(t);
    }

    private void agregarSeccionIII(Document document, ExpedienteResponseDto exp) throws Exception {
        PdfPTable t = new PdfPTable(3);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{45f, 20f, 35f});
        t.setSpacingAfter(3f);

        PdfPCell cTit = new PdfPCell(new Phrase("III DATOS DEL REPRESENTANTE LEGAL O APODERADO", F_SEC_TITLE));
        cTit.setColspan(3);
        cTit.setBackgroundColor(BG_HEADER);
        cTit.setBorderColor(BORDER_COLOR);
        cTit.setPadding(2f);
        t.addCell(cTit);

        String repNom = exp.getNombreRepresentante() != null ? exp.getNombreRepresentante() : "-";
        String repDni = exp.getDniRepresentante() != null ? exp.getDniRepresentante() : "-";
        String sunarp = (exp.getPartidaSunarp() != null ? "Partida: " + exp.getPartidaSunarp() : "")
                + (exp.getAsientoSunarp() != null ? " Asiento: " + exp.getAsientoSunarp() : "");
        if (sunarp.isBlank()) sunarp = "-";

        t.addCell(crearCeldaDoble("Apellidos y Nombres:", repNom));
        t.addCell(crearCeldaDoble("N° DNI / N° C.E.:", repDni));
        t.addCell(crearCeldaDoble("N° partida electrónica y asiento SUNARP:", sunarp));

        document.add(t);
    }

    private void agregarSeccionIV(Document document, ExpedienteResponseDto exp) throws Exception {
        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{25f, 25f, 25f, 25f});
        t.setSpacingAfter(3f);

        PdfPCell cTit = new PdfPCell(new Phrase("IV DATOS DEL ESTABLECIMIENTO", F_SEC_TITLE));
        cTit.setColspan(4);
        cTit.setBackgroundColor(BG_HEADER);
        cTit.setBorderColor(BORDER_COLOR);
        cTit.setPadding(2f);
        t.addCell(cTit);

        // Nombre comercial
        PdfPCell cNom = new PdfPCell();
        cNom.setColspan(4);
        cNom.setBorderColor(BORDER_COLOR);
        cNom.setPadding(2f);
        cNom.addElement(new Phrase("Nombre comercial:", F_LABEL));
        cNom.addElement(new Phrase(" " + (exp.getNombreComercial() != null ? exp.getNombreComercial() : "-"), F_VAL));
        t.addCell(cNom);

        // Código CIIU, Giro, Actividad, Zonificación
        t.addCell(crearCeldaDoble("Código CIIU *:", exp.getCiiuCodigo() != null ? exp.getCiiuCodigo() : "-"));
        t.addCell(crearCeldaDoble("Giro/s *:", exp.getGiroNegocio() != null ? exp.getGiroNegocio() : "-"));
        t.addCell(crearCeldaDoble("Actividad:", exp.getActividadDetallada() != null ? exp.getActividadDetallada() : exp.getGiroNegocio()));
        t.addCell(crearCeldaDoble("Zonificación:", exp.getZonificacion() != null ? exp.getZonificacion() : "ZRE-CH"));

        // Dirección del local
        String via = (exp.getTipoVia() != null ? exp.getTipoVia() + " " : "") + (exp.getNombreVia() != null ? exp.getNombreVia() : exp.getDireccionEstablecimiento());
        String num = (exp.getNumeroVivienda() != null ? "N° " + exp.getNumeroVivienda() : "") + (exp.getInterior() != null ? " Int. " + exp.getInterior() : "");
        String urb = exp.getUrbanizacion() != null ? exp.getUrbanizacion() : "Huamanga";
        String prov = exp.getProvincia() != null ? exp.getProvincia() : "Huamanga";

        t.addCell(crearCeldaDoble("Av./Jr./Ca./Pje./Otros:", via));
        t.addCell(crearCeldaDoble("N°/Int. /Mz/Lt./Otros:", num.isBlank() ? "-" : num));
        t.addCell(crearCeldaDoble("Urb./ AA.HH./Otros:", urb));
        t.addCell(crearCeldaDoble("Provincia:", prov));

        // Autorización Sectorial
        PdfPCell cSecTit = new PdfPCell(new Phrase("Autorización Sectorial (de corresponder):", F_ITEM_BOLD));
        cSecTit.setColspan(4);
        cSecTit.setBackgroundColor(new Color(248, 248, 248));
        cSecTit.setBorderColor(BORDER_COLOR);
        cSecTit.setPadding(2f);
        t.addCell(cSecTit);

        String entidad = exp.getSectorEntidad() != null ? exp.getSectorEntidad() : "-";
        String denom = exp.getSectorDenominacion() != null ? exp.getSectorDenominacion() : "-";
        String fecha = exp.getSectorFecha() != null ? exp.getSectorFecha() : "-";
        String numero = exp.getSectorNumero() != null ? exp.getSectorNumero() : "-";

        t.addCell(crearCeldaDoble("Entidad que otorga:", entidad));
        t.addCell(crearCeldaDoble("Denominación:", denom));
        t.addCell(crearCeldaDoble("Fecha autorización:", fecha));
        t.addCell(crearCeldaDoble("Número autorización:", numero));

        // Fila Dividida: Área total solicitada y Croquis de Ubicación
        PdfPCell cArea = new PdfPCell();
        cArea.setColspan(2);
        cArea.setBorderColor(BORDER_COLOR);
        cArea.setPadding(3f);
        cArea.addElement(new Phrase("Área total solicitada (m²):", F_LABEL));
        cArea.addElement(new Phrase("\n" + (exp.getAreaMetrosCuadrados() != null ? exp.getAreaMetrosCuadrados() + " m²" : "-"), F_VAL));
        if (exp.getAforoPersonas() != null) {
            cArea.addElement(new Phrase("\nAforo estimado: " + exp.getAforoPersonas() + " personas", F_LABEL));
        }
        t.addCell(cArea);

        PdfPCell cCroquis = new PdfPCell();
        cCroquis.setColspan(2);
        cCroquis.setBorderColor(BORDER_COLOR);
        cCroquis.setPadding(3f);
        cCroquis.addElement(new Phrase("Croquis de ubicación:", F_LABEL));
        cCroquis.addElement(crearMiniCroquis());
        t.addCell(cCroquis);

        document.add(t);
    }

    private void agregarSeccionV(Document document, ExpedienteResponseDto exp) throws Exception {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingAfter(3f);

        PdfPCell cTit = new PdfPCell(new Phrase("V DECLARACIÓN JURADA", F_SEC_TITLE));
        cTit.setBackgroundColor(BG_HEADER);
        cTit.setBorderColor(BORDER_COLOR);
        cTit.setPadding(2f);
        t.addCell(cTit);

        PdfPCell cDecl = new PdfPCell();
        cDecl.setBorderColor(BORDER_COLOR);
        cDecl.setPadding(3f);
        cDecl.addElement(new Paragraph("Declaro (DE CORRESPONDER MARCAR CON X)", F_ITEM_BOLD));
        cDecl.addElement(crearCheckItem("Cuento con poder suficiente vigente para actuar como representante legal de la persona jurídica conductora (alternativamente, de la persona natural que represento).", true));
        cDecl.addElement(crearCheckItem("El establecimiento cumple con las condiciones de seguridad en edificaciones y me someto a la inspección técnica que corresponda en función al nivel de riesgo, de conformidad con la legislación aplicable.", true));
        cDecl.addElement(crearCheckItem("Cuento con título profesional vigente y estoy habilitado por el colegio profesional correspondiente (en el caso de servicios relacionados con la salud).", false));
        cDecl.addElement(crearCheckItem("Tengo conocimiento de que la presente Declaración Jurada y documentación está sujeta a la fiscalización posterior. En caso de haber proporcionado información, documentos, formatos o declaraciones que no corresponden a la verdad, se me aplicarán las sanciones administrativas y penales correspondientes, declarándose la nulidad o revocatoria de la licencia o autorización otorgada. Asimismo, brindaré las facilidades necesarias para las acciones de control de la autoridad municipal competente.", true));
        cDecl.addElement(new Paragraph("\nObservaciones o comentarios del solicitante: Ninguna.", F_ITEM));
        t.addCell(cDecl);

        // Fila de Fecha y Firma
        PdfPTable tFirma = new PdfPTable(2);
        tFirma.setWidthPercentage(100);
        tFirma.setWidths(new float[]{40f, 60f});

        PdfPCell cFecha = new PdfPCell();
        cFecha.setBorder(PdfPCell.NO_BORDER);
        cFecha.setPadding(4f);
        String fecha = exp.getFechaCreacion() != null ? exp.getFechaCreacion().format(DATE_FMT) : LocalDate.now().format(DATE_FMT);
        cFecha.addElement(new Phrase("Fecha: " + fecha, F_VAL));
        tFirma.addCell(cFecha);

        PdfPCell cFirmaBox = new PdfPCell();
        cFirmaBox.setBorder(PdfPCell.BOX);
        cFirmaBox.setBorderColor(BORDER_COLOR);
        cFirmaBox.setPadding(4f);
        cFirmaBox.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph pF = new Paragraph();
        pF.add(new Phrase("\n\n_____________________________________________________\n", F_ITEM));
        pF.add(new Phrase("Firma del solicitante/ Representante legal/ Apoderado\n", F_ITEM_BOLD));
        pF.add(new Phrase("DNI: " + (exp.getDocumentoIdentidad() != null ? exp.getDocumentoIdentidad() : "..................") + "\n", F_ITEM));
        pF.add(new Phrase("Nombres y Apellidos: " + (exp.getNombreTitular() != null ? exp.getNombreTitular() : "................................................"), F_ITEM));
        pF.setAlignment(Element.ALIGN_CENTER);
        cFirmaBox.addElement(pF);
        tFirma.addCell(cFirmaBox);

        PdfPCell cWrap = new PdfPCell(tFirma);
        cWrap.setBorderColor(BORDER_COLOR);
        cWrap.setPadding(3f);
        t.addCell(cWrap);

        document.add(t);
    }

    private void agregarSeccionVI(Document document, ExpedienteResponseDto exp) throws Exception {
        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{25f, 25f, 25f, 25f});
        t.setSpacingAfter(3f);

        PdfPCell cTit = new PdfPCell(new Phrase("VI CLASIFICACIÓN DEL NIVEL DE RIESGO (Para ser llenado por el calificador designado de la municipalidad) *", F_SEC_TITLE));
        cTit.setColspan(4);
        cTit.setBackgroundColor(BG_HEADER);
        cTit.setBorderColor(BORDER_COLOR);
        cTit.setPadding(2f);
        t.addCell(cTit);

        NivelRiesgo riesgo = exp.getNivelRiesgo();
        t.addCell(crearCeldaCheckCentro("ITSE Riesgo bajo", riesgo == NivelRiesgo.BAJO));
        t.addCell(crearCeldaCheckCentro("ITSE Riesgo medio", riesgo == NivelRiesgo.MEDIO));
        t.addCell(crearCeldaCheckCentro("ITSE Riesgo alto", riesgo == NivelRiesgo.ALTO));
        t.addCell(crearCeldaCheckCentro("ITSE Riesgo muy alto", riesgo == NivelRiesgo.MUY_ALTO));

        PdfPCell cFirmaMuni = new PdfPCell();
        cFirmaMuni.setColspan(4);
        cFirmaMuni.setBorderColor(BORDER_COLOR);
        cFirmaMuni.setPadding(4f);
        cFirmaMuni.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph pFM = new Paragraph();
        pFM.add(new Phrase("\n\n_____________________________________________________\n", F_ITEM));
        pFM.add(new Phrase("Firma y sello del calificador municipal\n", F_ITEM_BOLD));
        String inspector = exp.getNumeroInformeItse() != null
                ? "Subgerencia de Defensa Civil (Informe: " + exp.getNumeroInformeItse() + ")"
                : "Subgerencia de Defensa Civil y Gestión del Riesgo";
        pFM.add(new Phrase("Nombres y Apellidos: " + inspector + "\n", F_ITEM));
        pFM.setAlignment(Element.ALIGN_CENTER);
        cFirmaMuni.addElement(pFM);
        t.addCell(cFirmaMuni);

        document.add(t);

        Paragraph pNoteVI = new Paragraph("* Esta información debe ser llenada por el calificador designado por la municipalidad, de acuerdo con los anexos 2 y 3 del Manual de Ejecución de Inspección Técnica de Seguridad en Edificaciones.", F_FOOT_NOTE);
        pNoteVI.setSpacingAfter(3f);
        document.add(pNoteVI);
    }

    private void agregarInstruccionesLlenado(Document document) throws Exception {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);

        PdfPCell c = new PdfPCell();
        c.setBorderColor(BORDER_COLOR);
        c.setPadding(4f);

        Paragraph p = new Paragraph("INSTRUCCIONES PARA EL LLENADO\n", F_INST_TITLE);
        p.add(new Phrase("Sección I: Marcar con una \"X\" en la casilla según la modalidad del trámite que solicita, en caso de corresponder puede marcar más de una alternativa. De haber marcado \"Cambio de denominación o nombre comercial de la persona jurídica\" o \"Cese de actividades\", solo debe completar las secciones II, III y V. De haber marcado \"Transferencia de Licencia de Funcionamiento\", debe adjuntar copia simple del contrato de transferencia y solo debe completar las secciones II, III y V.\n" +
                "Nota: Si el establecimiento ya cuenta con una licencia de funcionamiento y el titular o un tercero va a realizar alguna de las actividades simultáneas y adicionales establecidas por el Ministerio de la Producción en el Numeral II denominado \"Listado de actividades simultáneas y adicionales que pueden desarrollarse con la presentación de una declaración jurada ante las municipalidades\" (D.S. N° 011-2017-PRODUCE), no corresponde utilizar este Formato sino el Formato de Actividades Simultáneas. Si ya cuenta con licencia, el titular puede realizar actividades de cajero corresponsal sin necesidad de trámite adicional.\n" +
                "Sección II: En caso de persona natural, consignar los datos personales del solicitante. En caso de persona jurídica, consignar la razón social y número de RUC.\n" +
                "Sección III: En caso de representación de personas naturales, adjuntar carta poder simple firmada por el poderdante indicando su documento de identidad. En caso de representación de personas jurídicas consignar los datos del representante legal, número de partida electrónica y asiento de inscripción en la Superintendencia Nacional de Registros Públicos (SUNARP).\n" +
                "Sección IV: Consignar los datos del establecimiento, tipo de actividad a desarrollar y zonificación. Los campos correspondientes al \"Código CIIU\" y \"Giro/s\" son completados por el representante de la municipalidad. Para aquellas actividades que, conforme al D.S. N° 006-2013-PCM, requieran autorización sectorial previa, consignar los datos de la autorización. Consignar el área total del establecimiento y en el croquis la ubicación exacta.\n" +
                "Sección V: De corresponder, marcar con una X los compromisos legales y firmar.\n" +
                "Sección VI: Sección llenada por el calificador designado de la municipalidad.", F_INST_BODY));
        c.addElement(p);
        t.addCell(c);

        document.add(t);
    }

    private Paragraph crearCheckItem(String texto, boolean marcado) {
        Paragraph p = new Paragraph();
        p.add(new Phrase(marcado ? "[ X ] " : "[   ] ", F_ITEM_BOLD));
        p.add(new Phrase(texto, F_ITEM));
        return p;
    }

    private PdfPCell crearCeldaCheckCentro(String texto, boolean marcado) {
        PdfPCell c = new PdfPCell();
        c.setBorderColor(BORDER_COLOR);
        c.setPadding(3f);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph p = new Paragraph();
        p.add(new Phrase(marcado ? "[ X ] " : "[   ] ", F_ITEM_BOLD));
        p.add(new Phrase(texto, F_ITEM));
        p.setAlignment(Element.ALIGN_CENTER);
        c.addElement(p);
        return c;
    }

    private PdfPCell crearCeldaDoble(String etiqueta, String valor) {
        PdfPCell c = new PdfPCell();
        c.setBorderColor(BORDER_COLOR);
        c.setPadding(2f);
        c.addElement(new Phrase(etiqueta, F_LABEL));
        c.addElement(new Phrase(" " + (valor != null ? valor : "-"), F_VAL));
        return c;
    }

    private Element crearMiniCroquis() {
        PdfPTable t = new PdfPTable(3);
        t.setWidthPercentage(90);
        t.setWidths(new float[]{45f, 10f, 45f});

        PdfPCell cM1 = new PdfPCell(new Phrase("MANZANA A", F_FOOT_NOTE));
        cM1.setBackgroundColor(new Color(230, 230, 230));
        cM1.setBorderColor(Color.GRAY);
        cM1.setFixedHeight(18f);
        cM1.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell cCalleV = new PdfPCell();
        cCalleV.setBorder(PdfPCell.NO_BORDER);

        PdfPCell cM2 = new PdfPCell(new Phrase("MANZANA B", F_FOOT_NOTE));
        cM2.setBackgroundColor(new Color(230, 230, 230));
        cM2.setBorderColor(Color.GRAY);
        cM2.setFixedHeight(18f);
        cM2.setHorizontalAlignment(Element.ALIGN_CENTER);

        t.addCell(cM1);
        t.addCell(cCalleV);
        t.addCell(cM2);

        PdfPCell cCalleH = new PdfPCell(new Phrase("=== VÍA PÚBLICA / CALLE PRINCIPAL ===", F_FOOT_NOTE));
        cCalleH.setColspan(3);
        cCalleH.setBorder(PdfPCell.NO_BORDER);
        cCalleH.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.addCell(cCalleH);

        PdfPCell cM3 = new PdfPCell(new Phrase("[ LOCAL OBJETO ]", F_ITEM_BOLD));
        cM3.setBackgroundColor(new Color(254, 240, 138));
        cM3.setBorderColor(Color.RED);
        cM3.setFixedHeight(18f);
        cM3.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell cM4 = new PdfPCell(new Phrase("MANZANA D", F_FOOT_NOTE));
        cM4.setBackgroundColor(new Color(230, 230, 230));
        cM4.setBorderColor(Color.GRAY);
        cM4.setFixedHeight(18f);
        cM4.setHorizontalAlignment(Element.ALIGN_CENTER);

        t.addCell(cM3);
        t.addCell(cCalleV);
        t.addCell(cM4);

        return t;
    }
}
