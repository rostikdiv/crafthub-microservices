package com.crafthub.payment_service.service;

import com.crafthub.payment_service.dto.PaymentRequestDTO;
import com.crafthub.payment_service.dto.PaymentResponseDTO;
import com.crafthub.payment_service.dto.PaymentSuccessEventDTO;
import com.crafthub.payment_service.entity.Transaction;
import com.crafthub.payment_service.entity.TransactionStatus;
import com.crafthub.payment_service.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final TransactionRepository repository;
    private final KafkaProducerService kafkaProducerService;

    public PaymentResponseDTO initPayment(PaymentRequestDTO request) {
        log.info("Creating payment transaction for Order: {}", request.orderId());

        Transaction transaction = Transaction.builder()
                .orderId(request.orderId())
                .userId(request.userId())
                .amount(request.amount())
                .status(TransactionStatus.PENDING)
                .provider("MOCK_PAY")
                .build();

        repository.save(transaction);

        // Генеруємо посилання на наш власний Webhook для тесту
        String mockUrl = "http://localhost:8086/api/v1/payments/webhook/" + transaction.getId() + "?status=SUCCESS";

        return new PaymentResponseDTO(transaction.getId(), "PENDING", mockUrl);
    }

    @Transactional
    public void processWebhook(UUID transactionId, String status) {
        log.info("Processing webhook: Transaction={}, Status={}", transactionId, status);

        Transaction transaction = repository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            log.warn("Transaction already processed");
            return;
        }

        if ("SUCCESS".equalsIgnoreCase(status)) {
            transaction.setStatus(TransactionStatus.SUCCESS);
            repository.save(transaction);

            PaymentSuccessEventDTO event = new PaymentSuccessEventDTO(
                    transaction.getOrderId(),
                    "user@placeholder.com",
                    transaction.getAmount()
            );
            kafkaProducerService.sendPaymentSuccessEvent(event);
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
            repository.save(transaction);
        }
    }
    // Метод для отримання історії
    public java.util.List<com.crafthub.payment_service.dto.TransactionDTO> getAllTransactions() {
        return repository.findAll().stream()
                .map(tx -> new com.crafthub.payment_service.dto.TransactionDTO(
                        tx.getId(),
                        tx.getOrderId(),
                        tx.getUserId(),
                        tx.getAmount(),
                        tx.getStatus().name(),
                        tx.getCreatedAt()
                ))
                .toList();
    }
}