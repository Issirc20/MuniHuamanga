package pe.gob.munihuamanga.licencias.verificacion.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio encargado de la generación criptográfica/única de códigos QR según RNF-13.
 */
@Slf4j
@Service
public class QrGeneratorService {

    @Value("${portal.verificacion.base-url:https://licencias.munihuamanga.gob.pe/verificar/}")
    private String portalBaseUrl;

    /**
     * Genera la imagen PNG del código QR conteniendo la URL pública de validación.
     */
    public byte[] generarImagenQr(String codigoLicencia, int ancho, int alto) {
        String textoContenido = portalBaseUrl + codigoLicencia;

        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix bitMatrix = qrCodeWriter.encode(textoContenido, BarcodeFormat.QR_CODE, ancho, alto, hints);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Error al generar código QR para la licencia: {}", codigoLicencia, e);
            throw new RuntimeException("Error en la generación del código QR", e);
        }
    }
}
