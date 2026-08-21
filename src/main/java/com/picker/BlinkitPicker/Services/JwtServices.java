package com.picker.BlinkitPicker.Services;

import com.picker.BlinkitPicker.Model.UserModel;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Service
public class JwtServices {

    private final String secret;
    private final long expirationMs;
    private final long refreshExpirationMs;

    public JwtServices(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expirationMs,
            @Value("${jwt.refresh-expiration}") long refreshExpirationMs) {
        this.secret = secret;
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // ─── Token Generation ──────────────────────────────────────────────────────

    public String generateAccessToken(UserModel user) {
        return buildToken(user, expirationMs);
    }

    public String generateRefreshToken(UserModel user) {
        return buildToken(user, refreshExpirationMs);
    }

    private String buildToken(UserModel user, long expMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expMs);

        return Jwts.builder()
                .setIssuer("blinkitpicker.dpdns.org")
                .setSubject(String.valueOf(user.getId()))
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ─── Token Validation ──────────────────────────────────────────────────────

    /**
     * Returns claims if valid, throws JwtException if invalid/expired.
     */
    public Claims validateAndExtractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(normalizeToken(token))
                .getBody();
    }

    /**
     * Safe wrapper — returns null if the token is invalid or expired.
     */
    public Claims extractClaimsSafely(String token) {
        try {
            return validateAndExtractClaims(token);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public Long extractUserId(String token) {
        return validateAndExtractClaims(token).get("userId", Long.class);
    }

    public boolean isTokenExpired(String token) {
        try {
            return validateAndExtractClaims(token).getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }

    private String normalizeToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }
}
