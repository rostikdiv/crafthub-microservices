package com.crafthub.payment_service.controller;

import com.crafthub.payment_service.dto.PaymentRequestDTO;
import com.crafthub.payment_service.dto.PaymentResponseDTO;
import com.crafthub.payment_service.dto.TransactionDTO;
import com.crafthub.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // Ініціювати платіж може тільки авторизований користувач (Service або User)
    // Оскільки OrderService викликає це через Feign і прокидає headers, це
    // працюватиме.
    @PostMapping("/init")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponseDTO> initPayment(@RequestBody PaymentRequestDTO request) {
        return ResponseEntity.ok(paymentService.initPayment(request));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponseDTO> getTransactionByOrderId(@PathVariable UUID orderId) {
        return ResponseEntity.ok(paymentService.getTransactionByOrderId(orderId));
    }

    // Webhook залишаємо відкритим (SecurityConfig.permitAll), або додаємо перевірку
    // підпису
    @PostMapping("/webhook/{transactionId}")
    public ResponseEntity<String> mockWebhook(
            @PathVariable UUID transactionId,
            @RequestParam String status) {
        paymentService.processWebhook(transactionId, status);
        return ResponseEntity.ok("Processed status: " + status);
    }

    // Перегляд усіх транзакцій - тільки для АДМІНА
    // (Покупець не повинен бачити чужі платежі)
    // Повернення коштів (Тільки внутрішній виклик або адмін)
    @PostMapping("/refund")
    @PreAuthorize("hasAuthority('order:update:status')")
    public ResponseEntity<String> refundPayment(
            @RequestParam UUID orderId,
            @RequestParam java.math.BigDecimal amount) {
        paymentService.refundPayment(orderId, amount);
        return ResponseEntity.ok("Refund processed");
    }

    @GetMapping
    @PreAuthorize("hasAuthority('order:read:all')") // Використаємо існуюче право адміна
    public ResponseEntity<List<TransactionDTO>> getAllTransactions() {
        return ResponseEntity.ok(paymentService.getAllTransactions());
    }
}