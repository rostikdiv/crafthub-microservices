package com.crafthub.order_service.repository;

import com.crafthub.order_service.entity.Order;
import com.crafthub.order_service.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Page<Order> findAllByUserId(UUID userId, Pageable pageable);

    boolean existsByUserIdAndItemsProductIdAndStatusIn(UUID userId, UUID productId, Collection<OrderStatus> statuses);

    boolean existsByUserIdAndSellerIdAndStatusIn(UUID userId, UUID sellerId, Collection<OrderStatus> statuses);

    Page<Order> findAllBySellerId(UUID sellerId, Pageable pageable);
}