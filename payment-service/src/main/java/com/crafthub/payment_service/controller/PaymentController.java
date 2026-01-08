package com.crafthub.payment_service.controller;

import com.crafthub.payment_service.dto.PaymentRequestDTO;
import com.crafthub.payment_service.dto.PaymentResponseDTO;
import com.crafthub.payment_service.dto.TransactionDTO;
import com.crafthub.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/init")
    public ResponseEntity<PaymentResponseDTO> initPayment(@RequestBody PaymentRequestDTO request) {
        return ResponseEntity.ok(paymentService.initPayment(request));
    }

    @PostMapping("/webhook/{transactionId}")
    public ResponseEntity<String> mockWebhook(
            @PathVariable UUID transactionId,
            @RequestParam String status
    ) {
        paymentService.processWebhook(transactionId, status);
        return ResponseEntity.ok("Processed status: " + status);
    }
    @GetMapping
    public ResponseEntity<List<TransactionDTO>> getAllTransactions() {
        return ResponseEntity.ok(paymentService.getAllTransactions());
    }

}