package com.milhub.api_gateway.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtUtil Tests")
class JwtUtilTest {

    private static final String SECRET_KEY = "bXktc3VwZXItc2VjcmV0LWtleS1mb3ItY3JhZnRodWItbWljcm9zZXJ2aWNlcy1hbmQtZm9yLWp3dC1zaWduYXR1cmUtZ2VuZXJhdGlvbg==";
    private static final String OTHER_KEY = "YW5vdGhlci1zdXBlci1zZWNyZXQta2V5LWZvci10ZXN0aW5nLXB1cnBvc2VzLW9ubHktbWlsaHViLWFwaS1nYXRld2F5==";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", SECRET_KEY);
    }

    private String generateToken(String subject, Map<String, Object> claims, long expirationMillis, String key) {
        byte[] keyBytes = Decoders.BASE64.decode(key);
        SecretKey signingKey = Keys.hmacShaKeyFor(keyBytes);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(signingKey)
                .compact();
    }

    @Test
    void testValidateToken_Valid() {
        String token = generateToken("user@milhub.ua", Map.of("id", "123"), 60000, SECRET_KEY);
        assertDoesNotThrow(() -> jwtUtil.validateToken(token));
        assertTrue(jwtUtil.isTokenValid(token));
    }

    @Test
    void testValidateToken_InvalidSignature() {
        String token = generateToken("user@milhub.ua", Map.of("id", "123"), 60000, OTHER_KEY);
        assertFalse(jwtUtil.isTokenValid(token));
        assertThrows(Exception.class, () -> jwtUtil.validateToken(token));
    }

    @Test
    void testValidateToken_Expired() {
        String token = generateToken("user@milhub.ua", Map.of("id", "123"), -1000, SECRET_KEY);
        assertFalse(jwtUtil.isTokenValid(token));
        assertThrows(Exception.class, () -> jwtUtil.validateToken(token));
    }

    @Test
    void testValidateToken_Malformed() {
        assertFalse(jwtUtil.isTokenValid("not.a.valid.jwt.token"));
        assertThrows(Exception.class, () -> jwtUtil.validateToken("not.a.valid.jwt.token"));
    }

    @Test
    void testExtractClaims_AllFieldsPresent() {
        Map<String, Object> claims = Map.of(
                "id", "user-uuid-999",
                "role", "SELLER",
                "permissions", List.of("READ", "WRITE"),
                "isVerified", true
        );
        String token = generateToken("seller@milhub.ua", claims, 60000, SECRET_KEY);

        assertEquals("user-uuid-999", jwtUtil.extractUserId(token));
        assertEquals("seller@milhub.ua", jwtUtil.extractUsername(token));
        assertEquals("SELLER", jwtUtil.extractUserRole(token));
        assertEquals(List.of("READ", "WRITE"), jwtUtil.extractPermissions(token));
        assertTrue(jwtUtil.extractIsVerified(token));
    }

    @Test
    void testExtractIsVerified_FalseAndNull() {
        String tokenFalse = generateToken("user@milhub.ua", Map.of("isVerified", false), 60000, SECRET_KEY);
        assertFalse(jwtUtil.extractIsVerified(tokenFalse));

        String tokenNull = generateToken("user@milhub.ua", Map.of("id", "123"), 60000, SECRET_KEY);
        assertFalse(jwtUtil.extractIsVerified(tokenNull));
    }
}
