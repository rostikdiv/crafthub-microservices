package com.crafthub.api_gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Configuration
public class RateLimiterConfig {

    /**
     * Identifies the user by their IP address for Rate Limiting.
     * If the IP address cannot be determined, it falls back to a default key ("anonymous").
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = "anonymous";
            try {
                if (exchange.getRequest().getRemoteAddress() != null) {
                    ip = Objects.requireNonNull(exchange.getRequest().getRemoteAddress()).getAddress().getHostAddress();
                }
            } catch (Exception e) {
                // fallback to anonymous if extraction fails
            }
            return Mono.just(ip);
        };
    }
}
