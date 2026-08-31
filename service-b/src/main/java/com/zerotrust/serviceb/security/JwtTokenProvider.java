package com.zerotrust.serviceb.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public Map<String, Object> getClaims(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            return new HashMap<>(Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (Exception e) {
            log.error("Failed to extract claims: {}", e.getMessage());
            return null;
        }
    }

    public String getUserIdFromToken(String token) {
        try {
            Map<String, Object> claims = getClaims(token);
            return (String) claims.get("userId");
        } catch (Exception e) {
            log.error("Failed to extract userId: {}", e.getMessage());
            return null;
        }
    }

    public String getRoleFromToken(String token) {
        try {
            Map<String, Object> claims = getClaims(token);
            return (String) claims.get("role");
        } catch (Exception e) {
            log.error("Failed to extract role: {}", e.getMessage());
            return null;
        }
    }

    public String getClientTypeFromToken(String token) {
        try {
            Map<String, Object> claims = getClaims(token);
            return (String) claims.get("clientType");
        } catch (Exception e) {
            log.error("Failed to extract clientType: {}", e.getMessage());
            return null;
        }
    }

    public Instant getIssuedAtFromToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            var issuedAt = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getIssuedAt();
            return issuedAt != null ? issuedAt.toInstant() : null;
        } catch (Exception e) {
            log.error("Failed to extract issuedAt: {}", e.getMessage());
            return null;
        }
    }

    public Instant getExpirationFromToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            var expiration = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration();
            return expiration != null ? expiration.toInstant() : null;
        } catch (Exception e) {
            log.error("Failed to extract expiration: {}", e.getMessage());
            return null;
        }
    }
}