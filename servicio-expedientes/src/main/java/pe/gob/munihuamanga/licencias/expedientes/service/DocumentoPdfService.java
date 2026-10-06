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
import lombok.RequiredArgsConstructor;
import pe.gob.munihuamanga.licencias.common.dto.ExpedienteResponseDto;
import pe.gob.munihuamanga.licencias.common.dto.VoucherDto;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio encargado de la renderización directa de formatos oficiales en PDF para el servicio de expedientes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentoPdfService {

    private final Anexo1PdfGenerator anexo1PdfGenerator;
    private final Anexo3PdfGenerator anexo3PdfGenerator;
    private final Anexo4PdfGenerator anexo4PdfGenerator;
    private final LicenciaPdfGenerator licenciaPdfGenerator;

    @Value("${portal.verificacion.url:http://localhost:8081/verificar-licencia.html?codigo=}")
    private String portalVerificacionUrl;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] generarAnexo1DeclaracionJurada(ExpedienteResponseDto expediente) {
        return anexo1PdfGenerator.generarPdf(expediente);
    }

    public byte[] generarDeclaracionJurada(ExpedienteResponseDto expediente) {
        return anexo1PdfGenerator.generarPdf(expediente);
    }

    public byte[] generarAnexo3MatrizRiesgoItse(ExpedienteResponseDto expediente) {
        return anexo3PdfGenerator.generarPdf(expediente);
    }

    public byte[] generarAnexo4CondicionesSeguridad(ExpedienteResponseDto expediente) {
        return anexo4PdfGenerator.generarPdf(expediente);
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
     * Genera el Certificado Oficial de Licencia de Funcionamiento en PDF.
     *
     * US-09 / US-10: Replica el formato físico oficial de la Municipalidad Provincial de Huamanga
     * (Ley N° 28976 / TUO D.S. N° 046-2017-PCM) con:
     * - Borde decorativo perimetral (azul + dorado)
     * - Número de licencia en esquina superior derecha
     * - Encabezado institucional con escudo, franjas naranjas (Gerencia / Subgerencia)
     * - Título ornamental "LICENCIA DE FUNCIONAMIENTO DEFINITIVA"
     * - Tabla de campos normativos: Razón Social, Nombre Comercial, Dirección,
     *   RUC, Categoría, Área m², Giro(s), Zonificación, Expediente
     * - Horario de autorización (HH:mm → HH:mm) + cuadro VENCE
     * - Sección INDICACIONES legales
     * - Código QR de verificación (RNF-20) + Firma del Subgerente
     *
     * @param expediente Datos del expediente aprobado.
     * @return Bytes del PDF generado.
     */
    public byte[] generarLicenciaPdf(ExpedienteResponseDto expediente) {
        // Generar QR de verificación
        String codigoLicencia = expediente.getLicenciaQrCode() != null
                ? expediente.getLicenciaQrCode() : "LIC-2026-PENDIENTE";
        byte[] qrBytes = generarImagenQr(codigoLicencia, 150, 150);

        // Delegar al generador oficial de Licencia
        return licenciaPdfGenerator.generarLicencia(expediente, qrBytes);
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
