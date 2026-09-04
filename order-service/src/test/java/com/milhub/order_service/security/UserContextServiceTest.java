package com.milhub.order_service.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserContextServiceTest {

    @Mock
    private HttpServletRequest request;

    private final UserContextService userContextService = new UserContextService();

    @BeforeEach
    void setUp() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("getUserId returns parsed UUID when header is present")
    void getUserId_Present() {
        UUID expected = UUID.randomUUID();
        when(request.getHeader("X-User-Id")).thenReturn(expected.toString());

        UUID actual = userContextService.getUserId();
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("getUserId throws RuntimeException when header is missing")
    void getUserId_Missing_ThrowsException() {
        when(request.getHeader("X-User-Id")).thenReturn(null);

        assertThrows(RuntimeException.class, userContextService::getUserId);
    }

    @Test
    @DisplayName("getUserEmail returns value from header")
    void getUserEmail() {
        when(request.getHeader("X-User-Email")).thenReturn("soldier@milhub.com");

        assertEquals("soldier@milhub.com", userContextService.getUserEmail());
    }

    @Test
    @DisplayName("getUserRole returns value from header")
    void getUserRole() {
        when(request.getHeader("X-User-Role")).thenReturn("SELLER");

        assertEquals("SELLER", userContextService.getUserRole());
    }

    @Test
    @DisplayName("isVerified returns true when header is true and false otherwise")
    void isVerified() {
        when(request.getHeader("X-User-Is-Verified")).thenReturn("true");
        assertTrue(userContextService.isVerified());

        when(request.getHeader("X-User-Is-Verified")).thenReturn("false");
        assertFalse(userContextService.isVerified());

        when(request.getHeader("X-User-Is-Verified")).thenReturn(null);
        assertFalse(userContextService.isVerified());
    }

    @Test
    @DisplayName("getHeader returns null when RequestAttributes is null (e.g. background/kafka thread)")
    void getHeader_NullAttributes() {
        RequestContextHolder.resetRequestAttributes();

        assertNull(userContextService.getUserEmail());
        assertNull(userContextService.getUserRole());
        assertFalse(userContextService.isVerified());
        assertThrows(RuntimeException.class, userContextService::getUserId);
    }
}
