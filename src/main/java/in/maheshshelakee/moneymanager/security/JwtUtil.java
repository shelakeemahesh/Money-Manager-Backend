package in.maheshshelakee.moneymanager.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * JWT utility for token generation, parsing, and validation.
 * <p>
 * Uses HMAC-SHA256 which requires a secret key of at least 32 bytes (256 bits).
 * The secret and expiration are injected from application.properties and validated
 * at startup via {@link #validateConfiguration()}.
 */
@Slf4j
@Component
public class JwtUtil {

    private static final int MIN_SECRET_LENGTH = 32;

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    private Key signingKey;

    /**
     * Fail-fast validation — if the secret is too short or expiration is invalid,
     * the app crashes immediately at startup with a clear error message instead of
     * failing silently on the first token operation at runtime.
     */
    @PostConstruct
    void validateConfiguration() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT secret (app.jwt.secret) is not configured. "
                    + "Set APP_JWT_SECRET environment variable with a minimum of "
                    + MIN_SECRET_LENGTH + " characters.");
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "JWT secret is too short (" + keyBytes.length + " bytes). "
                    + "HMAC-SHA256 requires at least " + MIN_SECRET_LENGTH + " bytes. "
                    + "Current value has " + keyBytes.length + " bytes.");
        }

        if (expirationMs <= 0) {
            throw new IllegalStateException(
                    "JWT expiration (app.jwt.expiration-ms) must be positive, got: " + expirationMs);
        }

        // Pre-compute the signing key once (immutable after init)
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);

        log.info("JwtUtil initialized — secret length: {} bytes, expiration: {} ms",
                keyBytes.length, expirationMs);
    }

    public String generateToken(String email, String role, String status, String passwordVersion) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .claim("status", status)
                .claim("pv", passwordVersion)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String getPasswordVersion(String passwordHash) {
        if (passwordHash == null) return "";
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((passwordHash + secret).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().substring(0, 16);
        } catch (Exception e) {
            return String.valueOf(passwordHash.hashCode());
        }
    }

    public String extractPasswordVersion(String token) {
        try {
            return extractClaims(token).get("pv", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    public String extractStatus(String token) {
        return extractClaims(token).get("status", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Single parsing method — avoids recreating the parser on every call.
     */
    private Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
