package com.milhub.order_service.client;

import com.milhub.order_service.dto.payment.PaymentRequestDTO;
import com.milhub.order_service.dto.payment.PaymentResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Feign client for the Payment Service.
 */
@FeignClient(name = "payment-service", url = "${PAYMENT_SERVICE_URL:}")
public interface PaymentServiceClient {

    @PostMapping("/api/v1/payments/init")
    PaymentResponseDTO initPayment(@RequestBody PaymentRequestDTO request);

    @PostMapping("/api/v1/payments/refund")
    String refundPayment(@RequestParam("orderId") UUID orderId,
            @RequestParam("amount") BigDecimal amount);
}