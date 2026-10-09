package pe.gob.munihuamanga.licencias.expedientes.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Proveedor y generador de tokens JWT para el sistema municipal (Fase 04 Sprint 4-C).
 * Utiliza JJWT 0.12.x con algoritmo HMAC-SHA256 y clave de 256 bits mínimo.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    public static final String DEFAULT_DEV_SECRET = "MunicipalidadProvincialDeHuamangaGestionLicenciasSecretKeySegura2026!";

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(String secret, long expirationMs) {
        this(secret, expirationMs, "");
    }

    @org.springframework.beans.factory.annotation.Autowired
    public JwtTokenProvider(
            @Value("${jwt.secret:" + DEFAULT_DEV_SECRET + "}") String secret,
            @Value("${jwt.expiration-ms:28800000}") long expirationMs,
            @Value("${spring.profiles.active:}") String activeProfiles
    ) {
        if ("prod".equalsIgnoreCase(activeProfiles) && (secret == null || secret.equals(DEFAULT_DEV_SECRET) || secret.length() < 32)) {
            throw new IllegalStateException("CRÍTICO DE SEGURIDAD: En perfil 'prod', JWT_SECRET debe configurarse mediante variable de entorno segura con al menos 256 bits (32 caracteres).");
        }
        if (DEFAULT_DEV_SECRET.equals(secret)) {
            log.warn("[SEGURIDAD - ADVERTENCIA] Se está utilizando la clave secreta JWT por defecto de desarrollo. Configurar variable JWT_SECRET en entornos productivos.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generarToken(Authentication authentication, UUID usuarioId, String nombreCompleto) {
        String username = authentication.getName();
        String rol = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_EVALUADOR");

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("userId", usuarioId != null ? usuarioId.toString() : null)
                .claim("nombreCompleto", nombreCompleto)
                .claim("rol", rol)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String generarToken(String username, String rol, String nombreCompleto, UUID usuarioId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("userId", usuarioId != null ? usuarioId.toString() : null)
                .claim("nombreCompleto", nombreCompleto)
                .claim("rol", rol)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String obtenerUsernameDelToken(String token) {
        return obtenerClaims(token).getSubject();
    }

    public String obtenerRolDelToken(String token) {
        return obtenerClaims(token).get("rol", String.class);
    }

    public Claims obtenerClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validarToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (MalformedJwtException ex) {
            log.error("[JWT] Token malformado: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.warn("[JWT] Token expirado: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("[JWT] Token no soportado: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("[JWT] Token vacío o nulo: {}", ex.getMessage());
        } catch (JwtException ex) {
            log.error("[JWT] Error en verificación de firma JWT: {}", ex.getMessage());
        }
        return false;
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
