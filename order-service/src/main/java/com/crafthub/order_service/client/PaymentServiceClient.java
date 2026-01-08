package com.crafthub.order_service.client;

import com.crafthub.order_service.dto.payment.PaymentRequestDTO;
import com.crafthub.order_service.dto.payment.PaymentResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// name = назва сервісу в Eureka (або docker-compose service name)
@FeignClient(name = "payment-service", url = "${application.config.payment-url:http://localhost:8086}")
public interface PaymentServiceClient {

    @PostMapping("/api/v1/payments/init")
    PaymentResponseDTO initPayment(@RequestBody PaymentRequestDTO request);
}