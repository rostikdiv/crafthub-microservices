package com.milhub.payment_service.service;

import com.milhub.payment_service.dto.TransactionDTO;
import com.milhub.payment_service.dto.payment.PaymentRequestDTO;
import com.milhub.payment_service.dto.payment.PaymentResponseDTO;
import com.milhub.payment_service.dto.payment.PaymentSuccessEventDTO;
import com.milhub.payment_service.entity.Transaction;
import com.milhub.payment_service.entity.TransactionStatus;
import com.milhub.payment_service.exception.ResourceNotFoundException;
import com.milhub.payment_service.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Unit Tests")
class PaymentServiceUnitTest {

    @Mock
    private TransactionRepository repository;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    private UUID transactionId;
    private UUID orderId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "baseUrl", "https://milhub.ua");
        transactionId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void testGetTransactionByOrderId_Success() {
        Transaction tx = Transaction.builder()
                .id(transactionId)
                .orderId(orderId)
                .userId(userId)
                .amount(BigDecimal.valueOf(150))
                .status(TransactionStatus.SUCCESS)
                .build();

        when(repository.findByOrderId(orderId)).thenReturn(Optional.of(tx));

        PaymentResponseDTO response = paymentService.getTransactionByOrderId(orderId);

        assertNotNull(response);
        assertEquals(transactionId, response.transactionId());
        assertEquals("SUCCESS", response.status());
        assertTrue(response.paymentUrl().contains(transactionId.toString()));
    }

    @Test
    void testGetTransactionByOrderId_NotFound_ThrowsException() {
        when(repository.findByOrderId(orderId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.getTransactionByOrderId(orderId));
    }

    @Test
    void testInitPayment_WithExistingIdempotencyKey_ReturnsExisting() {
        String idemKey = "idem-key-100";
        PaymentRequestDTO request = new PaymentRequestDTO(orderId, userId, BigDecimal.valueOf(200), idemKey);

        Transaction existingTx = Transaction.builder()
                .id(transactionId)
                .orderId(orderId)
                .userId(userId)
                .amount(BigDecimal.valueOf(200))
                .status(TransactionStatus.PENDING)
                .idempotencyKey(idemKey)
                .build();

        when(repository.findByIdempotencyKey(idemKey)).thenReturn(Optional.of(existingTx));

        PaymentResponseDTO response = paymentService.initPayment(request);

        assertNotNull(response);
        assertEquals(transactionId, response.transactionId());
        assertEquals("PENDING", response.status());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void testInitPayment_NewPayment_Success() {
        String idemKey = "idem-key-200";
        PaymentRequestDTO request = new PaymentRequestDTO(orderId, userId, BigDecimal.valueOf(300), idemKey);

        when(repository.findByIdempotencyKey(idemKey)).thenReturn(Optional.empty());
        when(repository.findByOrderId(orderId)).thenReturn(Optional.of(mock(Transaction.class))); // covers warning branch
        when(repository.saveAndFlush(any(Transaction.class))).thenAnswer(i -> {
            Transaction tx = i.getArgument(0);
            ReflectionTestUtils.setField(tx, "id", transactionId);
            return tx;
        });

        PaymentResponseDTO response = paymentService.initPayment(request);

        assertNotNull(response);
        assertEquals(transactionId, response.transactionId());
        assertEquals("PENDING", response.status());
        verify(repository, times(1)).saveAndFlush(any(Transaction.class));
    }

    @Test
    void testInitPayment_NullIdempotencyKey_Success() {
        PaymentRequestDTO request = new PaymentRequestDTO(orderId, userId, BigDecimal.valueOf(150), null);

        when(repository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(Transaction.class))).thenAnswer(i -> {
            Transaction tx = i.getArgument(0);
            ReflectionTestUtils.setField(tx, "id", transactionId);
            return tx;
        });

        PaymentResponseDTO response = paymentService.initPayment(request);

        assertNotNull(response);
        assertEquals(transactionId, response.transactionId());
        assertEquals("PENDING", response.status());
        verify(repository, times(1)).saveAndFlush(any(Transaction.class));
    }

    @Test
    void testInitPayment_ConcurrentDataIntegrityViolation_SuccessRecovery() {
        String idemKey = "idem-key-concurrent";
        PaymentRequestDTO request = new PaymentRequestDTO(orderId, userId, BigDecimal.valueOf(400), idemKey);

        when(repository.findByIdempotencyKey(idemKey)).thenReturn(Optional.empty());
        when(repository.findByOrderId(orderId)).thenReturn(Optional.empty());
        doThrow(new DataIntegrityViolationException("Duplicate key"))
                .when(repository).saveAndFlush(any(Transaction.class));

        Transaction concurrentTx = Transaction.builder()
                .id(transactionId)
                .orderId(orderId)
                .userId(userId)
                .status(TransactionStatus.PENDING)
                .build();

        // Second lookup for idempotency key succeeds
        when(repository.findByIdempotencyKey(idemKey)).thenReturn(Optional.empty()).thenReturn(Optional.of(concurrentTx));

        PaymentResponseDTO response = paymentService.initPayment(request);

        assertNotNull(response);
        assertEquals(transactionId, response.transactionId());
    }

    @Test
    void testInitPayment_ConcurrentDataIntegrityViolation_RecoveryFails_ThrowsIllegalState() {
        String idemKey = "idem-key-fail";
        PaymentRequestDTO request = new PaymentRequestDTO(orderId, userId, BigDecimal.valueOf(400), idemKey);

        when(repository.findByIdempotencyKey(idemKey)).thenReturn(Optional.empty());
        when(repository.findByOrderId(orderId)).thenReturn(Optional.empty());
        doThrow(new DataIntegrityViolationException("Duplicate key"))
                .when(repository).saveAndFlush(any(Transaction.class));

        assertThrows(IllegalStateException.class, () -> paymentService.initPayment(request));
    }

    @Test
    void testProcessWebhook_TransactionNotFound_ThrowsException() {
        when(repository.findById(transactionId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                paymentService.processWebhook(transactionId, "SUCCESS"));
    }

    @Test
    void testProcessWebhook_AlreadyProcessed_ReturnsEarly() {
        Transaction tx = Transaction.builder()
                .id(transactionId)
                .status(TransactionStatus.SUCCESS)
                .build();

        when(repository.findById(transactionId)).thenReturn(Optional.of(tx));

        paymentService.processWebhook(transactionId, "SUCCESS");

        verify(repository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void testProcessWebhook_SuccessStatus() {
        Transaction tx = Transaction.builder()
                .id(transactionId)
                .orderId(orderId)
                .amount(BigDecimal.valueOf(250))
                .status(TransactionStatus.PENDING)
                .build();

        when(repository.findById(transactionId)).thenReturn(Optional.of(tx));

        paymentService.processWebhook(transactionId, "success"); // test case-insensitivity

        assertEquals(TransactionStatus.SUCCESS, tx.getStatus());
        verify(repository, times(1)).save(tx);
        verify(eventPublisher, times(1)).publishEvent(any(PaymentSuccessEventDTO.class));
    }

    @Test
    void testProcessWebhook_FailedStatus() {
        Transaction tx = Transaction.builder()
                .id(transactionId)
                .orderId(orderId)
                .amount(BigDecimal.valueOf(250))
                .status(TransactionStatus.PENDING)
                .build();

        when(repository.findById(transactionId)).thenReturn(Optional.of(tx));

        paymentService.processWebhook(transactionId, "FAILED");

        assertEquals(TransactionStatus.FAILED, tx.getStatus());
        verify(repository, times(1)).save(tx);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void testHandlePaymentSuccessEvent() {
        PaymentSuccessEventDTO event = new PaymentSuccessEventDTO(orderId, "user@milhub.ua", BigDecimal.valueOf(500));

        paymentService.handlePaymentSuccessEvent(event);

        verify(kafkaProducerService, times(1)).sendPaymentSuccessEvent(event);
    }

    @Test
    void testRefundPayment_Success() {
        Transaction originalTx = Transaction.builder()
                .id(transactionId)
                .orderId(orderId)
                .userId(userId)
                .amount(BigDecimal.valueOf(300))
                .status(TransactionStatus.SUCCESS)
                .build();

        when(repository.findByOrderId(orderId)).thenReturn(Optional.of(originalTx));

        paymentService.refundPayment(orderId, BigDecimal.valueOf(300));

        verify(repository, times(1)).save(argThat(tx ->
                tx.getOrderId().equals(orderId) &&
                tx.getUserId().equals(userId) &&
                tx.getStatus() == TransactionStatus.REFUNDED &&
                tx.getAmount().equals(BigDecimal.valueOf(-300)) &&
                tx.getIdempotencyKey().equals("REFUND_" + orderId)
        ));
    }

    @Test
    void testRefundPayment_OriginalTxNotFound_ThrowsException() {
        when(repository.findByOrderId(orderId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                paymentService.refundPayment(orderId, BigDecimal.valueOf(100)));
    }

    @Test
    void testGetAllTransactions() {
        Transaction tx1 = Transaction.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .userId(userId)
                .amount(BigDecimal.valueOf(100))
                .status(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        when(repository.findAll()).thenReturn(List.of(tx1));

        List<TransactionDTO> list = paymentService.getAllTransactions();

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals(tx1.getId(), list.get(0).id());
        assertEquals(tx1.getOrderId(), list.get(0).orderId());
        assertEquals("SUCCESS", list.get(0).status());
    }
}
