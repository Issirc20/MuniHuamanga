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
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.dto.ExpedienteResponseDto;
import pe.gob.munihuamanga.licencias.common.enums.FuncionEdificacion;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generador PDF Oficial del Anexo N° 3:
 * REPORTE DE NIVEL DE RIESGO DEL ESTABLECIMIENTO OBJETO DE INSPECCIÓN (MATRIZ ITSE)
 * Conforme al Reglamento de Inspecciones Técnicas de Seguridad en Edificaciones (D.S. N° 002-2018-PCM)
 * y el Manual de Ejecución de ITSE aprobado por Resolución Jefatural N° 016-2018-CENEPRED/J.
 * Genera exactamente dos (2) páginas idénticas al formato estándar oficial de Defensa Civil.
 */
@Slf4j
@Component
public class Anexo3PdfGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // Tipografías oficiales
    private static final Font F_TITLE_INST = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, new Color(0, 51, 102));
    private static final Font F_TITLE_SUB = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7f, new Color(51, 65, 85));
    private static final Font F_TITLE_MAIN = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Color.BLACK);
    private static final Font F_TITLE_LAW = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 6.2f, Color.DARK_GRAY);

    private static final Font F_BOX_LABEL = FontFactory.getFont(FontFactory.HELVETICA, 6f, Color.BLACK);
    private static final Font F_BOX_VAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.5f, new Color(0, 51, 102));

    private static final Font F_SEC_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.8f, Color.WHITE);
    private static final Font F_COL_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6f, Color.BLACK);
    private static final Font F_ITEM = FontFactory.getFont(FontFactory.HELVETICA, 5.8f, Color.BLACK);
    private static final Font F_ITEM_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 5.8f, Color.BLACK);

    private static final Font F_LABEL = FontFactory.getFont(FontFactory.HELVETICA, 5.8f, Color.DARK_GRAY);
    private static final Font F_VAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.2f, Color.BLACK);

    private static final Font F_FOOT_NOTE = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 5.2f, Color.DARK_GRAY);
    private static final Font F_NORM_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.2f, new Color(0, 51, 102));
    private static final Font F_NORM_BODY = FontFactory.getFont(FontFactory.HELVETICA, 5.4f, new Color(40, 40, 40));

    // Colores normativos oficiales CENEPRED
    private static final Color COLOR_SEC_BAR = new Color(0, 51, 102); // Azul institucional Huamanga
    private static final Color COLOR_RIESGO_BAJO = new Color(34, 197, 94);     // Verde
    private static final Color COLOR_RIESGO_MEDIO = new Color(234, 179, 8);    // Amarillo
    private static final Color COLOR_RIESGO_ALTO = new Color(249, 115, 22);    // Naranja
    private static final Color COLOR_RIESGO_MUY_ALTO = new Color(239, 68, 68); // Rojo
    private static final Color BG_HEADER = new Color(240, 243, 246);
    private static final Color BORDER_COLOR = new Color(160, 160, 160);

    public byte[] generarPdf(ExpedienteResponseDto exp) {
        Document document = new Document(PageSize.A4, 20f, 20f, 18f, 18f);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // ==================== PÁGINA 1 ====================
            agregarEncabezado(document, exp, 1);
            agregarSeccionI_DatosGenerales(document, exp);
            agregarSeccionII_MatrizFunciones(document, exp);
            agregarSeccionIII_FactoresAgravantes(document, exp);
            agregarSeccionIV_CalificacionFinal(document, exp);

            Paragraph pFoot1 = new Paragraph("* Conforme al D.S. N° 002-2018-PCM y el Manual ITSE aprobado por R.J. N° 016-2018-CENEPRED/J.", F_FOOT_NOTE);
            pFoot1.setSpacingBefore(3f);
            document.add(pFoot1);

            // ==================== PÁGINA 2 ====================
            document.newPage();
            agregarEncabezado(document, exp, 2);
            agregarCriteriosDetalladosFunciones(document);
            agregarSeccionV_DictamenYSuscripcion(document, exp);
            agregarMarcoLegalYAdvertencias(document);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error al generar PDF del Anexo 3 oficial", e);
            throw new RuntimeException("Error al generar PDF del Anexo 3", e);
        }
    }

    private void agregarEncabezado(Document document, ExpedienteResponseDto exp, int pagina) throws Exception {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{72f, 28f});
        headerTable.setSpacingAfter(4f);

        // Columna Izquierda: Identificación institucional
        PdfPCell cLeft = new PdfPCell();
        cLeft.setBorderColor(BORDER_COLOR);
        cLeft.setPadding(4f);

        Paragraph pMuni = new Paragraph("MUNICIPALIDAD PROVINCIAL DE HUAMANGA", F_TITLE_INST);
        pMuni.setAlignment(Element.ALIGN_CENTER);
        cLeft.addElement(pMuni);

        Paragraph pGer = new Paragraph("SUBGERENCIA DE DEFENSA CIVIL Y GESTIÓN DEL RIESGO DE DESASTRES", F_TITLE_SUB);
        pGer.setAlignment(Element.ALIGN_CENTER);
        cLeft.addElement(pGer);

        Paragraph pTitulo = new Paragraph("ANEXO 3 — REPORTE DE NIVEL DE RIESGO DEL ESTABLECIMIENTO OBJETO DE INSPECCIÓN", F_TITLE_MAIN);
        pTitulo.setAlignment(Element.ALIGN_CENTER);
        cLeft.addElement(pTitulo);

        Paragraph pLey = new Paragraph("Matriz de Riesgo según D.S. N° 002-2018-PCM y Manual ITSE R.J. N° 016-2018-CENEPRED", F_TITLE_LAW);
        pLey.setAlignment(Element.ALIGN_CENTER);
        cLeft.addElement(pLey);

        headerTable.addCell(cLeft);

        // Columna Derecha: Recuadro de expediente y página
        PdfPCell cRight = new PdfPCell();
        cRight.setBorderColor(BORDER_COLOR);
        cRight.setPadding(3f);

        PdfPTable tRec = new PdfPTable(2);
        tRec.setWidthPercentage(100);
        tRec.setWidths(new float[]{45f, 55f});

        String numExp = exp.getNumeroTramite() != null ? exp.getNumeroTramite() : "EXP-2026-00000";
        String fecha = exp.getFechaCreacion() != null ? exp.getFechaCreacion().format(DATE_FMT) : LocalDate.now().format(DATE_FMT);
        String hora = exp.getFechaCreacion() != null ? exp.getFechaCreacion().format(TIME_FMT) : "08:30";

        agregarFilaRecuadro(tRec, "N° EXPEDIENTE:", numExp);
        agregarFilaRecuadro(tRec, "FECHA EVAL:", fecha);
        agregarFilaRecuadro(tRec, "HORA:", hora);
        agregarFilaRecuadro(tRec, "PÁGINA:", pagina + " de 2");

        cRight.addElement(tRec);
        headerTable.addCell(cRight);

        document.add(headerTable);
    }

    private void agregarSeccionI_DatosGenerales(Document document, ExpedienteResponseDto exp) throws Exception {
        document.add(crearBarraSeccion("I. DATOS GENERALES DEL ESTABLECIMIENTO OBJETO DE EVALUACIÓN"));

        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{24f, 32f, 20f, 24f});
        t.setSpacingAfter(4f);

        t.addCell(crearCeldaDoble("1.1 Administrado / Titular:", exp.getNombreTitular()));
        t.addCell(crearCeldaDoble("1.2 Documento Identidad (DNI/RUC):", exp.getDocumentoIdentidad()));
        t.addCell(crearCeldaDoble("1.3 Razón Social:", exp.getRazonSocial() != null ? exp.getRazonSocial() : "(Persona Natural)"));
        t.addCell(crearCeldaDoble("1.4 Teléfono / Móvil:", exp.getTelefono() != null ? exp.getTelefono() : "-"));

        t.addCell(crearCeldaDoble("1.5 Nombre Comercial:", exp.getNombreComercial()));
        t.addCell(crearCeldaDoble("1.6 Giro(s) de Actividad Comercial:", exp.getGiroNegocio()));
        t.addCell(crearCeldaDoble("1.7 Área Ocupada (m²):", (exp.getAreaMetrosCuadrados() != null ? exp.getAreaMetrosCuadrados().toString() : "0.00") + " m²"));
        t.addCell(crearCeldaDoble("1.8 Aforo Estimado (personas):", calcularAforoEstimado(exp) + " pers."));

        PdfPCell cDir = crearCeldaDoble("1.9 Dirección del Establecimiento:", exp.getDireccionEstablecimiento() != null ? exp.getDireccionEstablecimiento() + ", Huamanga, Ayacucho" : "Huamanga");
        cDir.setColspan(4);
        t.addCell(cDir);

        document.add(t);
    }

    private void agregarSeccionII_MatrizFunciones(Document document, ExpedienteResponseDto exp) throws Exception {
        document.add(crearBarraSeccion("II. MATRIZ DE RIESGO POR FUNCIÓN DE LA EDIFICACIÓN (MANUAL CENEPRED)"));

        PdfPTable t = new PdfPTable(5);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{16f, 42f, 16f, 16f, 10f});
        t.setSpacingAfter(4f);

        // Encabezados de columna
        agregarEncabezadoColumna(t, "FUNCIÓN");
        agregarEncabezadoColumna(t, "DESCRIPCIÓN / TIPOLOGÍA DE ACTIVIDAD");
        agregarEncabezadoColumna(t, "CRITERIO DE CORTE");
        agregarEncabezadoColumna(t, "NIVEL RESULTANTE");
        agregarEncabezadoColumna(t, "APLICA");

        FuncionEdificacion funcionActiva = exp.getFuncionEdificacion() != null ? exp.getFuncionEdificacion() : FuncionEdificacion.COMERCIO;
        NivelRiesgo nivel = exp.getNivelRiesgo() != null ? exp.getNivelRiesgo() : NivelRiesgo.BAJO;

        agregarFilaFuncion(t, "1. SALUD", "Hospitales, clínicas, consultorios médicos o dentales, centros de salud, tópicos.", "Aforo > 20 pers. o > 2 niveles = ALTO", "ALTO / MUY ALTO", funcionActiva == FuncionEdificacion.SALUD, nivel);
        agregarFilaFuncion(t, "2. ENCUENTRO", "Restaurantes, cafeterías, bares, auditorios, cines, templos, discotecas, recreos.", "Área > 200 m² o aforo > 50 pers. = ALTO", "MEDIO / ALTO", funcionActiva == FuncionEdificacion.ENCUENTRO, nivel);
        agregarFilaFuncion(t, "3. HOSPEDAJE", "Hoteles, hostales, apart-hoteles, albergues turísticos.", "Hasta 3 pisos = MEDIO / > 3 pisos = ALTO", "MEDIO / ALTO", funcionActiva == FuncionEdificacion.HOSPEDAJE, nivel);
        agregarFilaFuncion(t, "4. EDUCACIÓN", "Colegios, nidos, academias, institutos, centros de capacitación.", "Hasta 2 pisos = MEDIO / > 2 pisos = ALTO", "MEDIO / ALTO", funcionActiva == FuncionEdificacion.EDUCACION, nivel);
        agregarFilaFuncion(t, "5. INDUSTRIAL", "Talleres artesanales, panaderías, carpinterías, manufactura ligera.", "Área > 200 m² = ALTO / > 500 m² = MUY ALTO", "MEDIO / ALTO", funcionActiva == FuncionEdificacion.INDUSTRIAL, nivel);
        agregarFilaFuncion(t, "6. OFICINAS", "Oficinas administrativas, consultorías, notarías, estudios contables.", "Hasta 4 pisos o < 500 m² = BAJO/MEDIO", "BAJO / MEDIO", funcionActiva == FuncionEdificacion.OFICINAS_ADMINISTRATIVAS, nivel);
        agregarFilaFuncion(t, "7. COMERCIO", "Bodegas, farmacias, librerías, bazares, tiendas de ropa, minimarkets.", "Área ≤ 100 m² = BAJO / ≤ 500 m² = MEDIO", "BAJO / MEDIO", funcionActiva == FuncionEdificacion.COMERCIO, nivel);
        agregarFilaFuncion(t, "8. ALMACÉN", "Depósitos de mercadería general no inflamable, almacenamiento de insumos.", "Área ≤ 100 m² = MEDIO / > 100 m² = ALTO", "MEDIO / ALTO", funcionActiva == FuncionEdificacion.ALMACEN, nivel);

        document.add(t);
    }

    private void agregarSeccionIII_FactoresAgravantes(Document document, ExpedienteResponseDto exp) throws Exception {
        document.add(crearBarraSeccion("III. FACTORES DE RIESGO AGRAVANTES / CRÍTICOS (ELEVACIÓN AUTOMÁTICA DE RIESGO)"));

        PdfPTable t = new PdfPTable(3);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{74f, 13f, 13f});
        t.setSpacingAfter(4f);

        boolean tieneGlp = false;
        boolean tieneCaldero = false;
        if (exp.getNivelRiesgo() == NivelRiesgo.ALTO || exp.getNivelRiesgo() == NivelRiesgo.MUY_ALTO) {
            String giro = exp.getGiroNegocio() != null ? exp.getGiroNegocio().toLowerCase() : "";
            tieneGlp = giro.contains("restaurante") || giro.contains("poller") || giro.contains("parrilla") || giro.contains("comida");
            tieneCaldero = giro.contains("industrial") || giro.contains("fabrica") || giro.contains("planta");
        }
        boolean sotano = exp.getAreaMetrosCuadrados() != null && exp.getAreaMetrosCuadrados().compareTo(new BigDecimal("500")) > 0;

        agregarFilaAgravante(t, "3.1 ¿Usa tanques fijos de Gas Licuado de Petróleo (GLP) > 0.45 m³ (118.8 gal) o cilindros > 45 kg?", tieneGlp);
        agregarFilaAgravante(t, "3.2 ¿Cuenta con calderas de vapor, recipientes a presión o fluidos térmicos de alta temperatura?", tieneCaldero);
        agregarFilaAgravante(t, "3.3 ¿Posee sótano destinado al acceso de público con área ocupada mayor a 100 m²?", sotano);
        agregarFilaAgravante(t, "3.4 ¿Almacena, comercializa o manipula materiales químicos altamente combustibles, pirotécnicos o tóxicos?", false);

        document.add(t);
    }

    private void agregarSeccionIV_CalificacionFinal(Document document, ExpedienteResponseDto exp) throws Exception {
        document.add(crearBarraSeccion("IV. DETERMINACIÓN TÉCNICA DEL NIVEL DE RIESGO Y PROCEDIMIENTO ITSE APLICABLE"));

        NivelRiesgo nivel = exp.getNivelRiesgo() != null ? exp.getNivelRiesgo() : NivelRiesgo.BAJO;

        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{25f, 25f, 25f, 25f});
        t.setSpacingAfter(4f);

        t.addCell(crearCeldaRiesgo("RIESGO BAJO", "Área ≤ 100 m²\nCarga de fuego baja", nivel == NivelRiesgo.BAJO, COLOR_RIESGO_BAJO));
        t.addCell(crearCeldaRiesgo("RIESGO MEDIO", "Área 101 a 500 m²\nHasta 2 niveles", nivel == NivelRiesgo.MEDIO, COLOR_RIESGO_MEDIO));
        t.addCell(crearCeldaRiesgo("RIESGO ALTO", "Área > 500 m²\nAforo > 200 pers.", nivel == NivelRiesgo.ALTO, COLOR_RIESGO_ALTO));
        t.addCell(crearCeldaRiesgo("RIESGO MUY ALTO", "Salud / Grifos / GLP\nIndustria pesada", nivel == NivelRiesgo.MUY_ALTO, COLOR_RIESGO_MUY_ALTO));

        document.add(t);

        // Cuadro resumen de modalidad
        PdfPTable tMod = new PdfPTable(2);
        tMod.setWidthPercentage(100);
        tMod.setWidths(new float[]{40f, 60f});
        tMod.setSpacingAfter(4f);

        boolean esPost = (nivel == NivelRiesgo.BAJO || nivel == NivelRiesgo.MEDIO);

        PdfPCell cM1 = new PdfPCell();
        cM1.setBorderColor(BORDER_COLOR);
        cM1.setPadding(4f);
        cM1.addElement(new Phrase("MODALIDAD DE PROCEDIMIENTO:", F_ITEM_BOLD));
        Paragraph pM1Val = new Paragraph(esPost ? "[ X ] ITSE POSTERIOR AL OTORGAMIENTO\n     (Riesgo Bajo o Medio - Ex Post)" : "[ X ] ITSE PREVIA AL OTORGAMIENTO\n     (Riesgo Alto o Muy Alto - Inspección en campo previa)", F_VAL);
        cM1.addElement(pM1Val);
        tMod.addCell(cM1);

        PdfPCell cM2 = new PdfPCell();
        cM2.setBorderColor(BORDER_COLOR);
        cM2.setPadding(4f);
        cM2.addElement(new Phrase("PLAZO MÁXIMO NORMATIVO:", F_ITEM_BOLD));
        cM2.addElement(new Phrase(esPost ? "Inspección posterior dentro de los 30 días hábiles posteriores a la emisión de la licencia." : "Plazo de evaluación de 7 a 9 días hábiles. Requiere Informe Técnico Favorable previo.", F_ITEM));
        tMod.addCell(cM2);

        document.add(tMod);
    }

    private void agregarCriteriosDetalladosFunciones(Document document) throws Exception {
        document.add(crearBarraSeccion("V. RESUMEN DE PARÁMETROS TÉCNICOS POR FUNCIÓN (ANEXO 2 CENEPRED)"));

        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{22f, 26f, 26f, 26f});
        t.setSpacingAfter(5f);

        agregarEncabezadoColumna(t, "FUNCIÓN");
        agregarEncabezadoColumna(t, "RIESGO BAJO");
        agregarEncabezadoColumna(t, "RIESGO MEDIO");
        agregarEncabezadoColumna(t, "RIESGO ALTO / MUY ALTO");

        agregarFilaMatrizRef(t, "Salud", "No aplica", "Consultorios individuales ≤ 30 m²", "Clínicas, postas, centros quirúrgicos, hospitales.");
        agregarFilaMatrizRef(t, "Encuentro", "No aplica", "Restaurantes y fuentes de soda ≤ 100 m²", "Restaurantes > 100 m², bares, peñas, discotecas.");
        agregarFilaMatrizRef(t, "Hospedaje", "No aplica", "Hostales hasta 3 pisos sin sótano", "Hoteles > 3 pisos, apart-hoteles con sótano.");
        agregarFilaMatrizRef(t, "Educación", "No aplica", "Centros hasta 2 pisos sin laboratorios", "Institutos, universidades, colegios integrales.");
        agregarFilaMatrizRef(t, "Industrial", "No aplica", "Talleres artesanales ≤ 100 m²", "Fábricas, molinos, fundiciones, carpinterías.");
        agregarFilaMatrizRef(t, "Oficinas", "Hasta 3 pisos y ≤ 250 m²", "4 a 6 pisos o hasta 500 m²", "Edificios corporativos > 6 pisos con ascensor.");
        agregarFilaMatrizRef(t, "Comercio", "Locales ≤ 100 m² (bodegas, bazares)", "Locales de 101 m² a 500 m²", "Supermercados, centros comerciales, galerías.");
        agregarFilaMatrizRef(t, "Almacén", "No aplica", "Depósitos no techados ≤ 100 m²", "Almacenes techados > 100 m² o con inflamables.");

        document.add(t);
    }

    private void agregarSeccionV_DictamenYSuscripcion(Document document, ExpedienteResponseDto exp) throws Exception {
        document.add(crearBarraSeccion("VI. DICTAMEN TÉCNICO Y CONFORMIDAD DEL ÓRGANO EJECUTANTE (DEFENSA CIVIL)"));

        PdfPTable tDictamen = new PdfPTable(1);
        tDictamen.setWidthPercentage(100);
        tDictamen.setSpacingAfter(6f);

        PdfPCell cD = new PdfPCell();
        cD.setBorderColor(BORDER_COLOR);
        cD.setPadding(4f);

        Paragraph pTextoDictamen = new Paragraph();
        pTextoDictamen.add(new Phrase("DICTAMEN TÉCNICO DE LA SUBGERENCIA DE DEFENSA CIVIL:\n", F_ITEM_BOLD));
        String numItse = exp.getNumeroInformeItse() != null ? exp.getNumeroInformeItse() : "ITSE-2026-DC-00000";
        pTextoDictamen.add(new Phrase("Habiéndose analizado la documentación técnica y las características físicas del establecimiento, se determina que el objeto de inspección califica en el Nivel de Riesgo " +
                (exp.getNivelRiesgo() != null ? exp.getNivelRiesgo().name() : "BAJO") + ", emitiéndose el Reporte de Clasificación N° " + numItse + " para los fines de la emisión de la Licencia de Funcionamiento de acuerdo a la Ley N° 28976.", F_ITEM));
        cD.addElement(pTextoDictamen);
        tDictamen.addCell(cD);
        document.add(tDictamen);

        // Bloque de Firmas: Administrado e Inspector Técnico ITSE
        PdfPTable tFirmas = new PdfPTable(2);
        tFirmas.setWidthPercentage(100);
        tFirmas.setWidths(new float[]{50f, 50f});
        tFirmas.setSpacingAfter(6f);

        // Firma Administrado
        PdfPCell cF1 = new PdfPCell();
        cF1.setBorderColor(BORDER_COLOR);
        cF1.setPadding(5f);
        cF1.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph pF1 = new Paragraph();
        pF1.add(new Phrase("\n\n\n___________________________________________________\n", F_ITEM));
        pF1.add(new Phrase("FIRMA DEL ADMINISTRADO O REPRESENTANTE LEGAL\n", F_ITEM_BOLD));
        pF1.add(new Phrase("DNI / RUC: " + (exp.getDocumentoIdentidad() != null ? exp.getDocumentoIdentidad() : "..................") + "\n", F_ITEM));
        pF1.add(new Phrase("Nombres: " + (exp.getNombreTitular() != null ? exp.getNombreTitular() : "........................................"), F_ITEM));
        pF1.setAlignment(Element.ALIGN_CENTER);
        cF1.addElement(pF1);
        tFirmas.addCell(cF1);

        // Firma Inspector Defensa Civil
        PdfPCell cF2 = new PdfPCell();
        cF2.setBorderColor(BORDER_COLOR);
        cF2.setPadding(5f);
        cF2.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph pF2 = new Paragraph();
        pF2.add(new Phrase("\n\n\n___________________________________________________\n", F_ITEM));
        pF2.add(new Phrase("INSPECTOR TÉCNICO ITSE (DEFENSA CIVIL)\n", F_ITEM_BOLD));
        pF2.add(new Phrase("Ing. Víctor Calderón Prado - CIP N° 124859\n", F_ITEM));
        pF2.add(new Phrase("Acreditación CENEPRED / RITSE N° 00482-2024\n", F_ITEM));
        pF2.add(new Phrase("Subgerencia de Defensa Civil - MPH", F_ITEM_BOLD));
        pF2.setAlignment(Element.ALIGN_CENTER);
        cF2.addElement(pF2);
        tFirmas.addCell(cF2);

        document.add(tFirmas);
    }

    private void agregarMarcoLegalYAdvertencias(Document document) throws Exception {
        document.add(crearBarraSeccion("VII. BASE NORMATIVA Y ADVERTENCIA DE FISCALIZACIÓN POSTERIOR"));

        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);

        PdfPCell c = new PdfPCell();
        c.setBorderColor(BORDER_COLOR);
        c.setPadding(4f);
        c.setBackgroundColor(new Color(254, 252, 232)); // Amarillo muy suave advertencia

        Paragraph pMarco = new Paragraph();
        pMarco.add(new Phrase("1. Base Legal: ", F_NORM_TITLE));
        pMarco.add(new Phrase("Ley Marco de Licencia de Funcionamiento N° 28976 y su TUO (D.S. N° 046-2017-PCM); Reglamento de ITSE (D.S. N° 002-2018-PCM); Manual de Ejecución ITSE (R.J. N° 016-2018-CENEPRED/J); TUO de la Ley del Procedimiento Administrativo General N° 27444.\n", F_NORM_BODY));

        pMarco.add(new Phrase("2. Fiscalización y Sanción: ", F_NORM_TITLE));
        pMarco.add(new Phrase("El administrado asume responsabilidad penal y administrativa por la veracidad de los datos consignados. De verificarse falsedad, se declarará la nulidad de la licencia sin perjuicio de la denuncia penal correspondiente (Art. 34 del TUO de la Ley N° 27444) y la clausura inmediata del establecimiento por la Subgerencia de Comercio, Licencias y Fiscalización.", F_NORM_BODY));

        c.addElement(pMarco);
        t.addCell(c);

        document.add(t);
    }

    // ==================== MÉTODOS DE SOPORTE ====================

    private Paragraph crearBarraSeccion(String texto) {
        PdfPTable bar = new PdfPTable(1);
        bar.setWidthPercentage(100);

        PdfPCell c = new PdfPCell(new Phrase(texto, F_SEC_TITLE));
        c.setBackgroundColor(COLOR_SEC_BAR);
        c.setPadding(2.5f);
        c.setBorder(PdfPCell.NO_BORDER);

        bar.addCell(c);

        Paragraph p = new Paragraph();
        p.add(bar);
        p.setSpacingBefore(3f);
        p.setSpacingAfter(2f);
        return p;
    }

    private void agregarEncabezadoColumna(PdfPTable t, String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto, F_COL_HEADER));
        c.setBackgroundColor(BG_HEADER);
        c.setBorderColor(BORDER_COLOR);
        c.setPadding(2.5f);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.addCell(c);
    }

    private void agregarFilaRecuadro(PdfPTable t, String label, String val) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, F_BOX_LABEL));
        c1.setBorder(PdfPCell.NO_BORDER);
        c1.setPadding(1f);
        t.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(val, F_BOX_VAL));
        c2.setBorder(PdfPCell.NO_BORDER);
        c2.setPadding(1f);
        t.addCell(c2);
    }

    private PdfPCell crearCeldaDoble(String etiqueta, String valor) {
        PdfPCell c = new PdfPCell();
        c.setBorderColor(BORDER_COLOR);
        c.setPadding(2f);
        c.addElement(new Phrase(etiqueta, F_LABEL));
        c.addElement(new Phrase(" " + (valor != null ? valor : "-"), F_VAL));
        return c;
    }

    private void agregarFilaFuncion(PdfPTable t, String funcion, String desc, String criterio, String nivelStr, boolean aplica, NivelRiesgo nivel) {
        PdfPCell cFun = new PdfPCell(new Phrase(funcion, F_ITEM_BOLD));
        cFun.setBorderColor(BORDER_COLOR);
        cFun.setPadding(2f);
        t.addCell(cFun);

        PdfPCell cDesc = new PdfPCell(new Phrase(desc, F_ITEM));
        cDesc.setBorderColor(BORDER_COLOR);
        cDesc.setPadding(2f);
        t.addCell(cDesc);

        PdfPCell cCrit = new PdfPCell(new Phrase(criterio, F_ITEM));
        cCrit.setBorderColor(BORDER_COLOR);
        cCrit.setPadding(2f);
        t.addCell(cCrit);

        PdfPCell cNiv = new PdfPCell(new Phrase(aplica ? nivel.name() : nivelStr, aplica ? F_ITEM_BOLD : F_ITEM));
        cNiv.setBorderColor(BORDER_COLOR);
        cNiv.setPadding(2f);
        cNiv.setHorizontalAlignment(Element.ALIGN_CENTER);
        if (aplica) {
            cNiv.setBackgroundColor(obtenerColorRiesgo(nivel));
        }
        t.addCell(cNiv);

        PdfPCell cApl = new PdfPCell(new Phrase(aplica ? "[ X ]" : "[   ]", F_ITEM_BOLD));
        cApl.setBorderColor(BORDER_COLOR);
        cApl.setPadding(2f);
        cApl.setHorizontalAlignment(Element.ALIGN_CENTER);
        if (aplica) {
            cApl.setBackgroundColor(new Color(254, 240, 138));
        }
        t.addCell(cApl);
    }

    private void agregarFilaAgravante(PdfPTable t, String texto, boolean cumple) {
        PdfPCell cTexto = new PdfPCell(new Phrase(texto, F_ITEM));
        cTexto.setBorderColor(BORDER_COLOR);
        cTexto.setPadding(2f);
        t.addCell(cTexto);

        PdfPCell cSi = new PdfPCell(new Phrase(cumple ? "[ X ] SÍ" : "[   ] SÍ", cumple ? F_ITEM_BOLD : F_ITEM));
        cSi.setBorderColor(BORDER_COLOR);
        cSi.setPadding(2f);
        cSi.setHorizontalAlignment(Element.ALIGN_CENTER);
        if (cumple) {
            cSi.setBackgroundColor(new Color(254, 202, 202)); // Rojo suave
        }
        t.addCell(cSi);

        PdfPCell cNo = new PdfPCell(new Phrase(!cumple ? "[ X ] NO" : "[   ] NO", !cumple ? F_ITEM_BOLD : F_ITEM));
        cNo.setBorderColor(BORDER_COLOR);
        cNo.setPadding(2f);
        cNo.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.addCell(cNo);
    }

    private PdfPCell crearCeldaRiesgo(String titulo, String sub, boolean seleccionado, Color colorBase) {
        PdfPCell c = new PdfPCell();
        c.setBorderColor(seleccionado ? Color.BLACK : BORDER_COLOR);
        c.setBorderWidth(seleccionado ? 1.5f : 0.5f);
        c.setPadding(3f);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);

        if (seleccionado) {
            c.setBackgroundColor(colorBase);
        } else {
            c.setBackgroundColor(new Color(250, 250, 250));
        }

        Paragraph p = new Paragraph();
        p.add(new Phrase((seleccionado ? "★ [ X ] " : "[   ] ") + titulo + "\n", seleccionado ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7f, Color.BLACK) : FontFactory.getFont(FontFactory.HELVETICA, 6.5f, Color.DARK_GRAY)));
        p.add(new Phrase(sub, FontFactory.getFont(FontFactory.HELVETICA, 5.2f, Color.BLACK)));
        p.setAlignment(Element.ALIGN_CENTER);
        c.addElement(p);

        return c;
    }

    private void agregarFilaMatrizRef(PdfPTable t, String f, String rb, String rm, String ra) {
        PdfPCell c1 = new PdfPCell(new Phrase(f, F_ITEM_BOLD));
        c1.setBorderColor(BORDER_COLOR);
        c1.setPadding(2f);
        t.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(rb, F_ITEM));
        c2.setBorderColor(BORDER_COLOR);
        c2.setPadding(2f);
        t.addCell(c2);

        PdfPCell c3 = new PdfPCell(new Phrase(rm, F_ITEM));
        c3.setBorderColor(BORDER_COLOR);
        c3.setPadding(2f);
        t.addCell(c3);

        PdfPCell c4 = new PdfPCell(new Phrase(ra, F_ITEM));
        c4.setBorderColor(BORDER_COLOR);
        c4.setPadding(2f);
        t.addCell(c4);
    }

    private Color obtenerColorRiesgo(NivelRiesgo nivel) {
        if (nivel == null) return COLOR_RIESGO_BAJO;
        return switch (nivel) {
            case BAJO -> COLOR_RIESGO_BAJO;
            case MEDIO -> COLOR_RIESGO_MEDIO;
            case ALTO -> COLOR_RIESGO_ALTO;
            case MUY_ALTO -> COLOR_RIESGO_MUY_ALTO;
        };
    }

    private int calcularAforoEstimado(ExpedienteResponseDto exp) {
        if (exp.getAforoPersonas() != null && exp.getAforoPersonas() > 0) {
            return exp.getAforoPersonas();
        }
        if (exp.getAreaMetrosCuadrados() != null) {
            // Factor promedio normativo RNE: 2.8 m² por persona para comercio general
            return Math.max(1, (int) Math.round(exp.getAreaMetrosCuadrados().doubleValue() / 2.8));
        }
        return 5;
    }
}
