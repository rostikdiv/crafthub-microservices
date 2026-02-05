package com.crafthub.order_service.service;

import com.crafthub.order_service.client.PaymentServiceClient;
import com.crafthub.order_service.dto.external.ProductResponseDTO;
import com.crafthub.order_service.dto.payment.PaymentRequestDTO;
import com.crafthub.order_service.dto.payment.PaymentResponseDTO;
import com.crafthub.order_service.exception.BusinessException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentIntegrationService {

    private final PaymentServiceClient paymentServiceClient;

    @CircuitBreaker(name = "paymentService", fallbackMethod = "initPaymentFallback")
    public PaymentResponseDTO initPayment(PaymentRequestDTO request) {
        return paymentServiceClient.initPayment(request);
    }

    public PaymentResponseDTO initPaymentFallback(PaymentRequestDTO request, Throwable t) {
        log.error("Payment service is unavailable for user {}", request.userId());
        throw new BusinessException("Неможливо провести оплату сервіс не доступний.");
    }

    @CircuitBreaker(name = "paymentService", fallbackMethod = "refundPaymentFallback")
    public void refundPayment(UUID orderId, java.math.BigDecimal amount) {
        paymentServiceClient.refundPayment(orderId, amount);
    }

    public void refundPaymentFallback(UUID orderId, java.math.BigDecimal amount, Throwable t) {
        log.error("Payment service unavailable during refund for order {}", orderId, t);
        throw new BusinessException("Неможливо оформити повернення коштів. Сервіс оплати недоступний.");
    }
}
