package com.milhub.payment_service.service;

import com.milhub.payment_service.dto.payment.PaymentRequestDTO;
import com.milhub.payment_service.dto.payment.PaymentResponseDTO;
import com.milhub.payment_service.entity.Transaction;
import com.milhub.payment_service.entity.TransactionStatus;
import com.milhub.payment_service.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PaymentServiceTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private TransactionRepository transactionRepository;

    @MockBean
    private KafkaProducerService kafkaProducerService;

    private UUID transactionId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        orderId = UUID.randomUUID();
        PaymentRequestDTO request = new PaymentRequestDTO(
                orderId,
                UUID.randomUUID(),
                BigDecimal.valueOf(100.00),
                "idem-key-" + UUID.randomUUID()
        );
        PaymentResponseDTO response = paymentService.initPayment(request);
        transactionId = response.transactionId();
    }

    @Test
    void testConcurrentWebhookProcessing_PreventsDoubleExecution() throws InterruptedException {
        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    latch.await();
                    paymentService.processWebhook(transactionId, "SUCCESS");
                } catch (Exception e) {
                    System.err.println("Thread failed: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        latch.countDown();
        doneLatch.await();
        executorService.shutdown();

        // Verify that the transaction is SUCCESS
        Transaction transaction = transactionRepository.findById(transactionId).orElseThrow();
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.SUCCESS);

        // Verify that the success event was sent EXACTLY ONCE despite 5 threads attempting it
        verify(kafkaProducerService, times(1)).sendPaymentSuccessEvent(any());
    }
}
