package com.crafthub.order_service.repository;

import com.crafthub.order_service.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing {@link OrderItem} entities.
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}