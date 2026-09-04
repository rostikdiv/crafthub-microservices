package com.milhub.order_service;

import com.milhub.order_service.config.AppConfig;
import com.milhub.order_service.config.KafkaProducerConfig;
import com.milhub.order_service.config.KafkaTopicConfig;
import com.milhub.order_service.dto.ErrorResponse;
import com.milhub.order_service.dto.delivery.DeliveryDetailsDTO;
import com.milhub.order_service.dto.delivery.ReturnShipmentRequestDTO;
import com.milhub.order_service.dto.delivery.ReturnShipmentResponseDTO;
import com.milhub.order_service.dto.event.DeliveryStatusChangedEvent;
import com.milhub.order_service.dto.event.OrderCreatedEvent;
import com.milhub.order_service.dto.event.OrderPlacedEventDTO;
import com.milhub.order_service.dto.event.PaymentSuccessEventDTO;
import com.milhub.order_service.dto.event.RefundApprovedEventDTO;
import com.milhub.order_service.dto.order.OrderItemResponseDTO;
import com.milhub.order_service.dto.order.OrderResponseDTO;
import com.milhub.order_service.dto.payment.PaymentRequestDTO;
import com.milhub.order_service.dto.payment.PaymentResponseDTO;
import com.milhub.order_service.dto.seller.SellerPublicProfileDTO;
import com.milhub.order_service.entity.Order;
import com.milhub.order_service.entity.OrderItem;
import com.milhub.order_service.entity.OrderReturn;
import com.milhub.order_service.entity.OrderStatus;
import com.milhub.order_service.entity.OutboxEvent;
import com.milhub.order_service.entity.enums.DeliveryProvider;
import com.milhub.order_service.entity.enums.DeliveryType;
import com.milhub.order_service.entity.enums.PaymentMethod;
import com.milhub.order_service.entity.enums.ReturnReason;
import com.milhub.order_service.entity.enums.ReturnStatus;
import com.milhub.order_service.exception.AccessDeniedException;
import com.milhub.order_service.exception.AppException;
import com.milhub.order_service.exception.BusinessException;
import com.milhub.order_service.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

class OrderConfigAndEntityBranchTest {

    @Test
    @DisplayName("Test OrderServiceApplication main method")
    void testApplicationMain() {
        try (MockedStatic<SpringApplication> springApp = mockStatic(SpringApplication.class)) {
            springApp.when(() -> SpringApplication.run(eq(OrderServiceApplication.class), any(String[].class)))
                    .thenReturn(null);

            assertDoesNotThrow(() -> OrderServiceApplication.main(new String[]{}));
        }
    }

    @Test
    @DisplayName("Test AppConfig ObjectMapper bean")
    void testAppConfig() {
        AppConfig appConfig = new AppConfig();
        assertNotNull(appConfig.objectMapper());
    }

    @Test
    @DisplayName("Test KafkaProducerConfig and KafkaTopicConfig beans")
    void testKafkaConfigs() {
        KafkaProducerConfig producerConfig = new KafkaProducerConfig();
        ReflectionTestUtils.setField(producerConfig, "bootstrapServers", "localhost:9092");
        ProducerFactory<String, Object> factory = producerConfig.producerFactory();
        assertNotNull(factory);
        KafkaTemplate<String, Object> template = producerConfig.kafkaTemplate();
        assertNotNull(template);

        KafkaTopicConfig topicConfig = new KafkaTopicConfig();
        assertNotNull(topicConfig.orderPlacedTopic());
        assertNotNull(topicConfig.returnEventsTopic());
        assertNotNull(topicConfig.orderFailedEventsTopic());
    }

