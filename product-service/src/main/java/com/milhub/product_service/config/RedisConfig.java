package com.milhub.product_service.config;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Configuration for Redis caching.
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                // Optional but good practice: set cache time-to-live (TTL)
                .entryTtl(Duration.ofMinutes(10))

                // Key serialization fix:
                // Keys (e.g., "products::1") will be stored as plain strings.
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))

                // Values serialization fix:
                // Values (our ProductResponseDTO) will be stored as JSON.
                // GenericJackson2JsonRedisSerializer works well with Records and Optionals.
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))

                // Do not cache 'null' values (if Optional is empty)
                .disableCachingNullValues();
    }

    // This bean automatically applies 'cacheConfiguration' to all @Cacheable
    // annotations we create.
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return (builder) -> builder
                .withCacheConfiguration("products",
                        cacheConfiguration().entryTtl(Duration.ofMinutes(30)));

        // Here we can add specific configurations for other caches, e.g.,
        // .withCacheConfiguration("users", ...);
    }
}