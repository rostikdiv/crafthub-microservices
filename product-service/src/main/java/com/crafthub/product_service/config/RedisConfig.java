package com.crafthub.product_service.config;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
public class RedisConfig {

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                // ❗️ Це опціонально, але гарна практика: встановити час життя кешу
                .entryTtl(Duration.ofMinutes(10))

                // ❗️ Головне виправлення (Ключ):
                // Ключі (наприклад, "products::1") будуть зберігатися як звичайні рядки.
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))

                // ❗️ Головне виправлення (Значення):
                // Значення (наш ProductResponseDTO) будуть зберігатися як JSON.
                // GenericJackson2JsonRedisSerializer чудово працює з Records та Optionals.
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))

                // Не кешувати 'null' значення (якщо Optional порожній)
                .disableCachingNullValues();
    }

    // Цей бін автоматично застосує нашу конфігурацію 'cacheConfiguration'
    // до всіх @Cacheable, які ми створюємо.
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return (builder) -> builder
                .withCacheConfiguration("products",
                        cacheConfiguration().entryTtl(Duration.ofMinutes(30)));

        // Тут ми можемо додати специфічні конфігурації для інших кешів,
        // наприклад, .withCacheConfiguration("users", ...);
    }
}