package com.crafthub.product_service.config;

import com.crafthub.product_service.config.security.HeaderAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for the Product microservice.
 * Relies on custom HeaderAuthenticationFilter for user identification.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Enables @PreAuthorize
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // Allow all, as protection is now method-level
                )
                // Add our custom header-based authentication filter
                .addFilterBefore(new HeaderAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}