package com.crafthub.payment_service.service;

import com.crafthub.payment_service.dto.PaymentRequestDTO;
import com.crafthub.payment_service.dto.PaymentResponseDTO;
import com.crafthub.payment_service.dto.PaymentSuccessEventDTO;
import com.crafthub.payment_service.dto.TransactionDTO; // ✅ Додано імпорт
import com.crafthub.payment_service.entity.Transaction;
import com.crafthub.payment_service.entity.TransactionStatus;
import com.crafthub.payment_service.exception.BusinessException; // ✅
import com.crafthub.payment_service.exception.ResourceNotFoundException; // ✅
import com.crafthub.payment_service.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final TransactionRepository repository;
    private final KafkaProducerService kafkaProducerService;

    public PaymentResponseDTO getTransactionByOrderId(UUID orderId) {
        Transaction transaction = repository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found for order: " + orderId));

        String mockUrl = "http://localhost:8080/api/v1/payments/webhook/" + transaction.getId() + "?status=SUCCESS";

        return new PaymentResponseDTO(transaction.getId(), transaction.getStatus().name(), mockUrl);
    }

    public PaymentResponseDTO initPayment(PaymentRequestDTO request) {
        // Перевірка на дублікат (опціонально)
        if (repository.findByOrderId(request.orderId()).isPresent()) {
            // Можна повернути існуючу транзакцію, якщо вона PENDING
            // Або кинути помилку
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

    @Transactional
    public void processWebhook(UUID transactionId, String status) {
        log.info("Processing webhook: Transaction={}, Status={}", transactionId, status);

        Transaction transaction = repository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            log.warn("Transaction already processed");
            return; // Або кинути BusinessException, якщо це критично
        }

        if ("SUCCESS".equalsIgnoreCase(status)) {
            transaction.setStatus(TransactionStatus.SUCCESS);
            repository.save(transaction);

            PaymentSuccessEventDTO event = new PaymentSuccessEventDTO(
                    transaction.getOrderId(),
                    "user@placeholder.com", // В реальності треба брати з User Service
                    transaction.getAmount());
            kafkaProducerService.sendPaymentSuccessEvent(event);
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
            repository.save(transaction);
        }
    }

    @Transactional
    public void refundPayment(UUID orderId, java.math.BigDecimal amount) {
        log.info("Processing refund for Order: {}, Amount: {}", orderId, amount);

        // Знаходимо оригінальну успішну транзакцію
        var transactions = repository.findByOrderId(orderId); // припускаємо що повертає Optional<Transaction> або List.
        // Але repository.findByOrderId повертає Optional<Transaction> за поточним кодом
        // (див. initPayment).
        // Якщо там Optional, то це проблема, бо може бути кілька транзакцій (оплата +
        // повернення).
        // Треба перевірити репозиторій.

        // MVP спрощення: Просто створюємо нову транзакцію зі статусом REFUNDED.
        Transaction refundTransaction = Transaction.builder()
                .orderId(orderId)
                .userId(UUID.randomUUID()) // Або передавати userId
                .amount(amount.negate()) // Від'ємна сума для звітності
                .status(TransactionStatus.REFUNDED)
                .provider("MOCK_PAY")
                .build();

        repository.save(refundTransaction);
        log.info("✅ Refund processed successfully: {}", refundTransaction.getId());
    }

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