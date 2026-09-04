package com.milhub.user_service.service;

import com.milhub.user_service.entity.User;
import com.milhub.user_service.entity.enums.Role;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;
    private static final String TEST_SECRET = "bXktc3VwZXItc2VjcmV0LWtleS1mb3ItY3JhZnRodWItbWljcm9zZXJ2aWNlcy1hbmQtZm9yLWp3dC1zaWduYXR1cmUtZ2VuZXJhdGlvbg==";
    private static final long ONE_DAY_MS = 86_400_000L;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", ONE_DAY_MS);

        userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .email("soldier@milhub.ua")
                .password("encoded_pass")
                .role(Role.MILITARY_UNIT)
                .isVerified(true)
                .build();
    }

    @Test
    @DisplayName("Should generate token with custom claims (id, role, isVerified) and extract subject")
    void generateToken_ShouldIncludeExpectedClaims() {
        String token = jwtService.generateToken(testUser);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("soldier@milhub.ua");

        String extractedId = jwtService.extractClaim(token, claims -> claims.get("id", String.class));
        assertThat(extractedId).isEqualTo(userId.toString());

        String extractedRole = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
        assertThat(extractedRole).isEqualTo(Role.MILITARY_UNIT.name());

        Boolean isVerified = jwtService.extractClaim(token, claims -> claims.get("isVerified", Boolean.class));
        assertThat(isVerified).isTrue();
    }

    @Test
    @DisplayName("Should validate token successfully for matching user details")
    void isTokenValid_MatchingUser_ShouldReturnTrue() {
        String token = jwtService.generateToken(testUser);

        boolean isValid = jwtService.isTokenValid(token, testUser);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should return false when validating token against different user")
    void isTokenValid_DifferentUser_ShouldReturnFalse() {
        String token = jwtService.generateToken(testUser);

        User differentUser = User.builder()
                .id(UUID.randomUUID())
                .email("other@milhub.ua")
                .role(Role.BUYER)
                .build();

        boolean isValid = jwtService.isTokenValid(token, differentUser);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should throw ExpiredJwtException when validating an expired token")
    void isTokenValid_ExpiredToken_ShouldThrowExpiredJwtException() {
        // Set negative expiration to create an instantly expired token
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -5000L);
        String expiredToken = jwtService.generateToken(testUser);

        assertThatThrownBy(() -> jwtService.isTokenValid(expiredToken, testUser))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
