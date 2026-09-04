package com.milhub.cart_service.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("HeaderAuthenticationFilter Unit Tests")
class HeaderAuthenticationFilterTest {

    private HeaderAuthenticationFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new HeaderAuthenticationFilter();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testDoFilter_ValidUserIdAndPermissions() throws ServletException, IOException {
        when(request.getHeader("X-User-Id")).thenReturn("user-uuid-123");
        when(request.getHeader("X-User-Permissions")).thenReturn("cart:read,order:create");

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("user-uuid-123", auth.getPrincipal());
        assertEquals(2, auth.getAuthorities().size());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void testDoFilter_ValidUserId_NullPermissions() throws ServletException, IOException {
        when(request.getHeader("X-User-Id")).thenReturn("user-uuid-123");
        when(request.getHeader("X-User-Permissions")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("user-uuid-123", auth.getPrincipal());
        assertTrue(auth.getAuthorities().isEmpty());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void testDoFilter_ValidUserId_EmptyPermissions() throws ServletException, IOException {
        when(request.getHeader("X-User-Id")).thenReturn("user-uuid-123");
        when(request.getHeader("X-User-Permissions")).thenReturn("");

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.getAuthorities().isEmpty());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void testDoFilter_NullUserId_NoAuthentication() throws ServletException, IOException {
        when(request.getHeader("X-User-Id")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void testDoFilter_AlreadyAuthenticated_NoOverwrite() throws ServletException, IOException {
        Authentication existingAuth = new UsernamePasswordAuthenticationToken("existing-user", null);
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(request.getHeader("X-User-Id")).thenReturn("new-user");

        filter.doFilterInternal(request, response, filterChain);

        assertSame(existingAuth, SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }
}
