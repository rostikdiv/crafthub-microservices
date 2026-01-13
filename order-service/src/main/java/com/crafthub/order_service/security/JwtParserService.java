package com.crafthub.order_service.security;

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

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    public UUID extractUserId(String token) {
        token = cleanToken(token);
        return extractClaim(token, claims -> {
            String id = claims.get("id", String.class);
            return UUID.fromString(id);
        });
    }

    public String extractUserEmail(String token) {
        token = cleanToken(token);
        return extractClaim(token, Claims::getSubject);
    }

    public String extractUserRole(String token) {
        token = cleanToken(token);
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public boolean extractIsVerified(String token) {
        token = cleanToken(token);
        // Якщо поле null (наприклад, старий токен), вважаємо false
        Boolean isVerified = extractClaim(token, claims -> claims.get("isVerified", Boolean.class));
        return isVerified != null && isVerified;
    }

    private String cleanToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parser()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claimsResolver.apply(claims);
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}