package com.crafthub.cart_service.config.security;

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
 * Filter to extract user identity and permissions from HTTP headers.
 * Populates the Spring SecurityContext for downstream authorization.
 */
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");
        String permissionsHeader = request.getHeader("X-User-Permissions");

        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            List<SimpleGrantedAuthority> authorities = Collections.emptyList();

            // Parse permission comma-separated strings (e.g., "cart:read,order:create")
            if (permissionsHeader != null && !permissionsHeader.isEmpty()) {
                authorities = Arrays.stream(permissionsHeader.split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            }

            // Create Authentication object (password is ignored as the Gateway handles
            // primary auth)
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(request, response);
    }
}