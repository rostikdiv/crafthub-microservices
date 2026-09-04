package com.milhub.payment_service.entity;

import com.milhub.payment_service.PaymentServiceApplication;
import com.milhub.payment_service.dto.ErrorResponse;
import com.milhub.payment_service.dto.TransactionDTO;
import com.milhub.payment_service.dto.payment.PaymentRequestDTO;
import com.milhub.payment_service.dto.payment.PaymentResponseDTO;
import com.milhub.payment_service.dto.payment.PaymentSuccessEventDTO;
import com.milhub.payment_service.exception.AppException;
import com.milhub.payment_service.exception.BusinessException;
import com.milhub.payment_service.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Payment Entity & DTO Branch Coverage Tests")
class PaymentEntityBranchTest {

    @Test
    void testTransactionEqualsAndHashCode() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        Transaction t1 = Transaction.builder().id(id1).orderId(UUID.randomUUID()).build();
        Transaction t2 = Transaction.builder().id(id1).orderId(UUID.randomUUID()).build();
        Transaction t3 = Transaction.builder().id(id2).build();
        Transaction tNull1 = Transaction.builder().id(null).build();
        Transaction tNull2 = Transaction.builder().id(null).build();

        // Reflexive
        assertTrue(t1.equals(t1));
        // Symmetric
        assertTrue(t1.equals(t2));
        assertTrue(t2.equals(t1));
        // Different ID
        assertFalse(t1.equals(t3));
        // Null & different class
        assertFalse(t1.equals(null));
        assertFalse(t1.equals("Not a transaction"));
        // Null IDs
        assertFalse(tNull1.equals(t1));
        assertFalse(tNull1.equals(tNull2));

        // HashCode
        assertEquals(t1.hashCode(), t2.hashCode());
        assertEquals(Transaction.class.hashCode(), t1.hashCode());

        // ToString
        assertNotNull(t1.toString());
        assertTrue(t1.toString().contains(id1.toString()));
    }

    @Test
    void testTransactionGettersAndSetters() {
        UUID id = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Transaction tx = new Transaction();
        tx.setId(id);
        tx.setOrderId(orderId);
        tx.setUserId(userId);
        tx.setAmount(BigDecimal.valueOf(99.99));
        tx.setStatus(TransactionStatus.PENDING);
        tx.setProvider("STRIPE");
        tx.setIdempotencyKey("idem-123");
        tx.setVersion(1L);
        tx.setCreatedAt(now);
        tx.setUpdatedAt(now);

        assertEquals(id, tx.getId());
        assertEquals(orderId, tx.getOrderId());
        assertEquals(userId, tx.getUserId());
        assertEquals(BigDecimal.valueOf(99.99), tx.getAmount());
        assertEquals(TransactionStatus.PENDING, tx.getStatus());
        assertEquals("STRIPE", tx.getProvider());
        assertEquals("idem-123", tx.getIdempotencyKey());
        assertEquals(1L, tx.getVersion());
        assertEquals(now, tx.getCreatedAt());
        assertEquals(now, tx.getUpdatedAt());
    }

    @Test
    void testTransactionStatusEnum() {
        assertEquals(4, TransactionStatus.values().length);
        assertEquals(TransactionStatus.PENDING, TransactionStatus.valueOf("PENDING"));
        assertEquals(TransactionStatus.SUCCESS, TransactionStatus.valueOf("SUCCESS"));
        assertEquals(TransactionStatus.FAILED, TransactionStatus.valueOf("FAILED"));
        assertEquals(TransactionStatus.REFUNDED, TransactionStatus.valueOf("REFUNDED"));
    }

    @Test
    void testDtoCoverage() {
        UUID id = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        TransactionDTO txDto = new TransactionDTO(id, orderId, userId, BigDecimal.valueOf(50), "SUCCESS", now);
        assertEquals(id, txDto.id());
        assertEquals(orderId, txDto.orderId());
        assertEquals(userId, txDto.userId());
        assertEquals(BigDecimal.valueOf(50), txDto.amount());
        assertEquals("SUCCESS", txDto.status());
        assertEquals(now, txDto.createdAt());

        PaymentRequestDTO reqDto = new PaymentRequestDTO(orderId, userId, BigDecimal.TEN, "key");
        assertEquals(orderId, reqDto.orderId());
        assertEquals(userId, reqDto.userId());
        assertEquals(BigDecimal.TEN, reqDto.amount());
        assertEquals("key", reqDto.idempotencyKey());

        PaymentResponseDTO resDto = new PaymentResponseDTO(id, "SUCCESS", "http://pay");
        assertEquals(id, resDto.transactionId());
        assertEquals("SUCCESS", resDto.status());
        assertEquals("http://pay", resDto.paymentUrl());

        PaymentSuccessEventDTO eventDto = new PaymentSuccessEventDTO(orderId, "user@milhub.ua", BigDecimal.TEN);
        assertEquals(orderId, eventDto.orderId());
        assertEquals("user@milhub.ua", eventDto.userEmail());
        assertEquals(BigDecimal.TEN, eventDto.amount());

        ErrorResponse err = new ErrorResponse(now, 400, "Bad Request", "Error", "/path", null);
        assertEquals(400, err.status());
        assertEquals("Bad Request", err.error());
        assertEquals("Error", err.message());
    }

    @Test
    void testExceptions() {
        AppException appEx = new AppException("App", HttpStatus.BAD_REQUEST);
        assertEquals(HttpStatus.BAD_REQUEST, appEx.getStatus());
        assertEquals("App", appEx.getMessage());

        BusinessException busEx = new BusinessException("Bus");
        assertEquals(HttpStatus.CONFLICT, busEx.getStatus());
        assertEquals("Bus", busEx.getMessage());

        ResourceNotFoundException nfEx = new ResourceNotFoundException("Not found");
        assertEquals(HttpStatus.NOT_FOUND, nfEx.getStatus());
        assertEquals("Not found", nfEx.getMessage());
    }

    @Test
    void testApplicationInstantiation() {
        PaymentServiceApplication app = new PaymentServiceApplication();
        assertNotNull(app);
    }
}
