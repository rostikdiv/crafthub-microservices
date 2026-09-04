package com.milhub.notification_service.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HeaderAuthenticationFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private HeaderAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("doFilterInternal sets authentication when X-User-Id and X-User-Permissions are present")
    void doFilterInternal_WithUserIdAndPermissions_SetsAuthentication() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("user-123");
        when(request.getHeader("X-User-Permissions")).thenReturn("ROLE_USER,READ_PRIVILEGES");

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("user-123", auth.getPrincipal());
        assertEquals(2, auth.getAuthorities().size());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("READ_PRIVILEGES")));

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal sets empty authorities when X-User-Permissions is missing or empty")
    void doFilterInternal_WithUserIdAndMissingPermissions_SetsEmptyAuthorities() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("user-456");
        when(request.getHeader("X-User-Permissions")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("user-456", auth.getPrincipal());
        assertTrue(auth.getAuthorities().isEmpty());

        // Also test empty string permissions
        SecurityContextHolder.clearContext();
        when(request.getHeader("X-User-Permissions")).thenReturn("");

        filter.doFilterInternal(request, response, filterChain);

        auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.getAuthorities().isEmpty());

        verify(filterChain, times(2)).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal does not authenticate when X-User-Id is missing")
    void doFilterInternal_WithoutUserId_NoAuthentication() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn(null);
        when(request.getHeader("X-User-Permissions")).thenReturn("ROLE_USER");

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal does not overwrite existing authentication in SecurityContext")
    void doFilterInternal_ExistingAuthentication_NotOverwritten() throws Exception {
        Authentication existingAuth = new UsernamePasswordAuthenticationToken("already-authenticated", null);
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(request.getHeader("X-User-Id")).thenReturn("new-user");
        when(request.getHeader("X-User-Permissions")).thenReturn("ROLE_ADMIN");

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(existingAuth, SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
