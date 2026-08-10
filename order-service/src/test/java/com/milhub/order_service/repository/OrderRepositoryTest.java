package com.milhub.order_service.repository;

import com.milhub.order_service.entity.Order;
import com.milhub.order_service.entity.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Disabled;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto=create-drop"})
@Disabled("Fails without Docker / PostgreSQL due to JSONB type mismatch in H2")
class OrderRepositoryTest {
// Testcontainers removed for local execution without Docker

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void testFindAllByUserId_WithPaginationAndEntityGraph() {
        // Arrange
        UUID userId = UUID.randomUUID();
        Order order = new Order();
        order.setUserId(userId);
        order.setStatus(OrderStatus.CREATED);
        order.setTotalPrice(BigDecimal.TEN);
        order.setSellerId(UUID.randomUUID());
        orderRepository.save(order);

        // Act
        Page<Order> result = orderRepository.findAllByUserId(userId, PageRequest.of(0, 10));

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUserId()).isEqualTo(userId);
    }
}
