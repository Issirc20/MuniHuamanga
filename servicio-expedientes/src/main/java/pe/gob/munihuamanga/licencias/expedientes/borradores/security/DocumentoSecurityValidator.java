package pe.gob.munihuamanga.licencias.expedientes.borradores.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;

/**
 * Validador de Seguridad Documental y Prevención de Amenazas (OWASP A08:2021).
 * Verifica extensiones permitidas, número mágico MIME real, tamaño máximo,
 * sanitiza rutas contra Path Traversal y audita verificación antimalware e integridad SHA-256.
 */
@Slf4j
@Component
public class DocumentoSecurityValidator {

    public static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB
    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of(".pdf", ".jpg", ".jpeg", ".png");

    // Magic Numbers
    private static final byte[] MAGIC_PDF = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] MAGIC_PNG = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] MAGIC_JPEG_PREFIX = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    /**
     * Valida exhaustivamente el archivo antes de su almacenamiento.
     */
    public String validarYSanitizar(String nombreArchivoOriginal, String tipoContenido, byte[] contenido) {
        if (nombreArchivoOriginal == null || nombreArchivoOriginal.isBlank()) {
            throw new IllegalArgumentException("El nombre del archivo no puede ser nulo ni vacío.");
        }

        if (contenido == null || contenido.length == 0) {
            throw new IllegalArgumentException("El archivo adjunto está vacío (0 bytes).");
        }

        if (contenido.length > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    String.format("El archivo excede el tamaño máximo permitido de 10 MB (Tamaño actual: %.2f MB).",
                            contenido.length / (1024.0 * 1024.0))
            );
        }

        // 1. Sanitizar nombre contra Path Traversal
        String nombreLimpio = sanitizarNombreArchivo(nombreArchivoOriginal);

        // 2. Validar extensión permitida
        String extension = extraerExtension(nombreLimpio).toLowerCase();
        if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
            log.warn("[SEGURIDAD-DOC] Extensión no permitida rechazada: '{}'", extension);
            throw new IllegalArgumentException(
                    "Extensión de archivo no permitida. Solo se admiten documentos en formato PDF (.pdf) o imágenes (.jpg, .jpeg, .png)."
            );
        }

        // 3. Validar MIME real mediante inspección de Magic Numbers
        validarNumeroMagico(extension, contenido);

        // 4. Mecanismo de análisis antimalware (registro de estado real, sin simulación engañosa)
        registrarAuditoriaAntimalware(nombreLimpio, contenido.length);

        return nombreLimpio;
    }

    private void registrarAuditoriaAntimalware(String nombreLimpio, int tamanio) {
        log.info("[ANTIMALWARE] Archivo '{}' ({} bytes) recibido. Estado de escaneo: ESCANEO_DELEGADO_PENDIENTE_CONFIGURACION_CLAMAV. No certificado como libre de malware en esta fase.", nombreLimpio, tamanio);
    }

    /**
     * Sanitiza el nombre de archivo eliminando secuencias de path traversal y caracteres peligrosos.
     */
    public String sanitizarNombreArchivo(String nombreOriginal) {
        String baseName = new File(nombreOriginal).getName();
        // Eliminar secuencias ../ o ..\ residuales
        baseName = baseName.replace("..", "").replace("/", "").replace("\\", "");
        // Reemplazar caracteres no alfanuméricos salvo guiones y puntos
        baseName = baseName.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (baseName.isBlank() || baseName.equals(".")) {
            baseName = "documento_adjunto.pdf";
        }
        return baseName;
    }

    /**
     * Verifica la correspondencia estricta entre la extensión declarada y la firma binaria real.
     */
    private void validarNumeroMagico(String extension, byte[] contenido) {
        switch (extension) {
            case ".pdf" -> {
                if (!coincidePrefijo(contenido, MAGIC_PDF)) {
                    log.warn("[SEGURIDAD-DOC] Archivo con extensión .pdf no contiene firma binaria legítima de PDF (%PDF-).");
                    throw new IllegalArgumentException("El contenido del archivo no corresponde a un documento PDF legítimo.");
                }
            }
            case ".png" -> {
                if (!coincidePrefijo(contenido, MAGIC_PNG)) {
                    log.warn("[SEGURIDAD-DOC] Archivo con extensión .png no contiene cabecera binaria PNG válida.");
                    throw new IllegalArgumentException("El contenido del archivo no corresponde a una imagen PNG legítima.");
                }
            }
            case ".jpg", ".jpeg" -> {
                if (!coincidePrefijo(contenido, MAGIC_JPEG_PREFIX)) {
                    log.warn("[SEGURIDAD-DOC] Archivo con extensión JPEG no contiene cabecera binaria JPEG válida (0xFFD8FF).");
                    throw new IllegalArgumentException("El contenido del archivo no corresponde a una imagen JPEG legítima.");
                }
            }
            default -> throw new IllegalArgumentException("Tipo de formato no soportado: " + extension);
        }
    }

    private boolean coincidePrefijo(byte[] datos, byte[] prefijoEsperado) {
        if (datos.length < prefijoEsperado.length) return false;
        for (int i = 0; i < prefijoEsperado.length; i++) {
            if (datos[i] != prefijoEsperado[i]) return false;
        }
        return true;
    }

    private String extraerExtension(String nombreArchivo) {
        int ultimoPunto = nombreArchivo.lastIndexOf('.');
        if (ultimoPunto == -1 || ultimoPunto == nombreArchivo.length() - 1) {
            return "";
        }
        return nombreArchivo.substring(ultimoPunto);
    }

    /**
     * Comprueba en tiempo constante si el hash calculado coincide con el registrado en base de datos.
     */
    public void verificarIntegridadSha256(byte[] contenido, String hashEsperado) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashCalculadoBytes = digest.digest(contenido);
            String hashCalculadoHex = HexFormat.of().formatHex(hashCalculadoBytes);

            byte[] esperadoBytes = HexFormat.of().parseHex(hashEsperado);
            if (!MessageDigest.isEqual(hashCalculadoBytes, esperadoBytes)) {
                log.error("[INTEGRIDAD-DOCUMENTAL] Mismatch de SHA-256. Esperado: {}, Calculado: {}", hashEsperado, hashCalculadoHex);
                throw new IllegalStateException("Fallo de integridad: El hash SHA-256 del contenido no coincide con el registro original.");
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error criptográfico al calcular SHA-256", e);
        }
    }

    /**
     * Calcula el hash SHA-256 en formato hexadecimal.
     */
    public String calcularSha256(byte[] contenido) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(contenido);
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error criptográfico al calcular SHA-256", e);
        }
    }

    /**
     * Ofusca números de documento (DNI/RUC) para su registro seguro en logs operativos (Ley N° 29733).
     */
    public static String ofuscarDocumento(String doc) {
        if (doc == null || doc.isBlank()) return "ANON";
        String limpio = doc.trim();
        if (limpio.length() <= 4) return "****";
        return limpio.substring(0, 2) + "***" + limpio.substring(limpio.length() - 2);
    }
}
