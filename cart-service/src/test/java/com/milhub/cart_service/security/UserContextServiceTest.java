package com.milhub.cart_service.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserContextService Unit Tests")
class UserContextServiceTest {

    private UserContextService service;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        service = new UserContextService();
        request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void testGetUserId_Present() {
        UUID expectedId = UUID.randomUUID();
        request.addHeader("X-User-Id", expectedId.toString());

        assertEquals(expectedId, service.getUserId());
    }

    @Test
    void testGetUserId_Missing_ThrowsException() {
        assertThrows(RuntimeException.class, () -> service.getUserId());
    }

    @Test
    void testGetUserEmail() {
        request.addHeader("X-User-Email", "user@milhub.ua");
        assertEquals("user@milhub.ua", service.getUserEmail());
    }

    @Test
    void testGetUserRole() {
        request.addHeader("X-User-Role", "ADMIN");
        assertEquals("ADMIN", service.getUserRole());
    }

    @Test
    void testIsVerified() {
        request.addHeader("X-User-Is-Verified", "true");
        assertTrue(service.isVerified());

        request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        request.addHeader("X-User-Is-Verified", "false");
        assertFalse(service.isVerified());
    }

    @Test
    void testGetHeader_NullRequestContext_ReturnsNull() {
        RequestContextHolder.resetRequestAttributes();

        assertNull(service.getUserEmail());
        assertNull(service.getUserRole());
        assertFalse(service.isVerified());
    }
}
