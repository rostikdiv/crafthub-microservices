package com.milhub.payment_service.controller;

import com.milhub.payment_service.dto.payment.PaymentRequestDTO;
import com.milhub.payment_service.dto.TransactionDTO;
import com.milhub.payment_service.dto.payment.PaymentResponseDTO;
import com.milhub.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

/**
 * REST controller for managing financial transactions and payments.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Initializes a payment session for a specific order.
     *
     * @param request The payment initialization details.
     * @return Response containing the payment details and status.
     */
    @PostMapping("/init")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponseDTO> initPayment(@RequestBody PaymentRequestDTO request) {
        return ResponseEntity.ok(paymentService.initPayment(request));
    }

    /**
     * Retrieves payment transaction details for a specific order.
     *
     * @param orderId The unique identifier of the order.
     * @return Response containing the transaction status.
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponseDTO> getTransactionByOrderId(@PathVariable UUID orderId) {
        return ResponseEntity.ok(paymentService.getTransactionByOrderId(orderId));
    }

    /**
     * Webhook endpoint for receiving status updates from external payment
     * providers.
     *
     * @param transactionId The unique identifier of the transaction.
     * @param status        The updated status of the payment (e.g., SUCCESS,
     *                      FAILED).
     * @return Confirmation of processed status.
     */
    @PostMapping("/webhook/{transactionId}")
    public ResponseEntity<String> mockWebhook(
            @PathVariable UUID transactionId,
            @RequestParam String status) {
        paymentService.processWebhook(transactionId, status);
        return ResponseEntity.ok("Processed status: " + status);
    }

    /**
     * Processes a full or partial refund for a specific order.
     * Restricted to users with administrative order update permissions.
     *
     * @param orderId The unique identifier of the order.
     * @param amount  The amount to be refunded.
     * @return Confirmation of refund processing.
     */
    @PostMapping("/refund")
    @PreAuthorize("hasAuthority('order:update:status')")
    public ResponseEntity<String> refundPayment(
            @RequestParam UUID orderId,
            @RequestParam BigDecimal amount) {
        paymentService.refundPayment(orderId, amount);
        return ResponseEntity.ok("Refund processed");
    }

    /**
     * Retrieves all recorded transactions across the system.
     * Restricted to users with administrative order read permissions.
     *
     * @return A list of all transactions.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('order:read:all')")
    public ResponseEntity<List<TransactionDTO>> getAllTransactions() {
        return ResponseEntity.ok(paymentService.getAllTransactions());
    }
}