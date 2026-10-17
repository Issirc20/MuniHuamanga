package pe.gob.munihuamanga.licencias.expedientes.service;

import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;

public class PdfInspectionTool {

    private static final String DIR = "D:/01.ING SISTEMAS - UNSCH/ASIGNATURAS 2026-II (IS)/01 - Servicio Social Universitario (IS-480)/Info MuniHuamanga/";

    @Test
    void inspectPdfs() throws Exception {
        String[] files = {
            "Anexo-1-Formato-de-Solicitud-ITSE-ECSE.pdf",
            "Anexo-2-Informacion-proporcionada-por-el-solicitante (1).pdf",
            "Anexo-3-Reporte-del-Nivel-de-Riesgo (1).pdf",
            "Anexo-4-Declaracion-Jurada-ITSE-Posterior (1).pdf",
            "dj_licencia_de_funcionamiento-v2.pdf"
        };

        java.io.File outFile = new java.io.File("d:/ArqSoftware/MuniHuamanga/scratch/extracted_pdfs_text.txt");
        outFile.getParentFile().mkdirs();
        java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(outFile, java.nio.charset.StandardCharsets.UTF_8));

        for (String fName : files) {
            File f = new File(DIR, fName);
            pw.println("======================================================================");
            pw.println("FILE: " + fName + " (exists: " + f.exists() + ", size: " + f.length() + ")");
            if (!f.exists()) continue;

            PdfReader reader = new PdfReader(f.getAbsolutePath());
            pw.println("Number of pages: " + reader.getNumberOfPages());

            AcroFields fields = reader.getAcroFields();
            if (fields != null && fields.getAllFields() != null) {
                pw.println("AcroForm fields count: " + fields.getAllFields().size());
                for (Map.Entry<String, AcroFields.Item> entry : fields.getAllFields().entrySet()) {
                    pw.println("  Field: " + entry.getKey() + " = " + fields.getField(entry.getKey()));
                }
            }

            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            for (int p = 1; p <= reader.getNumberOfPages(); p++) {
                pw.println("--- PAGE " + p + " ---");
                String pageText = extractor.getTextFromPage(p);
                pw.println(pageText);
            }
            reader.close();
        }
        pw.close();
    }
}
