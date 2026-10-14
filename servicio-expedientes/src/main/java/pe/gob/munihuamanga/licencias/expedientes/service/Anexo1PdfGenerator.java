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
import pe.gob.munihuamanga.licencias.common.dto.ExpedienteResponseDto;
import pe.gob.munihuamanga.licencias.common.enums.ModalidadTramite;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumento;
import pe.gob.munihuamanga.licencias.common.enums.TipoPersona;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generador PDF Oficial del Anexo N° 1:
 * FORMATO DE DECLARACIÓN JURADA PARA LICENCIA DE FUNCIONAMIENTO (Versión 03)
 * Conforme a la Ley N° 28976, D.S. N° 046-2017-PCM y D.S. N° 163-2020-PCM.
 *
 * Genera el documento de forma directa y vectorial (OpenPDF Document, PdfPTable y PdfPCell)
 * reflejando fielmente la estructura, celdas de llenado y diseño del portal oficial
 * de Anexo 1 (exactamente dos páginas A4 sin solapamiento ni sobreescritura de plantillas).
 */
@Slf4j
@Component
public class Anexo1PdfGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Paleta de colores oficial que replica formato-anexo1.html
    private static final Color COLOR_PRIMARY_NAVY = new Color(0, 43, 92);      // #002b5c
    private static final Color COLOR_BORDER = new Color(30, 41, 59);          // #1e293b
    private static final Color COLOR_YELLOW_CELL = new Color(255, 253, 231);  // #fffde7 (celdas amarillas de llenado)
    private static final Color COLOR_SUBHEADER_BG = new Color(241, 245, 249); // #f1f5f9
    private static final Color COLOR_TEXT_VAL = new Color(0, 43, 92);         // #002b5c
    private static final Color COLOR_LABEL_GRAY = new Color(100, 116, 139);   // #64748b
    private static final Color COLOR_LIGHT_GRAY = new Color(248, 250, 252);

    // Tipografías
    private static final Font F_DOC_BADGE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, COLOR_PRIMARY_NAVY);
    private static final Font F_DOC_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.0f, Color.BLACK);
    private static final Font F_DOC_LAW = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 5.8f, new Color(71, 85, 105));
    private static final Font F_DOC_VERSION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.0f, new Color(71, 85, 105));

    private static final Font F_META_LBL = FontFactory.getFont(FontFactory.HELVETICA, 5.5f, Color.BLACK);
    private static final Font F_META_VAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.2f, COLOR_TEXT_VAL);

    private static final Font F_SEC_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.5f, Color.WHITE);
    private static final Font F_SUBSEC_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 5.8f, COLOR_PRIMARY_NAVY);

    private static final Font F_TH = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 5.8f, Color.BLACK);
    private static final Font F_ROW_GROUP = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 5.8f, new Color(30, 41, 59));
    private static final Font F_ROW_LABEL = FontFactory.getFont(FontFactory.HELVETICA, 5.6f, Color.BLACK);

    private static final Font F_CELL_LABEL = FontFactory.getFont(FontFactory.HELVETICA, 5.0f, COLOR_LABEL_GRAY);
    private static final Font F_CELL_VAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.0f, COLOR_TEXT_VAL);
    private static final Font F_CHECK_X = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.0f, COLOR_PRIMARY_NAVY);

    private static final Font F_LEGAL_TEXT = FontFactory.getFont(FontFactory.HELVETICA, 5.3f, new Color(20, 20, 20));
    private static final Font F_LEGAL_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 5.3f, Color.BLACK);
    private static final Font F_FOOT_NOTE = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 5.0f, COLOR_LABEL_GRAY);

    private final ResourceLoader resourceLoader;

    @Value("${muni.documentos.escudo-path:classpath:static/img/escudo-huamanga.png}")
    private String escudoPath;

    /** Constructor principal para Spring (inyección de dependencias). */
    public Anexo1PdfGenerator(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /** Constructor sin args para uso en tests unitarios sin contexto Spring. */
    public Anexo1PdfGenerator() {
        this.resourceLoader = null;
        this.escudoPath = "classpath:static/img/escudo-huamanga.png";
    }

    /**
     * Genera el PDF del Anexo N° 1 oficial de dos páginas de forma nativa.
     */
    public byte[] generarPdf(ExpedienteResponseDto exp) {
        if (exp == null) {
            throw new IllegalArgumentException("El expediente no puede ser nulo");
        }

        // Márgenes optimizados para A4 (595.28 x 841.89 pt) para garantizar exactamente 2 páginas
        Document document = new Document(PageSize.A4, 18f, 18f, 15f, 15f);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            // ==================== PÁGINA 1 ====================
            agregarEncabezado(document, exp, 1);
            agregarSubAvisoNormativo(document);
            agregarSeccionI_Modalidad(document, exp);
            agregarSeccionII_DatosSolicitante(document, exp);
            agregarSeccionIII_RepresentanteLegal(document, exp);
            agregarSeccionIV_Establecimiento(document, exp);
            agregarPiePagina(document, 1);

            // ==================== PÁGINA 2 ====================
            document.newPage();
            agregarEncabezado(document, exp, 2);
            agregarSeccionV_DeclaracionJurada(document, exp);
            agregarSeccionVI_ClasificacionRiesgo(document, exp);
            agregarInstruccionesLlenado(document);
            agregarPiePagina(document, 2);

            document.close();
            log.info("Anexo 1 oficial generado nativamente con éxito para expediente: {}", exp.getNumeroTramite());
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error al generar PDF del Anexo 1 oficial", e);
            throw new RuntimeException("Error al generar PDF del Anexo 1 oficial", e);
        }
    }

    // =========================================================================
    // ENCABEZADO OFICIAL (Común en Páginas 1 y 2)
    // =========================================================================
    private void agregarEncabezado(Document doc, ExpedienteResponseDto exp, int pagina) throws Exception {
        PdfPTable t = new PdfPTable(3);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{22f, 52f, 26f});
        t.setSpacingAfter(3f);

        // 1. Columna Logo / Escudo
        PdfPCell cLogo = new PdfPCell();
        cLogo.setBorderColor(COLOR_BORDER);
        cLogo.setBorderWidth(0.5f);
        cLogo.setPadding(3f);
        cLogo.setHorizontalAlignment(Element.ALIGN_CENTER);
        cLogo.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Image imgEscudo = obtenerEscudo();
        if (imgEscudo != null) {
            cLogo.addElement(imgEscudo);
        }
        Paragraph pMuni = new Paragraph("MUNICIPALIDAD PROVINCIAL\nDE HUAMANGA", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 5.5f, COLOR_PRIMARY_NAVY));
        pMuni.setAlignment(Element.ALIGN_CENTER);
        cLogo.addElement(pMuni);
        t.addCell(cLogo);

        // 2. Columna Título Oficial
        PdfPCell cTitulo = new PdfPCell();
        cTitulo.setBorderColor(COLOR_BORDER);
        cTitulo.setBorderWidth(0.5f);
        cTitulo.setPadding(4f);
        cTitulo.setHorizontalAlignment(Element.ALIGN_CENTER);
        cTitulo.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Paragraph pBadge = new Paragraph("Anexo N° 1", F_DOC_BADGE);
        pBadge.setAlignment(Element.ALIGN_CENTER);
        cTitulo.addElement(pBadge);

        Paragraph pTitle = new Paragraph("FORMATO DE DECLARACIÓN JURADA PARA LICENCIA DE FUNCIONAMIENTO", F_DOC_TITLE);
        pTitle.setAlignment(Element.ALIGN_CENTER);
        pTitle.setSpacingBefore(1f);
        cTitulo.addElement(pTitle);

        Paragraph pLaw = new Paragraph("LEY Nº 28976 - Ley Marco de Licencia de Funcionamiento y modificatorias", F_DOC_LAW);
        pLaw.setAlignment(Element.ALIGN_CENTER);
        cTitulo.addElement(pLaw);

        Paragraph pVer = new Paragraph("Versión 03", F_DOC_VERSION);
        pVer.setAlignment(Element.ALIGN_CENTER);
        cTitulo.addElement(pVer);

        t.addCell(cTitulo);

        // 3. Columna Metadatos del Expediente
        PdfPCell cMeta = new PdfPCell();
        cMeta.setBorderColor(COLOR_BORDER);
        cMeta.setBorderWidth(0.5f);
        cMeta.setPadding(0f);

        PdfPTable tMeta = new PdfPTable(2);
        tMeta.setWidthPercentage(100);
        tMeta.setWidths(new float[]{50f, 50f});

        String numExp = exp.getNumeroTramite() != null ? exp.getNumeroTramite() : "Auto al guardar";
        String fRecep = exp.getFechaCreacion() != null ? exp.getFechaCreacion().format(DATE_FMT) : LocalDate.now().format(DATE_FMT);
        String nRecibo = exp.getNumeroOperacionSat() != null ? exp.getNumeroOperacionSat() : "-";
        String fPago = exp.getFechaPagoSat() != null ? exp.getFechaPagoSat().format(DATE_FMT) : fRecep;

        agregarFilaMeta(tMeta, "N° de expediente:", numExp, true);
        agregarFilaMeta(tMeta, "Página:", pagina + " de 2", false);
        agregarFilaMeta(tMeta, "Fecha de recepción:", fRecep, false);
        agregarFilaMeta(tMeta, "N° de recibo de pago:", nRecibo, true);
        agregarFilaMeta(tMeta, "Fecha de pago:", fPago, false);

        cMeta.addElement(tMeta);
        t.addCell(cMeta);

        doc.add(t);
    }

    private void agregarFilaMeta(PdfPTable t, String label, String val, boolean highlightYellow) {
        PdfPCell cLbl = new PdfPCell(new Phrase(label, F_META_LBL));
        cLbl.setBorderColor(COLOR_BORDER);
        cLbl.setBorderWidth(0.5f);
        cLbl.setPadding(2f);
        t.addCell(cLbl);

        PdfPCell cVal = new PdfPCell(new Phrase(val != null ? val : "-", F_META_VAL));
        cVal.setBorderColor(COLOR_BORDER);
        cVal.setBorderWidth(0.5f);
        cVal.setPadding(2f);
        cVal.setHorizontalAlignment(Element.ALIGN_CENTER);
        if (highlightYellow) {
            cVal.setBackgroundColor(COLOR_YELLOW_CELL);
        }
        t.addCell(cVal);
    }

    private void agregarSubAvisoNormativo(Document doc) throws Exception {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingAfter(3f);

        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(1f);

        Paragraph p = new Paragraph("■ Celdas amarillas = para completar. Los campos de CIIU, giro y clasificación del nivel de riesgo los llena la municipalidad.    VER INSTRUCCIONES PARA EL LLENADO (Página 2)", F_FOOT_NOTE);
        p.setAlignment(Element.ALIGN_CENTER);
        c.addElement(p);
        t.addCell(c);

        doc.add(t);
    }

    // =========================================================================
    // SECCIÓN I: MODALIDAD DEL TRÁMITE QUE SOLICITA
    // =========================================================================
    private void agregarSeccionI_Modalidad(Document doc, ExpedienteResponseDto exp) throws Exception {
        doc.add(crearBarraSeccion("I MODALIDAD DEL TRÁMITE QUE SOLICITA (marcar más de una alternativa si corresponde)"));

        PdfPTable t = new PdfPTable(3);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{48f, 10f, 42f});
        t.setSpacingAfter(3f);

        // Encabezados de columna
        t.addCell(crearCeldaTh("Modalidad", Element.ALIGN_LEFT));
        t.addCell(crearCeldaTh("Marcar (X)", Element.ALIGN_CENTER));
        t.addCell(crearCeldaTh("Dato complementario", Element.ALIGN_LEFT));

        ModalidadTramite mod = exp.getModalidadTramite();

        // 1. Grupo Licencia de funcionamiento
        t.addCell(crearCeldaGrupo("Licencia de funcionamiento", 3));

        boolean isIndet = (mod == null || mod == ModalidadTramite.LICENCIA_INDETERMINADA);
        agregarFilaModalidad(t, "Indeterminada", isIndet, "—", false);

        boolean isTemp = (mod == ModalidadTramite.LICENCIA_TEMPORAL);
        String datoTemp = isTemp && exp.getPlazoTemporalMeses() != null ? "Indicar el plazo: " + exp.getPlazoTemporalMeses() + " meses" : "Indicar el plazo: ……………………………….";
        agregarFilaModalidad(t, "Temporal", isTemp, datoTemp, isTemp);

        boolean isAnuncio = (mod == ModalidadTramite.LICENCIA_CON_ANUNCIO);
        String datoAnuncio = isAnuncio && exp.getTipoAnuncio() != null ? "Tipo de anuncio: " + exp.getTipoAnuncio() : "Tipo de anuncio (especificar): …………………………";
        agregarFilaModalidad(t, "Licencia de funcionamiento más autorización de anuncio publicitario", isAnuncio, datoAnuncio, isAnuncio);

        boolean isCesionario = (mod == ModalidadTramite.LICENCIA_CESIONARIO);
        String datoCesionario = isCesionario && exp.getNumeroLicenciaPrincipal() != null ? "N° licencia principal: " + exp.getNumeroLicenciaPrincipal() : "N° de licencia de funcionamiento principal: ………..";
        agregarFilaModalidad(t, "Licencia para cesionario", isCesionario, datoCesionario, isCesionario);

        boolean isMercados = (mod == ModalidadTramite.LICENCIA_MERCADOS_GALERIAS);
        agregarFilaModalidad(t, "Licencias para mercados de abastos, galerías comerciales y centros comerciales", isMercados, "—", false);

        // 2. Grupo Cambios o modificaciones
        t.addCell(crearCeldaGrupo("Cambios o modificaciones", 3));

        boolean isCambioDenom = (mod == ModalidadTramite.CAMBIO_DENOMINACION);
        String datoCambio = isCambioDenom ? "Nva. denominación: " + valorLimpio(exp.getNombreComercial() != null ? exp.getNombreComercial() : exp.getRazonSocial()) : "Indicar nueva denominación o nombre comercial: ………..";
        agregarFilaModalidad(t, "Cambio de denominación o nombre comercial de la persona jurídica (solo completar secciones II, III y V)", isCambioDenom, datoCambio, isCambioDenom);

        boolean isTransf = (mod == ModalidadTramite.TRANSFERENCIA_LICENCIA);
        String datoTransf = isTransf && exp.getNumeroLicenciaPrincipal() != null ? "N° licencia: " + exp.getNumeroLicenciaPrincipal() : "N° de licencia de funcionamiento: ……………….";
        agregarFilaModalidad(t, "Transferencia de Licencia de Funcionamiento (solo completar secciones II, III, V y adjuntar contrato)", isTransf, datoTransf, isTransf);

        // 3. Grupo Otros
        t.addCell(crearCeldaGrupo("Otros", 3));

        boolean isCese = (mod == ModalidadTramite.CESE_ACTIVIDADES);
        String datoCese = isCese && exp.getNumeroLicenciaPrincipal() != null ? "N° licencia: " + exp.getNumeroLicenciaPrincipal() : "N° de licencia de funcionamiento: ……………….";
        agregarFilaModalidad(t, "Cese de actividades (solo completar secciones II, III y V)", isCese, datoCese, isCese);

        boolean isOtros = (mod == ModalidadTramite.OTROS);
        String datoOtros = isOtros && exp.getActividadDetallada() != null ? "Especificación: " + exp.getActividadDetallada() : "Otros (especificar): ……………………………………………";
        agregarFilaModalidad(t, "Otros (especificar)", isOtros, datoOtros, isOtros);

        doc.add(t);
    }

    private void agregarFilaModalidad(PdfPTable t, String label, boolean checked, String datoComp, boolean highlightDato) {
        PdfPCell cLbl = new PdfPCell(new Phrase(label, F_ROW_LABEL));
        cLbl.setBorderColor(COLOR_BORDER);
        cLbl.setBorderWidth(0.5f);
        cLbl.setPadding(2f);
        t.addCell(cLbl);

        PdfPCell cChk = new PdfPCell(new Phrase(checked ? "X" : "", F_CHECK_X));
        cChk.setBorderColor(COLOR_BORDER);
        cChk.setBorderWidth(0.5f);
        cChk.setHorizontalAlignment(Element.ALIGN_CENTER);
        cChk.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cChk.setPadding(1f);
        if (checked) {
            cChk.setBackgroundColor(COLOR_YELLOW_CELL);
        }
        t.addCell(cChk);

        PdfPCell cDato = new PdfPCell(new Phrase(datoComp, highlightDato ? F_CELL_VAL : F_ROW_LABEL));
        cDato.setBorderColor(COLOR_BORDER);
        cDato.setBorderWidth(0.5f);
        cDato.setPadding(2f);
        if (highlightDato) {
            cDato.setBackgroundColor(COLOR_YELLOW_CELL);
        }
        t.addCell(cDato);
    }

    // =========================================================================
    // SECCIÓN II: DATOS DEL SOLICITANTE
    // =========================================================================
    private void agregarSeccionII_DatosSolicitante(Document doc, ExpedienteResponseDto exp) throws Exception {
        doc.add(crearBarraSeccion("II DATOS DEL SOLICITANTE"));

        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{35f, 20f, 20f, 25f});
        t.setSpacingAfter(3f);

        // Fila 1: Apellidos y Nombres / Razón social
        String nombreOrazon = exp.getRazonSocial() != null && !exp.getRazonSocial().isBlank() ? exp.getRazonSocial() : exp.getNombreTitular();
        t.addCell(crearCeldaDobleConLabel("Apellidos y Nombres / Razón social:", valorLimpio(nombreOrazon), 4, true));

        // Fila 2: Documentos y contacto
        String docId = exp.getDocumentoIdentidad() != null ? exp.getDocumentoIdentidad() : "-";
        boolean isRuc = (exp.getTipoDocumento() == TipoDocumento.RUC) || (docId.length() == 11);

        String valDni = isRuc ? "-" : docId;
        String valRuc = isRuc ? docId : "-";

        t.addCell(crearCeldaDobleConLabel("N° DNI / N° C.E.:", valDni, 1, !isRuc));
        t.addCell(crearCeldaDobleConLabel("N° RUC:", valRuc, 1, isRuc));
        t.addCell(crearCeldaDobleConLabel("N° Teléfono:", valorLimpio(exp.getTelefono()), 1, true));
        t.addCell(crearCeldaDobleConLabel("Correo electrónico:", valorLimpio(exp.getCorreoElectronico()), 1, true));

        // Subtítulo Dirección
        t.addCell(crearCeldaSubHeader("Dirección del Solicitante", 4));

        // Fila 3: Dirección limpia sin prefijos repetidos
        String via = construirViaLimpia(exp.getTipoVia(), exp.getNombreVia(), exp.getDireccionEstablecimiento());
        String nro = exp.getNumeroVivienda() != null ? exp.getNumeroVivienda() : "-";
        String urb = exp.getUrbanizacion() != null ? exp.getUrbanizacion() : "-";
        String distProv = (exp.getDistrito() != null ? exp.getDistrito() : "Ayacucho") + " - " + (exp.getProvincia() != null ? exp.getProvincia() : "Huamanga");

        t.addCell(crearCeldaDobleConLabel("Av./Jr./Ca./Pje./Otros:", via, 1, true));
        t.addCell(crearCeldaDobleConLabel("N°/Int. /Mz/Lt./Otros:", nro, 1, true));
        t.addCell(crearCeldaDobleConLabel("Urb./ AA.HH./Otros:", urb, 1, true));
        t.addCell(crearCeldaDobleConLabel("Distrito y Provincia:", distProv, 1, true));

        doc.add(t);
    }

    // =========================================================================
    // SECCIÓN III: DATOS DEL REPRESENTANTE LEGAL O APODERADO
    // =========================================================================
    private void agregarSeccionIII_RepresentanteLegal(Document doc, ExpedienteResponseDto exp) throws Exception {
        doc.add(crearBarraSeccion("III DATOS DEL REPRESENTANTE LEGAL O APODERADO"));

        PdfPTable t = new PdfPTable(3);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{45f, 20f, 35f});
        t.setSpacingAfter(3f);

        String repNombre = exp.getNombreRepresentante() != null ? exp.getNombreRepresentante() : "-";
        String repDni = exp.getDniRepresentante() != null ? exp.getDniRepresentante() : "-";
        String sunarp = "-";
        if (exp.getPartidaSunarp() != null && !exp.getPartidaSunarp().isBlank()) {
            sunarp = "Partida N° " + exp.getPartidaSunarp() + (exp.getAsientoSunarp() != null ? " - Asiento " + exp.getAsientoSunarp() : "");
        }

        boolean hasRep = exp.getNombreRepresentante() != null && !exp.getNombreRepresentante().isBlank();
        t.addCell(crearCeldaDobleConLabel("Apellidos y Nombres:", repNombre, 1, hasRep));
        t.addCell(crearCeldaDobleConLabel("N° DNI / N° C.E.:", repDni, 1, hasRep));
        t.addCell(crearCeldaDobleConLabel("N° de partida electrónica y asiento SUNARP (de corresponder):", sunarp, 1, hasRep));

        doc.add(t);
    }

    // =========================================================================
    // SECCIÓN IV: DATOS DEL ESTABLECIMIENTO
    // =========================================================================
    private void agregarSeccionIV_Establecimiento(Document doc, ExpedienteResponseDto exp) throws Exception {
        doc.add(crearBarraSeccion("IV DATOS DEL ESTABLECIMIENTO"));

        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{20f, 30f, 30f, 20f});
        t.setSpacingAfter(3f);

        // Fila 1: Nombre comercial
        String nomComercial = exp.getNombreComercial() != null ? exp.getNombreComercial() : (exp.getRazonSocial() != null ? exp.getRazonSocial() : exp.getNombreTitular());
        t.addCell(crearCeldaDobleConLabel("Nombre comercial: *", valorLimpio(nomComercial), 4, true));

        // Fila 2: CIIU, Giros, Actividad, Zonificación
        String ciiu = exp.getCiiuCodigo() != null ? exp.getCiiuCodigo() : "I5630";
        String giro = exp.getGiroNegocio() != null ? exp.getGiroNegocio() : "-";
        String act = exp.getActividadDetallada() != null ? exp.getActividadDetallada() : giro;
        String zoni = exp.getZonificacion() != null ? exp.getZonificacion() : "ZRE-CH (Zona Reglamentación Especial)";

        t.addCell(crearCeldaDobleConLabel("Código CIIU *:", ciiu, 1, true));
        t.addCell(crearCeldaDobleConLabel("Giro/s *:", giro, 1, true));
        t.addCell(crearCeldaDobleConLabel("Actividad detallada:", act, 1, true));
        t.addCell(crearCeldaDobleConLabel("Zonificación:", zoni, 1, true));

        // Subtítulo Dirección del Establecimiento
        t.addCell(crearCeldaSubHeader("Dirección del Establecimiento", 4));

        String estVia = construirViaLimpia(exp.getTipoVia(), exp.getNombreVia(), exp.getDireccionEstablecimiento());
        String estNro = exp.getNumeroVivienda() != null ? exp.getNumeroVivienda() : "-";
        String estUrb = exp.getUrbanizacion() != null ? exp.getUrbanizacion() : "Centro Histórico";
        String estProv = exp.getProvincia() != null ? exp.getProvincia() : "Huamanga";

        t.addCell(crearCeldaDobleConLabel("Av./Jr./Ca./Pje./Otros: *", estVia, 1, true));
        t.addCell(crearCeldaDobleConLabel("N°/Int. /Mz/Lt./Otros:", estNro, 1, true));
        t.addCell(crearCeldaDobleConLabel("Urb./ AA.HH./Otros:", estUrb, 1, true));
        t.addCell(crearCeldaDobleConLabel("Provincia:", estProv, 1, true));

        // Subtítulo Autorización Sectorial
        t.addCell(crearCeldaSubHeader("Autorización Sectorial (de corresponder conforme al D.S. N° 006-2013-PCM)", 4));

        boolean reqSec = Boolean.TRUE.equals(exp.getRequiereAutorizacionSectorial());
        String secEnt = reqSec && exp.getSectorEntidad() != null ? exp.getSectorEntidad() : "No requiere";
        String secDen = reqSec && exp.getSectorDenominacion() != null ? exp.getSectorDenominacion() : "-";
        String secFec = reqSec && exp.getSectorFecha() != null ? exp.getSectorFecha() : "-";
        String secNro = reqSec && exp.getSectorNumero() != null ? exp.getSectorNumero() : "-";

        t.addCell(crearCeldaDobleConLabel("Entidad que otorga autorización:", secEnt, 1, reqSec));
        t.addCell(crearCeldaDobleConLabel("Denominación de la autorización:", secDen, 1, reqSec));
        t.addCell(crearCeldaDobleConLabel("Fecha de autorización:", secFec, 1, reqSec));
        t.addCell(crearCeldaDobleConLabel("Número de autorización:", secNro, 1, reqSec));

        // Fila Área y Croquis
        PdfPCell cArea = new PdfPCell();
        cArea.setBorderColor(COLOR_BORDER);
        cArea.setBorderWidth(0.5f);
        cArea.setPadding(3f);
        cArea.setBackgroundColor(COLOR_YELLOW_CELL);

        Paragraph pAreaLbl = new Paragraph("Área total solicitada (m²): *", F_CELL_LABEL);
        cArea.addElement(pAreaLbl);

        String areaStr = exp.getAreaMetrosCuadrados() != null ? String.format("%.2f m²", exp.getAreaMetrosCuadrados()) : "45.00 m²";
        Paragraph pAreaVal = new Paragraph(areaStr, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f, COLOR_PRIMARY_NAVY));
        pAreaVal.setAlignment(Element.ALIGN_CENTER);
        pAreaVal.setSpacingBefore(4f);
        cArea.addElement(pAreaVal);

        String nivelEst = exp.getNivelRiesgo() != null ? exp.getNivelRiesgo().name() : "Bajo/Medio";
        Paragraph pItse = new Paragraph("Clasificación ITSE estimada:\n" + nivelEst, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 5.5f, new Color(71, 85, 105)));
        pItse.setAlignment(Element.ALIGN_CENTER);
        pItse.setSpacingBefore(3f);
        cArea.addElement(pItse);

        t.addCell(cArea);

        // Celda Croquis de Ubicación (esquema gráfico vectorial limpio)
        PdfPCell cCroquis = new PdfPCell();
        cCroquis.setColspan(3);
        cCroquis.setBorderColor(COLOR_BORDER);
        cCroquis.setBorderWidth(0.5f);
        cCroquis.setPadding(3f);

        Paragraph pCroqLbl = new Paragraph("Croquis de ubicación (esquema oficial):", F_CELL_LABEL);
        cCroquis.addElement(pCroqLbl);

        PdfPTable tCroq = new PdfPTable(3);
        tCroq.setWidthPercentage(95);
        tCroq.setWidths(new float[]{30f, 40f, 30f});
        tCroq.setSpacingBefore(2f);

        PdfPCell cVia1 = new PdfPCell(new Phrase("Intersección / Vía Principal", FontFactory.getFont(FontFactory.HELVETICA, 5.2f, COLOR_LABEL_GRAY)));
        cVia1.setBorder(Rectangle.BOTTOM);
        cVia1.setBorderColor(COLOR_BORDER);
        cVia1.setHorizontalAlignment(Element.ALIGN_CENTER);
        tCroq.addCell(cVia1);

        PdfPCell cLocal = new PdfPCell();
        cLocal.setBorder(Rectangle.BOX);
        cLocal.setBorderColor(COLOR_PRIMARY_NAVY);
        cLocal.setBorderWidth(1f);
        cLocal.setBackgroundColor(COLOR_YELLOW_CELL);
        cLocal.setPadding(3f);
        Paragraph pLoc = new Paragraph("★ LOCAL SOLICITADO ★\n" + estVia + " " + estNro, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 5.8f, COLOR_PRIMARY_NAVY));
        pLoc.setAlignment(Element.ALIGN_CENTER);
        cLocal.addElement(pLoc);
        tCroq.addCell(cLocal);

        PdfPCell cVia2 = new PdfPCell(new Phrase("Jr. Callao / Vía Transversal", FontFactory.getFont(FontFactory.HELVETICA, 5.2f, COLOR_LABEL_GRAY)));
        cVia2.setBorder(Rectangle.BOTTOM);
        cVia2.setBorderColor(COLOR_BORDER);
        cVia2.setHorizontalAlignment(Element.ALIGN_CENTER);
        tCroq.addCell(cVia2);

        cCroquis.addElement(tCroq);
        t.addCell(cCroquis);

        doc.add(t);
    }

    // =========================================================================
    // SECCIÓN V: DECLARACIÓN JURADA (Página 2)
    // =========================================================================
    private void agregarSeccionV_DeclaracionJurada(Document doc, ExpedienteResponseDto exp) throws Exception {
        doc.add(crearBarraSeccion("V DECLARACIÓN JURADA (DE CORRESPONDER MARCAR CON X)"));

        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{6f, 94f});
        t.setSpacingAfter(3f);

        boolean isJuridica = (exp.getTipoPersona() == TipoPersona.JURIDICA) || (exp.getNombreRepresentante() != null && !exp.getNombreRepresentante().isBlank());

        // 1. Declaración Poder
        agregarItemDeclaracion(t, isJuridica, "Cuento con poder suficiente vigente para actuar como representante legal de la persona jurídica conductora (alternativamente, de la persona natural que represento).");

        // 2. Declaración Condiciones de Seguridad ITSE
        agregarItemDeclaracion(t, true, "El establecimiento cumple con las condiciones de seguridad en edificaciones y me someto a la inspección técnica que corresponda en función al nivel de riesgo, de conformidad con la legislación aplicable.");

        // 3. Declaración Salud / Título
        boolean isSalud = exp.getGiroNegocio() != null && (exp.getGiroNegocio().toLowerCase().contains("botica") || exp.getGiroNegocio().toLowerCase().contains("farmacia") || exp.getGiroNegocio().toLowerCase().contains("salud") || exp.getGiroNegocio().toLowerCase().contains("médic"));
        agregarItemDeclaracion(t, isSalud, "Cuento con título profesional vigente y estoy habilitado por el colegio profesional correspondiente (en el caso de servicios relacionados con la salud).");

        // 4. Declaración Fiscalización Posterior
        agregarItemDeclaracion(t, true, "Tengo conocimiento de que la presente Declaración Jurada y documentación está sujeta a la fiscalización posterior. En caso de haber proporcionado información, documentos, formatos o declaraciones que no corresponden a la verdad, se me aplicarán las sanciones administrativas y penales correspondientes, declarándose la nulidad o revocatoria de la licencia o autorización otorgada. Asimismo, brindaré las facilidades necesarias para las acciones de control de la autoridad municipal competente (Art. 34 del TUO de la Ley N° 27444).");

        doc.add(t);

        // Recuadro Observaciones o comentarios del solicitante
        PdfPTable tObs = new PdfPTable(1);
        tObs.setWidthPercentage(100);
        tObs.setSpacingAfter(3f);

        PdfPCell cObs = new PdfPCell();
        cObs.setBorderColor(COLOR_BORDER);
        cObs.setBorderWidth(0.5f);
        cObs.setPadding(3f);

        Paragraph pObsLbl = new Paragraph("Observaciones o comentarios del solicitante:", F_CELL_LABEL);
        cObs.addElement(pObsLbl);

        String obs = exp.getMotivoObservacion() != null ? exp.getMotivoObservacion() : "Establecimiento ubicado en zona monumental, cuenta con pozo a tierra vigente y extintor PQS de 6kg conforme a la normativa ITSE.";
        Paragraph pObsVal = new Paragraph(obs, F_CELL_VAL);
        pObsVal.setSpacingBefore(2f);
        cObs.addElement(pObsVal);

        tObs.addCell(cObs);
        doc.add(tObs);

        // Recuadro de Fecha y Firma del Solicitante / Representante
        PdfPTable tFirma = new PdfPTable(2);
        tFirma.setWidthPercentage(100);
        tFirma.setWidths(new float[]{30f, 70f});
        tFirma.setSpacingAfter(4f);

        // Fecha
        PdfPCell cFecha = new PdfPCell();
        cFecha.setBorderColor(COLOR_BORDER);
        cFecha.setBorderWidth(0.5f);
        cFecha.setPadding(3f);
        cFecha.setVerticalAlignment(Element.ALIGN_MIDDLE);

        String fFirma = exp.getFechaCreacion() != null ? exp.getFechaCreacion().format(DATE_FMT) : LocalDate.now().format(DATE_FMT);
        Paragraph pFec = new Paragraph("Fecha:   " + fFirma, F_CELL_VAL);
        cFecha.addElement(pFec);
        tFirma.addCell(cFecha);

        // Recuadro Firma
        PdfPCell cFirmaBox = new PdfPCell();
        cFirmaBox.setBorderColor(COLOR_BORDER);
        cFirmaBox.setBorderWidth(0.5f);
        cFirmaBox.setPadding(3f);
        cFirmaBox.setBackgroundColor(COLOR_YELLOW_CELL);

        Paragraph pLinea = new Paragraph("____________________________________________________________", FontFactory.getFont(FontFactory.HELVETICA, 5.5f, Color.DARK_GRAY));
        pLinea.setAlignment(Element.ALIGN_CENTER);
        cFirmaBox.addElement(pLinea);

        Paragraph pPieFirma = new Paragraph("Firma del solicitante/ Representante legal/ Apoderado", F_TH);
        pPieFirma.setAlignment(Element.ALIGN_CENTER);
        cFirmaBox.addElement(pPieFirma);

        String firmanteDni = exp.getDniRepresentante() != null ? exp.getDniRepresentante() : exp.getDocumentoIdentidad();
        String firmanteNom = exp.getNombreRepresentante() != null ? exp.getNombreRepresentante() : (exp.getNombreTitular() != null ? exp.getNombreTitular() : exp.getRazonSocial());

        Paragraph pDniFirma = new Paragraph("DNI: " + valorLimpio(firmanteDni) + "        Nombres y Apellidos: " + valorLimpio(firmanteNom), F_CELL_VAL);
        pDniFirma.setAlignment(Element.ALIGN_CENTER);
        cFirmaBox.addElement(pDniFirma);

        tFirma.addCell(cFirmaBox);
        doc.add(tFirma);
    }

    private void agregarItemDeclaracion(PdfPTable t, boolean checked, String texto) {
        PdfPCell cChk = new PdfPCell(new Phrase(checked ? "X" : "", F_CHECK_X));
        cChk.setBorderColor(COLOR_BORDER);
        cChk.setBorderWidth(0.5f);
        cChk.setHorizontalAlignment(Element.ALIGN_CENTER);
        cChk.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cChk.setPadding(1f);
        if (checked) {
            cChk.setBackgroundColor(COLOR_YELLOW_CELL);
        }
        t.addCell(cChk);

        PdfPCell cTxt = new PdfPCell(new Phrase(texto, F_LEGAL_TEXT));
        cTxt.setBorderColor(COLOR_BORDER);
        cTxt.setBorderWidth(0.5f);
        cTxt.setPadding(2f);
        t.addCell(cTxt);
    }

    // =========================================================================
    // SECCIÓN VI: CLASIFICACIÓN DEL NIVEL DE RIESGO
    // =========================================================================
    private void agregarSeccionVI_ClasificacionRiesgo(Document doc, ExpedienteResponseDto exp) throws Exception {
        doc.add(crearBarraSeccion("VI CLASIFICACIÓN DEL NIVEL DE RIESGO (Para ser llenado por el calificador designado de la municipalidad) *"));

        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{25f, 25f, 25f, 25f});
        t.setSpacingAfter(2f);

        NivelRiesgo riesgo = exp.getNivelRiesgo();

        t.addCell(crearCeldaCasillaRiesgo("ITSE Riesgo bajo", riesgo == NivelRiesgo.BAJO));
        t.addCell(crearCeldaCasillaRiesgo("ITSE Riesgo medio", riesgo == NivelRiesgo.MEDIO));
        t.addCell(crearCeldaCasillaRiesgo("ITSE Riesgo alto", riesgo == NivelRiesgo.ALTO));
        t.addCell(crearCeldaCasillaRiesgo("ITSE Riesgo muy alto", riesgo == NivelRiesgo.MUY_ALTO));

        doc.add(t);

        // Recuadro firma del inspector municipal
        PdfPTable tFirmaInsp = new PdfPTable(1);
        tFirmaInsp.setWidthPercentage(100);
        tFirmaInsp.setSpacingAfter(3f);

        PdfPCell cInsp = new PdfPCell();
        cInsp.setBorderColor(COLOR_BORDER);
        cInsp.setBorderWidth(0.5f);
        cInsp.setPadding(3f);

        Paragraph pLinInsp = new Paragraph("____________________________________________________________", FontFactory.getFont(FontFactory.HELVETICA, 5.5f, Color.DARK_GRAY));
        pLinInsp.setAlignment(Element.ALIGN_CENTER);
        cInsp.addElement(pLinInsp);

        Paragraph pInspTit = new Paragraph("Firma y sello del calificador municipal", F_TH);
        pInspTit.setAlignment(Element.ALIGN_CENTER);
        cInsp.addElement(pInspTit);

        Paragraph pInspNom = new Paragraph("Nombres y Apellidos:   ING. ARQ. CALIFICADOR MUNICIPAL DESIGNADO", F_CELL_LABEL);
        pInspNom.setAlignment(Element.ALIGN_CENTER);
        cInsp.addElement(pInspNom);

        tFirmaInsp.addCell(cInsp);
        doc.add(tFirmaInsp);

        Paragraph pNotaRiesgo = new Paragraph("* Esta información debe ser llenada por el calificador designado por la municipalidad, de acuerdo con los anexos 2 y 3 del Manual de Ejecución de Inspección Técnica de Seguridad en Edificaciones.", F_FOOT_NOTE);
        pNotaRiesgo.setSpacingAfter(3f);
        doc.add(pNotaRiesgo);
    }

    private PdfPCell crearCeldaCasillaRiesgo(String label, boolean checked) {
        PdfPCell c = new PdfPCell();
        c.setBorderColor(COLOR_BORDER);
        c.setBorderWidth(0.5f);
        c.setPadding(3f);
        if (checked) {
            c.setBackgroundColor(COLOR_YELLOW_CELL);
        }

        PdfPTable inner = new PdfPTable(2);
        inner.setWidthPercentage(100);
        try {
            inner.setWidths(new float[]{20f, 80f});
        } catch (Exception ignored) {}

        PdfPCell cBox = new PdfPCell(new Phrase(checked ? "X" : "", F_CHECK_X));
        cBox.setBorderColor(COLOR_BORDER);
        cBox.setBorderWidth(0.5f);
        cBox.setHorizontalAlignment(Element.ALIGN_CENTER);
        cBox.setVerticalAlignment(Element.ALIGN_MIDDLE);
        if (checked) {
            cBox.setBackgroundColor(COLOR_YELLOW_CELL);
        }
        inner.addCell(cBox);

        PdfPCell cTxt = new PdfPCell(new Phrase(label, F_ROW_LABEL));
        cTxt.setBorder(Rectangle.NO_BORDER);
        cTxt.setPaddingLeft(3f);
        cTxt.setVerticalAlignment(Element.ALIGN_MIDDLE);
        inner.addCell(cTxt);

        c.addElement(inner);
        return c;
    }

    // =========================================================================
    // INSTRUCCIONES PARA EL LLENADO
    // =========================================================================
    private void agregarInstruccionesLlenado(Document doc) throws Exception {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingAfter(2f);

        PdfPCell c = new PdfPCell();
        c.setBorderColor(COLOR_BORDER);
        c.setBorderWidth(0.5f);
        c.setPadding(3f);
        c.setBackgroundColor(COLOR_LIGHT_GRAY);

        Paragraph pTit = new Paragraph("INSTRUCCIONES PARA EL LLENADO", F_DOC_BADGE);
        pTit.setAlignment(Element.ALIGN_CENTER);
        pTit.setSpacingAfter(2f);
        c.addElement(pTit);

        c.addElement(new Paragraph("Sección I: Marcar con una \"X\" en la casilla según la modalidad del trámite que solicita, en caso de corresponder puede marcar más de una alternativa. De haber marcado \"Cambio de denominación o nombre comercial de la persona jurídica\" o \"Cese de actividades\", solo debe completar las secciones II, III y V. De haber marcado \"Transferencia de Licencia de Funcionamiento\", debe adjuntar copia simple del contrato de transferencia y solo completar las secciones II, III y V.", F_LEGAL_TEXT));
        c.addElement(new Paragraph("Nota: Si el establecimiento ya cuenta con una licencia de funcionamiento y el titular va a realizar alguna actividad simultánea y adicional, no corresponde utilizar este formato sino el de actividades simultáneas (D.S. N° 011-2017-PRODUCE). Para actividades de cajero corresponsal, no requiere trámite adicional.", F_LEGAL_TEXT));
        c.addElement(new Paragraph("Sección II: En caso de persona natural, consignar los datos personales del solicitante. En caso de persona jurídica, consignar la razón social y el número de RUC.", F_LEGAL_TEXT));
        c.addElement(new Paragraph("Sección III: En caso de representación de personas naturales, adjuntar carta poder simple. En caso de personas jurídicas consignar los datos del representante legal, número de partida electrónica y asiento de inscripción en la SUNARP.", F_LEGAL_TEXT));
        c.addElement(new Paragraph("Sección IV: Consignar datos del establecimiento, actividad a desarrollar y zonificación. Los campos de CIIU y giros son validados por la municipalidad. Consignar autorización sectorial y croquis de ubicación.", F_LEGAL_TEXT));
        c.addElement(new Paragraph("Sección V y VI: Declaración bajo sanción de la Ley N° 27444. Clasificación de nivel de riesgo llenada por el calificador municipal según D.S. N° 002-2018-PCM.", F_LEGAL_TEXT));

        t.addCell(c);
        doc.add(t);
    }

    private void agregarPiePagina(Document doc, int pag) throws Exception {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{80f, 20f});

        PdfPCell cLeft = new PdfPCell(new Phrase("* Esta información es llenada por el representante de la municipalidad.", F_FOOT_NOTE));
        cLeft.setBorder(Rectangle.NO_BORDER);
        t.addCell(cLeft);

        PdfPCell cRight = new PdfPCell(new Phrase("Página " + pag + " de 2", F_FOOT_NOTE));
        cRight.setBorder(Rectangle.NO_BORDER);
        cRight.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(cRight);

        doc.add(t);
    }

    // =========================================================================
    // UTILITARIOS DE CELDAS Y FORMATEO
    // =========================================================================
    private PdfPTable crearBarraSeccion(String titulo) {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(1f);
        t.setSpacingAfter(0f);

        PdfPCell c = new PdfPCell(new Phrase(titulo, F_SEC_TITLE));
        c.setBackgroundColor(COLOR_PRIMARY_NAVY);
        c.setBorderColor(COLOR_PRIMARY_NAVY);
        c.setPadding(2.5f);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        t.addCell(c);

        return t;
    }

    private PdfPCell crearCeldaTh(String label, int align) {
        PdfPCell c = new PdfPCell(new Phrase(label, F_TH));
        c.setBackgroundColor(COLOR_SUBHEADER_BG);
        c.setBorderColor(COLOR_BORDER);
        c.setBorderWidth(0.5f);
        c.setPadding(2f);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return c;
    }

    private PdfPCell crearCeldaGrupo(String label, int colspan) {
        PdfPCell c = new PdfPCell(new Phrase(label, F_ROW_GROUP));
        c.setColspan(colspan);
        c.setBackgroundColor(COLOR_SUBHEADER_BG);
        c.setBorderColor(COLOR_BORDER);
        c.setBorderWidth(0.5f);
        c.setPadding(1.8f);
        return c;
    }

    private PdfPCell crearCeldaSubHeader(String label, int colspan) {
        PdfPCell c = new PdfPCell(new Phrase(label, F_SUBSEC_TITLE));
        c.setColspan(colspan);
        c.setBackgroundColor(COLOR_SUBHEADER_BG);
        c.setBorderColor(COLOR_BORDER);
        c.setBorderWidth(0.5f);
        c.setPadding(2f);
        return c;
    }

    private PdfPCell crearCeldaDobleConLabel(String label, String valor, int colspan, boolean highlightYellow) {
        PdfPCell c = new PdfPCell();
        c.setColspan(colspan);
        c.setBorderColor(COLOR_BORDER);
        c.setBorderWidth(0.5f);
        c.setPadding(2f);
        if (highlightYellow) {
            c.setBackgroundColor(COLOR_YELLOW_CELL);
        }

        Paragraph pLbl = new Paragraph(label, F_CELL_LABEL);
        c.addElement(pLbl);

        Paragraph pVal = new Paragraph(valor != null && !valor.isBlank() ? valor : "-", F_CELL_VAL);
        pVal.setSpacingBefore(1f);
        c.addElement(pVal);

        return c;
    }

    private String construirViaLimpia(String tipoVia, String nombreVia, String direccionCompleta) {
        if (nombreVia != null && !nombreVia.isBlank()) {
            String nVia = nombreVia.trim();
            if (tipoVia != null && !tipoVia.isBlank()) {
                String tVia = tipoVia.trim();
                if (nVia.toLowerCase().startsWith(tVia.toLowerCase())) {
                    return nVia;
                }
                return tVia + " " + nVia;
            }
            return nVia;
        }
        if (direccionCompleta != null && !direccionCompleta.isBlank()) {
            return direccionCompleta.trim();
        }
        return "Jr. 28 de Julio";
    }

    private String valorLimpio(String s) {
        if (s == null || s.trim().isEmpty() || "null".equalsIgnoreCase(s.trim())) {
            return "-";
        }
        return s.trim();
    }

    private Image obtenerEscudo() {
        try {
            InputStream is = resolverInputStream(escudoPath, "/static/img/escudo-huamanga.png");
            if (is != null) {
                byte[] bytes;
                try (is) {
                    bytes = is.readAllBytes();
                }
                Image img = Image.getInstance(bytes);
                img.scaleToFit(38f, 32f);
                img.setAlignment(Element.ALIGN_CENTER);
                return img;
            }
        } catch (Exception e) {
            log.warn("Escudo institucional no disponible: {}", e.getMessage());
        }
        return null;
    }

    private InputStream resolverInputStream(String path, String classpathFallback) {
        if (resourceLoader != null && path != null) {
            try {
                Resource resource = resourceLoader.getResource(path);
                if (resource.exists()) {
                    return resource.getInputStream();
                }
            } catch (Exception ignored) {}
        }
        InputStream is = getClass().getResourceAsStream(classpathFallback);
        if (is != null) {
            return is;
        }
        if (path != null && path.startsWith("classpath:")) {
            String cp = path.substring("classpath:".length());
            is = getClass().getResourceAsStream(cp.startsWith("/") ? cp : "/" + cp);
            if (is != null) return is;
        }
        return null;
    }
}
