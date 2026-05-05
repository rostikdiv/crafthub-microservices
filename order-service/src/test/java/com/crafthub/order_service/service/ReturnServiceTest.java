package com.crafthub.order_service.service;

import com.crafthub.order_service.dto.delivery.DeliveryDetailsDTO;
import com.crafthub.order_service.dto.delivery.ReturnShipmentRequestDTO;
import com.crafthub.order_service.dto.delivery.ReturnShipmentResponseDTO;
import com.crafthub.order_service.dto.event.RefundApprovedEventDTO;
import com.crafthub.order_service.dto.order.ReturnRequestDTO;
import com.crafthub.order_service.dto.order.ReturnResponseDTO;
import com.crafthub.order_service.entity.Order;
import com.crafthub.order_service.entity.OrderItem;
import com.crafthub.order_service.entity.OrderReturn;
import com.crafthub.order_service.entity.OrderStatus;
import com.crafthub.order_service.entity.enums.ReturnReason;
import com.crafthub.order_service.entity.enums.ReturnStatus;
import com.crafthub.order_service.exception.BusinessException;
import com.crafthub.order_service.exception.ResourceNotFoundException;
import com.crafthub.order_service.repository.OrderRepository;
import com.crafthub.order_service.repository.OrderReturnRepository;
import com.crafthub.order_service.security.UserContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReturnServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderReturnRepository orderReturnRepository;
    @Mock
    private DeliveryServiceIntegration deliveryService;
    @Mock
    private UserContextService userContextService;
    @Mock
    private PaymentIntegrationService paymentIntegrationService;
    @Mock
    private KafkaPublisherService kafkaPublisherService;

    @InjectMocks
    private ReturnService returnService;

    @Captor
    private ArgumentCaptor<OrderReturn> orderReturnCaptor;
    @Captor
    private ArgumentCaptor<Order> orderCaptor;
    @Captor
    private ArgumentCaptor<RefundApprovedEventDTO> refundEventCaptor;

    private UUID userId;
    private UUID orderId;
    private Order order;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setProductId(UUID.randomUUID());
        orderItem.setQuantity(2);
        orderItem.setPricePerUnit(BigDecimal.valueOf(100)); // Total 200

        order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setStatus(OrderStatus.DELIVERED);
        order.setUpdatedAt(LocalDateTime.now().minusDays(5)); // Inside 14 days
        order.setItems(List.of(orderItem));
        orderItem.setOrder(order);
        
        org.springframework.test.util.ReflectionTestUtils.setField(returnService, "kafkaPublisherService", kafkaPublisherService);
    }

    private void mockUserContext() {
        lenient().when(userContextService.getUserId()).thenReturn(userId);
    }

    // --- REQUEST RETURN TESTS ---

    @Test
    void requestReturn_Success_CustomerPaysShipping() {
        mockUserContext();
        DeliveryDetailsDTO returnAddress = DeliveryDetailsDTO.builder().cityRef("City").build();
        ReturnRequestDTO request = new ReturnRequestDTO("1", 2, "CHANGED_MIND", returnAddress);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        
        ReturnShipmentResponseDTO shipmentResponse = new ReturnShipmentResponseDTO(UUID.randomUUID(), "track456", BigDecimal.valueOf(50));
        when(deliveryService.createReturnShipment(any(ReturnShipmentRequestDTO.class))).thenReturn(shipmentResponse);

        ReturnResponseDTO response = returnService.requestReturn(orderId, request);

        assertThat(response).isNotNull();
        // Item total (200) - shipping (50) = 150
        assertThat(response.finalRefundAmount()).isEqualTo(BigDecimal.valueOf(150));
        assertThat(response.status()).isEqualTo("PENDING");

        verify(orderReturnRepository).save(orderReturnCaptor.capture());
        OrderReturn savedReturn = orderReturnCaptor.getValue();
        assertThat(savedReturn.getReason()).isEqualTo(ReturnReason.CHANGED_MIND);
        assertThat(savedReturn.isShippingDeducted()).isTrue();

        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.RETURN_REQUESTED);
    }

    @Test
    void requestReturn_Success_FullRefund() {
        mockUserContext();
        DeliveryDetailsDTO returnAddress = DeliveryDetailsDTO.builder().cityRef("City").build();
        ReturnRequestDTO request = new ReturnRequestDTO("1", 1, "DEFECTIVE", returnAddress); // Only returning 1 item

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        
        ReturnShipmentResponseDTO shipmentResponse = new ReturnShipmentResponseDTO(UUID.randomUUID(), "track456", BigDecimal.valueOf(50));
        when(deliveryService.createReturnShipment(any(ReturnShipmentRequestDTO.class))).thenReturn(shipmentResponse);

        ReturnResponseDTO response = returnService.requestReturn(orderId, request);

        assertThat(response).isNotNull();
        // 1 item * 100 = 100. Seller pays shipping -> refund is 100
        assertThat(response.finalRefundAmount()).isEqualTo(BigDecimal.valueOf(100));

        verify(orderReturnRepository).save(orderReturnCaptor.capture());
        OrderReturn savedReturn = orderReturnCaptor.getValue();
        assertThat(savedReturn.getReason()).isEqualTo(ReturnReason.DEFECTIVE);
        assertThat(savedReturn.isShippingDeducted()).isFalse();
    }

    @Test
    void requestReturn_NotOwner_ThrowsException() {
        lenient().when(userContextService.getUserId()).thenReturn(UUID.randomUUID()); // Different user
        DeliveryDetailsDTO returnAddress = DeliveryDetailsDTO.builder().cityRef("City").build();
        ReturnRequestDTO request = new ReturnRequestDTO("1", 1, "CHANGED_MIND", returnAddress);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> returnService.requestReturn(orderId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("You can only return items from your own orders");
    }

    @Test
    void requestReturn_NotDelivered_ThrowsException() {
        mockUserContext();
        order.setStatus(OrderStatus.SHIPPED); // Not delivered
        DeliveryDetailsDTO returnAddress = DeliveryDetailsDTO.builder().cityRef("City").build();
        ReturnRequestDTO request = new ReturnRequestDTO("1", 1, "CHANGED_MIND", returnAddress);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> returnService.requestReturn(orderId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Only DELIVERED orders can be returned");
    }

    @Test
    void requestReturn_ExpiredPeriod_ThrowsException() {
        mockUserContext();
        order.setUpdatedAt(LocalDateTime.now().minusDays(15)); // Passed 14 days
        DeliveryDetailsDTO returnAddress = DeliveryDetailsDTO.builder().cityRef("City").build();
        ReturnRequestDTO request = new ReturnRequestDTO("1", 1, "CHANGED_MIND", returnAddress);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> returnService.requestReturn(orderId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Return period (14 days) has expired");
    }

    @Test
    void requestReturn_QuantityExceeded_ThrowsException() {
        mockUserContext();
        DeliveryDetailsDTO returnAddress = DeliveryDetailsDTO.builder().cityRef("City").build();
        ReturnRequestDTO request = new ReturnRequestDTO("1", 3, "CHANGED_MIND", returnAddress); // Requesting 3, but bought 2

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> returnService.requestReturn(orderId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot return more items than purchased");
    }

    // --- APPROVE RETURN TESTS ---

    @Test
    void approveReturn_Success_Refunded() {
        UUID returnId = UUID.randomUUID();
        OrderReturn orderReturn = new OrderReturn();
        orderReturn.setId(returnId);
        orderReturn.setStatus(ReturnStatus.PENDING);
        orderReturn.setOrder(order);
        orderReturn.setProductId(orderItem.getProductId());
        orderReturn.setQuantity(2);
        orderReturn.setReason(ReturnReason.CHANGED_MIND);
        orderReturn.setFinalRefundAmount(BigDecimal.valueOf(150));

        when(orderReturnRepository.findById(returnId)).thenReturn(Optional.of(orderReturn));

        ReturnResponseDTO response = returnService.approveReturn(returnId, true);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("REFUNDED");

        verify(paymentIntegrationService).refundPayment(orderId, BigDecimal.valueOf(150));
        
        verify(kafkaPublisherService).sendRefundApprovedEvent(refundEventCaptor.capture());
        assertThat(refundEventCaptor.getValue().orderId()).isEqualTo(orderId);

        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.REFUNDED);
    }

    @Test
    void approveReturn_Rejected() {
        UUID returnId = UUID.randomUUID();
        OrderReturn orderReturn = new OrderReturn();
        orderReturn.setId(returnId);
        orderReturn.setStatus(ReturnStatus.PENDING);
        orderReturn.setOrder(order);

        when(orderReturnRepository.findById(returnId)).thenReturn(Optional.of(orderReturn));

        ReturnResponseDTO response = returnService.approveReturn(returnId, false);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("REJECTED");

        verify(paymentIntegrationService, never()).refundPayment(any(), any());
        
        verify(orderRepository).save(orderCaptor.capture());
        // Should revert to DELIVERED
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void approveReturn_AlreadyProcessed_ThrowsException() {
        UUID returnId = UUID.randomUUID();
        OrderReturn orderReturn = new OrderReturn();
        orderReturn.setId(returnId);
        orderReturn.setStatus(ReturnStatus.REFUNDED); // Already processed

        when(orderReturnRepository.findById(returnId)).thenReturn(Optional.of(orderReturn));

        assertThatThrownBy(() -> returnService.approveReturn(returnId, true))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Return request is already processed");
    }
}