    @Test
    @DisplayName("Test Exception classes hierarchy and status codes")
    void testExceptions() {
        AppException appEx = new AppException("App error", HttpStatus.BAD_REQUEST);
        assertEquals("App error", appEx.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, appEx.getStatus());

        BusinessException bizEx = new BusinessException("Business fault");
        assertEquals("Business fault", bizEx.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, bizEx.getStatus());

        ResourceNotFoundException notFoundEx = new ResourceNotFoundException("Not found");
        assertEquals("Not found", notFoundEx.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, notFoundEx.getStatus());

        AccessDeniedException accessEx = new AccessDeniedException("Forbidden");
        assertEquals("Forbidden", accessEx.getMessage());
    }

    @Test
    @DisplayName("Test Entity classes getters, setters, builders, equals, and hashCode")
    void testEntities() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        DeliveryDetailsDTO details = DeliveryDetailsDTO.builder()
                .provider(DeliveryProvider.NOVA_POSHTA)
                .type(DeliveryType.BRANCH)
                .cityRef("city")
                .branchRef("1")
                .build();

        Order order = Order.builder()
                .id(orderId)
                .userId(userId)
                .sellerId(sellerId)
                .status(OrderStatus.CREATED)
                .paymentMethod(PaymentMethod.CARD)
                .totalPrice(BigDecimal.valueOf(100))
                .deliveryInfo(details)
                .returnReason("Defect")
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertEquals(orderId, order.getId());
        assertEquals(userId, order.getUserId());
        assertEquals(sellerId, order.getSellerId());
        assertEquals(OrderStatus.CREATED, order.getStatus());
        assertEquals(PaymentMethod.CARD, order.getPaymentMethod());
        assertEquals(BigDecimal.valueOf(100), order.getTotalPrice());
        assertEquals(details, order.getDeliveryInfo());
        assertEquals("Defect", order.getReturnReason());
        assertEquals(now, order.getCreatedAt());
        assertEquals(now, order.getUpdatedAt());

        // Order equals and hashCode and lifecycle
        Order orderSame = Order.builder().id(orderId).build();
        Order orderDiff = Order.builder().id(UUID.randomUUID()).build();
        Order orderNullId = Order.builder().id(null).build();
        assertTrue(order.equals(order));
        assertFalse(order.equals(null));
        assertFalse(order.equals("not an order"));
        assertTrue(order.equals(orderSame));
        assertFalse(order.equals(orderDiff));
        assertFalse(orderNullId.equals(orderSame));
        assertEquals(order.hashCode(), orderSame.hashCode());
        assertTrue(order.toString().contains("Order"));

        Order orderLifecycle = new Order();
        ReflectionTestUtils.invokeMethod(orderLifecycle, "onCreate");
        assertNotNull(orderLifecycle.getCreatedAt());
        assertEquals(OrderStatus.PENDING_PAYMENT, orderLifecycle.getStatus());
        ReflectionTestUtils.invokeMethod(orderLifecycle, "onUpdate");
        assertNotNull(orderLifecycle.getUpdatedAt());

        // OrderItem
        OrderItem item = OrderItem.builder()
                .id(1L)
                .order(order)
                .productId(UUID.randomUUID())
                .name("Tactical Boots")
                .quantity(2)
                .pricePerUnit(BigDecimal.valueOf(50))
                .build();

        assertEquals(1L, item.getId());
        assertEquals(order, item.getOrder());
        assertEquals("Tactical Boots", item.getName());
        assertEquals(2, item.getQuantity());
        assertEquals(BigDecimal.valueOf(50), item.getPricePerUnit());

        OrderItem itemSame = OrderItem.builder().id(1L).build();
        OrderItem itemDiff = OrderItem.builder().id(2L).build();
        OrderItem itemNullId = OrderItem.builder().id(null).build();
        assertTrue(item.equals(item));
        assertFalse(item.equals(null));
        assertFalse(item.equals("not an item"));
        assertTrue(item.equals(itemSame));
        assertFalse(item.equals(itemDiff));
        assertFalse(itemNullId.equals(itemSame));
        assertEquals(item.hashCode(), itemSame.hashCode());
        assertTrue(item.toString().contains("OrderItem"));

