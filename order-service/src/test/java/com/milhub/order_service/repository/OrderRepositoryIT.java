package com.milhub.order_service.repository;

import com.milhub.order_service.dto.delivery.DeliveryDetailsDTO;
import com.milhub.order_service.entity.Order;
import com.milhub.order_service.entity.OrderItem;
import com.milhub.order_service.entity.OrderStatus;
import com.milhub.order_service.entity.enums.DeliveryProvider;
import com.milhub.order_service.entity.enums.DeliveryType;
import com.milhub.order_service.entity.enums.PaymentMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true"
})
class OrderRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private OrderRepository orderRepository;

    @Test
    @DisplayName("Should save and retrieve order with JSONB deliveryInfo in real PostgreSQL")
    void testSaveAndFindOrderWithJsonb() {
        UUID userId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();

        DeliveryDetailsDTO deliveryDetails = DeliveryDetailsDTO.builder()
                .provider(DeliveryProvider.NOVA_POSHTA)
                .type(DeliveryType.BRANCH)
                .cityName("Kyiv")
                .branchName("Warehouse 15")
                .recipientName("Ivan Petrenko")
                .recipientPhone("+380501234567")
                .build();

        Order order = Order.builder()
                .userId(userId)
                .sellerId(sellerId)
                .totalPrice(new BigDecimal("1250.00"))
                .status(OrderStatus.CREATED)
                .paymentMethod(PaymentMethod.CARD)
                .deliveryInfo(deliveryDetails)
                .build();

        Order saved = orderRepository.save(order);
        assertThat(saved.getId()).isNotNull();

        Optional<Order> fetchedOpt = orderRepository.findById(saved.getId());
        assertThat(fetchedOpt).isPresent();
        Order fetched = fetchedOpt.get();
        assertThat(fetched.getDeliveryInfo()).isNotNull();
        assertThat(fetched.getDeliveryInfo().recipientPhone()).isEqualTo("+380501234567");
        assertThat(fetched.getDeliveryInfo().provider()).isEqualTo(DeliveryProvider.NOVA_POSHTA);
        assertThat(fetched.getDeliveryInfo().cityName()).isEqualTo("Kyiv");
    }

    @Test
    @DisplayName("Should find orders by user with pagination")
    void testFindAllByUserId_WithPagination() {
        UUID userId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();

        Order order1 = Order.builder()
                .userId(userId)
                .sellerId(sellerId)
                .totalPrice(new BigDecimal("500.00"))
                .status(OrderStatus.CONFIRMED)
                .build();

        Order order2 = Order.builder()
                .userId(userId)
                .sellerId(sellerId)
                .totalPrice(new BigDecimal("750.00"))
                .status(OrderStatus.PAID)
                .build();

        orderRepository.save(order1);
        orderRepository.save(order2);

        Page<Order> page = orderRepository.findAllByUserId(userId, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(Order::getUserId).containsOnly(userId);
    }

    @Test
    @DisplayName("Should verify user purchased product with status filter")
    void testExistsByUserIdAndItemsProductIdAndStatusIn() {
        UUID userId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Order order = Order.builder()
                .userId(userId)
                .sellerId(sellerId)
                .totalPrice(new BigDecimal("300.00"))
                .status(OrderStatus.DELIVERED)
                .build();

        OrderItem item = OrderItem.builder()
                .order(order)
                .productId(productId)
                .name("Tactical Boots")
                .quantity(1)
                .pricePerUnit(new BigDecimal("300.00"))
                .build();

        order.getItems().add(item);
        orderRepository.save(order);

        boolean exists = orderRepository.existsByUserIdAndItemsProductIdAndStatusIn(
                userId, productId, List.of(OrderStatus.DELIVERED, OrderStatus.CONFIRMED)
        );
        assertThat(exists).isTrue();

        boolean notExists = orderRepository.existsByUserIdAndItemsProductIdAndStatusIn(
                userId, UUID.randomUUID(), List.of(OrderStatus.DELIVERED)
        );
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should verify user bought from seller with status filter")
    void testExistsByUserIdAndSellerIdAndStatusIn() {
        UUID userId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();

        Order order = Order.builder()
                .userId(userId)
                .sellerId(sellerId)
                .totalPrice(new BigDecimal("990.00"))
                .status(OrderStatus.SHIPPED)
                .build();

        orderRepository.save(order);

        boolean exists = orderRepository.existsByUserIdAndSellerIdAndStatusIn(
                userId, sellerId, List.of(OrderStatus.SHIPPED)
        );
        assertThat(exists).isTrue();

        boolean notExists = orderRepository.existsByUserIdAndSellerIdAndStatusIn(
                userId, UUID.randomUUID(), List.of(OrderStatus.SHIPPED)
        );
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should find orders by seller and status with pagination")
    void testFindAllBySellerIdAndStatus() {
        UUID sellerId = UUID.randomUUID();

        Order order1 = Order.builder()
                .userId(UUID.randomUUID())
                .sellerId(sellerId)
                .totalPrice(new BigDecimal("450.00"))
                .status(OrderStatus.DELIVERED)
                .build();

        Order order2 = Order.builder()
                .userId(UUID.randomUUID())
                .sellerId(sellerId)
                .totalPrice(new BigDecimal("600.00"))
                .status(OrderStatus.CONFIRMED)
                .build();

        orderRepository.save(order1);
        orderRepository.save(order2);

        Page<Order> deliveredPage = orderRepository.findAllBySellerIdAndStatus(
                sellerId, OrderStatus.DELIVERED, PageRequest.of(0, 10)
        );
        assertThat(deliveredPage.getTotalElements()).isEqualTo(1);
        assertThat(deliveredPage.getContent().get(0).getStatus()).isEqualTo(OrderStatus.DELIVERED);

        Page<Order> allSellerOrders = orderRepository.findAllBySellerId(
                sellerId, PageRequest.of(0, 10)
        );
        assertThat(allSellerOrders.getTotalElements()).isEqualTo(2);
    }
}
