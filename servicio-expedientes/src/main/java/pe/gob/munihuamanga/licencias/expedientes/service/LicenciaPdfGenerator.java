package pe.gob.munihuamanga.licencias.expedientes.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import pe.gob.munihuamanga.licencias.common.dto.ExpedienteResponseDto;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Generador del Certificado Oficial de Licencia de Funcionamiento en PDF.
 *
 * Replica fielmente el formato físico municipal emitido por la
 * Subgerencia de Comercio, Licencias y Control Sanitario de la
 * Municipalidad Provincial de Huamanga (Ley N° 28976 / TUO D.S. N° 046-2017-PCM).
 *
 * Estructura del documento:
 *  ┌─────────────────────────────────────────────────┐
 *  │  [Borde decorativo perimetral]      Nº XXXXXX   │
 *  │  [Escudo] MUNICIPALIDAD PROVINCIAL DE HUAMANGA  │
 *  │  [Franja] GERENCIA DE DESARROLLO ECONÓMICO      │
 *  │  [Franja] SUBGERENCIA DE COMERCIO Y LICENCIAS   │
 *  │       LICENCIA DE FUNCIONAMIENTO                │
 *  │       DEFINITIVA                                │
 *  │  Nombre o Razón Social: ___________             │
 *  │  Nombre Comercial:      ___________             │
 *  │  Dirección:             ___________             │
 *  │  R.U.C.: ______   Categoría: ___  Área: ___ m² │
 *  │  Giro(s): _________________________            │
 *  │  AYACUCHO, __ DE _______ 20XX  LICENCIA N°:    │
 *  │  Zonificación: ____________  Expediente N°:     │
 *  │  Autorización para operar: HH:mm  Hasta: HH:mm │
 *  │  Observación: _______________     VENCE: __/__  │
 *  │  ── INDICACIONES ──                             │
 *  │  [Firma Subgerente]             [QR]            │
 *  └─────────────────────────────────────────────────┘
 *
 * Ref: Formato Licencia N° 005023 y N° 000286 — Municipalidad de Huamanga.
 */
@Slf4j
@Service
public class LicenciaPdfGenerator {

    // ─── Paleta institucional ────────────────────────────────────────────────
    private static final Color NARANJA_INST  = new Color(210, 80, 15);
    private static final Color AZUL_INST     = new Color(0,  51, 120);
    private static final Color DORADO_TITULO = new Color(178, 124, 10);
    private static final Color GRIS_FILA     = new Color(245, 245, 245);
    private static final Color BORDE         = new Color(180, 180, 180);

    // ─── Formateadores ───────────────────────────────────────────────────────
    private static final DateTimeFormatter FMT_LARGA =
            DateTimeFormatter.ofPattern("dd 'DE' MMMM 'DE' yyyy", Locale.of("es", "PE"));

    // ════════════════════════════════════════════════════════════════════════
    // PUNTO DE ENTRADA PRINCIPAL
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Genera el PDF oficial de la Licencia de Funcionamiento.
     *
     * @param expediente Datos del expediente aprobado.
     * @param qrBytes    Bytes PNG del código QR de verificación.
     * @return Bytes del PDF generado.
     */
    public byte[] generarLicencia(ExpedienteResponseDto expediente, byte[] qrBytes) {
        Document doc = new Document(PageSize.A4, 30f, 30f, 30f, 30f);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            doc.open();
            PdfContentByte cb = writer.getDirectContent();

            // ── 1. Borde decorativo perimetral
            dibujarBorde(cb, doc);

            // ── 2. Número de Licencia en esquina superior derecha
            String numLic = resolverNumeroLicencia(expediente);
            dibujarNumeroEsquina(cb, doc, numLic);

            // ── 3. Encabezado institucional: Escudo + Municipalidad + Franjas
            encabezadoInstitucional(doc);

            // ── 4. Título "LICENCIA DE FUNCIONAMIENTO" / "DEFINITIVA"
            tituloPrincipal(doc);

            // ── 5. Marca de agua central (escudo semitransparente)
            marcaDeAgua(writer, doc);

            // ── 6. Tabla de campos normativos
            tablaCampos(doc, expediente);

            // ── 7. Fila: Ciudad, Fecha y Número de Licencia
            filaCiudadFechaLic(doc, expediente, numLic);

            // ── 8. Fila: Zonificación y Expediente N°
            filaZonificacionExpediente(doc, expediente);

            // ── 9. Fila: Autorización horaria + VENCE
            filaHorarioVencimiento(doc, expediente);

            // ── 10. Fila: Observación
            filaObservacion(doc);

            // ── 11. Sección INDICACIONES
            seccionIndicaciones(doc);

            // ── 12. Pie: Firma + QR
            pieFirmaQr(doc, expediente, qrBytes);

            doc.close();
            log.info("PDF Licencia generado — expediente: {}", expediente.getNumeroTramite());
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generando PDF Licencia para expediente: {}",
                    expediente != null ? expediente.getNumeroTramite() : "NULL", e);
            throw new RuntimeException("Error al generar PDF de Licencia de Funcionamiento", e);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 1. BORDE DECORATIVO
    // ════════════════════════════════════════════════════════════════════════

