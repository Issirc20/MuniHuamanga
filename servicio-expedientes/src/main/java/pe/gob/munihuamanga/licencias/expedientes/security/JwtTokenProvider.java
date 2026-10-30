package pe.gob.munihuamanga.licencias.expedientes.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Emisión y verificación de tokens de acceso firmados con HMAC-SHA256.
 * El token de acceso es de corta duración y referencia la sesión de la que procede (claim {@code sid});
 * la vigencia de la sesión y de la persona se comprueban en cada petición.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    public static final String DEFAULT_DEV_SECRET = "MunicipalidadProvincialDeHuamangaGestionLicenciasSecretKeySegura2026!";
    static final String CLAIM_USUARIO = "uid";
    static final String CLAIM_SESION = "sid";
    static final String CLAIM_ROLES = "roles";
    static final String CLAIM_NOMBRE = "nombre";

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(String secret, long expirationMs) {
        this(secret, expirationMs, "");
    }

    @Autowired
    public JwtTokenProvider(
            @Value("${jwt.secret:" + DEFAULT_DEV_SECRET + "}") String secret,
            @Value("${jwt.expiration-ms:1800000}") long expirationMs,
            @Value("${spring.profiles.active:}") String activeProfiles
    ) {
        boolean produccion = Arrays.stream(activeProfiles.split(",")).map(String::trim).anyMatch("prod"::equalsIgnoreCase);
        if (produccion && (secret == null || secret.equals(DEFAULT_DEV_SECRET) || secret.length() < 32)) {
            throw new IllegalStateException("En el perfil de producción la variable JWT_SECRET debe definirse con al menos 256 bits (32 caracteres).");
        }
        if (DEFAULT_DEV_SECRET.equals(secret)) {
            log.warn("[SEGURIDAD] Se está utilizando la clave de firma de desarrollo. Defina JWT_SECRET en entornos institucionales.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Emite el token de acceso de una persona autenticada para la sesión indicada.
     */
    public String generarTokenAcceso(CustomUserDetails persona, UUID sesionId) {
        if (persona.getRoles().isEmpty()) {
            throw new IllegalStateException("La persona no tiene roles asignados; no se emite token de acceso.");
        }
        Date ahora = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(persona.getUsername())
                .claim(CLAIM_USUARIO, persona.getId().toString())
                .claim(CLAIM_SESION, sesionId != null ? sesionId.toString() : null)
                .claim(CLAIM_NOMBRE, persona.getNombreCompleto())
                .claim(CLAIM_ROLES, persona.getRoles().stream().map(Enum::name).toList())
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    public Claims obtenerClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public boolean validarToken(String token) {
        try {
            obtenerClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("[SEGURIDAD] Token de acceso rechazado: {}", ex.getClass().getSimpleName());
            return false;
        }
    }

    public String obtenerUsernameDelToken(String token) {
        return obtenerClaims(token).getSubject();
    }

    public Optional<UUID> obtenerSesionId(String token) {
        String sesion = obtenerClaims(token).get(CLAIM_SESION, String.class);
        return sesion == null ? Optional.empty() : Optional.of(UUID.fromString(sesion));
    }

    @SuppressWarnings("unchecked")
    public List<String> obtenerRolesDelToken(String token) {
        Object roles = obtenerClaims(token).get(CLAIM_ROLES);
        return roles instanceof List<?> lista ? (List<String>) lista : List.of();
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
