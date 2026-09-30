package pe.gob.munihuamanga.licencias.expedientes.service;

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
import pe.gob.munihuamanga.licencias.common.dto.Anexo4CondicionesDto;
import pe.gob.munihuamanga.licencias.common.dto.ExpedienteResponseDto;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generador PDF Oficial del Anexo N° 4:
 * DECLARACIÓN JURADA DE CUMPLIMIENTO DE CONDICIONES DE SEGURIDAD EN LA EDIFICACIÓN
 * Conforme a la Ley Marco de Licencias de Funcionamiento N° 28976, su TUO aprobado por D.S. N° 046-2017-PCM,
 * y el Reglamento de ITSE aprobado por D.S. N° 002-2018-PCM.
 * Genera exactamente cuatro (4) páginas completas idénticas al formato estándar oficial de Defensa Civil.
 */
@Slf4j
@Component
public class Anexo4PdfGenerator {

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
    private static final Font F_DECL_BODY = FontFactory.getFont(FontFactory.HELVETICA, 5.5f, new Color(30, 30, 30));

    private static final Color COLOR_SEC_BAR = new Color(0, 51, 102);
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
            agregarSeccionII_Dimensionamiento(document, exp);
            agregarSeccionIII_CondicionesBasicas(document, exp);

            Paragraph pFoot1 = new Paragraph("* Formato oficial para establecimientos de Riesgo Bajo o Medio sujetos a ITSE posterior (Ley 28976).", F_FOOT_NOTE);
            pFoot1.setSpacingBefore(3f);
            document.add(pFoot1);

            // ==================== PÁGINA 2 ====================
            document.newPage();
            agregarEncabezado(document, exp, 2);
            agregarSeccionIV_SeguridadEstructuralYNoEstructural(document, exp);

            Paragraph pFoot2 = new Paragraph("* Eje I: Todo componente físico o arquitectónico debe brindar garantías ante movimientos sísmicos y evacuación.", F_FOOT_NOTE);
            pFoot2.setSpacingBefore(3f);
            document.add(pFoot2);

            // ==================== PÁGINA 3 ====================
            document.newPage();
            agregarEncabezado(document, exp, 3);
            agregarSeccionV_SeguridadProteccionIncendios(document, exp);

            Paragraph pFoot3 = new Paragraph("* Eje II: Los extintores y luces de emergencia deben contar con mantenimiento vigente y operatividad comprobada.", F_FOOT_NOTE);
            pFoot3.setSpacingBefore(3f);
            document.add(pFoot3);

