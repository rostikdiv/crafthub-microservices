package com.milhub.order_service.service;

import com.milhub.order_service.client.DeliveryServiceClient;
import com.milhub.order_service.client.PaymentServiceClient;
import com.milhub.order_service.client.ProductServiceClient;
import com.milhub.order_service.dto.delivery.DeliveryDetailsDTO;
import com.milhub.order_service.dto.delivery.ReturnShipmentRequestDTO;
import com.milhub.order_service.dto.delivery.ReturnShipmentResponseDTO;
import com.milhub.order_service.dto.external.ProductResponseDTO;
import com.milhub.order_service.dto.payment.PaymentRequestDTO;
import com.milhub.order_service.dto.payment.PaymentResponseDTO;
import com.milhub.order_service.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IntegrationServicesTest {

    @Mock
    private DeliveryServiceClient deliveryServiceClient;

    @Mock
    private ProductServiceClient productServiceClient;

    @Mock
    private PaymentServiceClient paymentServiceClient;

    @Test
    @DisplayName("DeliveryServiceIntegration delegates to Feign client and handles errors")
    void testDeliveryServiceIntegration() {
        DeliveryServiceIntegration deliveryIntegration = new DeliveryServiceIntegration(deliveryServiceClient);

        UUID orderId = UUID.randomUUID();
        DeliveryDetailsDTO returnAddress = DeliveryDetailsDTO.builder().cityRef("Kyiv").build();
        ReturnShipmentRequestDTO request = new ReturnShipmentRequestDTO(orderId, returnAddress, 1.5);
        ReturnShipmentResponseDTO expectedResponse = new ReturnShipmentResponseDTO(UUID.randomUUID(), "RET-12345", BigDecimal.valueOf(60));

        when(deliveryServiceClient.createReturnShipment(request)).thenReturn(expectedResponse);
        ReturnShipmentResponseDTO response = deliveryIntegration.createReturnShipment(request);
        assertEquals("RET-12345", response.trackingNumber());

        when(deliveryServiceClient.createReturnShipment(any())).thenThrow(new RuntimeException("Connection failed"));
        assertThrows(RuntimeException.class, () -> deliveryIntegration.createReturnShipment(request));
    }

    @Test
    @DisplayName("ProductIntegrationService delegates to Feign client and tests fallback methods")
    void testProductIntegrationService() {
        ProductIntegrationService productIntegration = new ProductIntegrationService(productServiceClient);

        UUID productId = UUID.randomUUID();
        ProductResponseDTO product = new ProductResponseDTO(productId, "Scope", BigDecimal.valueOf(100), "PUBLIC", 5, UUID.randomUUID());

        when(productServiceClient.getProductById(productId)).thenReturn(product);
        assertEquals(product, productIntegration.getProductById(productId));

        productIntegration.reduceStock(productId, 2);
        verify(productServiceClient).reduceStock(productId, 2, "");

        productIntegration.restoreStock(productId, 2);
        verify(productServiceClient).restoreStock(productId, 2, "");

        // Fallbacks
        Throwable error = new RuntimeException("Timeout");
        assertThrows(BusinessException.class, () -> productIntegration.getProductFallback(productId, error));
        assertThrows(BusinessException.class, () -> productIntegration.reduceStockFallback(productId, 2, error));
        assertDoesNotThrow(() -> productIntegration.restoreStockFallback(productId, 2, error));

        assertThrows(UnsupportedOperationException.class, () -> productIntegration.restoreStock(List.of()));
    }

    @Test
    @DisplayName("PaymentIntegrationService delegates to Feign client and tests fallback methods")
    void testPaymentIntegrationService() {
        PaymentIntegrationService paymentIntegration = new PaymentIntegrationService(paymentServiceClient);

        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PaymentRequestDTO request = new PaymentRequestDTO(orderId, userId, BigDecimal.valueOf(250));
        PaymentResponseDTO response = new PaymentResponseDTO(UUID.randomUUID(), "PENDING", "http://pay.url");

        when(paymentServiceClient.initPayment(request)).thenReturn(response);
        assertEquals(response, paymentIntegration.initPayment(request));

        paymentIntegration.refundPayment(orderId, BigDecimal.valueOf(250));
        verify(paymentServiceClient).refundPayment(orderId, BigDecimal.valueOf(250), "");

        // Fallbacks
        Throwable error = new RuntimeException("Service down");
        assertThrows(BusinessException.class, () -> paymentIntegration.initPaymentFallback(request, error));
        assertThrows(BusinessException.class, () -> paymentIntegration.refundPaymentFallback(orderId, BigDecimal.valueOf(250), error));
    }
}
