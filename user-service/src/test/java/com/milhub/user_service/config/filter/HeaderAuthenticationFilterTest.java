package com.milhub.user_service.config.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeaderAuthenticationFilterTest {

    private HeaderAuthenticationFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new HeaderAuthenticationFilter();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_WhenHeadersPresent_ShouldSetAuthentication() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("user-uuid-123");
        when(request.getHeader("X-User-Permissions")).thenReturn("user:read,user:write");

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo("user-uuid-123");
        assertThat(auth.getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder("user:read", "user:write");

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_WhenPermissionsHeaderEmpty_ShouldSetEmptyAuthorities() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("user-uuid-123");
        when(request.getHeader("X-User-Permissions")).thenReturn("");

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).isEmpty();

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_WhenNoUserIdHeader_ShouldNotSetAuthentication() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
