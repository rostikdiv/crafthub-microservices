package com.crafthub.api_gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.List;

@Component
public class JwtUtil {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    /**
     * ✅ Метод, який використовується в GatewayTestController.
     * Повертає true, якщо токен валідний, і false, якщо ні.
     */
    public boolean isTokenValid(String token) {
        try {
            validateToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Валідує токен. Якщо токен невалідний (минув час дії або підпис невірний),
     * бібліотека jjwt кине виняток (JwtException).
     */
    public void validateToken(final String token) {
        Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token);
    }

    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // --- Допоміжні методи для AuthenticationFilter ---
    // Вони знадобляться, щоб витягувати ID та ролі для передачі далі в мікросервіси

    public String extractUserId(String token) {
        return getAllClaimsFromToken(token).get("id", String.class);
    }

    public String extractUsername(String token) {
        return getAllClaimsFromToken(token).getSubject();
    }

    public String extractUserRole(String token) {
        return getAllClaimsFromToken(token).get("role", String.class);
    }

    public List<String> extractPermissions(String token) {
        return getAllClaimsFromToken(token).get("permissions", List.class);
    }

    public boolean extractIsVerified(String token) {
        Object isVerified = getAllClaimsFromToken(token).get("isVerified");
        return isVerified != null && Boolean.parseBoolean(isVerified.toString());
    }

    private SecretKey getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}