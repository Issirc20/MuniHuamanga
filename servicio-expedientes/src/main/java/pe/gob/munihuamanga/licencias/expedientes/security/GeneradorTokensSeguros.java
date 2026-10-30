package pe.gob.munihuamanga.licencias.expedientes.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Generación de secretos de un solo uso y de su resumen SHA-256 para almacenamiento.
 * Los secretos nunca se guardan en claro.
 */
public final class GeneradorTokensSeguros {

    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final int BYTES_TOKEN = 32;

    private GeneradorTokensSeguros() {
    }

    public static String nuevoToken() {
        byte[] bytes = new byte[BYTES_TOKEN];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String resumen(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("El algoritmo SHA-256 no está disponible en la plataforma.", ex);
        }
    }
}
