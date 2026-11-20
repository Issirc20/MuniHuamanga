package pe.gob.munihuamanga.licencias.expedientes.service;

import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.awt.Color;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Clase base institucional para todos los generadores PDF oficiales de la
 * Municipalidad Provincial de Huamanga (Anexos 1 a 4, resoluciones y certificados).
 * Centraliza la paleta de colores institucional, carga de escudos, tipografías y
 * métodos utilitarios de diagramación de tablas y celdas conforme a la Ley N° 28976.
 */
@Slf4j
public abstract class AbstractPdfMunicipalGenerator {

    protected static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    protected static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // Paleta de colores oficial institucional
    public static final Color COLOR_PRIMARY_NAVY = new Color(0, 43, 92);       // #002b5c
    public static final Color COLOR_PRIMARY_BLUE = new Color(0, 51, 102);       // #003366
    public static final Color COLOR_SEC_BAR = new Color(0, 51, 102);
    public static final Color COLOR_BORDER = new Color(30, 41, 59);            // #1e293b
    public static final Color COLOR_BORDER_LIGHT = new Color(160, 160, 160);
    public static final Color COLOR_YELLOW_CELL = new Color(255, 253, 231);    // #fffde7
    public static final Color COLOR_SUBHEADER_BG = new Color(241, 245, 249);   // #f1f5f9
    public static final Color COLOR_TEXT_VAL = new Color(0, 43, 92);
    public static final Color COLOR_LABEL_GRAY = new Color(100, 116, 139);     // #64748b
    public static final Color COLOR_LIGHT_GRAY = new Color(248, 250, 252);
    public static final Color COLOR_RIESGO_BAJO = new Color(34, 197, 94);
    public static final Color COLOR_RIESGO_MEDIO = new Color(234, 179, 8);
    public static final Color COLOR_RIESGO_ALTO = new Color(249, 115, 22);
    public static final Color COLOR_RIESGO_MUY_ALTO = new Color(239, 68, 68);

    // Tipografías compartidas estandarizadas
    public static final Font F_DOC_BADGE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, COLOR_PRIMARY_NAVY);
    public static final Font F_DOC_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.0f, Color.BLACK);
    public static final Font F_DOC_LAW = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 5.8f, new Color(71, 85, 105));
    public static final Font F_DOC_VERSION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.0f, new Color(71, 85, 105));
    public static final Font F_SEC_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.5f, Color.WHITE);
    public static final Font F_SUBSEC_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 5.8f, COLOR_PRIMARY_NAVY);
    public static final Font F_CELL_LABEL = FontFactory.getFont(FontFactory.HELVETICA, 5.0f, COLOR_LABEL_GRAY);
    public static final Font F_CELL_VAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.0f, COLOR_TEXT_VAL);
    public static final Font F_CHECK_X = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.0f, COLOR_PRIMARY_NAVY);
    public static final Font F_FOOT_NOTE = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 5.0f, COLOR_LABEL_GRAY);

    protected final ResourceLoader resourceLoader;

    @Value("${muni.documentos.escudo-path:classpath:static/img/escudo-huamanga.png}")
    protected String escudoPath = "classpath:static/img/escudo-huamanga.png";

    protected AbstractPdfMunicipalGenerator(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    protected AbstractPdfMunicipalGenerator() {
        this.resourceLoader = null;
    }

    /**
     * Carga el escudo institucional oficial con fallback al classpath.
     */
    protected Image obtenerEscudo(float maxWidth, float maxHeight) {
        try {
            InputStream is = resolverInputStream(escudoPath, "/static/img/escudo-huamanga.png");
            if (is != null) {
                byte[] bytes;
                try (is) {
                    bytes = is.readAllBytes();
                }
                Image img = Image.getInstance(bytes);
                img.scaleToFit(maxWidth, maxHeight);
                img.setAlignment(Element.ALIGN_CENTER);
                return img;
            }
        } catch (Exception e) {
            log.warn("[PDF] Escudo institucional no disponible: {}", e.getMessage());
        }
        return null;
    }

    protected Image obtenerEscudo() {
        return obtenerEscudo(38f, 32f);
    }

    protected InputStream resolverInputStream(String path, String classpathFallback) {
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

    protected PdfPCell crearCeldaTituloSeccion(String texto, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(texto, F_SEC_TITLE));
        c.setBackgroundColor(bg != null ? bg : COLOR_PRIMARY_NAVY);
        c.setPadding(3.5f);
        c.setHorizontalAlignment(Element.ALIGN_LEFT);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    protected PdfPCell crearCeldaEtiquetaValor(String etiqueta, String valor) {
        Paragraph p = new Paragraph();
        p.add(new Phrase(etiqueta + "\n", F_CELL_LABEL));
        p.add(new Phrase(sanitizar(valor), F_CELL_VAL));
        PdfPCell c = new PdfPCell(p);
        c.setBackgroundColor(COLOR_YELLOW_CELL);
        c.setPadding(3.0f);
        c.setBorderColor(COLOR_BORDER);
        c.setBorderWidth(0.5f);
        return c;
    }

    protected PdfPCell crearCeldaCheckbox(String etiqueta, boolean marcado) {
        PdfPTable sub = new PdfPTable(2);
        try {
            sub.setWidths(new float[]{15f, 85f});
        } catch (Exception ignored) {}
        sub.setWidthPercentage(100);

        PdfPCell box = new PdfPCell(new Phrase(marcado ? "X" : " ", F_CHECK_X));
        box.setHorizontalAlignment(Element.ALIGN_CENTER);
        box.setVerticalAlignment(Element.ALIGN_MIDDLE);
        box.setBorderColor(COLOR_BORDER);
        box.setBorderWidth(0.5f);
        box.setBackgroundColor(Color.WHITE);
        box.setPadding(1f);
        sub.addCell(box);

        PdfPCell lbl = new PdfPCell(new Phrase(" " + etiqueta, F_CELL_LABEL));
        lbl.setBorder(Rectangle.NO_BORDER);
        lbl.setVerticalAlignment(Element.ALIGN_MIDDLE);
        sub.addCell(lbl);

        PdfPCell c = new PdfPCell(sub);
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(1.5f);
        return c;
    }

    protected static String sanitizar(String s) {
        if (s == null || s.trim().isEmpty() || "null".equalsIgnoreCase(s.trim())) {
            return "-";
        }
        return s.trim();
    }

    protected static String formatearFecha(LocalDate fecha) {
        return fecha != null ? fecha.format(DATE_FMT) : "-";
    }

    protected static String formatearFecha(LocalDateTime fechaHora) {
        return fechaHora != null ? fechaHora.format(DATE_FMT) : "-";
    }
}
