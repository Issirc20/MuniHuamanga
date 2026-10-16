package pe.gob.munihuamanga.licencias.expedientes.resoluciones.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.gob.munihuamanga.licencias.common.enums.EstadoResolucion;
import pe.gob.munihuamanga.licencias.common.enums.TipoDecisionResolucion;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.resoluciones.model.ResolucionAdministrativa;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Generador OpenPDF de la Resolución Administrativa Gerencial (M09 - Incremento I08).
 * Distingue nítidamente entre Vista Preliminar (marca de agua no oficial) y Resolución Firmada.
 */
@Slf4j
@Service
public class ResolucionPdfGenerator {

    private static final Color AZUL_INST = new Color(0, 51, 120);
    private static final Color GRIS_BORDE = new Color(200, 200, 200);
    private static final Color GRIS_FONDO = new Color(248, 249, 250);

    private static final Font F_TITULO_MUNI = new Font(Font.HELVETICA, 13, Font.BOLD, AZUL_INST);
    private static final Font F_SUBTITULO = new Font(Font.HELVETICA, 10, Font.BOLD, Color.DARK_GRAY);
    private static final Font F_NUMERO_RES = new Font(Font.HELVETICA, 11, Font.BOLD, AZUL_INST);
    private static final Font F_FECHA = new Font(Font.HELVETICA, 9, Font.ITALIC, Color.DARK_GRAY);
    private static final Font F_SECCION = new Font(Font.HELVETICA, 9, Font.BOLD, AZUL_INST);
    private static final Font F_TEXTO = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.BLACK);
    private static final Font F_TEXTO_BOLD = new Font(Font.HELVETICA, 9, Font.BOLD, Color.BLACK);
    private static final Font F_FOOTER = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.GRAY);
    private static final Font F_FIRMA_LABEL = new Font(Font.HELVETICA, 8, Font.BOLD, AZUL_INST);
    private static final Font F_FIRMA_VAL = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.DARK_GRAY);

    private static final DateTimeFormatter FMT_FECHA_LARGA =
            DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", Locale.of("es", "PE"));
    private static final DateTimeFormatter FMT_TIMESTAMP =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public byte[] generarResolucionPdf(ResolucionAdministrativa resolucion) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 45, 45, 45, 45);
            PdfWriter writer = PdfWriter.getInstance(doc, baos);

            boolean esBorradorOPreliminar = resolucion.getEstado() != EstadoResolucion.FIRMADA;
            boolean esSimulada = Boolean.TRUE.equals(resolucion.getEsSimulada());

            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter writer, Document document) {
                    PdfContentByte cb = writer.getDirectContentUnder();

                    if (esBorradorOPreliminar || esSimulada) {
                        PdfGState gstate = new PdfGState();
                        gstate.setFillOpacity(0.14f);
                        cb.saveState();
                        cb.setGState(gstate);
                        cb.beginText();
                        cb.setColorFill(Color.RED);
                        try {
                            cb.setFontAndSize(com.lowagie.text.pdf.BaseFont.createFont(
                                    com.lowagie.text.pdf.BaseFont.HELVETICA_BOLD,
                                    com.lowagie.text.pdf.BaseFont.WINANSI, false), 34);
                            String textoMarca = esSimulada
                                    ? "NO OFICIAL — DEMOSTRACIÓN"
                                    : "VISTA PRELIMINAR - SIN VALIDEZ OFICIAL";
                            cb.showTextAligned(Element.ALIGN_CENTER,
                                    textoMarca,
                                    (document.right() + document.left()) / 2,
                                    (document.top() + document.bottom()) / 2, 45);
                        } catch (Exception ex) {
                            log.error("Error al pintar marca de agua", ex);
                        }
                        cb.endText();
                        cb.restoreState();
                    }

                    // Pie de página oficial
                    PdfPTable footer = new PdfPTable(1);
                    footer.setTotalWidth(document.right() - document.left());
                    String textoFooter;
                    if (esSimulada) {
                        textoFooter = "*** RESOLUCIÓN EMITIDA EN MODO DEMOSTRACIÓN — SIN EFICACIA LEGAL OFICIAL (LEY N° 27269) ***";
                    } else if (esBorradorOPreliminar) {
                        textoFooter = "*** BORRADOR DE RESOLUCIÓN ADMINISTRATIVA — NO CONSTITUYE LICENCIA DE FUNCIONAMIENTO NI ACTO VIGENTE ***";
                    } else {
                        textoFooter = "Resolución Gerencial N° " + resolucion.getNumeroResolucion() + " — Municipalidad Provincial de Huamanga | Ley N° 27972 / Ley N° 28976";
                    }
                    PdfPCell cell = new PdfPCell(new Phrase(textoFooter, F_FOOTER));
                    cell.setBorder(Rectangle.NO_BORDER);
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    footer.addCell(cell);
                    footer.writeSelectedRows(0, -1, document.left(), document.bottom() - 15, writer.getDirectContent());
                }
            });

            doc.open();

            Expediente exp = resolucion.getExpediente();

            // Membrete
            Paragraph pMuni = new Paragraph("MUNICIPALIDAD PROVINCIAL DE HUAMANGA", F_TITULO_MUNI);
            pMuni.setAlignment(Element.ALIGN_CENTER);
            doc.add(pMuni);

            Paragraph pGerencia = new Paragraph("GERENCIA DE DESARROLLO ECONÓMICO\nSubgerencia de Comercio, Licencias y Control Sanitario", F_SUBTITULO);
            pGerencia.setAlignment(Element.ALIGN_CENTER);
            pGerencia.setSpacingAfter(10f);
            doc.add(pGerencia);

            // Título de la Resolución
            String numOficial = resolucion.getNumeroResolucion() != null ? resolucion.getNumeroResolucion() : "RES-PRELIMINAR";
            Paragraph pResolucion = new Paragraph("RESOLUCIÓN GERENCIAL N° " + numOficial + "-MPH/GDE", F_NUMERO_RES);
            pResolucion.setAlignment(Element.ALIGN_CENTER);
            pResolucion.setSpacingAfter(8f);
            doc.add(pResolucion);

            // Fecha y lugar
            String fechaTexto = (resolucion.getFechaAprobacion() != null)
                    ? resolucion.getFechaAprobacion().format(FMT_FECHA_LARGA)
                    : java.time.LocalDate.now().format(FMT_FECHA_LARGA);
            Paragraph pFecha = new Paragraph("Ayacucho, " + fechaTexto, F_FECHA);
            pFecha.setAlignment(Element.ALIGN_RIGHT);
            pFecha.setSpacingAfter(12f);
            doc.add(pFecha);

            // VISTO
            Paragraph pVistoTit = new Paragraph("VISTO:", F_SECCION);
            pVistoTit.setSpacingAfter(4f);
            doc.add(pVistoTit);

            String vistoTexto = String.format(
                    "El Expediente Administrativo N° %s, iniciado por don(ña) %s con documento de identidad N° %s, "
                    + "solicitando el otorgamiento de Licencia de Funcionamiento para el establecimiento comercial denominado \"%s\", "
                    + "ubicado en %s, distrito de Ayacucho, provincia de Huamanga; y el Informe Administrativo Técnico N° %s emitido con fecha %s; y,",
                    exp.getNumeroTramite(),
                    exp.getNombreTitular(),
                    exp.getDocumentoIdentidad(),
                    exp.getNombreComercial(),
                    exp.getDireccionEstablecimiento(),
                    resolucion.getInformeAdministrativo().getNumeroInforme(),
                    resolucion.getInformeAdministrativo().getFechaElaboracion().format(FMT_FECHA_LARGA)
            );
            Paragraph pVisto = new Paragraph(vistoTexto, F_TEXTO);
            pVisto.setAlignment(Element.ALIGN_JUSTIFIED);
            pVisto.setSpacingAfter(10f);
            doc.add(pVisto);

            // CONSIDERANDO
            Paragraph pConsTit = new Paragraph("CONSIDERANDO:", F_SECCION);
            pConsTit.setSpacingAfter(4f);
            doc.add(pConsTit);

            agregarParrafo(doc, "Que, de conformidad con el artículo 194° de la Constitución Política del Perú, en concordancia con el artículo II del Título Preliminar de la Ley N° 27972, Ley Orgánica de Municipalidades, los gobiernos locales gozan de autonomía política, económica y administrativa en los asuntos de su competencia;");

            agregarParrafo(doc, "Que, el Texto Único Ordenado de la Ley N° 28976, Ley Marco de Licencia de Funcionamiento, aprobado mediante Decreto Supremo N° 046-2017-PCM, establece el marco jurídico de las disposiciones aplicables a los procedimientos para el otorgamiento de la licencia de funcionamiento expedida por las municipalidades;");

            agregarParrafo(doc, "Que, mediante Informe Administrativo Técnico N° " + resolucion.getInformeAdministrativo().getNumeroInforme()
                    + ", el equipo técnico de la Subgerencia de Comercio, Licencias y Control Sanitario evaluó el cumplimiento del procedimiento, concluyendo que la solicitud "
                    + (resolucion.getTipoDecision() == TipoDecisionResolucion.FAVORABLE ? "CUMPLE SATISFACTORIAMENTE" : "NO CUMPLE")
                    + " con los requisitos legales, pago de derechos administrativos, compatibilidad de zonificación y condiciones de seguridad en edificaciones (ITSE);");

            if (resolucion.getFundamentos() != null && !resolucion.getFundamentos().isBlank()) {
                agregarParrafo(doc, "Que, " + resolucion.getFundamentos().trim());
            }

            if (resolucion.getReferenciasLegales() != null && !resolucion.getReferenciasLegales().isBlank()) {
                agregarParrafo(doc, "Estando a lo expuesto y de conformidad con lo establecido en la Ley N° 27972, Ley N° 28976, TUO de la Ley N° 27444 del Procedimiento Administrativo General y el Reglamento de Organización y Funciones (ROF) de la Municipalidad Provincial de Huamanga;");
            }

            // SE RESUELVE
            Paragraph pResTit = new Paragraph("SE RESUELVE:", F_SECCION);
            pResTit.setSpacingBefore(6f);
            pResTit.setSpacingAfter(6f);
            doc.add(pResTit);

            String accionResuelve = (resolucion.getTipoDecision() == TipoDecisionResolucion.FAVORABLE)
                    ? "DECLARAR PROCEDENTE" : "DECLARAR IMPROCEDENTE";

            String art1 = String.format(
                    "ARTÍCULO PRIMERO.- %s la solicitud de Licencia de Funcionamiento presentada en el Expediente N° %s, "
                    + "a favor de %s (%s N° %s), para operar el establecimiento comercial denominado \"%s\", con giro de \"%s\" "
                    + "(Código CIIU %s), en el predio ubicado en %s, distrito de Ayacucho, provincia de Huamanga, departamento de Ayacucho.",
                    accionResuelve,
                    exp.getNumeroTramite(),
                    exp.getNombreTitular(),
                    exp.getTipoDocumento(),
                    exp.getDocumentoIdentidad(),
                    exp.getNombreComercial(),
                    exp.getGiroNegocio(),
                    exp.getCiiuCodigo() != null ? exp.getCiiuCodigo() : "N/A",
                    exp.getDireccionEstablecimiento()
            );
            agregarParrafo(doc, art1);

            if (resolucion.getTipoDecision() == TipoDecisionResolucion.FAVORABLE) {
                agregarParrafo(doc, "ARTÍCULO SEGUNDO.- DISPONER que la Subgerencia de Comercio, Licencias y Control Sanitario proceda con la emisión del respectivo Certificado Oficial de Licencia de Funcionamiento, incorporando el código de verificación digital QR y registro municipal correspondiente.");
            } else {
                agregarParrafo(doc, "ARTÍCULO SEGUNDO.- DISPONER el archivo definitivo de los actuados una vez consentida la presente resolución, dejando a salvo el derecho del recurrente de formular nueva solicitud conforme a ley.");
            }

            agregarParrafo(doc, "ARTÍCULO TERCERO.- NOTIFICAR el presente acto resolutivo al administrado en el domicilio procesal o electrónico consignado en su solicitud, de conformidad con el TUO de la Ley N° 27444.");

            // Recuadro de Firma Digital / Autoridad
            doc.add(new Paragraph(" "));
            PdfPTable tablaFirma = new PdfPTable(1);
            tablaFirma.setWidthPercentage(95);
            tablaFirma.setSpacingBefore(10f);

            PdfPCell cellFirma = new PdfPCell();
            cellFirma.setPadding(8f);
            cellFirma.setBackgroundColor(GRIS_FONDO);
            cellFirma.setBorderColor(GRIS_BORDE);

            if (resolucion.getEstado() == EstadoResolucion.FIRMADA) {
                cellFirma.addElement(new Paragraph("[DOCUMENTO FIRMADO DIGITALMENTE EN CUMPLIMIENTO DE LA LEY N° 27269]", F_FIRMA_LABEL));
                cellFirma.addElement(new Paragraph("Firmante: " + resolucion.getFirmanteNombre() + " (" + resolucion.getFirmanteCargo() + ")", F_FIRMA_VAL));
                cellFirma.addElement(new Paragraph("Entidad: " + (resolucion.getFirmanteEntidad() != null ? resolucion.getFirmanteEntidad() : "Municipalidad Provincial de Huamanga"), F_FIRMA_VAL));
                String fFirmaStr = (resolucion.getFechaFirma() != null) ? resolucion.getFechaFirma().format(FMT_TIMESTAMP) : "N/D";
                cellFirma.addElement(new Paragraph("Fecha y hora de firma: " + fFirmaStr, F_FIRMA_VAL));
                cellFirma.addElement(new Paragraph("Huella Digital SHA-256: " + resolucion.getHashDocumento(), F_FIRMA_VAL));
            } else {
                cellFirma.addElement(new Paragraph("[ESTADO: PENDIENTE DE FIRMA DIGITAL INSTITUCIONAL]", F_FIRMA_LABEL));
                String autNombre = resolucion.getAutoridadCompetenteNombre() != null ? resolucion.getAutoridadCompetenteNombre() : "Autoridad Competente";
                String autCargo = resolucion.getAutoridadCompetenteCargo() != null ? resolucion.getAutoridadCompetenteCargo() : "Gerente de Desarrollo Económico";
                cellFirma.addElement(new Paragraph("Aprobador del acto: " + autNombre + " - " + autCargo, F_FIRMA_VAL));
                cellFirma.addElement(new Paragraph("Nota: El documento carece de eficacia legal oficial mientras no cuente con la firma digital válida del titular (Ley N° 27269).", F_FIRMA_VAL));
            }

            tablaFirma.addCell(cellFirma);
            doc.add(tablaFirma);

            doc.close();
            return baos.toByteArray();
        } catch (Exception ex) {
            log.error("Error al generar PDF de la resolución administrativa", ex);
            throw new IllegalStateException("Error al generar el documento PDF de la resolución: " + ex.getMessage(), ex);
        }
    }

    public static String calcularHashSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Error al calcular hash SHA-256", e);
        }
    }

    private void agregarParrafo(Document doc, String texto) throws Exception {
        Paragraph p = new Paragraph(texto, F_TEXTO);
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        p.setSpacingAfter(6f);
        doc.add(p);
    }
}
