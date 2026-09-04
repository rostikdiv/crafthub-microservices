package com.milhub.notification_service.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class KafkaConsumerConfigTest {

    private final KafkaConsumerConfig kafkaConsumerConfig = new KafkaConsumerConfig();

    @Test
    @DisplayName("consumerFactory and kafkaListenerContainerFactory instantiate beans successfully")
    void testKafkaConfigBeans() {
        KafkaProperties properties = new KafkaProperties();
        ConsumerFactory<String, String> consumerFactory = kafkaConsumerConfig.consumerFactory(properties);
        assertNotNull(consumerFactory);

        @SuppressWarnings("unchecked")
        ConsumerFactory<String, String> mockFactory = mock(ConsumerFactory.class);
        ConcurrentKafkaListenerContainerFactory<String, String> containerFactory =
                kafkaConsumerConfig.kafkaListenerContainerFactory(mockFactory);
        assertNotNull(containerFactory);
        assertNotNull(containerFactory.getConsumerFactory());
    }
}
