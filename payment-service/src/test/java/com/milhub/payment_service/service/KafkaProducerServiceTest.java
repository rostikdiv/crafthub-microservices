package com.milhub.payment_service.service;

import com.milhub.payment_service.dto.payment.PaymentSuccessEventDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaProducerService Unit Tests")
class KafkaProducerServiceTest {

    @Mock
    private KafkaTemplate<String, PaymentSuccessEventDTO> kafkaTemplate;

    @InjectMocks
    private KafkaProducerService producerService;

    @Test
    void testSendPaymentSuccessEvent() {
        PaymentSuccessEventDTO event = new PaymentSuccessEventDTO(UUID.randomUUID(), "buyer@milhub.ua", BigDecimal.valueOf(150));

        producerService.sendPaymentSuccessEvent(event);

        verify(kafkaTemplate, times(1)).send("payment-success-topic", event);
    }
}
