package com.crafthub.cart_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtParserService {

    // ❗️ Встав сюди ТОЙ САМИЙ ключ, що і в User Service
    // Краще винести це в application.yaml
    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    public UUID extractUserId(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return extractClaim(token, claims -> {
            // У токені UUID лежить як звичайний String
            String idString = claims.get("id", String.class);
            return UUID.fromString(idString);
        });
    }
    public String extractUserRole(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return extractClaim(token, claims -> (String) claims.get("role"));
    }

    public boolean extractIsVerified(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        // Якщо поле null (наприклад, старий токен), вважаємо false
        Boolean isVerified = extractClaim(token, claims -> claims.get("isVerified", Boolean.class));
        return isVerified != null && isVerified;
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}