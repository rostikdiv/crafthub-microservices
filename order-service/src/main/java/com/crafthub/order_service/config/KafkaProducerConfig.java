package com.crafthub.order_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaProducerConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // ❗️ Реєструємо модуль JavaTimeModule. Це потрібно,
        // щоб ObjectMapper коректно серіалізував/десеріалізував
        // поля типу LocalDateTime, які є в Order Entity.
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}