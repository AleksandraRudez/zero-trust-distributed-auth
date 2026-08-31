package com.zerotrust.idp.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class JwtTokenService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    private static final Map<String, String> USER_DATABASE = new HashMap<>();
    static {
        USER_DATABASE.put("user1:password123", "USER:user1");
        USER_DATABASE.put("user2:password456", "USER:user2");
        USER_DATABASE.put("admin:admin123", "ADMIN:admin");
        USER_DATABASE.put("service-a:secret-a", "SERVICE:service-a");
        USER_DATABASE.put("service-b:secret-b", "SERVICE:service-b");
        USER_DATABASE.put("service-c:secret-c", "SERVICE:service-c");
    }

    public String generateToken(String username, String password, String clientType) {
        String key = username + ":" + password;
        String userInfo = USER_DATABASE.get(key);

        if (userInfo == null) {
            log.warn("Failed login attempt for user: {}", username);
            throw new RuntimeException("Invalid credentials");
        }

        String[] parts = userInfo.split(":");
        String role = parts[0];
        String userId = parts[1];

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("userId", userId);
        claims.put("clientType", clientType);

        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date expirationDate = new Date(nowMillis + jwtExpiration);

        SecretKey key2 = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(key2)
                .compact();
    }

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

    public Map<String, Object> getClaimsFromToken(String token) {
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

    public String getRoleForUser(String username, String password) {
        String key = username + ":" + password;
        String userInfo = USER_DATABASE.get(key);
        if (userInfo == null) {
            return null;
        }
        return userInfo.split(":")[0];
    }
}