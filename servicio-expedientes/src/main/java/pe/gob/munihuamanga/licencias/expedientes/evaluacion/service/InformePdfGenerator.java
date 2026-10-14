package pe.gob.munihuamanga.licencias.expedientes.evaluacion.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.gob.munihuamanga.licencias.common.dto.ChecklistAdministrativoDto;
import pe.gob.munihuamanga.licencias.common.dto.ItemChecklistDto;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.model.InformeAdministrativo;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/**
 * Generador OpenPDF del Informe Administrativo Técnico preliminar (Fase 3 - M08).
 * Incorpora la leyenda mandatoria «BORRADOR — NO CONSTITUYE RESOLUCIÓN NI LICENCIA».
 */
@Service
@Slf4j
public class InformePdfGenerator {

    private static final Color AZUL_INST = new Color(0, 51, 120);
    private static final Color GRIS_CLARO = new Color(245, 245, 245);
    private static final Color ROJO_ALERTA = new Color(180, 20, 20);

    private static final Font F_TITULO = new Font(Font.HELVETICA, 13, Font.BOLD, AZUL_INST);
    private static final Font F_SUBTITULO = new Font(Font.HELVETICA, 10, Font.BOLD, AZUL_INST);
    private static final Font F_ALERTA = new Font(Font.HELVETICA, 10, Font.BOLD, ROJO_ALERTA);
    private static final Font F_TEXTO = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
    private static final Font F_BOLD = new Font(Font.HELVETICA, 9, Font.BOLD, Color.BLACK);
    private static final Font F_MINI = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.GRAY);

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generarPdf(InformeAdministrativo informe, ChecklistAdministrativoDto checklist) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 40, 40);
            PdfWriter writer = PdfWriter.getInstance(doc, baos);

            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter writer, Document document) {
                    PdfPTable footer = new PdfPTable(1);
                    footer.setTotalWidth(document.right() - document.left());
                    PdfPCell cell = new PdfPCell(new Phrase(
                            "*** BORRADOR DE INFORME TÉCNICO — NO CONSTITUYE RESOLUCIÓN ADMINISTRATIVA NI LICENCIA DE FUNCIONAMIENTO ***",
                            F_MINI));
                    cell.setBorder(Rectangle.NO_BORDER);
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    footer.addCell(cell);
                    footer.writeSelectedRows(0, -1, document.left(), document.bottom() - 10, writer.getDirectContent());
                }
            });

            doc.open();

            // Membrete Institucional
            Paragraph pMuni = new Paragraph("MUNICIPALIDAD PROVINCIAL DE HUAMANGA", F_TITULO);
            pMuni.setAlignment(Element.ALIGN_CENTER);
            doc.add(pMuni);

            Paragraph pGerencia = new Paragraph("GERENCIA DE DESARROLLO ECONÓMICO — SUBGERENCIA DE COMERCIO Y LICENCIAS", F_SUBTITULO);
            pGerencia.setAlignment(Element.ALIGN_CENTER);
            pGerencia.setSpacingAfter(6);
            doc.add(pGerencia);

            // Franja de Advertencia Mandatoria
            PdfPTable tAlerta = new PdfPTable(1);
            tAlerta.setWidthPercentage(100);
            PdfPCell cAlerta = new PdfPCell(new Phrase("DOCUMENTO PRELIMINAR DE EVALUACIÓN INTERNA\nBORRADOR — NO CONSTITUYE RESOLUCIÓN NI LICENCIA DE FUNCIONAMIENTO", F_ALERTA));
            cAlerta.setBackgroundColor(new Color(255, 235, 235));
            cAlerta.setBorderColor(ROJO_ALERTA);
            cAlerta.setBorderWidth(1.2f);
            cAlerta.setHorizontalAlignment(Element.ALIGN_CENTER);
            cAlerta.setPadding(6);
            tAlerta.addCell(cAlerta);
            tAlerta.setSpacingAfter(12);
            doc.add(tAlerta);

            // Título del Informe
            Paragraph pTitulo = new Paragraph("INFORME ADMINISTRATIVO TÉCNICO N° " + informe.getNumeroInforme() + " (v" + informe.getVersion() + ")", F_TITULO);
            pTitulo.setAlignment(Element.ALIGN_LEFT);
            pTitulo.setSpacingAfter(10);
            doc.add(pTitulo);

            // Sección 1: Datos del Expediente y Titular
            Expediente exp = informe.getExpediente();
            PdfPTable tExp = new PdfPTable(4);
            tExp.setWidthPercentage(100);
            tExp.setWidths(new float[]{22, 28, 22, 28});

            agregarFilaSeccion(tExp, "1. IDENTIFICACIÓN DEL EXPEDIENTE Y TITULAR");
            agregarCelda(tExp, "Expediente N°:", exp != null ? exp.getNumeroTramite() : "—", true);
            agregarCelda(tExp, "Fecha Presentación:", exp != null && exp.getFechaCreacion() != null ? exp.getFechaCreacion().format(FMT_FECHA) : "—", false);
            agregarCelda(tExp, "Titular / Administrado:", exp != null ? exp.getNombreTitular() : "—", true);
            agregarCelda(tExp, "Doc. Identidad / RUC:", exp != null ? exp.getDocumentoIdentidad() : "—", false);
            agregarCelda(tExp, "Razón Social:", exp != null && exp.getRazonSocial() != null ? exp.getRazonSocial() : "—", true);
            agregarCelda(tExp, "Nivel de Riesgo ITSE:", exp != null && exp.getNivelRiesgo() != null ? exp.getNivelRiesgo().name() : "—", false);
            tExp.setSpacingAfter(10);
            doc.add(tExp);

            // Sección 2: Datos del Establecimiento
            PdfPTable tEst = new PdfPTable(2);
            tEst.setWidthPercentage(100);
            tEst.setWidths(new float[]{30, 70});

            agregarFilaSeccion(tEst, "2. DATOS DEL ESTABLECIMIENTO COMERCIAL");
            agregarCelda(tEst, "Nombre Comercial:", exp != null ? exp.getNombreComercial() : "—", true);
            agregarCelda(tEst, "Dirección Fiscal / Local:", exp != null ? exp.getDireccionEstablecimiento() : "—", false);
            agregarCelda(tEst, "Área Declarada:", (exp != null && exp.getAreaMetrosCuadrados() != null ? exp.getAreaMetrosCuadrados() + " m²" : "—"), true);
            agregarCelda(tEst, "Giro Comercial:", exp != null ? exp.getGiroNegocio() : "—", false);
            tEst.setSpacingAfter(10);
            doc.add(tEst);

            // Sección 3: Resultados del Checklist Integral
            if (checklist != null && checklist.getItems() != null && !checklist.getItems().isEmpty()) {
                PdfPTable tChk = new PdfPTable(3);
                tChk.setWidthPercentage(100);
                tChk.setWidths(new float[]{20, 60, 20});

                agregarFilaSeccion(tChk, "3. CHECKLIST INTEGRAL DE REQUISITOS (FASE 2)");
                agregarCabeceraTabla(tChk, "Código");
                agregarCabeceraTabla(tChk, "Criterio Verificado");
                agregarCabeceraTabla(tChk, "Resultado");

                for (ItemChecklistDto item : checklist.getItems()) {
                    tChk.addCell(crearCeldaTexto(item.getCodigo(), false));
                    tChk.addCell(crearCeldaTexto(item.getDescripcion() + "\n" + (item.getMotivo() != null ? "(" + item.getMotivo() + ")" : ""), false));
                    PdfPCell cRes = crearCeldaTexto(item.getEstado().name(), true);
                    if (item.getEstado().name().equals("CUMPLE")) {
                        cRes.setBackgroundColor(new Color(235, 255, 235));
                    } else if (item.getEstado().name().equals("NO_CUMPLE")) {
                        cRes.setBackgroundColor(new Color(255, 235, 235));
                    }
                    tChk.addCell(cRes);
                }
                tChk.setSpacingAfter(10);
                doc.add(tChk);
            }

            // Sección 4: Fundamentos Administrativos
            Paragraph sFund = new Paragraph("4. FUNDAMENTOS ADMINISTRATIVOS Y TÉCNICOS", F_SUBTITULO);
            sFund.setSpacingBefore(4);
            sFund.setSpacingAfter(4);
            doc.add(sFund);

            Paragraph pFund = new Paragraph(informe.getFundamentosAdministrativos(), F_TEXTO);
            pFund.setSpacingAfter(8);
            doc.add(pFund);

            // Sección 5: Referencias Documentales
            Paragraph sRef = new Paragraph("5. REFERENCIAS DOCUMENTALES", F_SUBTITULO);
            sRef.setSpacingBefore(4);
            sRef.setSpacingAfter(4);
            doc.add(sRef);

            Paragraph pRef = new Paragraph(informe.getReferenciasDocumentales(), F_TEXTO);
            pRef.setSpacingAfter(8);
            doc.add(pRef);

            // Sección 6: Conclusión
            PdfPTable tConcl = new PdfPTable(2);
            tConcl.setWidthPercentage(100);
            tConcl.setWidths(new float[]{30, 70});

            agregarFilaSeccion(tConcl, "6. CONCLUSIÓN TÉCNICA Y ESTADO");
            agregarCelda(tConcl, "Dictamen / Conclusión:", informe.getConclusion().name(), true);
            agregarCelda(tConcl, "Estado del Informe:", informe.getEstado().name(), false);
            agregarCelda(tConcl, "Evaluador Responsable:", informe.getEvaluadorNombre(), true);
            agregarCelda(tConcl, "Fecha de Elaboración:", informe.getFechaElaboracion() != null ? informe.getFechaElaboracion().format(FMT_FECHA) : "—", false);
            if (informe.getRevisorNombre() != null) {
                agregarCelda(tConcl, "Revisor / Supervisor:", informe.getRevisorNombre(), true);
                agregarCelda(tConcl, "Fecha de Revisión:", informe.getFechaRevision() != null ? informe.getFechaRevision().format(FMT_FECHA) : "—", false);
            }
            if (informe.getAprobadorNombre() != null) {
                agregarCelda(tConcl, "Aprobado por:", informe.getAprobadorNombre(), true);
                agregarCelda(tConcl, "Fecha de Aprobación:", informe.getFechaAprobacion() != null ? informe.getFechaAprobacion().format(FMT_FECHA) : "—", false);
            }
            tConcl.setSpacingAfter(14);
            doc.add(tConcl);

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generando PDF preliminar del informe {}: {}", informe.getNumeroInforme(), e.getMessage(), e);
            throw new RuntimeException("Error al generar PDF preliminar del informe: " + e.getMessage(), e);
        }
    }

    private void agregarFilaSeccion(PdfPTable table, String titulo) {
        PdfPCell cell = new PdfPCell(new Phrase(titulo, F_SUBTITULO));
        cell.setColspan(table.getNumberOfColumns());
        cell.setBackgroundColor(GRIS_CLARO);
        cell.setPadding(4);
        cell.setBorderWidth(0.8f);
        cell.setBorderColor(AZUL_INST);
        table.addCell(cell);
    }

    private void agregarCabeceraTabla(PdfPTable table, String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, F_BOLD));
        cell.setBackgroundColor(GRIS_CLARO);
        cell.setPadding(4);
        table.addCell(cell);
    }

    private void agregarCelda(PdfPTable table, String etiqueta, String valor, boolean nuevaFila) {
        PdfPCell c1 = new PdfPCell(new Phrase(etiqueta, F_BOLD));
        c1.setPadding(3);
        c1.setBorderColor(Color.LIGHT_GRAY);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(valor != null ? valor : "—", F_TEXTO));
        c2.setPadding(3);
        c2.setBorderColor(Color.LIGHT_GRAY);
        table.addCell(c2);
    }

    private PdfPCell crearCeldaTexto(String texto, boolean centrado) {
        PdfPCell c = new PdfPCell(new Phrase(texto != null ? texto : "", F_TEXTO));
        c.setPadding(3);
        c.setBorderColor(Color.LIGHT_GRAY);
        if (centrado) c.setHorizontalAlignment(Element.ALIGN_CENTER);
        return c;
    }
}