    private void dibujarBorde(PdfContentByte cb, Document doc) {
        float w = doc.getPageSize().getWidth();
        float h = doc.getPageSize().getHeight();
        float m = 12f;

        cb.saveState();
        // Borde exterior: azul institucional
        cb.setColorStroke(AZUL_INST);
        cb.setLineWidth(2.2f);
        cb.rectangle(m, m, w - 2 * m, h - 2 * m);
        cb.stroke();
        // Borde interior: dorado fino
        cb.setColorStroke(DORADO_TITULO);
        cb.setLineWidth(0.7f);
        float inn = m + 4f;
        cb.rectangle(inn, inn, w - 2 * inn, h - 2 * inn);
        cb.stroke();
        cb.restoreState();
    }

    // ════════════════════════════════════════════════════════════════════════
    // 2. NÚMERO DE LICENCIA EN ESQUINA
    // ════════════════════════════════════════════════════════════════════════

    private void dibujarNumeroEsquina(PdfContentByte cb, Document doc, String numLic) {
        try {
            float w = doc.getPageSize().getWidth();
            float h = doc.getPageSize().getHeight();

            cb.saveState();
            cb.setColorFill(new Color(170, 0, 0));
            cb.rectangle(w - 108f, h - 46f, 94f, 22f);
            cb.fill();

            cb.setColorFill(Color.WHITE);
            cb.beginText();
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);
            cb.setFontAndSize(bf, 9f);
            cb.showTextAligned(Element.ALIGN_CENTER, "N\u00ba " + numLic, w - 61f, h - 38f, 0);
            cb.endText();
            cb.restoreState();
        } catch (Exception e) {
            log.debug("Número de esquina omitido: {}", e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 3. ENCABEZADO INSTITUCIONAL
    // ════════════════════════════════════════════════════════════════════════

    private void encabezadoInstitucional(Document doc) throws Exception {
        // Tabla 2 columnas: Escudo (izq) | Textos institucionales (der)
        PdfPTable tbl = new PdfPTable(new float[]{16f, 84f});
        tbl.setWidthPercentage(100f);
        tbl.setSpacingAfter(0f);

        // Escudo — rowspan 3
        PdfPCell cEscudo = new PdfPCell();
        cEscudo.setBorder(Rectangle.NO_BORDER);
        cEscudo.setRowspan(3);
        cEscudo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cEscudo.setHorizontalAlignment(Element.ALIGN_CENTER);
        cEscudo.setPadding(3f);
        try {
            ClassPathResource r = new ClassPathResource("static/img/escudo-huamanga.png");
            try (InputStream is = r.getInputStream()) {
                Image img = Image.getInstance(is.readAllBytes());
                img.scaleToFit(54f, 54f);
                cEscudo.addElement(img);
            }
        } catch (Exception ex) {
            cEscudo.addElement(new Phrase("[Escudo]", f(8, Font.BOLD, AZUL_INST)));
        }
        tbl.addCell(cEscudo);

        // Fila 1: Nombre de la Municipalidad
        PdfPCell cMuni = new PdfPCell(new Phrase(
                "MUNICIPALIDAD PROVINCIAL DE HUAMANGA", f(13, Font.BOLD, AZUL_INST)));
        cMuni.setBorder(Rectangle.NO_BORDER);
        cMuni.setHorizontalAlignment(Element.ALIGN_CENTER);
        cMuni.setPaddingTop(6f);
        cMuni.setPaddingBottom(2f);
        tbl.addCell(cMuni);

        // Fila 2: Gerencia — franja naranja oscuro
        PdfPCell cGer = new PdfPCell(new Phrase(
                "GERENCIA DE DESARROLLO ECONÓMICO", f(9, Font.BOLD, Color.WHITE)));
        cGer.setBackgroundColor(NARANJA_INST);
        cGer.setBorder(Rectangle.NO_BORDER);
        cGer.setHorizontalAlignment(Element.ALIGN_CENTER);
        cGer.setPaddingTop(3f);
        cGer.setPaddingBottom(3f);
        tbl.addCell(cGer);

        // Fila 3: Subgerencia — franja naranja más claro
        PdfPCell cSub = new PdfPCell(new Phrase(
                "SUBGERENCIA DE COMERCIO, LICENCIAS Y CONTROL SANITARIO",
                f(8, Font.BOLD, Color.WHITE)));
        cSub.setBackgroundColor(new Color(220, 100, 30));
        cSub.setBorder(Rectangle.NO_BORDER);
        cSub.setHorizontalAlignment(Element.ALIGN_CENTER);
        cSub.setPaddingTop(2f);
        cSub.setPaddingBottom(2f);
        tbl.addCell(cSub);

        doc.add(tbl);
    }

    // ════════════════════════════════════════════════════════════════════════
    // 4. TÍTULO PRINCIPAL
    // ════════════════════════════════════════════════════════════════════════

    private void tituloPrincipal(Document doc) throws DocumentException {
        // "LICENCIA DE FUNCIONAMIENTO"
        Paragraph pTit = new Paragraph("LICENCIA  DE  FUNCIONAMIENTO",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22f, DORADO_TITULO));
        pTit.setAlignment(Element.ALIGN_CENTER);
        pTit.setSpacingBefore(5f);
        pTit.setSpacingAfter(1f);
        doc.add(pTit);

        // "DEFINITIVA"
        Paragraph pDef = new Paragraph("DEFINITIVA", f(10, Font.BOLD, AZUL_INST));
        pDef.setAlignment(Element.ALIGN_CENTER);
        pDef.setSpacingAfter(8f);
        doc.add(pDef);
    }

    // ════════════════════════════════════════════════════════════════════════
    // 5. MARCA DE AGUA
    // ════════════════════════════════════════════════════════════════════════

    private void marcaDeAgua(PdfWriter writer, Document doc) {
        try {
            ClassPathResource r = new ClassPathResource("static/img/escudo-huamanga.png");
            try (InputStream is = r.getInputStream()) {
                Image img = Image.getInstance(is.readAllBytes());
                float pw = doc.getPageSize().getWidth();
                float ph = doc.getPageSize().getHeight();
                img.scaleToFit(210f, 210f);
                img.setAbsolutePosition((pw - 210f) / 2f, (ph - 210f) / 2f);

                PdfContentByte cbUnder = writer.getDirectContentUnder();
                cbUnder.saveState();
                PdfGState gs = new PdfGState();
                gs.setFillOpacity(0.09f);
                cbUnder.setGState(gs);
                cbUnder.addImage(img);
                cbUnder.restoreState();
            }
        } catch (Exception e) {
            log.debug("Marca de agua no disponible: {}", e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 6. TABLA DE CAMPOS NORMATIVOS
    // ════════════════════════════════════════════════════════════════════════

    private void tablaCampos(Document doc, ExpedienteResponseDto exp) throws DocumentException {
        PdfPTable tbl = new PdfPTable(new float[]{34f, 66f});
        tbl.setWidthPercentage(97f);
        tbl.setSpacingAfter(2f);
        tbl.setHorizontalAlignment(Element.ALIGN_CENTER);

        // Nombre o Razón Social
        String razon = exp.getRazonSocial() != null && !exp.getRazonSocial().isBlank()
                ? exp.getRazonSocial().toUpperCase()
                : (exp.getNombreTitular() != null ? exp.getNombreTitular().toUpperCase() : "-");
        fila(tbl, "NOMBRE O RAZÓN SOCIAL:", razon, false);

        // Nombre Comercial
        String nomCom = exp.getNombreComercial() != null
                ? "\"" + exp.getNombreComercial().toUpperCase() + "\""
                : "-";
        fila(tbl, "NOMBRE COMERCIAL:", nomCom, true);

        // Dirección
        fila(tbl, "DIRECCIÓN:", construirDireccion(exp), false);

        // Fila especial RUC | Categoría | Área
        filaRucCategoriaArea(tbl, exp);

        // Giro(s)
        String giro = exp.getGiroNegocio() != null ? exp.getGiroNegocio().toUpperCase() : "-";
        if (exp.getCiiuCodigo() != null && !exp.getCiiuCodigo().isBlank()) {
            giro += "  (USO CONFORME " + exp.getCiiuCodigo() + ")";
        }
        fila(tbl, "GIRO (S):", giro, true);

        doc.add(tbl);
    }

    /** Fila estándar etiqueta-valor */
    private void fila(PdfPTable tbl, String campo, String valor, boolean sombreada) {
        Color bg = sombreada ? GRIS_FILA : Color.WHITE;

        PdfPCell cCampo = new PdfPCell(new Phrase(campo, f(9, Font.BOLD, Color.BLACK)));
        cCampo.setBackgroundColor(bg);
        cCampo.setBorderColor(BORDE);
        cCampo.setPadding(4f);
        cCampo.setPaddingLeft(8f);
        tbl.addCell(cCampo);

        PdfPCell cValor = new PdfPCell(new Phrase(valor, f(9, Font.NORMAL, Color.DARK_GRAY)));
        cValor.setBackgroundColor(bg);
        cValor.setBorderColor(BORDE);
        cValor.setPadding(4f);
        tbl.addCell(cValor);
    }

    /** Fila compuesta: R.U.C. | Categoría | Área en una misma fila */
    private void filaRucCategoriaArea(PdfPTable tbl, ExpedienteResponseDto exp) throws DocumentException {
        // Sub-tabla interna de 3 partes
        PdfPTable sub = new PdfPTable(new float[]{38f, 32f, 30f});
        sub.setWidthPercentage(100f);

        // Encabezados sub-columnas
        PdfPCell hRuc = new PdfPCell(new Phrase("R.U.C.:", f(9, Font.BOLD, Color.BLACK)));
        hRuc.setBorder(Rectangle.NO_BORDER); hRuc.setPadding(1f);
        sub.addCell(hRuc);

        PdfPCell hCat = new PdfPCell(new Phrase("CATEGORÍA:", f(8, Font.BOLD, Color.BLACK)));
        hCat.setBorder(Rectangle.LEFT); hCat.setBorderColorLeft(BORDE);
        hCat.setPadding(1f); hCat.setPaddingLeft(5f);
        sub.addCell(hCat);

        PdfPCell hArea = new PdfPCell(new Phrase("ÁREA:", f(8, Font.BOLD, Color.BLACK)));
        hArea.setBorder(Rectangle.LEFT); hArea.setBorderColorLeft(BORDE);
        hArea.setPadding(1f); hArea.setPaddingLeft(5f);
        sub.addCell(hArea);

        // Valores
        String ruc  = exp.getDocumentoIdentidad() != null ? exp.getDocumentoIdentidad() : "-";
        String cat  = exp.getCategoriaEstablecimiento() != null
                ? exp.getCategoriaEstablecimiento()
                : resolverCategoria(exp);
        String area = exp.getAreaMetrosCuadrados() != null
                ? exp.getAreaMetrosCuadrados() + " M²" : "- M²";

        PdfPCell vRuc = new PdfPCell(new Phrase(ruc, f(9, Font.NORMAL, Color.DARK_GRAY)));
        vRuc.setBorder(Rectangle.NO_BORDER); vRuc.setPadding(1f);
        sub.addCell(vRuc);

        PdfPCell vCat = new PdfPCell(new Phrase(cat, f(8, Font.BOLD, AZUL_INST)));
        vCat.setBorder(Rectangle.LEFT); vCat.setBorderColorLeft(BORDE);
        vCat.setPadding(1f); vCat.setPaddingLeft(5f);
        sub.addCell(vCat);

        PdfPCell vArea = new PdfPCell(new Phrase(area, f(8, Font.BOLD, AZUL_INST)));
        vArea.setBorder(Rectangle.LEFT); vArea.setBorderColorLeft(BORDE);
        vArea.setPadding(1f); vArea.setPaddingLeft(5f);
        sub.addCell(vArea);

        // Agregar al tbl principal
        PdfPCell etiq = new PdfPCell(new Phrase("DATOS FISCALES:", f(9, Font.BOLD, Color.BLACK)));
        etiq.setBackgroundColor(GRIS_FILA);
        etiq.setBorderColor(BORDE);
        etiq.setPadding(4f); etiq.setPaddingLeft(8f);
        tbl.addCell(etiq);

        PdfPCell cSub = new PdfPCell(sub);
        cSub.setBackgroundColor(GRIS_FILA);
        cSub.setBorderColor(BORDE);
        cSub.setPadding(4f);
        tbl.addCell(cSub);
    }

    // ════════════════════════════════════════════════════════════════════════
    // 7. FILA: CIUDAD, FECHA Y NÚMERO DE LICENCIA
    // ════════════════════════════════════════════════════════════════════════

    private void filaCiudadFechaLic(Document doc, ExpedienteResponseDto exp, String numLic)
            throws DocumentException {
        LocalDateTime fechaEm = exp.getFechaAprobacion() != null ? exp.getFechaAprobacion()
                : (exp.getFechaCreacion() != null ? exp.getFechaCreacion() : LocalDateTime.now());
        String fechaTxt = fechaEm.toLocalDate().format(FMT_LARGA);

        PdfPTable tbl = new PdfPTable(new float[]{55f, 45f});
        tbl.setWidthPercentage(97f); tbl.setSpacingAfter(2f);
        tbl.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell cFecha = new PdfPCell(new Phrase("AYACUCHO,  " + fechaTxt,
                f(9, Font.BOLD, Color.BLACK)));
        cFecha.setBorderColor(BORDE); cFecha.setPadding(5f); cFecha.setPaddingLeft(8f);
        tbl.addCell(cFecha);

        Phrase pLic = new Phrase();
        pLic.add(new Chunk("LICENCIA N°:  ", f(9, Font.BOLD, Color.BLACK)));
        pLic.add(new Chunk(numLic, f(10, Font.BOLD, AZUL_INST)));
        PdfPCell cLic = new PdfPCell(pLic);
        cLic.setBorderColor(BORDE); cLic.setPadding(5f); cLic.setPaddingLeft(8f);
        tbl.addCell(cLic);

        doc.add(tbl);
    }

    // ════════════════════════════════════════════════════════════════════════
    // 8. FILA: ZONIFICACIÓN Y EXPEDIENTE N°
    // ════════════════════════════════════════════════════════════════════════

    private void filaZonificacionExpediente(Document doc, ExpedienteResponseDto exp)
            throws DocumentException {
        String zon = exp.getZonificacion() != null
                ? exp.getZonificacion().toUpperCase() : "POR DETERMINAR";
        String numExp = exp.getNumeroTramite() != null ? exp.getNumeroTramite() : "-";

        PdfPTable tbl = new PdfPTable(new float[]{55f, 45f});
        tbl.setWidthPercentage(97f); tbl.setSpacingAfter(2f);
        tbl.setHorizontalAlignment(Element.ALIGN_CENTER);

        Phrase pZon = new Phrase();
        pZon.add(new Chunk("ZONIFICACIÓN:  ", f(9, Font.BOLD, Color.BLACK)));
        pZon.add(new Chunk(zon, f(9, Font.NORMAL, Color.DARK_GRAY)));
        PdfPCell cZon = new PdfPCell(pZon);
        cZon.setBackgroundColor(GRIS_FILA);
        cZon.setBorderColor(BORDE); cZon.setPadding(5f); cZon.setPaddingLeft(8f);
        tbl.addCell(cZon);

        Phrase pExp = new Phrase();
        pExp.add(new Chunk("EXPEDIENTE N°:  ", f(9, Font.BOLD, Color.BLACK)));
        pExp.add(new Chunk(numExp, f(9, Font.NORMAL, Color.DARK_GRAY)));
        PdfPCell cExp = new PdfPCell(pExp);
        cExp.setBackgroundColor(GRIS_FILA);
        cExp.setBorderColor(BORDE); cExp.setPadding(5f); cExp.setPaddingLeft(8f);
        tbl.addCell(cExp);

        doc.add(tbl);
    }

    // ════════════════════════════════════════════════════════════════════════
    // 9. FILA: HORARIO DE AUTORIZACIÓN + CUADRO VENCE
    // ════════════════════════════════════════════════════════════════════════

    private void filaHorarioVencimiento(Document doc, ExpedienteResponseDto exp)
            throws DocumentException {
        String hIni = exp.getHoraInicio() != null ? exp.getHoraInicio() : "00:00";
        String hFin = exp.getHoraFin()    != null ? exp.getHoraFin()    : "24:00";

        PdfPTable tbl = new PdfPTable(new float[]{42f, 34f, 24f});
        tbl.setWidthPercentage(97f); tbl.setSpacingAfter(2f);
        tbl.setHorizontalAlignment(Element.ALIGN_CENTER);

        // Etiqueta
        PdfPCell cLbl = new PdfPCell(new Phrase(
                "AUTORIZACIÓN PARA OPERAR DE:", f(9, Font.BOLD, Color.BLACK)));
        cLbl.setBorderColor(BORDE); cLbl.setPadding(5f); cLbl.setPaddingLeft(8f);
        tbl.addCell(cLbl);

        // Horas
        Phrase pH = new Phrase();
        pH.add(new Chunk(hIni + " HORAS     ", f(10, Font.BOLD, AZUL_INST)));
        pH.add(new Chunk("HASTA:  ", f(9, Font.BOLD, Color.BLACK)));
        pH.add(new Chunk(hFin + " HORAS", f(10, Font.BOLD, AZUL_INST)));
        PdfPCell cHoras = new PdfPCell(pH);
        cHoras.setBorderColor(BORDE); cHoras.setPadding(5f);
        tbl.addCell(cHoras);

        // Cuadro VENCE
        PdfPCell cVence = new PdfPCell();
        cVence.setBorderColor(BORDE); cVence.setPadding(2f);
        cVence.setHorizontalAlignment(Element.ALIGN_CENTER);
        cVence.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Paragraph pV = new Paragraph();
        pV.add(new Chunk("VENCE\n",      f(7, Font.BOLD, Color.BLACK)));
        pV.add(new Chunk("Día/Mes/Año\n",f(6, Font.NORMAL, Color.GRAY)));
        // Licencia definitiva — sin vencimiento (Art. 11, Ley 28976)
        pV.add(new Chunk("**/**/****", f(9, Font.BOLD, new Color(140, 0, 0))));
        pV.setAlignment(Element.ALIGN_CENTER);
        cVence.addElement(pV);
        tbl.addCell(cVence);

        doc.add(tbl);
    }

    // ════════════════════════════════════════════════════════════════════════
    // 10. FILA: OBSERVACIÓN
    // ════════════════════════════════════════════════════════════════════════

    private void filaObservacion(Document doc) throws DocumentException {
        PdfPTable tbl = new PdfPTable(new float[]{22f, 78f});
        tbl.setWidthPercentage(97f); tbl.setSpacingAfter(4f);
        tbl.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell cL = new PdfPCell(new Phrase("OBSERVACIÓN:", f(9, Font.BOLD, Color.BLACK)));
        cL.setBorderColor(BORDE); cL.setPadding(5f); cL.setPaddingLeft(8f);
        tbl.addCell(cL);

        PdfPCell cO = new PdfPCell(new Phrase(
                "CÓDIGO DE PROPIETARIO SIGETI: 0000000000000000000000000",
                f(8, Font.ITALIC, Color.DARK_GRAY)));
        cO.setBorderColor(BORDE); cO.setPadding(5f);
        tbl.addCell(cO);

        doc.add(tbl);
    }

    // ════════════════════════════════════════════════════════════════════════
    // 11. SECCIÓN INDICACIONES
    // ════════════════════════════════════════════════════════════════════════

    private void seccionIndicaciones(Document doc) throws DocumentException {
        // Franja de encabezado naranja
        PdfPTable tblH = new PdfPTable(1);
        tblH.setWidthPercentage(97f);
        tblH.setSpacingAfter(0f);
        tblH.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell cH = new PdfPCell(new Phrase("—  INDICACIONES:  —", f(9, Font.BOLD, Color.WHITE)));
        cH.setBackgroundColor(NARANJA_INST);
        cH.setBorder(Rectangle.NO_BORDER);
        cH.setHorizontalAlignment(Element.ALIGN_CENTER);
        cH.setPaddingTop(3f); cH.setPaddingBottom(3f);
        tblH.addCell(cH);
        doc.add(tblH);

        // Cuerpo de indicaciones
        PdfPTable tblI = new PdfPTable(1);
        tblI.setWidthPercentage(97f); tblI.setSpacingAfter(4f);
        tblI.setHorizontalAlignment(Element.ALIGN_CENTER);

        String[] inds = {
            "La licencia de funcionamiento debe estar exhibida en un lugar visible del local comercial.",
            "La licencia de funcionamiento no podrá ser utilizado por otra persona que no sea el titular de la misma.",
            "El titular debe cumplir con el pago de sus obligaciones tributarias por concepto de arbitrios municipales.",
            "Está prohibido el cambio de giro de negocio, venta de bebidas alcohólicas dentro y fuera del local y uso de la vía pública más allá de los límites permitidos, asumiendo el titular las responsabilidades civiles, penales y administrativas correspondientes.",
            "El titular de la licencia debe solicitar a la Municipalidad Provincial de Huamanga la autorización respectiva para efectuar modificaciones en su establecimiento personal.",
            "El titular de la licencia de funcionamiento debe comunicar a la Subgerencia de Comercio, Licencias y Control Sanitario el cese de actividades comerciales a fin de dar la suspensión definitiva de la misma."
        };

        StringBuilder sb = new StringBuilder();
        for (String ind : inds) sb.append("• ").append(ind).append("\n");

        PdfPCell cInd = new PdfPCell(new Phrase(sb.toString(),
                FontFactory.getFont(FontFactory.HELVETICA, 6.5f, Color.DARK_GRAY)));
        cInd.setBorder(Rectangle.BOX);
        cInd.setBorderColor(BORDE);
        cInd.setPadding(5f); cInd.setPaddingLeft(8f);
        tblI.addCell(cInd);
        doc.add(tblI);
    }

    // ════════════════════════════════════════════════════════════════════════
    // 12. PIE: FIRMA DEL SUBGERENTE + QR DE VERIFICACIÓN
    // ════════════════════════════════════════════════════════════════════════

    private void pieFirmaQr(Document doc, ExpedienteResponseDto exp, byte[] qrBytes)
            throws DocumentException {
        PdfPTable tbl = new PdfPTable(new float[]{50f, 50f});
        tbl.setWidthPercentage(97f);
        tbl.setHorizontalAlignment(Element.ALIGN_CENTER);

        // Celda izquierda: Firma del Subgerente
        PdfPCell cFirma = new PdfPCell();
        cFirma.setBorder(Rectangle.NO_BORDER);
        cFirma.setPadding(4f);
        cFirma.setHorizontalAlignment(Element.ALIGN_CENTER);
        cFirma.setVerticalAlignment(Element.ALIGN_BOTTOM);

        Paragraph pF = new Paragraph();
        pF.setAlignment(Element.ALIGN_CENTER);
        pF.add(new Chunk("\n\n_________________________________\n", f(8, Font.NORMAL, Color.DARK_GRAY)));
        pF.add(new Chunk("Ing. / Lic. ...............................\n",   f(8, Font.BOLD,   AZUL_INST)));
        pF.add(new Chunk("SUBGERENTE DE COMERCIO Y LICENCIAS\n",           f(7, Font.NORMAL, Color.DARK_GRAY)));
        pF.add(new Chunk("Municipalidad Provincial de Huamanga\n",         f(7, Font.ITALIC, Color.GRAY)));
        pF.add(new Chunk("\nPROVINCIA - HUAMANGA - AYACUCHO",              f(7, Font.BOLD,   AZUL_INST)));
        cFirma.addElement(pF);
        tbl.addCell(cFirma);

        // Celda derecha: QR de verificación
        PdfPCell cQr = new PdfPCell();
        cQr.setBorder(Rectangle.NO_BORDER);
        cQr.setPadding(4f);
        cQr.setHorizontalAlignment(Element.ALIGN_CENTER);
        cQr.setVerticalAlignment(Element.ALIGN_MIDDLE);

        if (qrBytes != null && qrBytes.length > 0) {
            try {
                Image qrImg = Image.getInstance(qrBytes);
                qrImg.scaleToFit(88f, 88f);
                qrImg.setAlignment(Element.ALIGN_CENTER);
                cQr.addElement(qrImg);
            } catch (Exception ex) {
                log.warn("QR no disponible: {}", ex.getMessage());
            }
        }

        Paragraph pQT = new Paragraph(
                "Escanee el código QR para verificar\nla autenticidad en el portal institucional.",
                f(6, Font.NORMAL, Color.GRAY));
        pQT.setAlignment(Element.ALIGN_CENTER);
        cQr.addElement(pQT);

        tbl.addCell(cQr);
        doc.add(tbl);
    }

    // ════════════════════════════════════════════════════════════════════════
    // UTILIDADES
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Resuelve el número correlativo de la Licencia de Funcionamiento.
     * Prioridad: numeroLicencia > licenciaQrCode > numeroTramite (solo dígitos).
     */
    private String resolverNumeroLicencia(ExpedienteResponseDto exp) {
        if (exp.getNumeroLicencia() != null && !exp.getNumeroLicencia().isBlank()) {
            return exp.getNumeroLicencia();
        }
        if (exp.getLicenciaQrCode() != null && !exp.getLicenciaQrCode().isBlank()) {
            String qr = exp.getLicenciaQrCode();
            String[] partes = qr.split("-");
            return partes[partes.length - 1];
        }
        if (exp.getNumeroTramite() != null) {
            return exp.getNumeroTramite().replaceAll("[^0-9]", "");
        }
        return "000000";
    }

    /**
     * Categoría ITSE del establecimiento según nivel de riesgo.
     * Formato: "1-A (BAJO)", "1-A (INTERMEDIO)", "2-A (ALTO)", "2-B (MUY ALTO)"
     */
    private String resolverCategoria(ExpedienteResponseDto exp) {
        if (exp.getNivelRiesgo() == null) return "-";
        return switch (exp.getNivelRiesgo()) {
            case BAJO     -> "1-A (BAJO)";
            case MEDIO    -> "1-A (INTERMEDIO)";
            case ALTO     -> "2-A (ALTO)";
            case MUY_ALTO -> "2-B (MUY ALTO)";
        };
    }

    /**
     * Construye la dirección completa del establecimiento.
     */
    private String construirDireccion(ExpedienteResponseDto exp) {
        if (exp.getDireccionEstablecimiento() != null && !exp.getDireccionEstablecimiento().isBlank()) {
            StringBuilder sb = new StringBuilder(exp.getDireccionEstablecimiento().toUpperCase());
            if (exp.getManzana()    != null && !exp.getManzana().isBlank())
                sb.append(" MZ ").append(exp.getManzana().toUpperCase());
            if (exp.getLote()       != null && !exp.getLote().isBlank())
                sb.append(" LOTE ").append(exp.getLote().toUpperCase());
            if (exp.getUrbanizacion() != null && !exp.getUrbanizacion().isBlank())
                sb.append(" — ").append(exp.getUrbanizacion().toUpperCase());
            return sb.toString();
        }
        StringBuilder sb = new StringBuilder();
        if (exp.getTipoVia()       != null) sb.append(exp.getTipoVia().toUpperCase()).append(" ");
        if (exp.getNombreVia()     != null) sb.append(exp.getNombreVia().toUpperCase()).append(" ");
        if (exp.getNumeroVivienda() != null) sb.append("N° ").append(exp.getNumeroVivienda()).append(" ");
        if (exp.getDistrito()      != null) sb.append("DIST. ").append(exp.getDistrito().toUpperCase());
        String dir = sb.toString().trim();
        return dir.isEmpty() ? "SIN DIRECCIÓN REGISTRADA" : dir;
    }

    /** Atajo para crear fuentes OpenPDF. */
    private Font f(float size, int style, Color color) {
        return FontFactory.getFont(FontFactory.HELVETICA, size, style, color);
    }
}
