package com.milhub.order_service.config.security;

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
        when(request.getHeader("X-User-Id")).thenReturn("user-uuid");
        when(request.getHeader("X-User-Permissions")).thenReturn("order:create,order:read");

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("user-uuid", auth.getPrincipal());
        assertEquals(2, auth.getAuthorities().size());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal sets empty authorities when X-User-Permissions is null or empty")
    void doFilterInternal_WithUserIdAndNullOrEmptyPermissions_SetsEmptyAuthorities() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("user-uuid");
        when(request.getHeader("X-User-Permissions")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.getAuthorities().isEmpty());

        SecurityContextHolder.clearContext();
        when(request.getHeader("X-User-Permissions")).thenReturn("");

        filter.doFilterInternal(request, response, filterChain);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().isEmpty());

        verify(filterChain, times(2)).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal does nothing when X-User-Id is missing")
    void doFilterInternal_WithoutUserId_NoAuthentication() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal preserves existing authentication in SecurityContext")
    void doFilterInternal_ExistingAuthentication_NotOverwritten() throws Exception {
        Authentication existingAuth = new UsernamePasswordAuthenticationToken("existing-user", null);
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(request.getHeader("X-User-Id")).thenReturn("another-user");

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(existingAuth, SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
