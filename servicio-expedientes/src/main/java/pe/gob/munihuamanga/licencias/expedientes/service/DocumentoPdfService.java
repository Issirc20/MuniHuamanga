package pe.gob.munihuamanga.licencias.expedientes.service;

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
 * Servicio encargado de la renderización directa de formatos oficiales en PDF para el servicio de expedientes.
 */
@Slf4j
@Service
public class DocumentoPdfService {

    @Value("${portal.verificacion.url:http://localhost:8081/verificar-licencia.html?codigo=}")
    private String portalVerificacionUrl;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] generarDeclaracionJurada(ExpedienteResponseDto expediente) {
        Document document = new Document(PageSize.A4, 36, 36, 40, 36);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(0, 51, 102));
            Paragraph header = new Paragraph("MUNICIPALIDAD PROVINCIAL DE HUAMANGA\nGERENCIA DE LICENCIAS Y AUTORIZACIONES", fontHeader);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);

            Font fontSub = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(180, 83, 9));
            Paragraph sub = new Paragraph("ANEXO 1 — DECLARACIÓN JURADA PARA LICENCIA DE FUNCIONAMIENTO\n(Ley N° 28976 — TUO aprobado por Decreto Supremo N° 046-2017-PCM)", fontSub);
            sub.setAlignment(Element.ALIGN_CENTER);
            document.add(sub);

            document.add(new Paragraph("\n"));

            // Sección I
            document.add(crearTituloSeccion("I. DATOS DEL SOLICITANTE O TITULAR"));
            PdfPTable t1 = new PdfPTable(2);
            t1.setWidthPercentage(100);
            t1.setSpacingBefore(4f);
            t1.setSpacingAfter(8f);

            agregarFila(t1, "Número de Trámite:", expediente.getNumeroTramite() != null ? expediente.getNumeroTramite() : "-");
            agregarFila(t1, "Titular / Solicitante:", expediente.getNombreTitular() != null ? expediente.getNombreTitular() : "-");
            agregarFila(t1, "Documento de Identidad:", expediente.getDocumentoIdentidad() != null ? expediente.getDocumentoIdentidad() : "-");
            agregarFila(t1, "Razón Social:", expediente.getRazonSocial() != null ? expediente.getRazonSocial() : "(Persona Natural)");
            agregarFila(t1, "Teléfono Celular:", expediente.getTelefono() != null ? expediente.getTelefono() : "-");
            agregarFila(t1, "Correo Electrónico:", expediente.getCorreoElectronico() != null ? expediente.getCorreoElectronico() : "-");
            document.add(t1);

            // Sección II
            document.add(crearTituloSeccion("II. DATOS DEL ESTABLECIMIENTO OBJETO DE LA SOLICITUD"));
            PdfPTable t2 = new PdfPTable(2);
            t2.setWidthPercentage(100);
            t2.setSpacingBefore(4f);
            t2.setSpacingAfter(8f);

            agregarFila(t2, "Nombre Comercial:", expediente.getNombreComercial() != null ? expediente.getNombreComercial() : "-");
            agregarFila(t2, "Giro o Actividad Comercial:", expediente.getGiroNegocio() != null ? expediente.getGiroNegocio() : "-");
            agregarFila(t2, "Dirección en Huamanga:", expediente.getDireccionEstablecimiento() != null ? expediente.getDireccionEstablecimiento() : "-");
            agregarFila(t2, "Área Total del Local:", expediente.getAreaMetrosCuadrados() != null ? expediente.getAreaMetrosCuadrados() + " m²" : "-");
            agregarFila(t2, "Riesgo ITSE Determinado:", expediente.getNivelRiesgo() != null ? expediente.getNivelRiesgo() + " (" + expediente.getTipoItse() + ")" : "Por Determinar");
            agregarFila(t2, "Fecha de Presentación:", expediente.getFechaCreacion() != null ? expediente.getFechaCreacion().format(FORMATTER) : "-");
            document.add(t2);

            // Sección III
            document.add(crearTituloSeccion("III. DECLARACIÓN JURADA DE CUMPLIMIENTO DE CONDICIONES"));
            Font fontText = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            Paragraph pCondiciones = new Paragraph(
                    "1. Declaro que el establecimiento cumple con la zonificación y compatibilidad de uso vigente según el Plan de Desarrollo Urbano de la Municipalidad Provincial de Huamanga.\n" +
                    "2. Declaro que el local cuenta con las condiciones de seguridad en edificaciones (extintores vigentes, señalética, pozo a tierra y cableado protegido) exigidas por el Reglamento de Inspecciones Técnicas de Seguridad en Edificaciones (D.S. N° 002-2018-PCM).\n" +
                    "3. Me sujeto al principio de presunción de veracidad y a la fiscalización posterior regulada por el TUO de la Ley N° 27444.",
                    fontText
            );
            pCondiciones.setAlignment(Element.ALIGN_JUSTIFIED);
            document.add(pCondiciones);

            Paragraph firma = new Paragraph(
                    "\n\n\n___________________________________________\n" +
                    "Firma del Administrado o Representante Legal\n" +
                    "DNI / RUC: " + (expediente.getDocumentoIdentidad() != null ? expediente.getDocumentoIdentidad() : "") + "\n\n" +
                    "--- DOCUMENTO GENERADO ELECTRÓNICAMENTE CON VALIDEZ OFICIAL ---",
                    FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY)
            );
            firma.setAlignment(Element.ALIGN_CENTER);
            document.add(firma);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error al generar PDF de Declaración Jurada", e);
            throw new RuntimeException("Error al generar PDF de Declaración Jurada", e);
        }
    }

    public byte[] generarVoucherSatPdf(VoucherDto voucher) {
        Document document = new Document(PageSize.A5, 30, 30, 30, 30);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

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
            Code128Writer writer = new Code128Writer();
            BitMatrix bitMatrix = writer.encode(codBarrasTexto, BarcodeFormat.CODE_128, 300, 50);
            ByteArrayOutputStream outBarcode = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outBarcode);

            Image barcodeImg = Image.getInstance(outBarcode.toByteArray());
            barcodeImg.setAlignment(Element.ALIGN_CENTER);
            barcodeImg.setSpacingBefore(12f);
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

    /**
     * US-10: Genera imagen PNG del código QR conteniendo la URL pública de validación.
     */
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

    private Paragraph crearTituloSeccion(String titulo) {
        Font fontSec = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(0, 51, 102));
        Paragraph p = new Paragraph(titulo, fontSec);
        p.setSpacingBefore(4f);
        p.setSpacingAfter(2f);
        return p;
    }

    private void agregarFila(PdfPTable table, String campo, String valor) {
        Font fontCampo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
        Font fontValor = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);

        PdfPCell c1 = new PdfPCell(new Phrase(campo, fontCampo));
        c1.setBackgroundColor(new Color(248, 250, 252));
        c1.setPadding(4);

        PdfPCell c2 = new PdfPCell(new Phrase(valor, fontValor));
        c2.setPadding(4);

        table.addCell(c1);
        table.addCell(c2);
    }
}
