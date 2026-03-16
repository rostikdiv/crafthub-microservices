package com.crafthub.payment_service.service;

import com.crafthub.payment_service.dto.payment.PaymentRequestDTO;
import com.crafthub.payment_service.dto.payment.PaymentResponseDTO;
import com.crafthub.payment_service.dto.payment.PaymentSuccessEventDTO;
import com.crafthub.payment_service.dto.TransactionDTO;
import com.crafthub.payment_service.entity.Transaction;
import com.crafthub.payment_service.entity.TransactionStatus;
import com.crafthub.payment_service.exception.ResourceNotFoundException;
import com.crafthub.payment_service.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

/**
 * Service for managing payment transactions, processing webhooks, and handling
 * refunds.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final TransactionRepository repository;
    private final KafkaProducerService kafkaProducerService;

    /**
     * Retrieves a payment transaction for a specific order.
     * Generates a mock webhook URL for manual simulation/testing.
     */
    public PaymentResponseDTO getTransactionByOrderId(UUID orderId) {
        Transaction transaction = repository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found for order: " + orderId));

        String mockUrl = "http://localhost:8080/api/v1/payments/webhook/" + transaction.getId() + "?status=SUCCESS";

        return new PaymentResponseDTO(transaction.getId(), transaction.getStatus().name(), mockUrl);
    }

    /**
     * Initializes a new payment transaction.
     * Starts with PENDING status and provides a mock payment URL.
     */
    public PaymentResponseDTO initPayment(PaymentRequestDTO request) {
        if (repository.findByOrderId(request.orderId()).isPresent()) {
            log.warn("Transaction for order {} already exists", request.orderId());
        }

        log.info("Creating payment transaction for Order: {}", request.orderId());

        Transaction transaction = Transaction.builder()
                .orderId(request.orderId())
                .userId(request.userId())
                .amount(request.amount())
                .status(TransactionStatus.PENDING)
                .provider("MOCK_PAY")
                .build();

        repository.save(transaction);

        String mockUrl = "http://localhost:8086/api/v1/payments/webhook/" + transaction.getId() + "?status=SUCCESS";

        return new PaymentResponseDTO(transaction.getId(), "PENDING", mockUrl);
    }

    /**
     * Processes payment status updates from external webhooks.
     * Triggers a success event via Kafka if the payment is confirmed.
     */
    @Transactional
    public void processWebhook(UUID transactionId, String status) {
        log.info("Processing webhook: Transaction={}, Status={}", transactionId, status);

        Transaction transaction = repository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            log.warn("Transaction ID {} has already been processed with status: {}", transactionId,
                    transaction.getStatus());
            return;
        }

        if ("SUCCESS".equalsIgnoreCase(status)) {
            transaction.setStatus(TransactionStatus.SUCCESS);
            repository.save(transaction);

            PaymentSuccessEventDTO event = new PaymentSuccessEventDTO(
                    transaction.getOrderId(),
                    "user@placeholder.com", // Fetch from context or User Service in production
                    transaction.getAmount());
            kafkaProducerService.sendPaymentSuccessEvent(event);
            log.info("✅ Payment confirmed for transaction {}", transactionId);
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
            repository.save(transaction);
            log.warn("❌ Payment failed for transaction {}", transactionId);
        }
    }

    /**
     * Initiates a refund for an order.
     * Records a new transaction entry with negative amount and REFUNDED status.
     */
    @Transactional
    public void refundPayment(UUID orderId, BigDecimal amount) {
        log.info("Processing refund for Order: {}, Amount: {}", orderId, amount);

        Transaction refundTransaction = Transaction.builder()
                .orderId(orderId)
                .userId(UUID.randomUUID()) // Placeholder userId
                .amount(amount.negate())
                .status(TransactionStatus.REFUNDED)
                .provider("MOCK_PAY")
                .build();

        repository.save(refundTransaction);
        log.info("✅ Refund processed successfully for transaction {}", refundTransaction.getId());
    }

    /**
     * Retrieves all transaction records from the system.
     */
    public List<TransactionDTO> getAllTransactions() {
        return repository.findAll().stream()
                .map(tx -> new TransactionDTO(
                        tx.getId(),
                        tx.getOrderId(),
                        tx.getUserId(),
                        tx.getAmount(),
                        tx.getStatus().name(),
                        tx.getCreatedAt()))
                .toList();
    }
}