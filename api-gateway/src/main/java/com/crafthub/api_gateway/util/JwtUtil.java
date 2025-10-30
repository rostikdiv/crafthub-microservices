package com.crafthub.api_gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Component
@Slf4j
public class JwtUtil {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    // ❗️ --- НОВИЙ МЕТОД --- ❗️
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // ❗️ --- НОВИЙ УНІВЕРСАЛЬНИЙ МЕТОД --- ❗️
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts
                    .parser()
                    .verifyWith(getSignInKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("Failed to parse JWT token: {}", e.getMessage());
            throw e;
        }
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String username = claims.getSubject();
            Date expiration = claims.getExpiration();

            log.debug("Token details - Username: {}, Expiration: {}", username, expiration);

            boolean isNotExpired = !isTokenExpired(token);
            log.debug("Token expired: {}", !isNotExpired);

            return isNotExpired;
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        try {
            Date expiration = extractAllClaims(token).getExpiration();
            boolean expired = expiration.before(new Date());
            log.debug("Checking expiration - Expiration date: {}, Is expired: {}", expiration, expired);
            return expired;
        } catch (Exception e) {
            log.error("Failed to check token expiration: {}", e.getMessage());
            return true;
        }
    }

    private SecretKey getSignInKey() {
        log.debug("Using secret key (first 20 chars): {}...",
                secretKey.substring(0, Math.min(20, secretKey.length())));
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}