        // OrderReturn
        UUID returnId = UUID.randomUUID();
        OrderReturn orderReturn = OrderReturn.builder()
                .id(returnId)
                .order(order)
                .orderItemId(1L)
                .productId(UUID.randomUUID())
                .quantity(1)
                .reason(ReturnReason.DEFECTIVE)
                .status(ReturnStatus.PENDING)
                .itemPriceSnapshot(BigDecimal.valueOf(50))
                .finalRefundAmount(BigDecimal.valueOf(50))
                .returnShippingCost(BigDecimal.ZERO)
                .isShippingDeducted(false)
                .returnTrackingNumber("TRACK-001")
                .returnShipmentId(UUID.randomUUID())
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertEquals(returnId, orderReturn.getId());
        assertEquals(order, orderReturn.getOrder());
        assertEquals(1L, orderReturn.getOrderItemId());
        assertEquals(ReturnReason.DEFECTIVE, orderReturn.getReason());
        assertEquals(ReturnStatus.PENDING, orderReturn.getStatus());
        assertEquals("TRACK-001", orderReturn.getReturnTrackingNumber());
        assertNotNull(orderReturn.getReturnShipmentId());
        assertEquals(BigDecimal.valueOf(50), orderReturn.getItemPriceSnapshot());
        assertEquals(BigDecimal.valueOf(50), orderReturn.getFinalRefundAmount());
        assertEquals(BigDecimal.ZERO, orderReturn.getReturnShippingCost());
        assertFalse(orderReturn.isShippingDeducted());

        OrderReturn returnSame = OrderReturn.builder().id(returnId).build();
        OrderReturn returnDiff = OrderReturn.builder().id(UUID.randomUUID()).build();
        OrderReturn returnNullId = OrderReturn.builder().id(null).build();
        assertTrue(orderReturn.equals(orderReturn));
        assertFalse(orderReturn.equals(null));
        assertFalse(orderReturn.equals("string"));
        assertTrue(orderReturn.equals(returnSame));
        assertFalse(orderReturn.equals(returnDiff));
        assertFalse(returnNullId.equals(returnSame));
        assertEquals(orderReturn.hashCode(), returnSame.hashCode());
        assertTrue(orderReturn.toString().contains("OrderReturn"));

        OrderReturn returnLifecycle = new OrderReturn();
        ReflectionTestUtils.invokeMethod(returnLifecycle, "onCreate");
        assertNotNull(returnLifecycle.getCreatedAt());
        assertEquals(ReturnStatus.PENDING, returnLifecycle.getStatus());
        ReflectionTestUtils.invokeMethod(returnLifecycle, "onUpdate");
        assertNotNull(returnLifecycle.getUpdatedAt());

        // OutboxEvent
        UUID outboxId = UUID.randomUUID();
        OutboxEvent outbox = OutboxEvent.builder()
                .id(outboxId)
                .aggregateId("agg-1")
                .eventType("TestEvent")
                .payload("{}")
                .status("PENDING")
                .createdAt(now)
                .build();

        assertEquals(outboxId, outbox.getId());
        assertEquals("agg-1", outbox.getAggregateId());
        assertEquals("TestEvent", outbox.getEventType());
        assertEquals("{}", outbox.getPayload());
        assertEquals("PENDING", outbox.getStatus());
        assertEquals(now, outbox.getCreatedAt());

