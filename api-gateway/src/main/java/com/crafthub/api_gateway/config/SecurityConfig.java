package com.crafthub.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                // Вимикаємо CSRF
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                // Вимикаємо форму логіну
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)

                // Вимикаємо HTTP Basic
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)

                // ❗️ КРИТИЧНО: Дозволяємо ВСІ запити без автентифікації Spring Security
                // Наш AuthenticationFilter сам перевірятиме JWT
                .authorizeExchange(exchanges -> exchanges
                        .anyExchange().permitAll()
                );

        return http.build();
    }
}