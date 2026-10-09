package pe.gob.munihuamanga.licencias.expedientes.service;

import com.lowagie.text.Image;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
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
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Generador PDF Oficial del Anexo N° 1:
 * FORMATO DE DECLARACIÓN JURADA PARA LICENCIA DE FUNCIONAMIENTO (Versión 03)
 * Conforme a la Ley N° 28976, D.S. N° 046-2017-PCM y D.S. N° 163-2020-PCM.
 *
 * Utiliza estampación vectorial directa (PdfReader + PdfStamper) sobre la plantilla oficial
 * anexo1-oficial-v03.pdf (2 páginas A4), preservando exactamente todos los textos legales,
 * tablas y formato normativo sin rasterización.
 */
@Slf4j
@Component
public class Anexo1PdfGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Color COLOR_VAL = new Color(0, 51, 102);     // Azul institucional para datos
    private static final Color COLOR_MARK = new Color(0, 32, 96);     // Azul oscuro para marcas X
    private static final Color COLOR_BLACK = Color.BLACK;

    private final ResourceLoader resourceLoader;

    @Value("${muni.documentos.anexo1.plantilla-path:classpath:templates/pdf/anexo1-oficial-v03.pdf}")
    private String plantillaPath;

    @Value("${muni.documentos.escudo-path:classpath:static/img/escudo-huamanga.png}")
    private String escudoPath;

    public Anexo1PdfGenerator(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    // Constructor sin dependencias para compatibilidad con tests unitarios
    public Anexo1PdfGenerator() {
        this.resourceLoader = null;
        this.plantillaPath = "classpath:templates/pdf/anexo1-oficial-v03.pdf";
        this.escudoPath = "classpath:static/img/escudo-huamanga.png";
    }

    public byte[] generarPdf(ExpedienteResponseDto exp) {
        if (exp == null) {
            throw new IllegalArgumentException("El expediente no puede ser nulo");
        }

        byte[] templateBytes = cargarPlantilla();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfReader reader = new PdfReader(templateBytes);
            int totalPages = reader.getNumberOfPages();
            if (totalPages != 2) {
                log.warn("La plantilla oficial tiene {} páginas (se esperaban exactamente 2)", totalPages);
            }

            PdfStamper stamper = new PdfStamper(reader, baos);

            BaseFont fontRegular = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            BaseFont fontBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);

            // ==================== ESTAMPADO PÁGINA 1 ====================
            PdfContentByte cb1 = stamper.getOverContent(1);
            estamparEscudo(cb1, 1);
            estamparEncabezado(cb1, exp, 1, fontRegular, fontBold);
            estamparSeccionI(cb1, exp, fontRegular, fontBold);
            estamparSeccionII(cb1, exp, fontRegular, fontBold);
            estamparSeccionIII(cb1, exp, fontRegular, fontBold);
            estamparSeccionIV(cb1, exp, fontRegular, fontBold);

            // ==================== ESTAMPADO PÁGINA 2 ====================
            if (totalPages >= 2) {
                PdfContentByte cb2 = stamper.getOverContent(2);
                estamparEscudo(cb2, 2);
                estamparEncabezado(cb2, exp, 2, fontRegular, fontBold);
                estamparSeccionV(cb2, exp, fontRegular, fontBold);
                estamparSeccionVI(cb2, exp, fontRegular, fontBold);
            }

            stamper.close();
            reader.close();

            log.info("Anexo 1 oficial generado exitosamente para expediente: {}", exp.getNumeroTramite());
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error crítico al estampar PDF del Anexo 1 oficial: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar PDF del Anexo 1 oficial", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // CARGA DE RECURSOS
    // ─────────────────────────────────────────────────────────────────────────────

    private byte[] cargarPlantilla() {
        try {
            InputStream is = resolverInputStream(plantillaPath, "/templates/pdf/anexo1-oficial-v03.pdf");
            if (is == null) {
                throw new IllegalStateException("No se pudo localizar la plantilla PDF oficial en: " + plantillaPath);
            }
            try (is) {
                return is.readAllBytes();
            }
        } catch (Exception e) {
            log.error("Fallo al leer plantilla PDF de: {}", plantillaPath, e);
            throw new RuntimeException("Plantilla oficial no encontrada", e);
        }
    }

    private void estamparEscudo(PdfContentByte cb, int page) {
        try {
            InputStream is = resolverInputStream(escudoPath, "/static/img/escudo-huamanga.png");
            if (is == null) {
                log.debug("Escudo no disponible, conservando espacio del recuadro oficial");
                return;
            }
            byte[] imgBytes;
            try (is) {
                imgBytes = is.readAllBytes();
            }
            Image img = Image.getInstance(imgBytes);

            // Dimensiones del recuadro "Logo de la Entidad": Ancho máx ~75pt, Alto máx ~45pt
            float maxW = 75f;
            float maxH = 46f;
            float origW = img.getWidth();
            float origH = img.getHeight();
            float scale = Math.min(maxW / origW, maxH / origH);
            float finalW = origW * scale;
            float finalH = origH * scale;

            // Centro del recuadro Logo: x ~ 115, y ~ 700
            float posX = 115f - (finalW / 2f);
            float posY = 700f - (finalH / 2f);

            img.setAbsolutePosition(posX, posY);
            img.scaleAbsolute(finalW, finalH);
            cb.addImage(img);
        } catch (Exception e) {
            log.warn("No se pudo cargar o estampar el escudo institucional (se continúa sin imagen): {}", e.getMessage());
        }
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
        // Fallback por classloader
        InputStream is = getClass().getResourceAsStream(classpathFallback);
        if (is != null) {
            return is;
        }
        if (path != null && path.startsWith("classpath:")) {
            String cp = path.substring("classpath:".length());
            if (!cp.startsWith("/")) cp = "/" + cp;
            return getClass().getResourceAsStream(cp);
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // ENCABEZADO (Páginas 1 y 2)
    // ─────────────────────────────────────────────────────────────────────────────

    private void estamparEncabezado(PdfContentByte cb, ExpedienteResponseDto exp, int pagina, BaseFont fReg, BaseFont fBold) {
        // N° de expediente (ej: EXP-2024-001234)
        String nroExp = exp.getNumeroTramite() != null ? exp.getNumeroTramite() : "";
        drawText(cb, nroExp, 420f, 710.44f, 6.8f, true, COLOR_VAL, fBold);

        // Fecha de recepción
        if (exp.getFechaCreacion() != null) {
            String fRecep = exp.getFechaCreacion().format(DATE_FMT);
            drawText(cb, fRecep, 465f, 696.47f, 6.5f, true, COLOR_VAL, fBold);
        }

        // N° de recibo de pago
        String nroRecibo = exp.getNumeroOperacionSat() != null ? exp.getNumeroOperacionSat() : exp.getVoucherId();
        if (nroRecibo != null && !nroRecibo.isBlank()) {
            drawText(cb, nroRecibo, 425f, 682.71f, 6.5f, true, COLOR_VAL, fBold);
        }

        // Fecha de pago
        if (exp.getFechaPagoSat() != null) {
            String fPago = exp.getFechaPagoSat().format(DATE_FMT);
            drawText(cb, fPago, 425f, 668.41f, 6.5f, true, COLOR_VAL, fBold);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // SECCIÓN I: MODALIDAD DEL TRÁMITE
    // ─────────────────────────────────────────────────────────────────────────────

    private void estamparSeccionI(PdfContentByte cb, ExpedienteResponseDto exp, BaseFont fReg, BaseFont fBold) {
        ModalidadTramite mod = exp.getModalidadTramite() != null ? exp.getModalidadTramite() : ModalidadTramite.LICENCIA_INDETERMINADA;

        switch (mod) {
            case LICENCIA_INDETERMINADA -> drawMark(cb, 88.5f, 615.82f, fBold);
            case LICENCIA_TEMPORAL -> {
                drawMark(cb, 162.5f, 615.82f, fBold);
                if (exp.getPlazoTemporalMeses() != null && exp.getPlazoTemporalMeses() > 0) {
                    drawText(cb, exp.getPlazoTemporalMeses() + " meses", 155f, 591.60f, 6.0f, true, COLOR_VAL, fBold);
                }
            }
            case LICENCIA_CON_ANUNCIO -> {
                drawMark(cb, 88.5f, 579.55f, fBold);
                if (exp.getTipoAnuncio() != null) {
                    drawFittedText(cb, exp.getTipoAnuncio(), 100f, 558.21f, 130f, 6.0f, false, COLOR_VAL, fReg);
                }
            }
            case LICENCIA_CESIONARIO -> {
                drawMark(cb, 88.5f, 542.85f, fBold);
                if (exp.getNumeroLicenciaPrincipal() != null) {
                    drawText(cb, exp.getNumeroLicenciaPrincipal(), 100f, 522.47f, 6.0f, true, COLOR_VAL, fBold);
                }
            }
            case LICENCIA_MERCADOS_GALERIAS -> drawMark(cb, 88.5f, 503.06f, fBold);
            case CAMBIO_DENOMINACION -> {
                drawMark(cb, 236.5f, 615.82f, fBold);
                if (exp.getNumeroLicenciaPrincipal() != null) {
                    drawText(cb, exp.getNumeroLicenciaPrincipal(), 246.76f, 591.60f, 6.0f, true, COLOR_VAL, fBold);
                }
                if (exp.getRazonSocial() != null) {
                    drawFittedText(cb, exp.getRazonSocial(), 246.76f, 566.96f, 135f, 5.8f, true, COLOR_VAL, fBold);
                }
            }
            case TRANSFERENCIA_LICENCIA -> {
                drawMark(cb, 236.5f, 549.46f, fBold);
                if (exp.getNumeroLicenciaPrincipal() != null) {
                    drawText(cb, exp.getNumeroLicenciaPrincipal(), 246.76f, 497.72f, 6.0f, true, COLOR_VAL, fBold);
                }
            }
            case CESE_ACTIVIDADES -> {
                drawMark(cb, 382.5f, 615.82f, fBold);
                if (exp.getNumeroLicenciaPrincipal() != null) {
                    drawText(cb, exp.getNumeroLicenciaPrincipal(), 392.91f, 589.58f, 6.0f, true, COLOR_VAL, fBold);
                }
            }
            case OTROS -> drawMark(cb, 382.5f, 580.40f, fBold);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // SECCIÓN II: DATOS DEL SOLICITANTE
    // ─────────────────────────────────────────────────────────────────────────────

    private void estamparSeccionII(PdfContentByte cb, ExpedienteResponseDto exp, BaseFont fReg, BaseFont fBold) {
        // Fila 1: Apellidos y Nombres / Razón Social
        String titular = exp.getTipoPersona() == TipoPersona.JURIDICA && exp.getRazonSocial() != null
                ? exp.getRazonSocial()
                : (exp.getNombreTitular() != null ? exp.getNombreTitular() : "");
        drawFittedText(cb, titular, 80f, 453.5f, 430f, 6.8f, true, COLOR_VAL, fBold);

        // Fila 2: Documentos y Contacto
        String doc = exp.getDocumentoIdentidad() != null ? exp.getDocumentoIdentidad() : "";
        if (exp.getTipoPersona() == TipoPersona.JURIDICA || (exp.getTipoDocumento() == TipoDocumento.RUC)) {
            drawCenteredText(cb, doc, 185f, 428.0f, 6.5f, true, COLOR_VAL, fBold);
        } else {
            drawCenteredText(cb, doc, 112f, 428.0f, 6.5f, true, COLOR_VAL, fBold);
        }

        if (exp.getTelefono() != null) {
            drawCenteredText(cb, exp.getTelefono(), 286f, 428.0f, 6.5f, true, COLOR_VAL, fBold);
        }
        if (exp.getCorreoElectronico() != null) {
            drawFittedText(cb, exp.getCorreoElectronico(), 345f, 428.0f, 160f, 6.5f, true, COLOR_VAL, fBold);
        }

        // Fila 3: Dirección desglosada
        String via = construirVia(exp);
        drawFittedText(cb, via, 80f, 393.0f, 115f, 6.2f, false, COLOR_VAL, fReg);

        String nroInt = construirNroInt(exp);
        drawCenteredText(cb, nroInt, 235f, 393.0f, 6.2f, true, COLOR_VAL, fBold);

        String urb = exp.getUrbanizacion() != null ? exp.getUrbanizacion() : "";
        drawFittedText(cb, urb, 275f, 393.0f, 105f, 6.2f, false, COLOR_VAL, fReg);

        String distProv = construirDistritoProvincia(exp);
        drawFittedText(cb, distProv, 385f, 393.0f, 125f, 6.2f, false, COLOR_VAL, fReg);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // SECCIÓN III: REPRESENTANTE LEGAL O APODERADO
    // ─────────────────────────────────────────────────────────────────────────────

    private void estamparSeccionIII(PdfContentByte cb, ExpedienteResponseDto exp, BaseFont fReg, BaseFont fBold) {
        if (exp.getNombreRepresentante() != null && !exp.getNombreRepresentante().isBlank()) {
            drawFittedText(cb, exp.getNombreRepresentante(), 80f, 345.0f, 230f, 6.5f, true, COLOR_VAL, fBold);
        }
        if (exp.getDniRepresentante() != null && !exp.getDniRepresentante().isBlank()) {
            drawCenteredText(cb, exp.getDniRepresentante(), 345f, 345.0f, 6.5f, true, COLOR_VAL, fBold);
        }
        if (exp.getPartidaSunarp() != null && !exp.getPartidaSunarp().isBlank()) {
            String sunarp = exp.getPartidaSunarp();
            if (exp.getAsientoSunarp() != null && !exp.getAsientoSunarp().isBlank()) {
                sunarp += " - Asiento " + exp.getAsientoSunarp();
            }
            drawFittedText(cb, sunarp, 380f, 345.0f, 130f, 6.2f, true, COLOR_VAL, fBold);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // SECCIÓN IV: DATOS DEL ESTABLECIMIENTO
    // ─────────────────────────────────────────────────────────────────────────────

    private void estamparSeccionIV(PdfContentByte cb, ExpedienteResponseDto exp, BaseFont fReg, BaseFont fBold) {
        // Fila 1: Nombre comercial
        if (exp.getNombreComercial() != null) {
            drawFittedText(cb, exp.getNombreComercial(), 80f, 298.0f, 430f, 6.8f, true, COLOR_VAL, fBold);
        }

        // Fila 2: CIIU, Giro, Actividad, Zonificación
        if (exp.getCiiuCodigo() != null) {
            drawCenteredText(cb, exp.getCiiuCodigo(), 114f, 271.0f, 6.5f, true, COLOR_VAL, fBold);
        }
        if (exp.getGiroNegocio() != null) {
            drawFittedText(cb, exp.getGiroNegocio(), 175f, 271.0f, 90f, 6.0f, false, COLOR_VAL, fReg);
        }
        if (exp.getActividadDetallada() != null) {
            drawFittedText(cb, exp.getActividadDetallada(), 270f, 271.0f, 140f, 6.0f, false, COLOR_VAL, fReg);
        }
        if (exp.getZonificacion() != null) {
            drawFittedText(cb, exp.getZonificacion(), 415f, 271.0f, 95f, 6.0f, true, COLOR_VAL, fBold);
        }

        // Fila 3: Dirección del establecimiento
        String via = construirVia(exp);
        drawFittedText(cb, via, 80f, 237.0f, 115f, 6.2f, false, COLOR_VAL, fReg);

        String nroInt = construirNroInt(exp);
        drawCenteredText(cb, nroInt, 235f, 237.0f, 6.2f, true, COLOR_VAL, fBold);

        String urb = exp.getUrbanizacion() != null ? exp.getUrbanizacion() : "";
        drawFittedText(cb, urb, 275f, 237.0f, 105f, 6.2f, false, COLOR_VAL, fReg);

        String distProv = construirDistritoProvincia(exp);
        drawFittedText(cb, distProv, 385f, 237.0f, 125f, 6.2f, false, COLOR_VAL, fReg);

        // Fila 4: Autorización Sectorial (solo si corresponde)
        if (Boolean.TRUE.equals(exp.getRequiereAutorizacionSectorial())) {
            if (exp.getSectorEntidad() != null) {
                drawFittedText(cb, exp.getSectorEntidad(), 80f, 198.0f, 95f, 6.0f, true, COLOR_VAL, fBold);
            }
            if (exp.getSectorDenominacion() != null) {
                drawFittedText(cb, exp.getSectorDenominacion(), 180f, 198.0f, 140f, 6.0f, false, COLOR_VAL, fReg);
            }
            if (exp.getSectorFecha() != null) {
                drawCenteredText(cb, exp.getSectorFecha(), 360f, 198.0f, 6.0f, true, COLOR_VAL, fBold);
            }
            if (exp.getSectorNumero() != null) {
                drawFittedText(cb, exp.getSectorNumero(), 410f, 198.0f, 95f, 6.0f, true, COLOR_VAL, fBold);
            }
        }

        // Fila 5: Área total solicitada (m²)
        if (exp.getAreaMetrosCuadrados() != null) {
            String areaStr = String.format("%.2f", exp.getAreaMetrosCuadrados());
            drawCenteredText(cb, areaStr, 130f, 145.0f, 7.5f, true, COLOR_VAL, fBold);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // SECCIÓN V: DECLARACIÓN JURADA (Página 2)
    // ─────────────────────────────────────────────────────────────────────────────

    private void estamparSeccionV(PdfContentByte cb, ExpedienteResponseDto exp, BaseFont fReg, BaseFont fBold) {
        // Casilla 1: Poder suficiente (PJ o representante)
        if (exp.getTipoPersona() == TipoPersona.JURIDICA || (exp.getNombreRepresentante() != null && !exp.getNombreRepresentante().isBlank())) {
            drawMark(cb, 83.5f, 628.0f, fBold);
        }

        // Casilla 2: Condiciones de seguridad ITSE (aplica por defecto a toda solicitud válida)
        drawMark(cb, 83.5f, 619.0f, fBold);

        // Casilla 3: Título profesional en salud (si aplica al giro)
        if (exp.getGiroNegocio() != null && exp.getGiroNegocio().toLowerCase().contains("salud")) {
            drawMark(cb, 83.5f, 604.0f, fBold);
        }

        // Observaciones o comentarios del solicitante
        if (exp.getMotivoObservacion() != null && !exp.getMotivoObservacion().isBlank()) {
            drawFittedText(cb, exp.getMotivoObservacion(), 80f, 540.0f, 430f, 6.0f, false, COLOR_VAL, fReg);
        }

        // Fecha de declaración
        String fechaDec = exp.getFechaCreacion() != null ? exp.getFechaCreacion().format(DATE_FMT) : "";
        if (!fechaDec.isBlank()) {
            drawText(cb, fechaDec, 130f, 509.74f, 6.5f, true, COLOR_VAL, fBold);
        }

        // Datos del firmante (DNI y Nombres) - El espacio de firma física se conserva limpio
        String dniFirmante = exp.getDniRepresentante() != null && !exp.getDniRepresentante().isBlank()
                ? exp.getDniRepresentante()
                : (exp.getDocumentoIdentidad() != null ? exp.getDocumentoIdentidad() : "");
        if (!dniFirmante.isBlank()) {
            drawText(cb, dniFirmante, 310f, 458.61f, 6.5f, true, COLOR_VAL, fBold);
        }

        String nombreFirmante = exp.getNombreRepresentante() != null && !exp.getNombreRepresentante().isBlank()
                ? exp.getNombreRepresentante()
                : (exp.getNombreTitular() != null ? exp.getNombreTitular() : "");
        if (!nombreFirmante.isBlank()) {
            drawFittedText(cb, nombreFirmante, 330f, 447.0f, 180f, 6.5f, true, COLOR_VAL, fBold);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // SECCIÓN VI: CLASIFICACIÓN DEL NIVEL DE RIESGO (Página 2)
    // ─────────────────────────────────────────────────────────────────────────────

    private void estamparSeccionVI(PdfContentByte cb, ExpedienteResponseDto exp, BaseFont fReg, BaseFont fBold) {
        if (exp.getNivelRiesgo() != null) {
            switch (exp.getNivelRiesgo()) {
                case BAJO -> drawMark(cb, 88.0f, 405.0f, fBold);
                case MEDIO -> drawMark(cb, 190.0f, 405.0f, fBold);
                case ALTO -> drawMark(cb, 295.0f, 405.0f, fBold);
                case MUY_ALTO -> drawMark(cb, 400.0f, 405.0f, fBold);
            }
        }
        // Firma y sello del calificador municipal se conservan en blanco para el funcionario.
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // HELPERS DE RENDERIZADO
    // ─────────────────────────────────────────────────────────────────────────────

    private void drawText(PdfContentByte cb, String text, float x, float y, float size, boolean bold, Color color, BaseFont font) {
        if (text == null || text.isBlank()) return;
        cb.saveState();
        cb.beginText();
        cb.setFontAndSize(font, size);
        cb.setColorFill(color);
        cb.setTextMatrix(x, y);
        cb.showText(text);
        cb.endText();
        cb.restoreState();
    }

    private void drawCenteredText(PdfContentByte cb, String text, float xCenter, float y, float size, boolean bold, Color color, BaseFont font) {
        if (text == null || text.isBlank()) return;
        cb.saveState();
        cb.beginText();
        cb.setFontAndSize(font, size);
        cb.setColorFill(color);
        cb.showTextAligned(PdfContentByte.ALIGN_CENTER, text, xCenter, y, 0);
        cb.endText();
        cb.restoreState();
    }

    private void drawFittedText(PdfContentByte cb, String text, float x, float y, float maxWidth, float baseSize, boolean bold, Color color, BaseFont font) {
        if (text == null || text.isBlank()) return;
        float size = baseSize;
        float textWidth = font.getWidthPoint(text, size);
        while (textWidth > maxWidth && size > 4.5f) {
            size -= 0.3f;
            textWidth = font.getWidthPoint(text, size);
        }
        drawText(cb, text, x, y, size, bold, color, font);
    }

    private void drawMark(PdfContentByte cb, float x, float y, BaseFont fBold) {
        cb.saveState();
        cb.beginText();
        cb.setFontAndSize(fBold, 8.5f);
        cb.setColorFill(COLOR_MARK);
        cb.showTextAligned(PdfContentByte.ALIGN_CENTER, "X", x, y, 0);
        cb.endText();
        cb.restoreState();
    }

    private String construirVia(ExpedienteResponseDto exp) {
        if (exp.getTipoVia() != null && exp.getNombreVia() != null) {
            return exp.getTipoVia() + " " + exp.getNombreVia();
        }
        if (exp.getDireccionEstablecimiento() != null) {
            return exp.getDireccionEstablecimiento();
        }
        return "";
    }

    private String construirNroInt(ExpedienteResponseDto exp) {
        StringBuilder sb = new StringBuilder();
        if (exp.getNumeroVivienda() != null && !exp.getNumeroVivienda().isBlank()) {
            sb.append("N° ").append(exp.getNumeroVivienda());
        }
        if (exp.getInterior() != null && !exp.getInterior().isBlank()) {
            if (!sb.isEmpty()) sb.append(" Int. ");
            sb.append(exp.getInterior());
        }
        if (exp.getManzana() != null && !exp.getManzana().isBlank()) {
            if (!sb.isEmpty()) sb.append(" Mz. ");
            sb.append(exp.getManzana());
        }
        if (exp.getLote() != null && !exp.getLote().isBlank()) {
            if (!sb.isEmpty()) sb.append(" Lt. ");
            sb.append(exp.getLote());
        }
        return sb.toString();
    }

    private String construirDistritoProvincia(ExpedienteResponseDto exp) {
        String dist = exp.getDistrito() != null ? exp.getDistrito() : "Ayacucho";
        String prov = exp.getProvincia() != null ? exp.getProvincia() : "Huamanga";
        return dist + " - " + prov;
    }
}
