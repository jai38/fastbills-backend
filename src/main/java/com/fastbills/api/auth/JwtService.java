package com.fastbills.api.auth;

import com.fastbills.api.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMinutes;
    private final long refreshExpirationDays;

    public JwtService(
            @Value("${JWT_SECRET:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}") String secret,
            @Value("${JWT_EXPIRATION_MINUTES:60}") long expirationMinutes,
            @Value("${JWT_REFRESH_EXPIRATION_DAYS:7}") long refreshExpirationDays
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
        this.refreshExpirationDays = refreshExpirationDays;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(expirationMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("orgId", user.getOrganisationId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("fullName", user.getFullName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseAndValidateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid or expired JWT token: " + e.getMessage(), e);
        }
    }

    public UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    public UUID extractOrganisationId(Claims claims) {
        return UUID.fromString(claims.get("orgId", String.class));
    }

    public String extractRole(Claims claims) {
        return claims.get("role", String.class);
    }

    public String extractEmail(Claims claims) {
        return claims.get("email", String.class);
    }

    public Instant getRefreshExpiry() {
        return Instant.now().plus(refreshExpirationDays, ChronoUnit.DAYS);
    }
}
