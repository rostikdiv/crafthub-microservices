package com.crafthub.order_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
@Profile("kafka")
public class KafkaProducerConfig {

    // (Ми можемо прибрати @Autowired KafkaProperties,
    // оскільки Spring Boot автоматично інжектує його в producerFactory)

    /**
     * Цей бін створює стандартну фабрику.
     * Нам не потрібно її кастомізувати.
     */
    @Bean
    public ProducerFactory<String, String> producerFactory(KafkaProperties kafkaProperties) {
        return new DefaultKafkaProducerFactory<>(kafkaProperties.buildProducerProperties(null));
    }

    /**
     * ❗️ ВИРІШЕННЯ ТУТ:
     * Ми створюємо наш KafkaTemplate і вмикаємо Observation (трасування)
     * *саме на ньому*.
     */
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
        KafkaTemplate<String, String> template = new KafkaTemplate<>(producerFactory);

        // ❗️ Ось правильний метод
        template.setObservationEnabled(true);

        return template;
    }

    /**
     * Цей бін залишається (він потрібен для серіалізації в JSON)
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}