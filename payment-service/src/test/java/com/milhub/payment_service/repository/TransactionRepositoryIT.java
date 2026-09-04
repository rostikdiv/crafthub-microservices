package com.milhub.payment_service.repository;

import com.milhub.payment_service.entity.Transaction;
import com.milhub.payment_service.entity.TransactionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true"
})
class TransactionRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    @DisplayName("Should save and find transaction by order ID with timestamps and initial version")
    void testSaveAndFindByOrderId() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("1250.50");
        String idempotencyKey = "idem-" + UUID.randomUUID();

        Transaction transaction = Transaction.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(amount)
                .status(TransactionStatus.PENDING)
                .provider("LIQPAY")
                .idempotencyKey(idempotencyKey)
                .build();

        Transaction saved = transactionRepository.saveAndFlush(transaction);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getVersion()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        Optional<Transaction> fetchedOpt = transactionRepository.findByOrderId(orderId);
        assertThat(fetchedOpt).isPresent();

        Transaction fetched = fetchedOpt.get();
        assertThat(fetched.getOrderId()).isEqualTo(orderId);
        assertThat(fetched.getUserId()).isEqualTo(userId);
        assertThat(fetched.getAmount()).isEqualByComparingTo("1250.50");
        assertThat(fetched.getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(fetched.getProvider()).isEqualTo("LIQPAY");
        assertThat(fetched.getIdempotencyKey()).isEqualTo(idempotencyKey);
    }

    @Test
    @DisplayName("Should retrieve transaction by idempotency key")
    void testFindByIdempotencyKey() {
        UUID orderId = UUID.randomUUID();
        String idempotencyKey = "key-checkout-safe-456";

        Transaction transaction = Transaction.builder()
                .orderId(orderId)
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("750.00"))
                .status(TransactionStatus.SUCCESS)
                .provider("STRIPE")
                .idempotencyKey(idempotencyKey)
                .build();

        transactionRepository.saveAndFlush(transaction);

        Optional<Transaction> found = transactionRepository.findByIdempotencyKey(idempotencyKey);
        assertThat(found).isPresent();
        assertThat(found.get().getOrderId()).isEqualTo(orderId);
        assertThat(found.get().getStatus()).isEqualTo(TransactionStatus.SUCCESS);
    }

    @Test
    @DisplayName("Should throw DataIntegrityViolationException on duplicate idempotency key (PostgreSQL constraint)")
    void testDuplicateIdempotencyKeyThrowsException() {
        String sharedKey = "shared-duplicate-key-" + UUID.randomUUID();

        Transaction tx1 = Transaction.builder()
                .orderId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .status(TransactionStatus.SUCCESS)
                .provider("MOCK_PAY")
                .idempotencyKey(sharedKey)
                .build();

        transactionRepository.saveAndFlush(tx1);

        Transaction tx2 = Transaction.builder()
                .orderId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("200.00"))
                .status(TransactionStatus.PENDING)
                .provider("MOCK_PAY")
                .idempotencyKey(sharedKey)
                .build();

        assertThatThrownBy(() -> transactionRepository.saveAndFlush(tx2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should increment version upon updating transaction (Optimistic Locking)")
    void testOptimisticLockingVersionIncrement() {
        Transaction tx = Transaction.builder()
                .orderId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("500.00"))
                .status(TransactionStatus.PENDING)
                .provider("LIQPAY")
                .idempotencyKey("idem-version-" + UUID.randomUUID())
                .build();

        Transaction saved = transactionRepository.saveAndFlush(tx);
        Long initialVersion = saved.getVersion();

        // Update status to SUCCESS
        saved.setStatus(TransactionStatus.SUCCESS);
        Transaction updated = transactionRepository.saveAndFlush(saved);

        assertThat(updated.getVersion()).isGreaterThan(initialVersion);
        assertThat(updated.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
    }

    @Test
    @DisplayName("Should update transaction status from SUCCESS to REFUNDED")
    void testRefundStatusTransition() {
        UUID orderId = UUID.randomUUID();
        Transaction tx = Transaction.builder()
                .orderId(orderId)
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("3500.00"))
                .status(TransactionStatus.SUCCESS)
                .provider("LIQPAY")
                .idempotencyKey("idem-refund-" + UUID.randomUUID())
                .build();

        Transaction saved = transactionRepository.saveAndFlush(tx);
        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.SUCCESS);

        saved.setStatus(TransactionStatus.REFUNDED);
        transactionRepository.saveAndFlush(saved);

        Transaction refreshed = transactionRepository.findByOrderId(orderId).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo(TransactionStatus.REFUNDED);
    }
}