            // ==================== PÁGINA 4 ====================
            document.newPage();
            agregarEncabezado(document, exp, 4);
            agregarSeccionVI_SeguridadInstalacionesElectricas(document, exp);
            agregarSeccionVII_DeclaracionJuradaFinal(document, exp);
            agregarBloqueFirmasYRecepcion(document, exp);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error al generar PDF del Anexo 4 oficial", e);
            throw new RuntimeException("Error al generar PDF del Anexo 4", e);
        }
    }

    private void agregarEncabezado(Document document, ExpedienteResponseDto exp, int pagina) throws Exception {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{72f, 28f});
        headerTable.setSpacingAfter(4f);

        PdfPCell cLeft = new PdfPCell();
        cLeft.setBorderColor(BORDER_COLOR);
        cLeft.setPadding(4f);

        Paragraph pMuni = new Paragraph("MUNICIPALIDAD PROVINCIAL DE HUAMANGA", F_TITLE_INST);
        pMuni.setAlignment(Element.ALIGN_CENTER);
        cLeft.addElement(pMuni);

        Paragraph pGer = new Paragraph("SUBGERENCIA DE COMERCIO, LICENCIAS Y FISCALIZACIÓN / DEFENSA CIVIL", F_TITLE_SUB);
        pGer.setAlignment(Element.ALIGN_CENTER);
        cLeft.addElement(pGer);

        Paragraph pTitulo = new Paragraph("ANEXO 4 — DECLARACIÓN JURADA DE CUMPLIMIENTO DE CONDICIONES DE SEGURIDAD", F_TITLE_MAIN);
        pTitulo.setAlignment(Element.ALIGN_CENTER);
        cLeft.addElement(pTitulo);

        Paragraph pLey = new Paragraph("Para establecimientos objeto de ITSE con nivel de Riesgo BAJO o MEDIO (D.S. N° 002-2018-PCM)", F_TITLE_LAW);
        pLey.setAlignment(Element.ALIGN_CENTER);
        cLeft.addElement(pLey);

        headerTable.addCell(cLeft);

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
        agregarFilaRecuadro(tRec, "FECHA:", fecha);
        agregarFilaRecuadro(tRec, "HORA:", hora);
        agregarFilaRecuadro(tRec, "PÁGINA:", pagina + " de 4");

        cRight.addElement(tRec);
        headerTable.addCell(cRight);

        document.add(headerTable);
    }

    private void agregarSeccionI_DatosGenerales(Document document, ExpedienteResponseDto exp) throws Exception {
        document.add(crearBarraSeccion("I. DATOS DEL ADMINISTRADO Y DEL ESTABLECIMIENTO OBJETO DE INSPECCIÓN"));

        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{24f, 32f, 20f, 24f});
        t.setSpacingAfter(4f);

        t.addCell(crearCeldaDoble("1.1 Administrado / Titular:", exp.getNombreTitular()));
        t.addCell(crearCeldaDoble("1.2 Documento de Identidad:", exp.getDocumentoIdentidad()));
        t.addCell(crearCeldaDoble("1.3 Razón Social:", exp.getRazonSocial() != null ? exp.getRazonSocial() : "(Persona Natural)"));
        t.addCell(crearCeldaDoble("1.4 Teléfono / Celular:", exp.getTelefono() != null ? exp.getTelefono() : "-"));

        t.addCell(crearCeldaDoble("1.5 Nombre Comercial:", exp.getNombreComercial()));
        t.addCell(crearCeldaDoble("1.6 Giro(s) de Negocio:", exp.getGiroNegocio()));
        t.addCell(crearCeldaDoble("1.7 RIESGO ITSE:", exp.getNivelRiesgo() != null ? exp.getNivelRiesgo().name() : "BAJO"));
        t.addCell(crearCeldaDoble("1.8 MODALIDAD:", "ITSE POSTERIOR"));

        PdfPCell cDir = crearCeldaDoble("1.9 Dirección del Establecimiento:", exp.getDireccionEstablecimiento() != null ? exp.getDireccionEstablecimiento() + ", Huamanga, Ayacucho" : "Huamanga");
        cDir.setColspan(4);
        t.addCell(cDir);

        document.add(t);
    }

    private void agregarSeccionII_Dimensionamiento(Document document, ExpedienteResponseDto exp) throws Exception {
        document.add(crearBarraSeccion("II. DIMENSIONAMIENTO Y AFORO TOTAL POR NIVELES O PISOS"));

        Anexo4CondicionesDto c = exp.getAnexo4Condiciones();
        BigDecimal areaTotal = exp.getAreaMetrosCuadrados() != null ? exp.getAreaMetrosCuadrados() : new BigDecimal("45.00");
        BigDecimal areaP1 = (c != null && c.getAreaPiso1() != null) ? c.getAreaPiso1() : areaTotal;
        BigDecimal areaP2 = (c != null && c.getAreaPiso2() != null) ? c.getAreaPiso2() : BigDecimal.ZERO;
        BigDecimal areaP3 = (c != null && c.getAreaPiso3() != null) ? c.getAreaPiso3() : BigDecimal.ZERO;
        BigDecimal areaP4 = (c != null && c.getAreaPiso4() != null) ? c.getAreaPiso4() : BigDecimal.ZERO;
        BigDecimal areaOtros = (c != null && c.getAreaOtrosPisos() != null) ? c.getAreaOtrosPisos() : BigDecimal.ZERO;

        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{25f, 25f, 25f, 25f});
        t.setSpacingAfter(4f);

        t.addCell(crearCeldaDoble("Área Piso 1:", areaP1.toString() + " m²"));
        t.addCell(crearCeldaDoble("Área Piso 2:", areaP2.compareTo(BigDecimal.ZERO) > 0 ? areaP2.toString() + " m²" : "0.00 m²"));
        t.addCell(crearCeldaDoble("Área Piso 3:", areaP3.compareTo(BigDecimal.ZERO) > 0 ? areaP3.toString() + " m²" : "0.00 m²"));
        t.addCell(crearCeldaDoble("Área Piso 4 / Azotea:", areaP4.add(areaOtros).compareTo(BigDecimal.ZERO) > 0 ? areaP4.add(areaOtros).toString() + " m²" : "0.00 m²"));

        int aforo = (exp.getAforoPersonas() != null && exp.getAforoPersonas() > 0) ? exp.getAforoPersonas() : Math.max(1, (int) Math.round(areaTotal.doubleValue() / 2.8));
        int antigEdif = (c != null && c.getAntiguedadEdificacionAnios() != null) ? c.getAntiguedadEdificacionAnios() : 10;
        int antigGiro = (c != null && c.getAntiguedadGiroAnios() != null) ? c.getAntiguedadGiroAnios() : 2;

        t.addCell(crearCeldaDoble("Área Techada Total:", areaTotal.toString() + " m²"));
        t.addCell(crearCeldaDoble("Área Ocupada Total:", areaTotal.toString() + " m²"));
        t.addCell(crearCeldaDoble("Aforo Total Declarado:", aforo + " personas"));
        t.addCell(crearCeldaDoble("Antigüedad Edif. / Giro:", antigEdif + " años / " + antigGiro + " años"));

        document.add(t);
    }

    private void agregarSeccionIII_CondicionesBasicas(Document document, ExpedienteResponseDto exp) throws Exception {
        document.add(crearBarraSeccion("III. CONDICIONES BÁSICAS DE LA EDIFICACIÓN OBJETO DE INSPECCIÓN"));

        PdfPTable t = new PdfPTable(3);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{76f, 12f, 12f});
        t.setSpacingAfter(4f);

        agregarEncabezadoColumna(t, "REQUISITO BÁSICO DE PROCEDENCIA DE LA ITSE");
        agregarEncabezadoColumna(t, "CUMPLE");
        agregarEncabezadoColumna(t, "NO CUMPLE");

        agregarFilaCheckNormativo(t, "3.1 El establecimiento NO se encuentra en proceso de construcción, remodelación o demolición.", true);
        agregarFilaCheckNormativo(t, "3.2 El establecimiento cuenta con servicios básicos operativos de agua potable y suministro de energía eléctrica.", true);
        agregarFilaCheckNormativo(t, "3.3 El establecimiento cuenta con el mobiliario comercial y equipos necesarios instalados para su funcionamiento.", true);
        agregarFilaCheckNormativo(t, "3.4 Desarrolla exclusivamente las actividades declaradas sin obstaculizar la vía pública ni el retiro municipal.", true);

        document.add(t);
    }

    private void agregarSeccionIV_SeguridadEstructuralYNoEstructural(Document document, ExpedienteResponseDto exp) throws Exception {
        document.add(crearBarraSeccion("IV. EJE I: SEGURIDAD ESTRUCTURAL, NO ESTRUCTURAL Y RUTAS DE EVACUACIÓN"));

        PdfPTable t = new PdfPTable(3);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{76f, 12f, 12f});
        t.setSpacingAfter(4f);

        agregarEncabezadoColumna(t, "CRITERIO TÉCNICO DE SEGURIDAD FÍSICA Y EVACUACIÓN");
        agregarEncabezadoColumna(t, "CUMPLE");
        agregarEncabezadoColumna(t, "NO APLICA");

        agregarFilaCheckNormativo(t, "4.1 Los elementos estructurales (vigas, columnas, muros portantes y techos) no presentan rajaduras, fisuras críticas, deformaciones ni asentamientos que pongan en peligro la estabilidad del local.", true);
        agregarFilaCheckNormativo(t, "4.2 Los falsos techos, cielo rasos, luminarias fluorescentes, artefactos colgantes y ventiladores se encuentran firmemente anclados y asegurados para prevenir caídas ante sismos.", true);
        agregarFilaCheckNormativo(t, "4.3 Los vidrios crudos instalados en áreas de tránsito de personas, puertas o vitrinas cuentan con láminas de seguridad adhesivas transparentes o son de vidrio templado/laminado reglamentario.", true);
        agregarFilaCheckNormativo(t, "4.4 Los pasadizos y vías de circulación interna mantienen un ancho libre mínimo reglamentario (mínimo 0.90 m a 1.20 m según aforo) encontrándose permanentemente libres de obstáculos, mercadería o cajas.", true);
        agregarFilaCheckNormativo(t, "4.5 Las puertas de ingreso y de escape abren fácilmente hacia el exterior o no obstruyen la evacuación continua; durante las horas de atención comercial no se encuentran con llave o candado.", true);
        agregarFilaCheckNormativo(t, "4.6 Las escaleras cuentan con pasos y contrapasos uniformes, superficies antideslizantes y pasamanos continuos firmemente fijados en al menos uno de sus lados.", true);
        agregarFilaCheckNormativo(t, "4.7 El espacio ubicado debajo o sobre las escaleras de evacuación se encuentra totalmente libre de almacenamiento de materiales combustibles, bultos o archivos.", true);
        agregarFilaCheckNormativo(t, "4.8 Los pisos se encuentran nivelados, firmes y sin desniveles pronunciados no señalizados que puedan ocasionar tropiezos o caídas de los usuarios y trabajadores.", true);

        document.add(t);
    }

    private void agregarSeccionV_SeguridadProteccionIncendios(Document document, ExpedienteResponseDto exp) throws Exception {
        document.add(crearBarraSeccion("V. EJE II: SEGURIDAD DE PROTECCIÓN CONTRA INCENDIOS Y EMERGENCIAS"));

        PdfPTable t = new PdfPTable(3);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{76f, 12f, 12f});
        t.setSpacingAfter(4f);

        agregarEncabezadoColumna(t, "CRITERIO TÉCNICO DE PROTECCIÓN CONTRA INCENDIOS");
        agregarEncabezadoColumna(t, "CUMPLE");
        agregarEncabezadoColumna(t, "NO APLICA");

        agregarFilaCheckNormativo(t, "5.1 Cuenta con extintor(es) portátil(es) operativo(s) del agente extintor adecuado (Polvo Químico Seco PQS tipo ABC de 6 o 9 kg, o Gas Carbónico CO2 para equipos electrónicos).", true);
        agregarFilaCheckNormativo(t, "5.2 El extintor cuenta con tarjeta de control y mantenimiento vigente emitida por empresa de recarga autorizada, con vigencia no mayor a un (1) año y manómetro en zona verde de presión adecuada.", true);
        agregarFilaCheckNormativo(t, "5.3 El extintor se encuentra suspendido en la pared o soporte a una altura no mayor a 1.50 m del piso terminado, con numeración visible y señal de extintor normalizada.", true);
        agregarFilaCheckNormativo(t, "5.4 El acceso al extintor se encuentra 100% despejado y libre de cualquier mercadería, vitrinas o bultos en un radio mínimo libre de 1.00 metro.", true);
        agregarFilaCheckNormativo(t, "5.5 Cuenta con luces de emergencia autónomas (LED o halógenas) operativas, permanentemente enchufadas a tomacorriente y ubicadas en las rutas de escape y puertas de salida.", true);
        agregarFilaCheckNormativo(t, "5.6 Cuenta con señalización fotoluminiscente de seguridad según la Norma Técnica Peruana NTP 399.010-1 (Salida, Flechas de escape, Zona Segura en Sismos, Riesgo Eléctrico).", true);
        agregarFilaCheckNormativo(t, "5.7 El establecimiento se encuentra totalmente libre de almacenamiento de combustibles líquidos, pinturas en grandes cantidades, pirotécnicos o productos químicos sin autorización expresa.", true);
        agregarFilaCheckNormativo(t, "5.8 En caso de actividades de preparación de alimentos: la campana extractora y ductos de extracción se encuentran limpios y libres de grasa acumulada, con filtro metálico antigrasa.", true);

        document.add(t);
    }

    private void agregarSeccionVI_SeguridadInstalacionesElectricas(Document document, ExpedienteResponseDto exp) throws Exception {
        document.add(crearBarraSeccion("VI. EJE III: SEGURIDAD EN INSTALACIONES ELÉCTRICAS"));

        PdfPTable t = new PdfPTable(3);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{76f, 12f, 12f});
        t.setSpacingAfter(4f);

        agregarEncabezadoColumna(t, "CRITERIO TÉCNICO DE INSTALACIONES ELÉCTRICAS SEGURAS");
        agregarEncabezadoColumna(t, "CUMPLE");
        agregarEncabezadoColumna(t, "NO APLICA");

        agregarFilaCheckNormativo(t, "6.1 El tablero eléctrico general o de distribución es de material incombustible (resina termoplástica o metal), contando con tapa, mandil protector contra contactos directos y chapa o seguro.", true);
        agregarFilaCheckNormativo(t, "6.2 Todos los circuitos cuentan con interruptores termomagnéticos debidamente rotulados en el mandil (Alumbrado, Tomacorrientes, Aire Acondicionado, Fuerza, etc.).", true);
        agregarFilaCheckNormativo(t, "6.3 Cuenta con interruptor(es) diferencial(es) de alta sensibilidad (30 mA) para protección de personas contra electrocución en todos los circuitos de tomacorrientes.", true);
        agregarFilaCheckNormativo(t, "6.4 Cuenta con sistema de puesta a tierra operativo con certificado de medición y protocolo vigente firmado por ingeniero electricista o mecánico-electricista colegiado (resistencia < 25 ohms).", true);
        agregarFilaCheckNormativo(t, "6.5 El cableado eléctrico se encuentra completamente protegido, embutido en tubería PVC o canaletas autoextinguibles; terminantemente prohibido el uso de cables mellizos expuestos.", true);
        agregarFilaCheckNormativo(t, "6.6 Los tomacorrientes, interruptores y placas se encuentran en perfecto estado mecánico, sin fisuras ni recalentamientos, y cuentan con espiga de puesta a tierra.", true);

        document.add(t);
    }

    private void agregarSeccionVII_DeclaracionJuradaFinal(Document document, ExpedienteResponseDto exp) throws Exception {
        document.add(crearBarraSeccion("VII. DECLARACIÓN JURADA DE VERACIDAD Y RESPONSABILIDAD LEGAL"));

        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingAfter(4f);

        PdfPCell c = new PdfPCell();
        c.setBorderColor(BORDER_COLOR);
        c.setPadding(4f);
        c.setBackgroundColor(new Color(254, 252, 232));

        Paragraph p = new Paragraph();
        p.add(new Phrase("DECLARO BAJO JURAMENTO QUE:\n", F_ITEM_BOLD));
        p.add(new Phrase("1. La edificación objeto de inspección cumple a cabalidad con todas y cada una de las condiciones de seguridad en edificación verificadas en el presente documento (Eje I, Eje II y Eje III) de conformidad con el D.S. N° 002-2018-PCM y el D.S. N° 046-2017-PCM.\n", F_DECL_BODY));
        p.add(new Phrase("2. Me comprometo a mantener permanentemente operativas dichas condiciones durante todo el ejercicio de la actividad económica autorizada.\n", F_DECL_BODY));
        p.add(new Phrase("3. Asumo plena responsabilidad legal, administrativa y penal (Art. 411 del Código Penal por Falsa Declaración en Procedimiento Administrativo) en caso se constate la falsedad o alteración de la información consignada, autorizando a la Municipalidad Provincial de Huamanga a revocar la licencia y ordenar la clausura definitiva inmediata del local.", F_DECL_BODY));

        c.addElement(p);
        t.addCell(c);

        document.add(t);
    }

    private void agregarBloqueFirmasYRecepcion(Document document, ExpedienteResponseDto exp) throws Exception {
        PdfPTable t = new PdfPTable(3);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{45f, 20f, 35f});
        t.setSpacingBefore(4f);

        // Firma Administrado
        PdfPCell cFirma = new PdfPCell();
        cFirma.setBorderColor(BORDER_COLOR);
        cFirma.setPadding(4f);
        cFirma.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph pF = new Paragraph();
        pF.add(new Phrase("\n\n\n_________________________________________________\n", F_ITEM));
        pF.add(new Phrase("FIRMA DEL ADMINISTRADO / TITULAR\n", F_ITEM_BOLD));
        pF.add(new Phrase("DNI / RUC: " + (exp.getDocumentoIdentidad() != null ? exp.getDocumentoIdentidad() : "..................") + "\n", F_ITEM));
        pF.add(new Phrase("Nombres: " + (exp.getNombreTitular() != null ? exp.getNombreTitular() : "........................................"), F_ITEM));
        pF.setAlignment(Element.ALIGN_CENTER);
        cFirma.addElement(pF);
        t.addCell(cFirma);

        // Huella Digital
        PdfPCell cHuella = new PdfPCell();
        cHuella.setBorderColor(BORDER_COLOR);
        cHuella.setPadding(4f);
        cHuella.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph pH = new Paragraph();
        pH.add(new Phrase("\n\n\n[ HUELLA DACTILAR ]\n\nÍndice Derecho", F_FOOT_NOTE));
        pH.setAlignment(Element.ALIGN_CENTER);
        cHuella.addElement(pH);
        t.addCell(cHuella);

        // Sello y Firma Municipal
        PdfPCell cMuni = new PdfPCell();
        cMuni.setBorderColor(BORDER_COLOR);
        cMuni.setPadding(4f);
        cMuni.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph pM = new Paragraph();
        pM.add(new Phrase("\nRECEPCIÓN MUNICIPAL\nMESA DE PARTES / DEFENSA CIVIL\n", F_ITEM_BOLD));
        String fecha = exp.getFechaCreacion() != null ? exp.getFechaCreacion().format(DATE_FMT) : LocalDate.now().format(DATE_FMT);
        pM.add(new Phrase("Fecha de Recepción: " + fecha + "\n", F_ITEM));
        pM.add(new Phrase("Expediente: " + (exp.getNumeroTramite() != null ? exp.getNumeroTramite() : "-") + "\n", F_ITEM));
        pM.add(new Phrase("Municipalidad Provincial de Huamanga", F_TITLE_INST));
        pM.setAlignment(Element.ALIGN_CENTER);
        cMuni.addElement(pM);
        t.addCell(cMuni);

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

    private void agregarFilaCheckNormativo(PdfPTable t, String descripcion, boolean cumple) {
        PdfPCell cDesc = new PdfPCell(new Phrase(descripcion, F_ITEM));
        cDesc.setBorderColor(BORDER_COLOR);
        cDesc.setPadding(2f);
        t.addCell(cDesc);

        PdfPCell cCumple = new PdfPCell(new Phrase(cumple ? "[ X ] SÍ" : "[   ] SÍ", cumple ? F_ITEM_BOLD : F_ITEM));
        cCumple.setBorderColor(BORDER_COLOR);
        cCumple.setPadding(2f);
        cCumple.setHorizontalAlignment(Element.ALIGN_CENTER);
        if (cumple) {
            cCumple.setBackgroundColor(new Color(220, 252, 231)); // Verde suave
        }
        t.addCell(cCumple);

        PdfPCell cNoAplica = new PdfPCell(new Phrase(!cumple ? "[ X ] N/A" : "[   ] N/A", !cumple ? F_ITEM_BOLD : F_ITEM));
        cNoAplica.setBorderColor(BORDER_COLOR);
        cNoAplica.setPadding(2f);
        cNoAplica.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.addCell(cNoAplica);
    }
}
