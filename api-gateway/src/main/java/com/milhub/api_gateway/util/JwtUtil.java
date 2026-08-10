package com.milhub.api_gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.List;

/**
 * Utility class for JSON Web Token (JWT) processing.
 * Handles validation and extraction of claims such as user ID, roles, and
 * permissions.
 */
@Component
public class JwtUtil {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    /**
     * Checks if a token is valid without throwing exceptions.
     *
     * @param token The JWT string to check.
     * @return true if valid, false otherwise.
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
     * Validates the given JWT.
     * This method will throw a JwtException if the token is expired or the
     * signature is invalid.
     *
     * @param token The JWT string to validate.
     */
    public void validateToken(final String token) {
        Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token);
    }

    /**
     * Extracts all claims from the given JWT.
     */
    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts the user ID from the token.
     */
    public String extractUserId(String token) {
        return getAllClaimsFromToken(token).get("id", String.class);
    }

    /**
     * Extracts the username (subject) from the token.
     */
    public String extractUsername(String token) {
        return getAllClaimsFromToken(token).getSubject();
    }

    /**
     * Extracts the user role from the token.
     */
    public String extractUserRole(String token) {
        return getAllClaimsFromToken(token).get("role", String.class);
    }

    /**
     * Extracts the list of permissions from the token.
     */
    public List<String> extractPermissions(String token) {
        return getAllClaimsFromToken(token).get("permissions", List.class);
    }

    /**
     * Extracts the verification status from the token.
     */
    public boolean extractIsVerified(String token) {
        Object isVerified = getAllClaimsFromToken(token).get("isVerified");
        return isVerified != null && Boolean.parseBoolean(isVerified.toString());
    }

    /**
     * Retrieves the signing key from the configured secret.
     */
    private SecretKey getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}