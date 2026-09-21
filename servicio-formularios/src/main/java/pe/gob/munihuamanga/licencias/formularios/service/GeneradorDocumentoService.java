package pe.gob.munihuamanga.licencias.formularios.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.gob.munihuamanga.licencias.common.dto.ExpedienteResponseDto;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class GeneradorDocumentoService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Genera en PDF la Declaración Jurada para Licencia de Funcionamiento (Ley N° 28976).
     */
    public byte[] generarDeclaracionJurada(ExpedienteResponseDto expediente) {
        Document document = new Document(PageSize.A4, 40, 40, 50, 40);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Encabezado institucional
            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(0, 51, 102));
            Paragraph header = new Paragraph("MUNICIPALIDAD PROVINCIAL DE HUAMANGA\nGERENCIA DE LICENCIAS Y AUTORIZACIONES", fontHeader);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);

            Font fontSub = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.DARK_GRAY);
            Paragraph sub = new Paragraph("\nANEXO 1: DECLARACIÓN JURADA PARA LICENCIA DE FUNCIONAMIENTO\n(Ley N° 28976 - TUO D.S. N° 046-2017-PCM)", fontSub);
            sub.setAlignment(Element.ALIGN_CENTER);
            document.add(sub);

            document.add(new Paragraph("\n"));

            // Tabla de datos del expediente
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);

            agregarFila(table, "Número de Trámite:", expediente.getNumeroTramite() != null ? expediente.getNumeroTramite() : "-");
            agregarFila(table, "Titular / Solicitante:", expediente.getNombreTitular() != null ? expediente.getNombreTitular() : "-");
            agregarFila(table, "DNI / RUC:", expediente.getDocumentoIdentidad() != null ? expediente.getDocumentoIdentidad() : "-");
            agregarFila(table, "Razón Social:", expediente.getRazonSocial() != null ? expediente.getRazonSocial() : "(Persona Natural)");
            agregarFila(table, "Nombre Comercial:", expediente.getNombreComercial() != null ? expediente.getNombreComercial() : "-");
            agregarFila(table, "Giro de Actividad:", expediente.getGiroNegocio() != null ? expediente.getGiroNegocio() : "-");
            agregarFila(table, "Dirección Establecimiento:", expediente.getDireccionEstablecimiento() != null ? expediente.getDireccionEstablecimiento() : "-");
            agregarFila(table, "Área Total:", expediente.getAreaMetrosCuadrados() != null ? expediente.getAreaMetrosCuadrados() + " m²" : "-");
            agregarFila(table, "Fecha de Registro:", expediente.getFechaCreacion() != null ? expediente.getFechaCreacion().format(FORMATTER) : "-");

            document.add(table);

            // Declaración legal
            Font fontText = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Paragraph textoLegal = new Paragraph(
                    "\nDECLARO BAJO JURAMENTO que los datos consignados en el presente documento son verdaderos y se sujetan al principio de presunción de veracidad establecido en el TUO de la Ley N° 27444. Asimismo, me comprometo a mantener las condiciones de seguridad en la edificación declaradas.",
                    fontText
            );
            textoLegal.setAlignment(Element.ALIGN_JUSTIFIED);
            document.add(textoLegal);

            // Espacio de firma
            Paragraph firma = new Paragraph("\n\n\n\n_____________________________________\nFirma Digital / Manuscrita del Titular\nDocumento Verificable Electrónicamente", fontText);
            firma.setAlignment(Element.ALIGN_CENTER);
            document.add(firma);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error al generar PDF de Declaración Jurada", e);
            throw new RuntimeException("Error al generar el formato PDF de la Declaración Jurada", e);
        }
    }

    private void agregarFila(PdfPTable table, String campo, String valor) {
        Font fontCampo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
        Font fontValor = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);

        PdfPCell c1 = new PdfPCell(new Phrase(campo, fontCampo));
        c1.setBackgroundColor(new Color(245, 247, 250));
        c1.setPadding(6);

        PdfPCell c2 = new PdfPCell(new Phrase(valor, fontValor));
        c2.setPadding(6);

        table.addCell(c1);
        table.addCell(c2);
    }
}