        OutboxEvent outboxSame = OutboxEvent.builder().id(outboxId).build();
        OutboxEvent outboxDiff = OutboxEvent.builder().id(UUID.randomUUID()).build();
        OutboxEvent outboxNullId = OutboxEvent.builder().id(null).build();
        assertTrue(outbox.equals(outbox));
        assertFalse(outbox.equals(null));
        assertFalse(outbox.equals("not an outbox"));
        assertTrue(outbox.equals(outboxSame));
        assertFalse(outbox.equals(outboxDiff));
        assertFalse(outboxNullId.equals(outboxSame));
        assertEquals(outbox.hashCode(), outboxSame.hashCode());
        assertTrue(outbox.toString().contains("OutboxEvent"));
    }

    @Test
    @DisplayName("Test Enums and DTO Records")
    void testEnumsAndRecords() {
        LocalDateTime now = LocalDateTime.now();
        // Enums
        assertEquals(3, DeliveryProvider.values().length);
        assertEquals(3, DeliveryType.values().length);
        assertEquals(2, PaymentMethod.values().length);
        assertEquals(4, ReturnReason.values().length);
        assertEquals(4, ReturnStatus.values().length);
        assertEquals(16, OrderStatus.values().length);

        // DTOs
        UUID uuid = UUID.randomUUID();
        DeliveryStatusChangedEvent dsEvent = new DeliveryStatusChangedEvent(uuid, "DELIVERED", now);
        assertEquals(uuid, dsEvent.orderId());
        assertEquals("DELIVERED", dsEvent.status());
        assertEquals(now, dsEvent.timestamp());

        OrderCreatedEvent ocEvent = new OrderCreatedEvent("ORD-123", "user@test.com", BigDecimal.TEN);
        assertEquals("ORD-123", ocEvent.orderNumber());
        assertEquals("user@test.com", ocEvent.userEmail());
        assertEquals(BigDecimal.TEN, ocEvent.totalPrice());

        PaymentSuccessEventDTO psEvent = new PaymentSuccessEventDTO(uuid, "e@m.com", BigDecimal.ONE);
        assertEquals(uuid, psEvent.orderId());
        assertEquals("e@m.com", psEvent.userEmail());
        assertEquals(BigDecimal.ONE, psEvent.amount());

        OrderPlacedEventDTO opEvent = new OrderPlacedEventDTO(uuid, uuid, "e@m.com", BigDecimal.TEN, "item", List.of(uuid));
        assertEquals(uuid, opEvent.orderId());

        RefundApprovedEventDTO raEvent = new RefundApprovedEventDTO(uuid, uuid, 1, "fault");
        assertEquals("fault", raEvent.reason());

        DeliveryDetailsDTO details = DeliveryDetailsDTO.builder().cityRef("Kyiv").build();
        ReturnShipmentRequestDTO retShipReq = new ReturnShipmentRequestDTO(uuid, details, 2.5);
        assertEquals(2.5, retShipReq.weight());
        assertEquals(details, retShipReq.returnAddress());

        ReturnShipmentResponseDTO retShipRes = new ReturnShipmentResponseDTO(uuid, "TRK-001", BigDecimal.TEN);
        assertEquals(uuid, retShipRes.shipmentId());
        assertEquals("TRK-001", retShipRes.trackingNumber());
        assertEquals(BigDecimal.TEN, retShipRes.shippingCost());

        SellerPublicProfileDTO sellerDTO = new SellerPublicProfileDTO(uuid, "Seller Name", true);
        assertEquals("Seller Name", sellerDTO.companyName());
        assertTrue(sellerDTO.isVerified());

        PaymentRequestDTO payReq = new PaymentRequestDTO(uuid, uuid, BigDecimal.TEN);
        assertEquals(BigDecimal.TEN, payReq.amount());

        PaymentResponseDTO payRes = new PaymentResponseDTO(uuid, "PAID", "http://pay");
        assertEquals("PAID", payRes.status());

        OrderItemResponseDTO itemRes = new OrderItemResponseDTO(uuid, "Vest", 1, BigDecimal.TEN);
        assertEquals("Vest", itemRes.name());

        ErrorResponse err = new ErrorResponse(now, 400, "Bad", "Msg", "/path", Map.of());
        assertEquals(400, err.status());
        assertEquals(Map.of(), err.errors());
    }
}
