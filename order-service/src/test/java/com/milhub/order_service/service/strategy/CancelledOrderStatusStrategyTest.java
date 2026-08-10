package com.milhub.order_service.service.strategy;

import com.milhub.order_service.entity.Order;
import com.milhub.order_service.entity.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CancelledOrderStatusStrategyTest {

    private final CancelledOrderStatusStrategy strategy = new CancelledOrderStatusStrategy();

    @Test
    void supports_ShouldReturnTrueForCancelledStatus() {
        assertThat(strategy.supports(OrderStatus.CANCELLED)).isTrue();
    }

    @Test
    void supports_ShouldReturnFalseForOtherStatuses() {
        assertThat(strategy.supports(OrderStatus.CREATED)).isFalse();
        assertThat(strategy.supports(OrderStatus.DELIVERED)).isFalse();
    }

    @Test
    void applyStatusChange_ShouldUpdateOrderStatus() {
        // Arrange
        Order order = new Order();
        order.setStatus(OrderStatus.CREATED);

        // Act
        strategy.applyStatusChange(order, OrderStatus.CANCELLED);

        // Assert
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
}
