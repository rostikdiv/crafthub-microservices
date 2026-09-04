package com.milhub.product_service.service;

import com.milhub.product_service.client.OrderServiceClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceIntegrationTest {

    @Mock
    private OrderServiceClient orderServiceClient;

    @InjectMocks
    private OrderServiceIntegration orderServiceIntegration;

    @Test
    @DisplayName("checkPurchase: delegates to OrderServiceClient")
    void checkPurchase_ShouldCallClient() {
        UUID productId = UUID.randomUUID();
        when(orderServiceClient.checkPurchase(productId)).thenReturn(true);

        boolean result = orderServiceIntegration.checkPurchase(productId);

        assertThat(result).isTrue();
        verify(orderServiceClient).checkPurchase(productId);
    }

    @Test
    @DisplayName("checkPurchaseFallback: returns false when circuit breaker triggers")
    void checkPurchaseFallback_ShouldReturnFalse() {
        UUID productId = UUID.randomUUID();
        boolean fallbackResult = orderServiceIntegration.checkPurchaseFallback(productId, new RuntimeException("Service Down"));

        assertThat(fallbackResult).isFalse();
    }
}
