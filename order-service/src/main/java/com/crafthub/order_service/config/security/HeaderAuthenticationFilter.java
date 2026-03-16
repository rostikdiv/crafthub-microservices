package com.crafthub.order_service.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filter to extract user context and permissions from HTTP headers.
 * Populates the Spring SecurityContext based on headers provided by the API
 * Gateway.
 */
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");
        String permissionsHeader = request.getHeader("X-User-Permissions");

        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Convert permission string (e.g., "product:create,order:read") to authorities
            List<SimpleGrantedAuthority> authorities = Collections.emptyList();

            if (permissionsHeader != null && !permissionsHeader.isEmpty()) {
                authorities = Arrays.stream(permissionsHeader.split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            }

            // Create authentication token (password is null as we trust the gateway)
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    authorities);

            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        chain.doFilter(request, response);
    }
}