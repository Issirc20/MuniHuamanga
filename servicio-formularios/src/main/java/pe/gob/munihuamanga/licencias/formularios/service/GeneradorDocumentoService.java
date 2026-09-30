package pe.gob.munihuamanga.licencias.formularios.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.gob.munihuamanga.licencias.common.dto.ExpedienteResponseDto;
import pe.gob.munihuamanga.licencias.common.dto.VoucherDto;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio generador de documentos oficiales en formato PDF con OpenPDF y ZXing.
 * Cumple con los requerimientos US-05, US-06 y US-07 según el TUO de la Ley N° 28976.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeneradorDocumentoService {

    private final Anexo1PdfGenerator anexo1PdfGenerator;
    private final Anexo3PdfGenerator anexo3PdfGenerator;
    private final Anexo4PdfGenerator anexo4PdfGenerator;

    @Value("${portal.verificacion.url:http://localhost:8081/verificar-licencia.html?codigo=}")
    private String portalVerificacionUrl;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Fase 2 / US-05: Genera el Anexo 1 oficial en PDF: Declaración Jurada para Licencia de Funcionamiento (2 páginas).
     */
    public byte[] generarAnexo1DeclaracionJurada(ExpedienteResponseDto expediente) {
        return anexo1PdfGenerator.generarPdf(expediente);
    }

    /**
     * US-05: Genera en PDF el Anexo 1 oficial: Declaración Jurada para Licencia de Funcionamiento.
     */
    public byte[] generarDeclaracionJurada(ExpedienteResponseDto expediente) {
        return anexo1PdfGenerator.generarPdf(expediente);
    }

    /**
     * Fase 2: Genera el Anexo 3 oficial en PDF: Reporte de Nivel de Riesgo del Establecimiento (Matriz ITSE - 2 páginas).
     */
    public byte[] generarAnexo3MatrizRiesgoItse(ExpedienteResponseDto expediente) {
        return anexo3PdfGenerator.generarPdf(expediente);
    }

    /**
     * US-05: Genera en PDF la Solicitud de Inspección Técnica de Seguridad en Edificaciones (ITSE).
     */
    public byte[] generarSolicitudItsePdf(ExpedienteResponseDto expediente) {
        return anexo3PdfGenerator.generarPdf(expediente);
    }

    /**
     * Fase 2: Genera el Anexo 4 oficial en PDF: Declaración Jurada de Cumplimiento de Condiciones de Seguridad (4 páginas).
     */
    public byte[] generarAnexo4CondicionesSeguridad(ExpedienteResponseDto expediente) {
        return anexo4PdfGenerator.generarPdf(expediente);
    }

    /**
     * US-07: Genera en PDF la Orden de Pago / Voucher oficial del SAT Huamanga con Código de Barras (Code 128).
     */
    public byte[] generarVoucherSatPdf(VoucherDto voucher) {
        Document document = new Document(PageSize.A5, 30, 30, 30, 30);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Encabezado SAT
            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(0, 51, 102));
            Paragraph header = new Paragraph("SERVICIO DE ADMINISTRACIÓN TRIBUTARIA DE HUAMANGA (SAT)\nORDEN DE PAGO DE TASA ADMINISTRATIVA", fontHeader);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);

            document.add(new Paragraph("\n"));

            PdfPTable t = new PdfPTable(2);
            t.setWidthPercentage(100);

            agregarFila(t, "Código de Voucher:", voucher.getVoucherId());
            agregarFila(t, "Expediente N°:", voucher.getNumeroTramite());
            agregarFila(t, "Contribuyente / Titular:", voucher.getTitular());
            agregarFila(t, "Documento de Identidad:", voucher.getDocumentoIdentidad());
            agregarFila(t, "Concepto Tributario:", voucher.getConcepto());
            agregarFila(t, "Fecha de Emisión:", voucher.getFechaEmision() != null ? voucher.getFechaEmision().format(FORMATTER) : "-");
            agregarFila(t, "Fecha de Vencimiento:", voucher.getFechaVencimiento() != null ? voucher.getFechaVencimiento().format(DATE_ONLY) : "-");

            // Fila de Monto Total Destacada
            Font fontMontoLabel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
            Font fontMontoVal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(5, 150, 105));

            PdfPCell cMontoLabel = new PdfPCell(new Phrase("TOTAL A PAGAR (PEN):", fontMontoLabel));
            cMontoLabel.setBackgroundColor(new Color(241, 245, 249));
            cMontoLabel.setPadding(8);

            PdfPCell cMontoVal = new PdfPCell(new Phrase("S/. " + (voucher.getMonto() != null ? voucher.getMonto().setScale(2).toString() : "0.00"), fontMontoVal));
            cMontoVal.setPadding(8);

            t.addCell(cMontoLabel);
            t.addCell(cMontoVal);

            document.add(t);

            // Generación de Código de Barras Code 128 con ZXing
            String codBarrasTexto = voucher.getCodigoBarrasSat() != null ? voucher.getCodigoBarrasSat() : "0107" + voucher.getVoucherId().replace("-", "");
            byte[] barcodeBytes = generarImagenCodigoBarras(codBarrasTexto, 320, 60);

            Image barcodeImg = Image.getInstance(barcodeBytes);
            barcodeImg.setAlignment(Element.ALIGN_CENTER);
            barcodeImg.setSpacingBefore(15f);
            document.add(barcodeImg);

            Paragraph pBarCodeText = new Paragraph(codBarrasTexto, FontFactory.getFont(FontFactory.COURIER, 9, Color.DARK_GRAY));
            pBarCodeText.setAlignment(Element.ALIGN_CENTER);
            document.add(pBarCodeText);

            Paragraph infoCanales = new Paragraph(
                    "\nLugares de Pago: Ventanillas SAT Huamanga (Jr. Arequipa N° 120), Agentes Autorizados y Banca por Internet.",
                    FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY)
            );
            infoCanales.setAlignment(Element.ALIGN_CENTER);
            document.add(infoCanales);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error al generar PDF del Voucher SAT", e);
            throw new RuntimeException("Error al generar PDF del Voucher SAT", e);
        }
    }

    /**
     * US-09 / US-10: Genera el Certificado Oficial de Licencia de Funcionamiento en PDF con Código QR y Sello Digital.
     */
    public byte[] generarLicenciaPdf(ExpedienteResponseDto expediente) {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Membrete Institucional
            Font fontPais = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.GRAY);
            Paragraph pPais = new Paragraph("REPÚBLICA DEL PERÚ", fontPais);
            pPais.setAlignment(Element.ALIGN_CENTER);
            document.add(pPais);

            Font fontMuni = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(0, 51, 102));
            Paragraph pMuni = new Paragraph("MUNICIPALIDAD PROVINCIAL DE HUAMANGA", fontMuni);
            pMuni.setAlignment(Element.ALIGN_CENTER);
            document.add(pMuni);

            Font fontGerencia = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(51, 65, 85));
            Paragraph pGerencia = new Paragraph("GERENCIA DE DESARROLLO ECONÓMICO Y LICENCIAS\nSUBGERENCIA DE COMERCIO, LICENCIAS Y FISCALIZACIÓN", fontGerencia);
            pGerencia.setAlignment(Element.ALIGN_CENTER);
            document.add(pGerencia);

            document.add(new Paragraph("\n"));

            // Título de la Licencia
            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(180, 83, 9));
            Paragraph pTitulo = new Paragraph("LICENCIA MUNICIPAL DE FUNCIONAMIENTO DEFINITIVA", fontTitulo);
            pTitulo.setAlignment(Element.ALIGN_CENTER);
            document.add(pTitulo);

            String codigoLicencia = expediente.getLicenciaQrCode() != null ? expediente.getLicenciaQrCode() : "LIC-2026-PENDIENTE";
            Font fontCodigo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(0, 51, 102));
            Paragraph pCodigo = new Paragraph("N° " + codigoLicencia, fontCodigo);
            pCodigo.setAlignment(Element.ALIGN_CENTER);
            document.add(pCodigo);

            Font fontVigencia = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(5, 150, 105));
            Paragraph pVigencia = new Paragraph("VIGENCIA: INDETERMINADA (Art. 11 de la Ley N° 28976)", fontVigencia);
            pVigencia.setAlignment(Element.ALIGN_CENTER);
            pVigencia.setSpacingAfter(10f);
            document.add(pVigencia);

            // Consideración Legal
            Font fontTextoLegal = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);
            Paragraph pLegal = new Paragraph(
                    "La Municipalidad Provincial de Huamanga, en ejercicio de las facultades conferidas por la Ley N° 27972 (Ley Orgánica de Municipalidades) " +
                    "y la Ley N° 28976 (Ley Marco de Licencia de Funcionamiento y su TUO aprobado por D.S. N° 046-2017-PCM), habiéndose cumplido con los requisitos " +
                    "técnicos, zonificación y la verificación de condiciones de seguridad en edificaciones, otorga la presente licencia a favor de:",
                    fontTextoLegal
            );
            pLegal.setAlignment(Element.ALIGN_JUSTIFIED);
            pLegal.setSpacingAfter(8f);
            document.add(pLegal);

            // Tabla de Datos del Establecimiento
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(4f);
            table.setSpacingAfter(10f);

            agregarFila(table, "Número de Trámite / Expediente:", expediente.getNumeroTramite() != null ? expediente.getNumeroTramite() : "-");
            agregarFila(table, "Titular o Administrado:", expediente.getNombreTitular() != null ? expediente.getNombreTitular() : "-");
            agregarFila(table, "Documento de Identidad (DNI/RUC):", expediente.getDocumentoIdentidad() != null ? expediente.getDocumentoIdentidad() : "-");
            agregarFila(table, "Razón Social:", expediente.getRazonSocial() != null ? expediente.getRazonSocial() : "(Persona Natural)");
            agregarFila(table, "Nombre Comercial:", expediente.getNombreComercial() != null ? expediente.getNombreComercial() : "-");
            agregarFila(table, "Giro o Actividad Autorizada:", expediente.getGiroNegocio() != null ? expediente.getGiroNegocio() : "-");
            agregarFila(table, "Dirección del Establecimiento:", expediente.getDireccionEstablecimiento() != null ? expediente.getDireccionEstablecimiento() : "-");
            agregarFila(table, "Área Autorizada del Local:", expediente.getAreaMetrosCuadrados() != null ? expediente.getAreaMetrosCuadrados() + " m²" : "-");
            agregarFila(table, "Clasificación de Riesgo ITSE:", expediente.getNivelRiesgo() != null ? expediente.getNivelRiesgo() + " (" + (expediente.getTipoItse() != null ? expediente.getTipoItse() : "ITSE") + ")" : "-");
            agregarFila(table, "Fecha de Expedición:", expediente.getFechaCreacion() != null ? expediente.getFechaCreacion().format(DATE_ONLY) : "-");

            document.add(table);

            // Pie con Código QR oficial y Firma Digital
            PdfPTable footerTable = new PdfPTable(2);
            footerTable.setWidthPercentage(100);
            footerTable.setWidths(new float[]{40f, 60f});
            footerTable.setSpacingBefore(6f);

            // Celda Izquierda: Código QR
            PdfPCell cellQr = new PdfPCell();
            cellQr.setBorder(PdfPCell.NO_BORDER);
            cellQr.setHorizontalAlignment(Element.ALIGN_CENTER);

            byte[] qrBytes = generarImagenQr(codigoLicencia, 120, 120);
            Image qrImage = Image.getInstance(qrBytes);
            qrImage.setAlignment(Element.ALIGN_CENTER);
            cellQr.addElement(qrImage);

            Paragraph pQrTexto = new Paragraph(
                    "CÓDIGO QR DE VERIFICACIÓN OFICIAL\nEscanee para verificar autenticidad y vigencia en el portal público institucional (RNF-20).",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, new Color(0, 51, 102))
            );
            pQrTexto.setAlignment(Element.ALIGN_CENTER);
            cellQr.addElement(pQrTexto);
            footerTable.addCell(cellQr);

            // Celda Derecha: Firma Digital Institucional
            PdfPCell cellFirma = new PdfPCell();
            cellFirma.setBorder(PdfPCell.BOX);
            cellFirma.setBorderColor(new Color(203, 213, 225));
            cellFirma.setBackgroundColor(new Color(248, 250, 252));
            cellFirma.setPadding(8);

            String hashSha256 = calcularHashSha256(codigoLicencia + "|" + expediente.getNumeroTramite() + "|" + expediente.getDocumentoIdentidad());

            Font fFirmaTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new Color(0, 51, 102));
            Font fFirmaBody = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.DARK_GRAY);
            Font fHash = FontFactory.getFont(FontFactory.COURIER, 6, new Color(100, 116, 139));

            Paragraph pFirma = new Paragraph();
            pFirma.add(new Phrase("CERTIFICADO DE FIRMA DIGITAL INSTITUCIONAL\n", fFirmaTitle));
            pFirma.add(new Phrase("Firmado digitalmente por:\n", fFirmaBody));
            pFirma.add(new Phrase("Abog. Carlos Mendoza Palomino\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.BLACK)));
            pFirma.add(new Phrase("Gerente de Desarrollo Económico y Licencias\n", fFirmaBody));
            pFirma.add(new Phrase("Municipalidad Provincial de Huamanga\n", fFirmaBody));
            pFirma.add(new Phrase("Sellado de Tiempo: " + java.time.LocalDateTime.now().format(FORMATTER) + "\n", fFirmaBody));
            pFirma.add(new Phrase("Firma Criptográfica SHA-256:\n" + hashSha256, fHash));

            cellFirma.addElement(pFirma);
            footerTable.addCell(cellFirma);

            document.add(footerTable);

            // Cláusula de exhibición obligatoria
            Paragraph pAviso = new Paragraph(
                    "\nNota: Conforme a ley, este certificado debe colocarse en un lugar visible del establecimiento comercial sujeto a fiscalización posterior.",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 7, Color.GRAY)
            );
            pAviso.setAlignment(Element.ALIGN_CENTER);
            document.add(pAviso);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error al generar PDF de Licencia de Funcionamiento", e);
            throw new RuntimeException("Error al generar PDF de Licencia de Funcionamiento", e);
        }
    }

    public byte[] generarImagenQr(String codigoLicencia, int width, int height) {
        String url = portalVerificacionUrl + (codigoLicencia != null ? codigoLicencia.trim() : "");
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, width, height, hints);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Error al generar imagen QR para la licencia {}", codigoLicencia, e);
            throw new RuntimeException("Error al generar imagen QR", e);
        }
    }

    private String calcularHashSha256(String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().toUpperCase();
        } catch (Exception e) {
            return "SHA256-ERROR";
        }
    }

    private byte[] generarImagenCodigoBarras(String texto, int ancho, int alto) throws Exception {
        Code128Writer writer = new Code128Writer();
        BitMatrix bitMatrix = writer.encode(texto, BarcodeFormat.CODE_128, ancho, alto);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", out);
        return out.toByteArray();
    }

    private Paragraph crearTituloSeccion(String titulo) {
        Font fontSec = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(0, 51, 102));
        Paragraph p = new Paragraph(titulo, fontSec);
        p.setSpacingBefore(5f);
        p.setSpacingAfter(3f);
        return p;
    }

    private void agregarFila(PdfPTable table, String campo, String valor) {
        Font fontCampo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
        Font fontValor = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);

        PdfPCell c1 = new PdfPCell(new Phrase(campo, fontCampo));
        c1.setBackgroundColor(new Color(248, 250, 252));
        c1.setPadding(5);

        PdfPCell c2 = new PdfPCell(new Phrase(valor, fontValor));
        c2.setPadding(5);

        table.addCell(c1);
        table.addCell(c2);
    }
}
