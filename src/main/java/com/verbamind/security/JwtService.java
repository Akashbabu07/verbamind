package com.verbamind.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Service
@EnableConfigurationProperties(JwtProperties.class)
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final JwtProperties jwtProperties;
    private final SecretKey key;

    @Autowired
    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        if (jwtProperties.getSecret() == null || jwtProperties.getSecret().isBlank()) {
            throw new IllegalStateException(
                    "verbamind.jwt.secret is not set — check that JWT_SECRET is present in your .env "
                            + "and actually loaded (e.g. via docker-compose env_file, or exported in your shell).");
        }
        this.key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }

    public String generateAccessToken(UUID userId, String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getAccessTokenExpirationMs());

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String generateRefreshTokenValue() {
        return UUID.randomUUID().toString() + "-" + UUID.randomUUID();
    }

    public long getRefreshTokenExpirationMs() {
        return jwtProperties.getRefreshTokenExpirationMs();
    }

    public UUID extractUserId(String token) {
        Claims claims = parseClaims(token);
        return UUID.fromString(claims.getSubject());
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("Rejected access token: expired at {}", e.getClaims().getExpiration());
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Rejected access token: {}", e.getMessage());
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}