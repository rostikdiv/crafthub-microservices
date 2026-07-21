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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

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
    private final ApplicationEventPublisher eventPublisher;

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
        if (request.idempotencyKey() != null) {
            var existingTx = repository.findByIdempotencyKey(request.idempotencyKey());
            if (existingTx.isPresent()) {
                log.info("Idempotent request: returning existing transaction for key {}", request.idempotencyKey());
                Transaction tx = existingTx.get();
                String url = "http://localhost:8086/api/v1/payments/webhook/" + tx.getId() + "?status=SUCCESS";
                return new PaymentResponseDTO(tx.getId(), tx.getStatus().name(), url);
            }
        }

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
                .idempotencyKey(request.idempotencyKey())
                .build();

        try {
            repository.saveAndFlush(transaction);
        } catch (DataIntegrityViolationException e) {
            log.warn("Concurrent payment initialization detected for key: {}", request.idempotencyKey());
            Transaction tx = repository.findByIdempotencyKey(request.idempotencyKey())
                    .orElseThrow(() -> new IllegalStateException("Failed to retrieve idempotency key"));
            String url = "http://localhost:8086/api/v1/payments/webhook/" + tx.getId() + "?status=SUCCESS";
            return new PaymentResponseDTO(tx.getId(), tx.getStatus().name(), url);
        }

        String mockUrl = "http://localhost:8086/api/v1/payments/webhook/" + transaction.getId() + "?status=SUCCESS";

        return new PaymentResponseDTO(transaction.getId(), "PENDING", mockUrl);
    }

    /**
     * Processes payment status updates from external webhooks.
     * Triggers a success event via Kafka if the payment is confirmed.
     */
    @Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 5, backoff = @org.springframework.retry.annotation.Backoff(delay = 100, maxDelay = 500, random = true))
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
            eventPublisher.publishEvent(event);
            log.info("✅ Payment confirmed for transaction {}", transactionId);
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
            repository.save(transaction);
            log.warn("❌ Payment failed for transaction {}", transactionId);
        }
    }

    /**
     * Listener that sends the Kafka event ONLY after the transaction is successfully committed.
     * This prevents duplicate Kafka events if OptimisticLockingFailureException causes a retry.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentSuccessEvent(PaymentSuccessEventDTO event) {
        kafkaProducerService.sendPaymentSuccessEvent(event);
    }

    /**
     * Initiates a refund for an order.
     * Records a new transaction entry with negative amount and REFUNDED status.
     */
    @Transactional
    public void refundPayment(UUID orderId, BigDecimal amount) {
        log.info("Processing refund for Order: {}, Amount: {}", orderId, amount);

        Transaction originalTx = repository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found for order: " + orderId));

        Transaction refundTransaction = Transaction.builder()
                .orderId(orderId)
                .userId(originalTx.getUserId())
                .amount(amount.negate())
                .status(TransactionStatus.REFUNDED)
                .provider("MOCK_PAY")
                .idempotencyKey("REFUND_" + orderId)
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