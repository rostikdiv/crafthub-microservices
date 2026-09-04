package com.milhub.payment_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.milhub.payment_service.dto.TransactionDTO;
import com.milhub.payment_service.dto.payment.PaymentRequestDTO;
import com.milhub.payment_service.dto.payment.PaymentResponseDTO;
import com.milhub.payment_service.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentController Unit Tests")
class PaymentControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private UUID transactionId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController).build();
        objectMapper = new ObjectMapper();
        transactionId = UUID.randomUUID();
        orderId = UUID.randomUUID();
    }

    @Test
    void testInitPayment() throws Exception {
        PaymentRequestDTO req = new PaymentRequestDTO(orderId, UUID.randomUUID(), BigDecimal.valueOf(100), "idem-key-1");
        PaymentResponseDTO res = new PaymentResponseDTO(transactionId, "PENDING", "http://mock-url");

        when(paymentService.initPayment(any(PaymentRequestDTO.class))).thenReturn(res);

        mockMvc.perform(post("/api/v1/payments/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(transactionId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void testGetTransactionByOrderId() throws Exception {
        PaymentResponseDTO res = new PaymentResponseDTO(transactionId, "SUCCESS", "http://mock-url");

        when(paymentService.getTransactionByOrderId(orderId)).thenReturn(res);

        mockMvc.perform(get("/api/v1/payments/order/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(transactionId.toString()))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void testMockWebhook() throws Exception {
        doNothing().when(paymentService).processWebhook(eq(transactionId), eq("SUCCESS"));

        mockMvc.perform(post("/api/v1/payments/webhook/{transactionId}", transactionId)
                        .param("status", "SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(content().string("Processed status: SUCCESS"));

        verify(paymentService, times(1)).processWebhook(transactionId, "SUCCESS");
    }

    @Test
    void testRefundPayment() throws Exception {
        doNothing().when(paymentService).refundPayment(eq(orderId), eq(BigDecimal.valueOf(50)));

        mockMvc.perform(post("/api/v1/payments/refund")
                        .param("orderId", orderId.toString())
                        .param("amount", "50"))
                .andExpect(status().isOk())
                .andExpect(content().string("Refund processed"));

        verify(paymentService, times(1)).refundPayment(orderId, BigDecimal.valueOf(50));
    }

    @Test
    void testGetAllTransactions() throws Exception {
        TransactionDTO dto = new TransactionDTO(transactionId, orderId, UUID.randomUUID(), BigDecimal.valueOf(100), "SUCCESS", LocalDateTime.now());
        when(paymentService.getAllTransactions()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(transactionId.toString()))
                .andExpect(jsonPath("$[0].status").value("SUCCESS"));
    }
}
